# Audio Analyzer

Audio Analyzer is an Eclipse/OSGi application for analyzing audio input in real-time. This project has been converted from a simple Maven JAR project to a Tycho-based Eclipse/OSGi build.

## Project Structure

This is a multi-module Tycho project with the following structure:

- **audioin.bundle** - OSGi bundle containing the application code
- **audioin.feature** - Eclipse feature grouping the bundle
- **audioin.repository** - P2 update site/repository for distribution

## Prerequisites

- JDK 17 or later
- Apache Maven 3.6.0 or later

## Building

To build the entire project, run:

```bash
mvn -U -e -V clean verify
```

This will:
1. Compile all source code
2. Create the OSGi bundle
3. Package the Eclipse feature
4. Generate the P2 repository

Build artifacts will be located in:
- Bundle JAR: `audioin.bundle/target/audioin.bundle-0.0.1-SNAPSHOT.jar`
- Feature: `audioin.feature/target/audioin.feature-0.0.1-SNAPSHOT.jar`
- P2 Repository: `audioin.repository/target/repository/`

## Installation

### Using Eclipse IDE

1. Build the project using the command above
2. In Eclipse, go to **Help > Install New Software**
3. Click **Add** and then **Local**
4. Browse to `audioin.repository/target/repository`
5. Select the "Audio Analyzer" feature
6. Complete the installation wizard

### Standalone Usage

The bundle can also be used in any OSGi runtime (such as Apache Felix or Eclipse Equinox) by adding the generated JAR to the runtime.

## Running the Application

After installation in Eclipse:
1. The application can be launched as an Eclipse application
2. Look for the "audioin.bundle.application" in the Eclipse application list

Alternatively, you can run the main class directly from the bundle:
- Main class: `org.hammer.AudioAnalyseFrame`

## Technology Stack

- Java 17
- Eclipse Tycho 4.0.5
- OSGi/Eclipse Platform
- AWT/Swing for UI

## Development

The main application code is in the `audioin.bundle` module under `src/org/hammer/`:
- `AudioAnalyseFrame.java` - Main application window
- `AudioInDataRunnable.java` - Audio capture and processing
- `WaveformPanel.java` - Waveform visualization
- `PhaseDiagramPanel.java` - Phase diagram visualization
- `PhaseDiagramCanvas.java` - Phase diagram canvas
- `AudioApp.java` - Eclipse application entry point

## License

Copyright (c) Carsten Hammer

## Contact

Author: Carsten Hammer (carsten.hammer@t-online.de)