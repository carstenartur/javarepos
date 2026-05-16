# Audio Analyzer

[![Java CI with Maven](https://github.com/carstenartur/javarepos/actions/workflows/maven.yml/badge.svg)](https://github.com/carstenartur/javarepos/actions/workflows/maven.yml)
[![codecov](https://codecov.io/gh/carstenartur/javarepos/graph/badge.svg)](https://codecov.io/gh/carstenartur/javarepos)
[![CodeQL](https://github.com/carstenartur/javarepos/actions/workflows/codeql.yml/badge.svg)](https://github.com/carstenartur/javarepos/actions/workflows/codeql.yml)

**Audio Analyzer** is a modular real-time audio processing platform built around a Java/Swing
demo. It provides a layered architecture for audio acquisition, ring-buffering, DSP, analysis
(RMS/peak, FFT spectrum) and visualization, with deterministic synthetic signal generators and
JMH benchmarks. The bundled Swing UI renders a live waveform and phase diagram of the system
audio input.

> The Maven artifact is named `audioin` for historical reasons; the application is **Audio Analyzer**.

## Features

- **Layered architecture** — capture → ring buffer → DSP pipeline → analysis → snapshots → UI.
  See [`ARCHITECTURE.md`](ARCHITECTURE.md).
- **Immutable audio domain** — `AudioBlock` and `AudioFormatDescriptor` carry normalized
  `float[channels][frames]` samples, frame indices and timestamps; no UI types.
- **Lock-free SPSC ring buffer** for realtime workloads, with `offer` and `offerOverwrite`.
- **DSP extension points** — implement `DSPProcessor` and chain stages with `DSPPipeline`.
- **Analysis modules** — `RmsPeakAnalyzer`, `SpectrumAnalyzer` (pure-Java radix-2 FFT) producing
  immutable snapshots suitable for any UI or remote API.
- **Deterministic synthetic signals** — `SineGenerator`, `SquareGenerator`, `ChirpGenerator` for
  tests, headless demos and DSP verification.
- **Live Swing UI** — selectable microphone input, waveform, phase diagram, FFT spectrum,
  demo mode (sine/square/chirp), pause/freeze, peak-frequency + measurement readouts, and CSV/PNG
  export for quick acoustic diagnostics.
- **Headless-friendly tests** — 81 unit tests covering immutability, FFT correctness, SPSC
  concurrency stress, signal determinism, DSP pipeline composition and sample decoding.
- **JMH benchmarks** for ring buffer throughput, FFT throughput and signal-generator
  allocations.
- **Java 21**, no heavyweight frameworks.

## Quickstart

Requires **Java 21** or higher.

```bash
# Build, test, run static analysis and coverage
./mvnw clean verify

# Run the application (after package/verify, requires target/lib runtime jars)
java -jar target/audioin-0.0.1-SNAPSHOT.jar
# Runtime dependencies are copied to target/lib during the Maven package phase.

# Run JMH benchmarks
./mvnw -Pjmh package
java -jar target/audioin-0.0.1-SNAPSHOT.jar  # see exec-maven-plugin config
```

On Windows use `mvnw.cmd` instead of `./mvnw`.

## Hero screenshot

![Audio Analyzer modern dashboard screenshot](docs/images/screenshot.png)

## Use case: Detecting the dominant frequency of a signal

The screenshot shows a practical measurement workflow:

- **Waveform** panel: time-domain shape of the current signal.
- **FFT spectrum** panel: frequency-domain energy distribution.
- **Peak / dominant frequency** readout: strongest spectral component.
- **RMS / Peak level** readouts: average and maximum signal level.
- **Clipping** indicator: warns when samples reach clipping threshold.
- **Demo / Live input mode** controls: switch between synthetic test input and microphone capture.

### Why this is useful

This helps with quick acoustic diagnostics, validating test signals, inspecting microphone input, and exporting CSV/PNG evidence for reports or bug tickets.

### Reproducible demo scenario

Use this scenario to reproduce a screenshot similar to `docs/images/screenshot.png`:

1. Open **Settings** and switch input mode to **Demo mode**.
2. Select **Sine** or **Chirp** as the demo signal.
3. Start capture with **File → Start/Stop**.
4. Freeze the current view with **File → Pause/Freeze**.
5. Export the result via **File → Export measurement PNG...**.

## Documentation

- [Architecture](ARCHITECTURE.md) — layered architecture, packages, design choices, capture
  lifecycle, extension points.
- [Migration notes](docs/MIGRATION.md) — moving from the legacy `WaveformModel`-centric API to
  the new platform.
- [Audio configuration & threading](docs/audio-and-threading.md) — capture parameters, threading
  model, performance notes, logging.
- [Development](docs/development.md) — build, code style, CI, headless testing, JMH benchmarks,
  contributing.
- [Quality gates & coverage](docs/quality.md) — current gates, hardening roadmap, coverage
  targets.
- [Roadmap](ROADMAP.md) — planned features and next issues.

## License

No license file is currently provided in this repository. Until one is added, all rights are reserved by the author.
