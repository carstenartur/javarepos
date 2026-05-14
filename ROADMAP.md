# Roadmap

Suggested next features and improvements for Audio Analyzer. Items are not strictly ordered, but the top of each section is roughly higher value.

## Features

- **Audio device selection in the UI** — pick the input device at runtime instead of relying on the platform default.
- **Export of measurement data** — save captured samples as WAV, raw measurement series, or screenshots of the waveform / phase diagram.
- **FFT / spectrum view** — render a frequency-domain panel alongside the waveform.
- **Latency / FPS readout** — surface end-to-end capture latency and render frame rate in the UI for diagnostics.
- **Mock audio input** — a deterministic, file- or function-driven `AudioCaptureService` for reproducible demos and headless integration tests.

## Testing

- More unit tests for `readSample`, `scaleToPixel`, `recomputeXValues`, and the threading paths in `AudioCaptureServiceImpl` (see [`docs/quality.md`](docs/quality.md)).
- Integration test using the mock audio input to assert end-to-end model publication.

## Quality Gates

Tracked in [`docs/quality.md`](docs/quality.md):

- Block *new* Checkstyle / SpotBugs / PMD violations on PRs.
- Then block high-severity SpotBugs / PMD findings.
- Raise JaCoCo line-coverage floor in steps: 5% → 10% → 20% → 30%.

