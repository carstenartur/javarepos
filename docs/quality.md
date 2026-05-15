# Quality Gates & Coverage

This page describes the project's current quality gates and the planned path to harden them.

## Current Gates

|      Gate      |                            Configuration                            |                Behavior                 |
|----------------|---------------------------------------------------------------------|-----------------------------------------|
| **JaCoCo**     | `BUNDLE` line coverage minimum **5%**                               | Fails the build if violated.            |
| **Checkstyle** | `checkstyle.xml`, severity = `warning`                              | Reports only — `failOnViolation=false`. |
| **SpotBugs**   | `effort=Max`, `threshold=Low`, exclusions in `spotbugs-exclude.xml` | Reports only — `failOnError=false`.     |
| **PMD**        | `pmd-ruleset.xml`                                                   | Reports only — `failOnViolation=false`. |

The 5% JaCoCo floor is intentionally low so coverage can be improved incrementally without breaking the build during the work.

Reports after `mvn verify`:

- **Checkstyle**: `target/checkstyle-result.xml`
- **SpotBugs**:  `target/spotbugsXml.xml`
- **PMD**:      `target/pmd.xml`
- **Coverage**: `target/site/jacoco/index.html`

## Hardening Roadmap

The intent is to tighten gates in three steps so that builds stay green during the transition.

1. **Block new violations only.** Wire Checkstyle / SpotBugs / PMD to a baseline of currently-known findings; fail only when a PR introduces a *new* finding.
2. **Block high-severity findings.** Once new-violation gating is stable, switch SpotBugs / PMD to fail on high-severity issues. Set Checkstyle `failOnViolation=true` for the rules listed in `checkstyle.xml` at severity `error`.
3. **Raise coverage.** Lift the JaCoCo `BUNDLE` line-coverage minimum in steps: **5% → 10% → 20% → 30%**, accompanied by new tests (see below).

## Target Areas for Increased Coverage

Focus tests on these areas to reach the higher coverage tiers:

1. **`readSample()` variants** — 8/16-bit, signed/unsigned, big/little-endian, fallback path.
2. **`captureLoop()` buffer reuse** — verify the private `datas` capture buffer field is reused across reads (the name is non-idiomatic but is the existing implementation), exercise partial buffer fills, validate atomic model updates under concurrent access.
3. **`recomputeXValues()`** — boundary conditions (`panelWidth=1`, `numberOfPoints=1`), even distribution, `numberOfPoints > panelWidth`.
4. **`scaleToPixel()`** — signed normalization at extremes, unsigned scaling at boundaries, `panelHeight=0`.
5. **Thread safety** — concurrent `setDivisor()` / `recomputeLayout()`, `modelLock` race protection, snapshot consistency under concurrent writes.

Run tests and inspect coverage:

```bash
mvn clean test
# Open target/site/jacoco/index.html
```

