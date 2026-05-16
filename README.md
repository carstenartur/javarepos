# Audio Analyzer

[![Java CI with Maven](https://github.com/carstenartur/javarepos/actions/workflows/maven.yml/badge.svg)](https://github.com/carstenartur/javarepos/actions/workflows/maven.yml)
[![codecov](https://codecov.io/gh/carstenartur/javarepos/graph/badge.svg)](https://codecov.io/gh/carstenartur/javarepos)
[![CodeQL](https://github.com/carstenartur/javarepos/actions/workflows/codeql.yml/badge.svg)](https://github.com/carstenartur/javarepos/actions/workflows/codeql.yml)

**Audio Analyzer** is a Maven multi-module real-time audio analysis laboratory built around a
Java/Swing measurement dashboard. It provides a layered architecture for audio acquisition,
ring-buffering, DSP, analysis (RMS/peak, FFT spectrum, stereo delay estimation) and visualization,
with deterministic synthetic signal generators and JMH benchmarks. The bundled Swing UI renders
live waveform, spectrum, phase and stereo-delay readouts for microphone input or reproducible demos.

> The root Maven parent is named `audioin-parent`; the runnable application artifact is
> `audio-app`.

## Features

- **Layered architecture** — capture → ring buffer → DSP pipeline → analysis → snapshots → UI.
  See [`ARCHITECTURE.md`](ARCHITECTURE.md).
- **Immutable audio domain** — `AudioBlock` and `AudioFormatDescriptor` carry normalized
  `float[channels][frames]` samples, frame indices and timestamps; no UI types.
- **Lock-free SPSC ring buffer** for realtime workloads, with `offer` and `offerOverwrite`.
- **DSP extension points** — implement `DSPProcessor` and chain stages with `DSPPipeline`.
- **Analysis modules** — `RmsPeakAnalyzer`, `SpectrumAnalyzer` (pure-Java radix-2 FFT),
  `StereoDelayAnalyzer`, `SpectrogramAnalyzer` and rule-based `DiagnosisAnalyzer` producing
  immutable snapshots for UI or other consumers.
- **Acquisition abstractions** — `Microphone`, `MicrophoneArray`, `MultiChannelAudioSource` and
  `SampleClock` in `audio-acquisition` provide UI-/JavaSound-free contracts that experimental
  multi-channel research code (e.g. acoustic localization) builds on.
- **Deterministic synthetic signals** — sine, square, chirp, hum/harmonics, clipping,
  stereo-delay, moving-chirp and mosquito-like high-frequency burst presets for tests, headless
  demos and DSP verification.
- **Live Swing UI** — selectable microphone input, waveform, phase diagram, FFT spectrum,
  spectrogram and diagnosis panels, demo mode, pause/freeze, peak-frequency + measurement
  readouts, stereo-delay / approximate direction estimate, and CSV/PNG / evidence-bundle export
  for quick acoustic diagnostics.
- **Experimental acoustic localization** (`audio-experimental-acoustic`) — isolated research
  plugin with wingbeat frequency tracking, GCC-PHAT / cross-correlation TDOA estimators,
  delay-and-sum beamforming and a deterministic 2D room simulator. Not a production tracker —
  see the [experimental docs](docs/plugins/acoustic-localization/README.md) for limitations.
- **Headless-friendly tests** — unit tests covering immutability, FFT correctness, SPSC
  concurrency stress, signal determinism, DSP pipeline composition and sample decoding.
- **JMH benchmarks** for ring buffer throughput, FFT throughput and signal-generator
  allocations.
- **Java 21**, no heavyweight frameworks.

## Quickstart

Requires **Java 21** or higher.

```bash
# Build, test, run static analysis and coverage
./mvnw clean verify

# Run the application (after package/verify, requires audio-app/target/lib runtime jars)
java -jar audio-app/target/audio-app-0.0.1-SNAPSHOT.jar
# Runtime dependencies are copied to audio-app/target/lib during the Maven package phase.

# Run JMH benchmarks
./mvnw -pl audio-dsp -Pjmh package
```

On Windows use `mvnw.cmd` instead of `./mvnw`.

## Real-time acoustic use cases

![Audio Analyzer modern dashboard screenshot](docs/images/screenshot.png)

_Staged demo: Demo mode is running a frozen 440 Hz sine signal, with the waveform, FFT peak,
dominant frequency, RMS/peak level and clipping status visible in one view._

### What you can see

- **Demo mode** selected with the **Sine** signal.
- A visible **waveform** for the generated signal.
- An **FFT spectrum** with a marked peak at **440.0 Hz**.
- **Peak Frequency** and **Dominant frequency** readouts showing **440.0 Hz**.
- **RMS**, **Peak level** and **Clipping** readouts.
- **Stereo delay**, confidence and approximate direction readouts when a stereo signal is present.
- A **Paused / Frozen demo** state for repeatable inspection.

### Try it yourself

1. Build and run the app.
2. Open **Settings** and switch input mode to **Demo mode**.
3. Select **Sine**, **Chirp**, **Stereo delay test**, **Mosquito-like high-frequency burst**,
   **Moving chirp source**, **50 Hz hum + harmonics** or **Clipping test** as the demo signal.
4. Start capture with **File → Start/Stop**.
5. Freeze the current view with **File → Pause/Freeze**.
6. Export evidence via **File → Export measurement CSV...** or
   **File → Export measurement PNG...**.

### Use cases

- **Detect dominant frequency** by finding the strongest FFT peak.
- **Find hum/noise/resonance problems** with the 50 Hz hum + harmonics demo and spectrum readouts.
- **Estimate stereo delay / broad left-right sound direction** from inter-channel
  cross-correlation and microphone spacing. See
  [Stereo localization](docs/use-cases/stereo-localization.md).
- **Inspect high-frequency intermittent sounds** with the mosquito-like burst scenario, framed as a
  localized high-frequency intermittent sound source rather than species detection.
- **Validate generated test signals** by comparing the selected demo signal with the measured
  dominant frequency and level.
- **Export evidence** as CSV or PNG for reports, diagnostics or bug tickets.

## Maven modules

The repository is now split into build-enforced modules so stable audio APIs cannot accidentally
depend on Swing UI or experimental acoustic localization code:

```text
audio-core
audio-geometry
audio-acquisition          -> audio-core, audio-geometry
audio-dsp                  -> audio-core
audio-experimental-acoustic -> audio-core, audio-geometry, audio-acquisition, audio-dsp
audio-app                  -> audio-core, audio-dsp
```

- `audio-core` — immutable audio-domain types, snapshots and the ring buffer.
- `audio-geometry` — reusable 2D geometry and localization constraints.
- `audio-acquisition` — microphone metadata, arrays, multichannel sources and sample clocks.
- `audio-dsp` — FFT, DSP pipeline, analysis, diagnostics, spectrogram and stereo-delay logic.
- `audio-experimental-acoustic` — isolated acoustic localization experiments built only on stable
  modules.
- `audio-app` — Swing UI, JavaSound/demo wiring, export and the application entry point.

## Documentation

- [Architecture](ARCHITECTURE.md) — layered architecture, packages, design choices, capture
  lifecycle, extension points.
- [QA findings & technical debt](docs/QA-FINDINGS.md) — current QA-pass summary, known doc/code
  drift items and prioritized follow-ups.
- [Contributing guide](docs/contributing/README.md) — where stable APIs vs experiments live,
  how to add a new analyzer or DSP experiment safely.
- [Migration notes](docs/MIGRATION.md) — moving from the legacy `WaveformModel`-centric API to
  the new platform.
- [Audio configuration & threading](docs/audio-and-threading.md) — capture parameters, threading
  model, performance notes, logging.
- [Development](docs/development.md) — build, code style, CI, headless testing, JMH benchmarks,
  contributing.
- [Quality gates & coverage](docs/quality.md) — current gates, hardening roadmap, coverage
  targets.
- [Stereo localization](docs/use-cases/stereo-localization.md) — what stereo time-delay analysis
  can and cannot infer, microphone spacing, cross-correlation and demo usage.
- [Experimental acoustic localization architecture](docs/architecture/experimental-acoustic-localization.md) —
  module boundaries for reusable core APIs versus research plugin code.
- [Acoustic localization plugin](docs/plugins/acoustic-localization/README.md) — DSP concepts,
  microphone setup, simulator, limitations and future research directions.
- [Roadmap](ROADMAP.md) — planned features and next issues.

## Project status

This repository is best read as **a reusable Java audio/DSP laboratory with experimental
acoustic-localization extensions**, not a finished production acoustic tracking platform:

- The capture, ring-buffer, DSP-pipeline, FFT, spectrum, RMS/peak, stereo-delay, spectrogram,
  rule-based diagnosis and Swing visualization layers are **stable infrastructure** with unit
  tests and architecture-boundary tests.
- The contents of `audio-experimental-acoustic` (wingbeat tracking, TDOA, beamforming, room
  simulation) are **experimental research code** and intentionally isolated from stable modules.
  They are validated mostly against synthetic signals.
- Quality gates are reporting-only today; see [docs/quality.md](docs/quality.md) for the
  hardening roadmap.

See [docs/QA-FINDINGS.md](docs/QA-FINDINGS.md) for a current list of remaining technical debt.

## License

This project is licensed under the MIT License. See LICENSE.
