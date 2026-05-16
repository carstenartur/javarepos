# QA findings & remaining technical debt

This document captures the state of the repository after the documentation/architecture QA
pass. It is intended as a living record of known doc/code drift and prioritized follow-ups.

The QA pass deliberately did **not** add features or rename APIs. It corrected wording,
removed false claims and added contributor-facing guidance.

## What the QA pass changed

- **README.md** — Features list now mentions the spectrogram and rule-based diagnosis
  analyzers, the spectrogram / diagnosis Swing panels, the evidence-bundle export and the
  `audio-acquisition` contracts; added an explicit "Project status" section that frames the
  repository as a reusable audio/DSP laboratory with experimental acoustic-localization
  extensions, and links to this file and the new contributing guide.
- **ROADMAP.md** — Marked the spectrogram view, initial noise-diagnosis rules and
  evidence-bundle export as **done** instead of as future phases (they have all landed in
  `audio-dsp` / `audio-app`). Re-numbered the remaining open phases. Corrected the quality-gates
  bullet so it no longer implies a JaCoCo gate is currently enforced.
- **ARCHITECTURE.md** — Clarified that the capture worker still produces a legacy
  `WaveformModel` via `WaveformRenderer` for backwards compatibility, and added spectrogram,
  diagnosis and experimental-localization entries to the extension-points table.
- **docs/audio-and-threading.md** — Rewritten. The previous version described a
  `ReentrantLock modelLock` and mutable `xPoints`/`yPoints` state on
  `AudioCaptureServiceImpl`; that design was retired when the capture path moved to immutable
  `AudioBlock` publication. The page now describes the current `AudioRingBuffer` + volatile
  `latestBlock` / `latestModel` model.
- **docs/quality.md** — Removed the incorrect "JaCoCo `BUNDLE` line coverage minimum 5%
  fails the build if violated" claim — no `jacoco:check` execution exists in the POM today;
  all gates are currently report-only. Replaced the stale `readSample()` / `recomputeXValues()`
  / `scaleToPixel()` coverage-target list (those methods no longer exist) with one referencing
  the actual `SampleDecoder`, `AudioRingBuffer`, `WaveformRenderer`, `Fft`, `SpectrumAnalyzer`,
  `StereoDelayAnalyzer`, `SpectrogramAnalyzer` and `DiagnosisAnalyzer` surface.
- **docs/MIGRATION.md** — Softened the "Removing the legacy `WaveformModel`" section so it
  no longer implies a follow-up PR is imminent; clarified it is kept for backwards
  compatibility until a deprecation window has been provided.
- **docs/contributing/README.md** — New: repository map (stable vs. experimental),
  where-new-code-belongs table, how-to-add-a-new-analyzer and how-to-add-a-DSP-experiment
  recipes, testing and coding expectations.

No production code (Java sources, POMs, CI workflows) was modified by this pass.

## Confirmed alignment between docs and code

The following were verified to match the current implementation and need no changes:

- The six reactor modules (`audio-core`, `audio-geometry`, `audio-acquisition`, `audio-dsp`,
  `audio-experimental-acoustic`, `audio-app`) match `pom.xml` and the `ARCHITECTURE.md`
  Maven-module section.
- `ArchitectureBoundaryTest` is present in `audio-app` and enforces both source-import and
  POM-dependency rules described in `ARCHITECTURE.md`.
- `AnalysisModule<S extends AnalysisSnapshot>` and `AnalysisSnapshot` exist as documented.
- Capture services use `AudioRingBuffer.offer(...)` (not `offerOverwrite(...)`) with a separate
  `volatile latestBlock` pointer for "latest wins" UI consumers — matching the
  `AudioRingBuffer.offerOverwrite` Javadoc warning about concurrent consumers.
- The experimental acoustic localization package list in
  `docs/plugins/acoustic-localization/README.md` (`MosquitoLocalizationPipeline`,
  `WingbeatFrequencyTracker`, `GccPhatTdoaEstimator`, `CrossCorrelationTdoaEstimator`,
  `DelayAndSumBeamformer`, `SimulatedMicrophoneArraySource`, `Room2D`, `SoundEmitter2D`,
  `AcousticDebugFrame`, `AcousticLocalizationSnapshot`) all exist under
  `audio-experimental-acoustic`.
- The screenshot referenced by the README (`docs/images/screenshot.png`) is present.

## Remaining technical debt

The items below are deliberately **not** addressed in this pass. They are listed so future PRs
can pick them up without rediscovery.

### Documentation

- **`docs/` reorganization.** The problem statement suggested a nested
  `docs/architecture/`, `docs/plugins/`, `docs/dsp/`, `docs/localization/`,
  `docs/simulation/`, `docs/use-cases/`, `docs/contributing/`, `docs/roadmap/`
  layout. Today only `architecture/`, `plugins/`, `use-cases/` and (newly) `contributing/`
  exist; `dsp/`, `localization/`, `simulation/` and `roadmap/` are not yet split out. Doing
  this incrementally — moving each existing page once it has a sibling — avoids churning all
  links in one go.
- **`docs/quality.md` coverage-target list** is now factually correct but still aspirational;
  actual JaCoCo numbers per module have not been added.
- **Module-level package-info Javadoc.** Most packages have a `package-info.java`, but a few
  (e.g. `org.hammer` in `audio-app`) do not document their intended role.

### Build / structure

- **No enforced coverage gate.** `pom.xml` configures only `jacoco:prepare-agent` + `report`,
  not `check`. Introducing a low BUNDLE floor (5–10%) is step 3 of the hardening roadmap.
- **Checkstyle / SpotBugs / PMD are report-only** (`failOnViolation=false` /
  `failOnError=false`). Baseline-then-fail-on-new is the documented next step.
- **`org.hammer.audio` package is split across two modules** (`audio-dsp` has
  `DemoSignalType`; `audio-app` has `AudioCaptureService` / `AudioCaptureServiceImpl` /
  `WaveformModel` / `DemoAudioCaptureService` / `AudioLineProvider`). This is intentional for
  package-stability of `DemoSignalType` but is a split-package situation that would block a
  future Java Platform Module System (JPMS) migration. `ARCHITECTURE.md` mentions this in the
  packages table but the long-term plan is not written down.
- **Legacy `WaveformModel` still produced on every capture iteration.** There is no
  deprecation timeline; that is intentional but should be tracked as long-running technical
  debt.

### Tests

- **No architecture guard for `audio-acquisition` / `audio-geometry`.** The current
  `ArchitectureBoundaryTest` (in `audio-app`) covers the most important rules (stable ↛ app,
  stable ↛ experimental). Adding explicit per-module rules — and moving the test out of
  `audio-app` so a Maven module-graph cycle cannot hide a violation — is worthwhile.
- **`SampleClock` only carries nominal timestamps.** No drift / jitter test exists. This is
  called out in the acoustic-localization plugin docs but is also a coverage gap.

### Experimental acoustic localization

The plugin documentation is already explicit about what is and is not implemented; no
further wording changes were required in this pass. Open research items are tracked in
[`docs/plugins/acoustic-localization/README.md`](plugins/acoustic-localization/README.md#future-research-directions).

## Prioritized follow-up recommendations

1. **Introduce a baseline + new-violations gate** for Checkstyle / SpotBugs / PMD (step 1 of
   `docs/quality.md`). This is the highest-leverage quality improvement and is purely
   tooling.
2. **Add a low JaCoCo `BUNDLE` line-coverage floor** (5%) and verify it does not break the
   current build, then raise it.
3. **Move `ArchitectureBoundaryTest` to its own small test module** (or duplicate the
   essential POM checks into each stable module) so that boundary violations cannot be hidden
   by a missing test-time dependency.
4. **Split `docs/` into the nested structure suggested in the problem statement**, moving one
   page at a time and updating cross-links as you go.
5. **Document the long-term `WaveformModel` deprecation policy** (in `MIGRATION.md`) so
   consumers know whether to plan a migration.
6. **Add a `SampleClock` drift / jitter test** in `audio-acquisition` so the limitation
   currently only documented in the acoustic-localization plugin is also enforced in code.

