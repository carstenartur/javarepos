# Audio Platform — Architecture

## Overview

This project has been refactored from a single-purpose Swing waveform demo into a layered
real-time audio processing platform. The current application separates audio acquisition,
buffering, DSP processing, analysis, localization, snapshots and visualization. It includes
microphone capture, deterministic demo input, FFT spectrum analysis, stereo delay estimation,
CSV/PNG evidence export and extension points for future modules (spectrograms, noise diagnosis,
triggered oscilloscopes, recording/replay, plug-in DSP, alternative UIs, ...).

Stereo delay analysis estimates inter-channel delay and broad left/right direction from a stereo
pair. It does not provide full 3D localization or exact source coordinates.

## Layered architecture

```
┌──────────────────────────┐
│ audio source             │
│  microphone / demo input │
└────────────┬─────────────┘
             │ raw PCM bytes or generated AudioBlock
             ▼
┌──────────────────────────────────────────┐
│ capture / signal                         │
│  AudioCaptureService(Impl)               │
│    ├─ SampleDecoder  (bytes -> float[])  │
│    ├─ DemoAudioCaptureService            │
│    └─ SignalGenerator / demo presets     │
└────────────┬─────────────────────────────┘
             │ AudioBlock (immutable)
             ▼
┌──────────────────────────────────────────┐
│ buffer                                   │
│  AudioRingBuffer<AudioBlock>             │
│  (lock-free SPSC, bounded)               │
└────────────┬─────────────────────────────┘
             │ AudioBlock (consumer thread)
             ▼
┌──────────────────────────────────────────┐
│ dsp                                      │
│  DSPPipeline = [DSPProcessor, ...]       │
│  stateless, immutable                    │
└────────────┬─────────────────────────────┘
             │ AudioBlock
             ▼
┌──────────────────────────────────────────┐
│ analysis / localization                  │
│  AnalysisModule<S extends                │
│  AnalysisSnapshot>                       │
│    ├─ RmsPeakAnalyzer -> RmsPeakSnapshot │
│    ├─ SpectrumAnalyzer                   │
│    │  -> SpectrumSnapshot                │
│    └─ StereoDelayAnalyzer                │
│       -> StereoDelaySnapshot / Status    │
└────────────┬─────────────────────────────┘
             │ AnalysisSnapshot (immutable)
             ▼
┌──────────────────────────────────────────┐
│ snapshot                                 │
│  WaveformSnapshot                        │
│  PhaseScopeSnapshot                      │
│  (UI-friendly, audio-domain only)        │
└────────────┬─────────────────────────────┘
             │ snapshot (or block)
             ▼
┌──────────────────────────────────────────┐
│ ui                                       │
│  Swing panels and renderers              │
│  waveform, spectrum, phase, measurements │
│  export CSV / PNG                        │
└──────────────────────────────────────────┘
```

The key invariant: **everything below `ui` stays in normalized `float` audio space.** No
DSP/analysis/buffer/localization code knows about pixels, panel dimensions, Swing or JavaFX.

## Packages

|                 Package                  |                                       Responsibility                                       |
|------------------------------------------|--------------------------------------------------------------------------------------------|
| `org.hammer.audio.core`                  | Immutable audio-domain models: `AudioBlock`, `AudioFormatDescriptor`                       |
| `org.hammer.audio.acquisition`           | API-neutral synchronized multichannel source, microphone metadata and sample clock APIs    |
| `org.hammer.audio.geometry`              | Reusable 2D positions, rays and localization constraints                                   |
| `org.hammer.audio.capture`               | Sample decoding utilities (`SampleDecoder`)                                                |
| `org.hammer.audio.buffer`                | `AudioRingBuffer<T>` — bounded lock-free SPSC ring buffer                                  |
| `org.hammer.audio.dsp`                   | `DSPProcessor` extension point + `DSPPipeline` composition                                 |
| `org.hammer.audio.analysis`              | `AnalysisModule`, snapshots, `Fft`, `RmsPeakAnalyzer`, `SpectrumAnalyzer`, measurements    |
| `org.hammer.audio.localization`          | Stereo delay estimation: `StereoDelayAnalyzer`, `StereoDelaySnapshot`, `StereoDelayStatus` |
| `org.hammer.audio.signal`                | Deterministic generators, including `DemoPresetGenerator` demo scenarios                   |
| `org.hammer.audio.snapshot`              | UI-friendly immutable snapshots: `WaveformSnapshot`, `PhaseScopeSnapshot`                  |
| `org.hammer.audio.ui`                    | Render helpers and theme classes for pixel-aware UI code                                   |
| `org.hammer.audio`                       | Capture service API, JavaSound/demo capture implementations, legacy `WaveformModel`        |
| `org.hammer`                             | Swing application frame and panels                                                         |
| `org.hammer.audio.benchmark`             | JMH benchmarks (ring buffer, FFT, signal generators)                                       |
| `org.hammer.audio.experimental.acoustic` | Isolated research plugin for wingbeat tracking, TDOA, beamforming and simulation           |

## Key design choices

### 1. Immutable audio domain

`AudioFormatDescriptor` and `AudioBlock` are immutable, thread-safe and free of any UI or JavaSound
types. Samples are normalized `float[channels][frames]` in `[-1, 1]`. Each block carries a
monotonic `frameIndex` and a `timestampNanos` so any downstream consumer (analysis, recording,
replay) can correlate it back to the source.

### 2. Lock-free SPSC ring buffer

The audio capture thread is the sole producer; downstream DSP/analysis is the sole consumer.
`AudioRingBuffer` uses two `AtomicLong` sequences with `lazySet` semantics and a power-of-two mask,
avoiding locks on the hot path. The capacity is rounded up to the next power of two. Two write
strategies are exposed:

- `offer(T)` — fail fast if full (caller can decide what to do).
- `offerOverwrite(T)` — drop the oldest element if full (typical for "latest wins" UI feeds).

### 3. Composable DSP pipeline

`DSPProcessor` is a single-method functional interface (`AudioBlock -> AudioBlock`). Pipelines are
immutable lists of stages, threaded sequentially. Plug-in DSP modules implement `DSPProcessor` (or
`AnalysisModule` if they produce a snapshot rather than another block).

### 4. Pure-Java FFT and measurements

`Fft` is a dependency-free in-place radix-2 Cooley-Tukey FFT with cached twiddle and bit-reverse
tables. `SpectrumAnalyzer` produces immutable `SpectrumSnapshot` values for the Swing spectrum
panel and measurement/export paths. `MeasurementCalculator` combines block and spectrum data into
RMS, peak, clipping, dominant-frequency and spectrum-peak readouts.

### 5. Stereo delay estimation

`StereoDelayAnalyzer` computes normalized cross-correlation between the first two channels across
physically possible lags. It returns a `StereoDelaySnapshot` with delay in samples/milliseconds,
path-length difference, approximate angle, confidence and the correlation curve. `StereoDelayStatus`
classifies valid results and common rejection reasons: mono input, silence, low correlation or
physically impossible delay.

The angle is only a broad direction estimate from a two-microphone time-delay model. Reflections,
channel mismatch and microphone geometry can dominate the result; this is not full 3D localization.

### 6. Deterministic synthetic signals and demo presets

`SineGenerator`, `SquareGenerator` and `ChirpGenerator` produce repeatable `AudioBlock` streams with
no audio device. `DemoPresetGenerator` adds UI-oriented scenarios used by `DemoAudioCaptureService`:

- sine
- square
- chirp
- stereo delay test
- mosquito-like high-frequency burst
- moving chirp source
- 50 Hz hum + harmonics
- clipping test

These presets enable headless tests, repeatable demos and deterministic DSP/localization checks.

### 7. UI-only pixel scaling

`WaveformRenderer` is the single place that converts a `WaveformSnapshot` into pixel-space arrays
for a Swing canvas. Swing panels consume immutable audio-domain snapshots or `AudioBlock` data and
perform rendering/export at the application boundary.

## Capture lifecycle

```
start()
   │
   ├─ open TargetDataLine or start demo worker
   ├─ allocate decode/generation buffers
   ├─ spawn worker thread (daemon, single-thread executor)
   │
 capture loop (worker):
   │  read raw bytes or generate demo block
   │  -> SampleDecoder.decode if using microphone input
   │  -> AudioBlock (frameIndex, timestamp)
   │  -> ringBuffer.offer(block), dropping the new block if full
   │  -> latestBlock = block (volatile, for "latest" consumers)
   │  -> latestModel = WaveformRenderer(snapshot, panelWidth, panelHeight)
   │
stop()
   │
   ├─ flag running=false
   ├─ shutdownNow worker
   └─ close TargetDataLine when live input is active
```

The legacy `WaveformModel` is still produced by the worker so existing Swing panels keep working.
New consumers should prefer `getRingBuffer()` or `getLatestBlock()`.

## Extension points

|                Want to add                |                                   Implement                                   |
|-------------------------------------------|-------------------------------------------------------------------------------|
| New DSP stage (filter, gain, ...)         | `DSPProcessor`, plug into a `DSPPipeline`                                     |
| New analyzer (loudness, correlation, ...) | `AnalysisModule<MySnapshot>` where `MySnapshot` implements `AnalysisSnapshot` |
| New localization diagnostic               | Analyzer in `org.hammer.audio.localization` returning a snapshot              |
| New visualization                         | Concrete snapshot class or `AudioBlock` input plus a UI renderer/panel        |
| Alternative FFT backend                   | Replace `SpectrumAnalyzer`'s internal `Fft` with your own                     |
| Recording / replay                        | New writer/reader around `AudioBlock` and `SignalGenerator`                   |
| Headless demo / test                      | Use `SignalGenerator` or `DemoPresetGenerator`                                |

## Experimental acoustic localization plugin

The acoustic localization work is intentionally isolated under
`org.hammer.audio.experimental.acoustic`. Stable packages provide only reusable acquisition and
geometry abstractions; mosquito-specific frequency tracking, room simulation, GCC-PHAT/TDOA
experiments and beamforming stay in the plugin. Core code must not import `org.hammer.audio.experimental.*`.
`ArchitectureBoundaryTest` enforces that stable audio packages do not import experimental packages
and that DSP/acquisition/geometry do not depend on Swing/UI/app packages.

See [`docs/architecture/experimental-acoustic-localization.md`](docs/architecture/experimental-acoustic-localization.md)
and [`docs/plugins/acoustic-localization/README.md`](docs/plugins/acoustic-localization/README.md) for the
architecture review, coupling analysis, module-boundary rationale and current limitations.

## Concurrency model

- Capture worker thread (single, daemon) — sole producer for the ring buffer.
- DSP / analysis threads — single consumer per ring buffer (SPSC).
- UI threads — read `latestBlock` / `latestModel` via volatile pointers; never mutate.
- Snapshots are immutable — safe to pass between threads without synchronization.

## Build, test, benchmark

```
./mvnw verify           # spotless, build, unit tests, spotbugs, pmd, checkstyle
./mvnw test             # unit tests only
./mvnw -Pjmh package    # JMH benchmarks (org.hammer.audio.benchmark.*)
```

See [`docs/MIGRATION.md`](docs/MIGRATION.md) for migration notes from the previous
`WaveformModel`-centric architecture.
