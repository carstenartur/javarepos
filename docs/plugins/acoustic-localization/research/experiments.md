# Acoustic tracking experiments

Reproducible evaluation scenarios for the `audio-experimental-acoustic` module.

Every experiment that maps to a deterministic scenario is anchored on the
corresponding factory method of
[`SimulationScenarios`](../../../../audio-experimental-acoustic/src/main/java/org/hammer/audio/experimental/acoustic/simulation/SimulationScenarios.java).
Two runs of the same factory produce bit-identical signals; ground-truth values
for each scenario are duplicated in machine-readable form under
[`simulation-datasets/`](simulation-datasets/).

| ID |                Scenario factory                |                     Status                     |
|----|------------------------------------------------|------------------------------------------------|
| A  | `singleSource()`                               | Implemented + tested                           |
| B  | `movingTowardArray()`                          | Implemented + tested                           |
| C  | (mirror of B with inverted velocity)           | Future — easy to add                           |
| D  | `movingAcrossArray()`                          | Implemented + tested                           |
| E  | `twoMovingSources()` / `twoCloseFrequencies()` | Implemented + tested                           |
| F  | `twoCloseFrequencies()`                        | Implemented + tested                           |
| G  | `noisyRoom()`                                  | Implemented + tested                           |
| H  | `reflectedEnvironment()`                       | Implemented + tested                           |
| I  | Synchronization stress                         | Future — requires drift hooks in `SampleClock` |
| J  | Simulation vs hardware                         | Future — requires real recordings              |

---

## Experiment A — Static single source

### Goal

Validate frequency detection, TDOA localization and short-term localization
stability.

### Setup

- Scenario: `SimulationScenarios.singleSource()`.
- Single source at (1.5, 1.0), 600 Hz, amplitude 0.5, zero velocity.
- Anechoic 3×2 m room (`reflectionGain = 0`, `noiseAmplitude = 0`).
- Default 4-mic array; 16 kHz; 0.5 s.

### Metrics

- localization error (target: < array spacing/2, i.e. < 0.15 m);
- frequency variance over the run (target: < 1 Hz²).

---

## Experiment B — Moving source toward array

### Goal

Validate Doppler estimation and the sign convention for radial velocity.

### Setup

- Scenario: `SimulationScenarios.movingTowardArray()`.
- Source at (1.5, 1.8) moving with velocity (0, −2.0) m/s toward the array
  centerline (1.5, 0.15), 700 Hz, amplitude 0.5.

### Expected

- positive radial velocity reported by `SimpleDopplerEstimator` /
  `ExactDopplerEstimator` for microphones along the source-to-array axis;
- observed frequency strictly above 700 Hz throughout the run.

---

## Experiment C — Moving source away from array (future)

### Goal

Validate that negative Doppler shifts are reported with consistent sign.

### Setup

Mirror of Experiment B with velocity (0, +2.0) m/s. A scenario factory
`movingAwayFromArray()` would unblock this — it is not yet in the catalog.

### Expected

- decreasing observed frequency;
- negative radial velocity from all microphones along the line of motion.

---

## Experiment D — Lateral motion

### Goal

Validate that perpendicular motion produces near-zero radial velocity.

### Setup

- Scenario: `SimulationScenarios.movingAcrossArray()`.
- Source moving (2.0, 0.0) m/s in the x-direction, 760 Hz, starting at
  (0.6, 1.0).

### Expected

- |observed frequency − 760 Hz| ≈ 0 when the source is broadside to the
  microphone pair m0/m1;
- radial velocity small compared to the in-line case of Experiment B.

---

## Experiment E — Two independent sources

### Goal

Validate frequency separation and identity persistence across frames.

### Setup

- Scenario: `SimulationScenarios.twoMovingSources()` (220 Hz separation) or
  `twoCloseFrequencies()` for the static variant.
- 620 Hz + 840 Hz emitters in the moving variant; 600 Hz + 640 Hz in the static
  variant.

### Metrics

- track continuity: number of `TrackedSource#id` values observed should equal the
  number of emitters;
- source identity persistence: same id stays attached to the same emitter for
  the duration of the scenario, as exercised by `SourceTrackerTest`.

---

## Experiment F — Close frequencies

### Goal

Stress-test the frequency clusterer with closely spaced tones.

### Setup

- Scenario: `SimulationScenarios.twoCloseFrequencies()`.
- 600 Hz and 640 Hz (40 Hz gap) at distinct positions.

### Metrics

- source confusion rate (fraction of frames where the two emitters are merged
  into a single cluster);
- track switching frequency (number of id swaps per second).

---

## Experiment G — Noise robustness

### Goal

Measure robustness as broadband room noise increases.

### Procedure

- Baseline: `SimulationScenarios.noisyRoom()` (`noiseAmplitude = 0.05`).
- Sweep `noiseAmplitude` over {0.0, 0.02, 0.05, 0.10, 0.20} by constructing
  custom `Room2D` instances; the rest of the scenario stays fixed.

### Measure

- localization degradation: error vs. noise amplitude;
- frequency stability degradation: variance vs. noise amplitude.

---

## Experiment H — Reflection environment

### Goal

Evaluate behaviour under multipath conditions.

### Setup

- Scenario: `SimulationScenarios.reflectedEnvironment()`
  (`reflectionGain = 0.35`).

### Metrics

- false localization rate: fraction of frames whose estimated position lies
  outside a 0.3 m disk around the true emitter;
- velocity instability: variance of the reconstructed velocity (should be small
  because the source is stationary; non-zero readings indicate phantom motion).

---

## Experiment I — Synchronization error (future)

### Goal

Investigate sensitivity to per-channel clock mismatch.

### Setup

Requires extending `SampleClock` (or wrapping `SimulatedMicrophoneArraySource`)
with a per-channel sample-delay and drift parameter, neither of which currently
exists. Synthetic injection would let us repeat Experiment A while varying:
- a fixed sample offset on one channel;
- a linear drift in samples per second on one channel.

### Metrics

- localization error increase versus drift rate;
- tracking instability (number of identity drops per second).

---

## Experiment J — Simulation vs real hardware (future)

### Goal

Compare deterministic simulator output against physical recordings made with the
hardware setup described in
[`hardware/microphone-array-setup.md`](hardware/microphone-array-setup.md).

### Metrics

- transferability: difference between simulated and recorded localization error
  on the same nominal geometry;
- model mismatch: per-frequency-band gain / phase discrepancy between simulator
  output and recorded array data.

