# Roadmap

Suggested next feature phases for Audio Analyzer. Completed foundations such as audio device
selection, FFT/spectrum view, basic CSV/PNG export and deterministic demo input are now treated as
current capabilities rather than roadmap items.

## Phase 1: Spectrogram / waterfall

- Add a time-frequency spectrogram or waterfall view alongside the waveform and spectrum panels.
- Reuse existing FFT snapshots where practical so the feature stays consistent with current
  spectrum measurements.
- Keep rendering headless-testable and separate from audio-domain analysis.

## Phase 2: Noise diagnosis rules

- Add transparent diagnostic rules for common issues such as hum, clipping, narrowband resonance,
  excessive broadband noise and intermittent high-frequency bursts.
- Base rules on existing measurement, spectrum and stereo-delay snapshots before adding new signal
  processing primitives.
- Report rule confidence and limitations clearly in the UI and exports.

## Phase 3: Triggered oscilloscope + peak hold / averaging

- Add trigger controls for stable waveform inspection of repeating or transient events.
- Add spectrum peak hold and averaging modes for slower-changing diagnostics.
- Preserve current pause/freeze behavior for manual inspection.

## Phase 4: Recording / replay

- Record normalized `AudioBlock` streams with format metadata for deterministic replay.
- Replay captures through the same analysis and UI paths as live/demo input.
- Keep file I/O outside the core DSP and analysis packages.

## Phase 5: Evidence export bundle

- Bundle measurement CSV, visualization PNGs, metadata and diagnostic summaries into a single
  export artifact.
- Include enough context to reproduce microphone/demo settings and analysis parameters.
- Avoid replacing the existing quick CSV/PNG exports unless migration is deliberate.

## Phase 6: A/B comparison and report generation

- Compare two recordings or replay sessions using shared measurements and diagnostics.
- Surface before/after differences for dominant frequency, level, clipping, noise rules and
  stereo-delay estimates.
- Generate a concise report suitable for diagnostics, QA notes or bug tickets.

## Testing

- Add focused unit tests for new analysis rules, recording/replay serialization and report/export
  assembly as each phase lands.
- Add integration tests around replay-driven end-to-end model publication when recording/replay is
  implemented.

## Quality Gates

Tracked in [`docs/quality.md`](docs/quality.md):

- Block *new* Checkstyle / SpotBugs / PMD violations on PRs.
- Then block high-severity SpotBugs / PMD findings.
- Raise JaCoCo line-coverage floor in steps: 5% → 10% → 20% → 30%.

