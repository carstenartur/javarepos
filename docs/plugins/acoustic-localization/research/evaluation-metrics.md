# Evaluation metrics

Definitions of the metrics used by the experiments in
[`experiments.md`](experiments.md) and demos in
[`demo-scenarios.md`](demo-scenarios.md). All metrics are computed from the
immutable snapshots emitted by the tracking pipeline
(`TrackingSnapshot`, `TrackedSource`) or the localization pipeline
(`AcousticLocalizationSnapshot`) and the ground truth declared by the matching
`SimulationScenarios` factory or by
[`simulation-datasets/`](simulation-datasets/).

## Conventions

- Positions are 2D in meters; velocities in m/s; frequencies in Hz.
- "Per-frame" means once per emitted snapshot (one snapshot per `AudioBlock`).
- Aggregation across a scenario uses the full set of frames after a short warm-up
  window (typically 50 ms) to let frequency tracking and the Kalman filter
  converge.

---

## Localization error

Distance between the estimated and the ground-truth source position for a given
emitter `e` at frame `t`:

```text
err_loc(e, t) = ‖ TrackedSource.position(e, t) − GroundTruth.position(e, t) ‖₂
```

- Unit: meters.
- Aggregations: mean, median, 95th percentile, max.
- Target for the bundled scenarios: less than half the largest inter-microphone
  spacing of the array (≈0.15 m for `defaultArray`).

---

## Velocity error

Difference between the reconstructed and the actual emitter velocity:

```text
err_vel(e, t) = ‖ VelocityReconstructor.estimate(e, t) − GroundTruth.velocity(e, t) ‖₂
```

- Unit: m/s.
- Reconstructed velocity comes from fusing per-microphone radial estimates with
  the current array geometry via `VelocityReconstructor`.

---

## Frequency stability

Variance of the cluster frequency assigned to a tracked source over time:

```text
var_f(e) = Var_t( FrequencyCluster.frequencyHz(e, t) )
```

- Unit: Hz².
- Low variance indicates stable tracking and a reliable Doppler baseline; large
  spikes typically point at peak-detector confusion under noise or reflections.

---

## Tracking continuity

Two complementary metrics:

- **Identity persistence:** fraction of frames where the `TrackedSource#id`
  attached to ground-truth emitter `e` matches the id observed in the previous
  frame. Target: 1.0.
- **Track switching frequency:** count of id changes per second across the
  scenario duration. Target: 0.

Both are exercised by `SourceTrackerTest` and
`TrackingPipelineScenarioTest` in `audio-experimental-acoustic`.

---

## Doppler consistency

Variance of the per-microphone radial-velocity estimates produced by
`SimpleMultiSensorDopplerEstimator` for the same emitter and frame:

```text
var_v_r(e, t) = Var_m( RadialVelocityEstimate.radialVelocity(m, e, t) )
```

- Unit: (m/s)².
- Low variance indicates consistent multi-sensor fusion; spikes signal that one
  microphone disagrees, usually because of reflections or peak swaps.

---

## Latency

Wall-clock processing delay between audio acquisition and the corresponding
tracker output:

```text
latency(t) = TrackingSnapshot.wallClockNanos(t) − AudioBlock.arrivalNanos(t)
```

- Unit: milliseconds (after dividing by 1e6).
- Budgeted per frame by `FrameSchedule` / `ProcessingBudget`; a frame that
  exceeds its budget is logged and counted against this metric.

---

## False localization rate

Fraction of frames where the estimated position lies outside an acceptance disk
around the ground-truth position:

```text
flr(e) = | { t : err_loc(e, t) > R } | / N
```

- Default `R = 0.3 m` for the bundled scenarios.
- Used primarily for the reflection and noise-stress experiments.

---

## Signal-to-noise ratio (SNR)

Ratio of the per-frame source energy to the background-noise energy, expressed in
dB. SNR is not yet reported directly by snapshots; it is computed by the
benchmark harness from the configured emitter amplitude and
`Room2D#noiseAmplitude`. SNR is the key control variable for the noise-stress
experiment in [`experiments.md`](experiments.md).
