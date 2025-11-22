# Audio Analyzer - Architecture Documentation

## Overview

This document describes the refactored architecture that implements a clean separation of concerns between the GUI (Swing components) and the audio capture/data model.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    AudioAnalyseFrame (GUI)                   │
│  - Creates AudioCaptureService                               │
│  - Injects service into UI panels                           │
│  - Handles Start/Stop menu actions                          │
└────────────┬────────────────────────────────────────────────┘
             │ creates & injects
             ▼
┌─────────────────────────────────────────────────────────────┐
│              AudioCaptureService (Interface)                 │
│  + start()                                                   │
│  + stop()                                                    │
│  + isRunning()                                              │
│  + getLatestModel() : WaveformModel                         │
│  + setDivisor(int)                                          │
│  + recomputeLayout(int width, int height)                   │
└────────────┬────────────────────────────────────────────────┘
             │ implemented by
             ▼
┌─────────────────────────────────────────────────────────────┐
│           AudioCaptureServiceImpl (Implementation)           │
│  - Manages worker thread                                     │
│  - Handles audio line (TargetDataLine)                      │
│  - Processes audio samples                                   │
│  - Creates thread-safe WaveformModel snapshots              │
│  - Thread-safe via locks and atomic variables               │
└────────────┬────────────────────────────────────────────────┘
             │ produces
             ▼
┌─────────────────────────────────────────────────────────────┐
│              WaveformModel (Immutable Data)                  │
│  - xPoints: int[]  (defensive copy)                         │
│  - yPoints: int[][] (defensive deep copy)                   │
│  - tickEveryNSample: int                                    │
│  - numberOfPoints: int                                      │
│  - Immutable and thread-safe                                │
└────────────┬────────────────────────────────────────────────┘
             │ consumed by
             ▼
┌─────────────────────────────────────────────────────────────┐
│        WaveformPanel & PhaseDiagramCanvas (UI)              │
│  - Receive service via setAudioCaptureService()             │
│  - Request model snapshots for painting                     │
│  - No direct access to mutable state                        │
└─────────────────────────────────────────────────────────────┘
```

## Key Components

### 1. AudioCaptureService (Interface)

- **Location**: `org.hammer.audio.AudioCaptureService`
- **Responsibility**: Defines the contract for audio capture operations
- **Key Methods**:
  - `start()`: Initialize and start audio capture
  - `stop()`: Stop capture and release resources
  - `getLatestModel()`: Get immutable snapshot of current waveform data
  - `setDivisor(int)`: Adjust buffer size for capture
  - `recomputeLayout(int, int)`: Update coordinates based on panel dimensions

### 2. AudioCaptureServiceImpl (Implementation)

- **Location**: `org.hammer.audio.AudioCaptureServiceImpl`
- **Responsibility**:
  - Audio device management (TargetDataLine)
  - Background thread for continuous audio capture
  - Sample processing and conversion to display coordinates
  - Thread-safe model generation
- **Thread Safety**:
  - Uses `ReentrantLock` for model data
  - Uses `AtomicBoolean` for running state
  - Worker thread managed internally
  - All public methods are thread-safe

### 3. WaveformModel (Immutable Data Model)

- **Location**: `org.hammer.audio.WaveformModel`
- **Responsibility**: Immutable snapshot of waveform data
- **Key Features**:
  - Defensive copies of all arrays
  - Thread-safe (immutable after construction)
  - Contains x/y coordinates for rendering
  - Includes metadata (tick intervals, point count, etc.)

### 4. AudioAnalyseFrame (Main GUI)

- **Location**: `org.hammer.AudioAnalyseFrame`
- **Responsibility**:
  - Application lifecycle management
  - Service creation and injection
  - Start/Stop menu handling
  - UI updates via Swing Timer
- **Refactoring Changes**:
  - No longer uses singleton pattern
  - Creates service in constructor
  - Injects service into panels
  - Delegates start/stop to service

### 5. WaveformPanel & PhaseDiagramCanvas (UI Components)

- **Locations**: `org.hammer.WaveformPanel`, `org.hammer.PhaseDiagramCanvas`
- **Responsibility**: Visualize audio waveform data
- **Refactoring Changes**:
  - Accept service via `setAudioCaptureService()`
  - Request model snapshots in `paintComponent()`
  - No initialization logic in constructors
  - No direct access to mutable arrays

## Design Principles Applied

### 1. Separation of Concerns

- **GUI Layer**: Only responsible for rendering and user interaction
- **Service Layer**: Handles all audio capture logic
- **Model Layer**: Pure data, no logic

### 2. Dependency Injection

- Services are injected into UI components
- No direct singleton access from UI code
- Easier to test and maintain

### 3. Thread Safety

- Immutable models prevent concurrent modification
- Defensive copies ensure data integrity
- Locks protect mutable state during updates
- Clear ownership of worker threads

### 4. Clean APIs

- Service interface defines clear contract
- Model provides read-only access
- No deprecated methods in new code

## Data Flow

### Startup Sequence

```
1. AudioAnalyseFrame constructor
   ↓
2. Create AudioCaptureServiceImpl(16000Hz, 8bit, 2ch, ...)
   ↓
3. Inject service into WaveformPanel
   ↓
4. Inject service into PhaseDiagramPanel
   ↓
5. Service initializes but does NOT start capture yet
   ↓
6. GUI becomes visible, waiting for user to Start
```

### Audio Capture Flow (after user clicks Start)

```
1. User clicks Start/Stop menu
   ↓
2. AudioAnalyseFrame calls audioCaptureService.start()
   ↓
3. Service opens TargetDataLine
   ↓
4. Service starts background worker thread
   ↓
5. Worker thread continuously:
   a. Reads audio bytes from line
   b. Converts samples to pixel coordinates
   c. Creates temporary arrays
   d. Locks model and updates arrays atomically
   e. Releases lock
   ↓
6. UI panels request getLatestModel() periodically
   ↓
7. Service locks, creates defensive copies, returns WaveformModel
   ↓
8. UI panels render using immutable model
```

### Shutdown Sequence

```
1. User closes window or clicks Stop
   ↓
2. AudioAnalyseFrame calls audioCaptureService.stop()
   ↓
3. Service sets running flag to false
   ↓
4. Worker thread exits loop
   ↓
5. Service closes TargetDataLine
   ↓
6. Resources are released
```

## Thread Safety Guarantees

### AudioCaptureServiceImpl

- **Worker Thread**: Internal, not exposed
- **Model Data**: Protected by `ReentrantLock`
- **Running State**: `AtomicBoolean` for lock-free checks
- **Public Methods**: All synchronized where needed

### WaveformModel

- **Immutability**: All fields final
- **Defensive Copies**: Arrays copied on construction and access
- **Thread-Safe**: Can be shared between threads safely

### UI Components

- **Swing EDT**: All UI updates happen on Event Dispatch Thread
- **Timer**: Periodic repaints scheduled on EDT
- **Model Access**: Only reads immutable snapshots

## Migration Notes

### Removed Components

- **AudioInDataRunnable (Enum Singleton)**: Completely removed
  - Replaced by AudioCaptureServiceImpl
  - No more global mutable state
  - No more direct thread management in UI

### Deprecated Methods (Removed)

- `AudioInDataRunnable.INSTANCE.*` - All singleton access removed
- `init()`, `computedatasize()`, `recomputexvalues()` - Moved to service
- Direct array access (`xPoints()`, `yPoints()`) - Replaced by model snapshots

### Breaking Changes

- UI components must receive service via setter
- No implicit initialization in UI constructors
- Service must be started explicitly

## Benefits of New Architecture

1. **Testability**: Services can be mocked/stubbed for testing
2. **Thread Safety**: Clear ownership and immutable models
3. **Maintainability**: Concerns separated, easier to understand
4. **Flexibility**: Easy to swap implementations
5. **No Global State**: No singleton, better for testing and reuse
6. **Clear Lifecycle**: Explicit start/stop, resource management

## Future Enhancements

Potential improvements that are now easier with this architecture:

1. **Multiple Instances**: Could run multiple capture services
2. **Unit Testing**: Service logic can be tested without UI
3. **Mock Audio Input**: Easy to create test implementations
4. **Performance Monitoring**: Add instrumentation to service
5. **Configuration**: Inject different audio parameters
6. **Recording**: Add recording capability to service

## Code Organization

```
org.hammer/
├── AudioAnalyseFrame.java        (Main GUI)
├── WaveformPanel.java            (Waveform visualization)
├── PhaseDiagramCanvas.java       (Phase diagram visualization)
├── PhaseDiagramPanel.java        (Phase diagram container)
└── audio/
    ├── AudioCaptureService.java      (Service interface)
    ├── AudioCaptureServiceImpl.java  (Service implementation)
    └── WaveformModel.java            (Immutable data model)
```

## Logging

The service uses `java.util.logging` for diagnostics:

- **INFO**: Service lifecycle (start/stop)
- **FINE**: Detailed operations (buffer computations)
- **WARNING**: Non-fatal issues (line already running)
- **SEVERE**: Fatal errors (capture failures)

Configure logging in your application as needed:

```java
Logger.getLogger("org.hammer.audio").setLevel(Level.FINE);
```

## Summary

This refactoring achieves the goal of clean separation between GUI and audio capture. The architecture is now:
- More maintainable with clear responsibilities
- Thread-safe with immutable models
- Testable with dependency injection
- Flexible for future enhancements

All acceptance criteria from the original requirements have been met.
