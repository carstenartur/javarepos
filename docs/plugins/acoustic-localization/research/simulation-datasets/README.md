# Simulation datasets

Machine-readable scenario descriptors and ground-truth values for the
deterministic experiments defined by
[`SimulationScenarios`](../../../../../audio-experimental-acoustic/src/main/java/org/hammer/audio/experimental/acoustic/simulation/SimulationScenarios.java).
The JSON files here are the source of truth for external benchmark scripts and
for evaluation harnesses that do not link against the Java code.

The Java factory remains authoritative for the deterministic bit-identical
signal generation; these JSON files re-state the same numbers in a portable
format so that:

- third-party tools (Python notebooks, comparison harnesses) can reproduce the
  scenarios without depending on the JVM;
- the metrics in [`../evaluation-metrics.md`](../evaluation-metrics.md) can be
  computed against an explicit ground truth instead of reading it from code at
  runtime;
- changes to the canonical scenarios are reviewed both in code and in data.

## Layout

```text
simulation-datasets/
├── single-source/
│   └── scenario.json
├── moving-source/
│   └── scenario.json
├── dual-source/
│   └── scenario.json
└── noisy-room/
    └── scenario.json
```

Each `scenario.json` follows the schema documented below.

## Schema

```jsonc
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "name": "single-source",                       // matches Scenario#name
  "description": "Human-readable summary.",
  "room": {
    "widthMeters": 3.0,
    "heightMeters": 2.0,
    "reflectionGain": 0.0,                       // [0, 1]
    "noiseAmplitude": 0.0                        // [0, 1]
  },
  "array": {
    "sampleRateHz": 16000,
    "microphones": [
      { "id": "m0", "channel": 0, "position": [1.35, 0.0] }
    ]
  },
  "emitters": [
    {
      "id": "e0",
      "position": [1.5, 1.0],                    // ground truth at t = 0
      "velocity": [0.0, 0.0],                    // m/s, constant for all bundled scenarios
      "frequencyHz": 600.0,
      "amplitude": 0.5
    }
  ],
  "durationSeconds": 0.5,
  "randomSeed": 1,
  "groundTruth": {
    "trajectory": "linear",                      // position(t) = position(0) + velocity * t
    "expectedTrackCount": 1
  }
}
```

### Field notes

- **`array.sampleRateHz`** mirrors `SimulationScenarios.SAMPLE_RATE` (16000).
- **`emitters[].velocity`** is constant; trajectories are linear, computed by the
  scenario harness as `p(t) = p₀ + v · t`. Non-linear trajectories will get an
  explicit `trajectory` discriminator when introduced.
- **`randomSeed`** matches the Java seed so that bit-identical signals can be
  reproduced by either runtime.
- **`groundTruth.expectedTrackCount`** is the number of `TrackedSource`s a
  correctly-functioning pipeline should report at steady state, used by
  identity-persistence checks in
  [`../evaluation-metrics.md`](../evaluation-metrics.md).

## Validating against the Java factory

The JSON files are reviewed alongside any change to `SimulationScenarios`. The
intent is that a future helper test asserts equality between the Java scenario
record and the JSON file with the same `name`, so the two cannot drift apart
silently. That test does not exist yet and is tracked as future work in
[`../experiments.md`](../experiments.md).
