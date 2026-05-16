# Roadmap

Suggested next feature phases for Audio Analyzer. Completed foundations such as audio device
selection, FFT/spectrum view, basic CSV/PNG export and deterministic demo input are now treated as
current capabilities rather than roadmap items.

> **Status note.** Earlier roadmap phases for a spectrogram/waterfall view, rule-based noise
> diagnosis and an evidence-export bundle have landed and now ship as part of the application
> (`SpectrogramAnalyzer` / `SpectrogramPanel`, `DiagnosisAnalyzer` / `DiagnosisPanel`,
> `EvidenceBundleExporter`). They are kept here for context but are no longer open work.

## ✅ Done — spectrogram / waterfall

- Time-frequency spectrogram is implemented in `audio-dsp` (`SpectrogramAnalyzer`,
  `SpectrogramFrame`, `SpectrogramHistory`) and rendered in the Swing UI by `SpectrogramPanel`.
- Reuses the existing FFT path for consistency with the spectrum panel.

## ✅ Done — initial noise diagnosis rules

- Transparent rule-based analyzer in `audio-dsp` (`DiagnosisAnalyzer`, `DiagnosisFinding`,
  `DiagnosisSeverity`, `DiagnosisType`, `DiagnosisSnapshot`) surfaced by `DiagnosisPanel`.
- Rules consume existing measurement / spectrum / stereo-delay snapshots; their wording and
  confidence calibration remain a follow-up area.

## ✅ Done — evidence export bundle (initial)

- `EvidenceBundleExporter` and `EvidenceData` assemble a bundle of measurement CSV, spectrum
  image, spectrogram, stereo-delay and diagnosis context. Quick CSV/PNG exports are unchanged.
- Bundle metadata, reproducibility hints and richer report formatting are still open.

## Phase A: Triggered oscilloscope + peak hold / averaging

- Add trigger controls for stable waveform inspection of repeating or transient events.
- Add spectrum peak hold and averaging modes for slower-changing diagnostics.
- Preserve current pause/freeze behavior for manual inspection.

## Phase B: Recording / replay

- Record normalized `AudioBlock` streams with format metadata for deterministic replay.
- Replay captures through the same analysis and UI paths as live/demo input.
- Keep file I/O outside the core DSP and analysis packages.

## Phase C: A/B comparison and report generation

- Compare two recordings or replay sessions using shared measurements and diagnostics.
- Surface before/after differences for dominant frequency, level, clipping, noise rules and
  stereo-delay estimates.
- Generate a concise report suitable for diagnostics, QA notes or bug tickets.

## Experimental acoustic localization

The `audio-experimental-acoustic` module hosts research code (wingbeat tracking, GCC-PHAT/TDOA,
delay-and-sum beamforming, 2D room simulation). Open research items are tracked in the
[plugin README](docs/plugins/acoustic-localization/README.md#future-research-directions);
this module is intentionally not promoted to a production feature on this roadmap.

## Testing

- Add focused unit tests for new analysis rules, recording/replay serialization and report/export
  assembly as each phase lands.
- Add integration tests around replay-driven end-to-end model publication when recording/replay is
  implemented.

## Quality Gates

Tracked in [`docs/quality.md`](docs/quality.md):

- Wire Checkstyle / SpotBugs / PMD to fail PRs that introduce *new* findings against a baseline.
- Then block high-severity SpotBugs / PMD findings.
- Introduce and then raise a JaCoCo line-coverage gate in steps: 5% → 10% → 20% → 30%.
  (No coverage gate is enforced today; only reports are generated.)

