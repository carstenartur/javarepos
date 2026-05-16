# Roadmap

Suggested next feature phases for Audio Analyzer. Completed foundations such as audio device
selection, FFT/spectrum view, basic CSV/PNG export and deterministic demo input are now treated as
current capabilities rather than roadmap items.

> **Status note.** Earlier roadmap phases for a spectrogram/waterfall view, rule-based noise
> diagnosis and an evidence-export bundle have landed and now ship as part of the application
> (`SpectrogramAnalyzer` / `SpectrogramPanel`, `DiagnosisAnalyzer` / `DiagnosisPanel`,
> `EvidenceBundleExporter`). Phases A (oscilloscope trigger + peak hold / averaging), B (recording
> and replay) and C (A/B comparison and report generation) have all landed too — they ship today
> and are kept here for context but are no longer open work. See
> [docs/features/](docs/features/) for the per-feature documentation.

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

## ✅ Done — Phase A: Triggered oscilloscope + peak hold / averaging

- Oscilloscope-style waveform trigger (level, slope, holdoff, auto/normal mode) shipped as
  `WaveformTrigger` (`audio-dsp`) and wired into `WaveformPanel`. Controllable from the **File**
  menu of the main window. See [docs/features/oscilloscope-trigger.md](docs/features/oscilloscope-trigger.md).
- Spectrum peak hold and exponential averaging shipped as `PeakHoldSpectrum` and `SpectrumAverager`
  with `SpectrumDisplayState`. Controllable from the **File** menu. See
  [docs/features/peak-hold-and-averaging.md](docs/features/peak-hold-and-averaging.md).
- Pause / freeze behavior on the waveform panel is preserved and works alongside the trigger.

## ✅ Done — Phase B: Recording / replay

- Binary `.aar` recording format (`AudioBlockRecordingFormat`, `AudioBlockRecordingWriter`,
  `AudioBlockRecordingReader` in `audio-dsp`) records normalized `AudioBlock` streams with their
  format header, frame index and timestamp metadata.
- `RecordingTap` (in `audio-app`) captures the active session into a file; `RecordedAudioCaptureService`
  implements the regular `AudioCaptureService` interface so replay behaves identically to live
  capture for every downstream panel. See
  [docs/features/recording-and-replay.md](docs/features/recording-and-replay.md).
- All file I/O lives outside the core DSP / analysis packages.

## ✅ Done — Phase C: A/B comparison and report generation

- `RecordingComparator` replays two recordings through the standard analyzer stack and produces a
  `ComparisonReport`; `MarkdownComparisonReportRenderer` writes the report as Markdown suitable
  for QA notes or bug tickets. See [docs/features/ab-comparison.md](docs/features/ab-comparison.md).
- The same machinery is reusable from CI / batch workflows with no Swing dependency.

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

