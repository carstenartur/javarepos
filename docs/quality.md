# Quality Gates & Coverage

This page describes the project's current quality gates and the planned path to harden them.

## Current Gates

|      Gate      |                            Configuration                            |                    Behavior                     |
|----------------|---------------------------------------------------------------------|-------------------------------------------------|
| **JaCoCo**     | `prepare-agent` + `report` only (no `check` execution configured)   | Generates reports; does **not** fail the build. |
| **Checkstyle** | `checkstyle.xml`, severity = `warning`                              | Reports only — `failOnViolation=false`.         |
| **SpotBugs**   | `effort=Max`, `threshold=Low`, exclusions in `spotbugs-exclude.xml` | Reports only — `failOnError=false`.             |
| **PMD**        | `pmd-ruleset.xml`                                                   | Reports only — `failOnViolation=false`.         |

All four gates are currently advisory: they generate reports but do not break the build. Earlier
revisions of this document claimed a 5% JaCoCo `BUNDLE` line-coverage minimum; no such
`jacoco:check` execution is configured in the POM today. Introducing that floor (and tightening
the others) is tracked under "Hardening Roadmap" below.

Reports after `mvn verify`:

- **Checkstyle**: `target/checkstyle-result.xml`
- **SpotBugs**:  `target/spotbugsXml.xml`
- **PMD**:      `target/pmd.xml`
- **Coverage**: `target/site/jacoco/index.html`

## Hardening Roadmap

The intent is to introduce gates in three steps so that builds stay green during the transition.

1. **Block new violations only.** Wire Checkstyle / SpotBugs / PMD to a baseline of currently-known findings; fail only when a PR introduces a *new* finding.
2. **Block high-severity findings.** Once new-violation gating is stable, switch SpotBugs / PMD to fail on high-severity issues. Set Checkstyle `failOnViolation=true` for the rules listed in `checkstyle.xml` at severity `error`.
3. **Introduce and raise coverage.** Add a `jacoco:check` execution with a `BUNDLE` line-coverage minimum and lift it in steps: **5% → 10% → 20% → 30%**, accompanied by new tests (see below).

## Target Areas for Increased Coverage

Focus tests on these areas to reach the higher coverage tiers:

1. **`org.hammer.audio.capture.SampleDecoder`** — 8/16-bit, signed/unsigned, big/little-endian, partial-buffer fallback paths.
2. **`AudioCaptureServiceImpl` capture loop** — verify the worker-owned `datas` byte buffer is reused across reads, exercise partial buffer fills, and validate that `AudioBlock` publication, `latestBlock` updates and legacy `WaveformModel` rendering stay consistent under reconfiguration.
3. **`AudioRingBuffer`** — `offer` / `offerOverwrite` behaviour at the boundary, `drainTo` ordering, SPSC stress under contention.
4. **`org.hammer.audio.ui.WaveformRenderer`** — boundary conditions (`panelWidth=1`, single-point input), even distribution of x-points and y-scaling at signal extremes.
5. **DSP / analyzers** — `Fft` against known impulses / sinusoids, `SpectrumAnalyzer` window/length combinations, `StereoDelayAnalyzer` rejection paths, `SpectrogramAnalyzer` history rollover, `DiagnosisAnalyzer` rule activation thresholds.
6. **Thread safety** — concurrent `setDivisor()` / `recomputeLayout()`, snapshot consistency under concurrent worker writes, ring-buffer SPSC invariants.

Run tests and inspect coverage:

```bash
mvn clean test
# Open target/site/jacoco/index.html
```

