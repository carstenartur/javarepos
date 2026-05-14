# Audio Analyzer

[![Java CI with Maven](https://github.com/carstenartur/javarepos/actions/workflows/maven.yml/badge.svg)](https://github.com/carstenartur/javarepos/actions/workflows/maven.yml)
[![codecov](https://codecov.io/gh/carstenartur/javarepos/graph/badge.svg)](https://codecov.io/gh/carstenartur/javarepos)
[![CodeQL](https://github.com/carstenartur/javarepos/actions/workflows/codeql.yml/badge.svg)](https://github.com/carstenartur/javarepos/actions/workflows/codeql.yml)

**Audio Analyzer** is a Java/Swing desktop application for real-time waveform and phase-diagram visualization of audio captured from the system's input device.

> The Maven artifact is named `audioin` for historical reasons; the application is **Audio Analyzer**.

## Features

- Live **waveform** rendering of mono or stereo audio input.
- Live **phase diagram** for stereo signals.
- Configurable sample rate, sample size, signed/unsigned, endianness, and buffer divisor.
- Clean separation between UI (Swing) and audio capture (`AudioCaptureService` + immutable `WaveformModel`).
- Headless-friendly: GUI components are unit-testable in CI without a display.
- Optional [JMH](https://openjdk.org/projects/code-tools/jmh/) micro-benchmarks for sample decoding.

## Quickstart

Requires **Java 21** or higher.

```bash
# Build, test, run static analysis and coverage
./mvnw clean verify

# Run the application
java -jar target/audioin-0.0.1-SNAPSHOT.jar
```

On Windows use `mvnw.cmd` instead of `./mvnw`.

## Documentation

- [Architecture](ARCHITECTURE.md) — GUI / service / model separation, threading overview, sequence diagrams.
- [Audio configuration & threading](docs/audio-and-threading.md) — capture parameters, threading model, performance notes, logging.
- [Development](docs/development.md) — build, code style, CI, headless testing, JMH benchmarks, contributing.
- [Quality gates & coverage](docs/quality.md) — current gates, hardening roadmap, coverage targets.
- [Roadmap](ROADMAP.md) — planned features and next issues.

## License

No license file is currently provided in this repository. Until one is added, all rights are reserved by the author.
