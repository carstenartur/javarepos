# Quality Gates & Coverage

This page describes the quality checks that are actually enforced today and the checks that remain
report-only. It intentionally avoids claiming hard gates that are not present in Maven or CI.

## Current gates

|          Gate           |                                      Configuration                                       |                                    Fails build/CI?                                    |
|-------------------------|------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------|
| Java version            | Maven Enforcer requires Java `[21,)`                                                     | **Yes**, in `mvn verify`                                                              |
| Unit tests              | Surefire, `java.awt.headless=true`                                                       | **Yes**, in `mvn verify`                                                              |
| Spotless                | Java, POM and Markdown format check                                                      | **Yes**, in `mvn verify`                                                              |
| Architecture boundaries | JUnit test in `audio-app`                                                                | **Yes**, in `mvn verify`                                                              |
| JaCoCo                  | `prepare-agent`, `report`, `check`; `BUNDLE` line coverage minimum `0.05`                | **Yes**, in `mvn verify`                                                              |
| Checkstyle              | `checkstyle.xml`, severity `warning`, `failOnViolation=false`                            | Report-only locally; **CI fails only if counts exceed `quality-baseline.properties`** |
| PMD                     | `pmd-ruleset.xml`, `failOnViolation=false`                                               | Report-only locally; **CI fails only if counts exceed `quality-baseline.properties`** |
| SpotBugs                | `effort=Max`, `threshold=Low`, exclusions in `spotbugs-exclude.xml`, `failOnError=false` | Report-only locally; **CI fails only if counts exceed `quality-baseline.properties`** |
| Codecov upload          | `codecov/codecov-action`, `fail_ci_if_error=false`                                       | **No**; upload failures are not a quality gate                                        |
| CodeQL                  | GitHub workflow with explicit Maven package build                                        | **Yes** when the CodeQL workflow runs                                                 |

## Baseline captured during this pass

Baseline command:

```bash
JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64 ./mvnw -B clean verify
```

Result: exit code 0.

Static-analysis findings are intentionally not all fixed in this pass. CI compares the current XML
report counts with `quality-baseline.properties` and fails if a module introduces additional
Checkstyle, PMD or SpotBugs findings above that baseline.

Observed JaCoCo line coverage after the baseline run:

|            Module             | Line coverage |
|-------------------------------|--------------:|
| `audio-core`                  |        69.56% |
| `audio-plugin-api`            |       100.00% |
| `audio-geometry`              |        61.05% |
| `audio-acquisition`           |     no report |
| `audio-dsp`                   |        78.65% |
| `audio-app`                   |        29.96% |
| `audio-experimental-acoustic` |        86.03% |

`audio-acquisition` is included in the Maven reactor and JaCoCo configuration, but the baseline run
did not produce a module JaCoCo XML/HTML report for it because there was no test execution data in
that module.

The 5% JaCoCo floor is deliberately low because its purpose is to prove the gate and prevent
coverage from disappearing, not to claim comprehensive test coverage.

## Report locations after `mvn verify`

- Checkstyle: `*/target/checkstyle-result.xml`
- PMD: `*/target/pmd.xml`
- SpotBugs: `*/target/spotbugsXml.xml`
- JaCoCo HTML: `*/target/site/jacoco/index.html`
- JaCoCo XML: `*/target/site/jacoco/jacoco.xml`

## Hardening roadmap

1. Reduce the committed Checkstyle/PMD/SpotBugs baselines by fixing existing findings module by
   module.
2. Switch high-confidence Checkstyle rules to `failOnViolation=true` once the baseline is small
   enough that local failures are actionable.
3. Raise JaCoCo line coverage in small steps: **5% → 10% → 20% → 30%**, backed by tests for behavior
   rather than coverage-only assertions.
4. Consider severity-filtered hard gates for SpotBugs and PMD once current findings are triaged.

## Target areas for increased coverage

- `SampleDecoder` format and partial-buffer paths.
- `AudioRingBuffer` boundary and SPSC stress behavior.
- `WaveformRenderer`, trigger and spectrum display-state edge cases.
- `SpectrumAnalyzer`, `StereoDelayAnalyzer`, `SpectrogramAnalyzer` and `DiagnosisAnalyzer` rejection
  and threshold paths.
- Recording/replay and evidence-bundle assembly.
- `SampleClock` drift/jitter assumptions: currently documented as a limitation and should become a
  focused test or tracked issue before real synchronized-array claims are made.

