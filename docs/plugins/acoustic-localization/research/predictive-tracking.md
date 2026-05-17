# Predictive tracking

> Exploratory notes on extending the `audio-experimental-acoustic` tracker from a
> per-frame state estimator toward a short-horizon predictor. Wording in this
> document is intentionally conservative ("may", "could", "exploratory"): the
> per-frame tracking and Kalman update path is implemented, but the prediction
> *application* layer (latency compensation, trigger scheduling, prediction
> evaluation) is currently a research target rather than a shipping feature.

## Why predictive tracking

A purely reactive tracker reports where a source *was* at the moment a frame was
captured. For latency-sensitive downstream stages (event triggering, external
sensor synchronization, visualization smoothing) it can be useful to estimate
where the source *will be* a short time `Δ` into the future.

Concrete motivations include:

- **Latency compensation.** Each frame carries a wall-clock processing delay
  surfaced by `TrackingSnapshot.processingNanos()`. A predicted future state
  may close that gap.
- **Smoothing.** A bounded forward extrapolation could feed a UI that wants a
  more recent-looking estimate without waiting for the next frame.
- **Event-driven sensing.** Pipelines that drive an external trigger (see
  [`event-driven-sensing.md`](event-driven-sensing.md)) need an estimate of the
  future state at the trigger moment, not at the last observation moment.

## What is already in the code

The building blocks for prediction already exist inside the tracker:

- `Kalman2D.predict(double dtSeconds)` advances the internal 2D
  constant-velocity state by `dt`. It is called once per frame inside the
  pipeline.
- `TrackedSource.positionMeters()` and `TrackedSource.velocityMetersPerSecond()`
  expose the per-frame point estimate from which a constant-velocity
  extrapolation can be computed by callers.
- `Kalman2D.positionVariance()` exposes a scalar uncertainty proxy that can be
  used to bound prediction confidence.

What is **not** yet implemented as a first-class API:

- a public `TrackedSource.predict(Δ)` helper;
- horizon-aware confidence reporting;
- a benchmark harness that evaluates prediction error against ground truth.

These are tracked as future work in [`experiments.md`](experiments.md).

## Prediction strategies

### Constant-velocity extrapolation

For a track snapshot, the simplest prediction is:

```text
predict_cv(track, Δ) = track.positionMeters() + Δ · track.velocityMetersPerSecond()
```

- Cheap, allocation-free and matches the Kalman process model.
- Assumes zero acceleration over the horizon. May be appropriate for the
  bundled scenarios in `SimulationScenarios` (sources move at constant
  velocity) but breaks for accelerating sources.

### Kalman-based prediction

A predictor that needs a richer state may copy the current `Kalman2D` and call
`predict(Δ)` on the copy. The same step is what the tracker itself performs
between frames; doing it on a copy avoids disturbing the live filter.

- Reuses the existing process-noise model.
- Exposes a position-variance estimate after the prediction step, which can
  drive confidence-aware downstream logic.
- Allocation footprint matches one `Kalman2D` instance per predicted horizon
  per track.

### Acceleration-aware variants (future work)

Both strategies above assume the underlying 2D constant-velocity model. Sources
with significant acceleration could be handled by:

- inflating process noise as a function of `Δ`;
- moving the filter to a constant-acceleration state model;
- or using a short polynomial fit over the recent track history.

None of these is implemented today.

## Prediction horizons

The bundled experiments and the metrics in
[`evaluation-metrics.md`](evaluation-metrics.md) focus on four nominal horizons:

| Horizon |                         Typical use case                          |
|---------|-------------------------------------------------------------------|
| 10 ms   | latency compensation within a single frame                        |
| 50 ms   | UI smoothing, light external scheduling                           |
| 100 ms  | event triggers with mid-range pipeline latency                    |
| 250 ms  | upper bound for short-term extrapolation in the bundled scenarios |

These horizons are chosen so that the longest horizon is still small compared
to typical scenario durations (0.5 s in `SimulationScenarios`) and so that the
constant-velocity assumption may still be defensible.

## Confidence-aware prediction

A useful predictor should report not only a future position but also a measure
of how much that position can be trusted. Sources of growing uncertainty over a
horizon `Δ` include:

- process noise accumulated by `Kalman2D.predict(Δ)`;
- frequency-track variance, surfaced via
  `track.frequencyVarianceHzSquared()` and used by the tracker to weight
  Doppler influence;
- per-microphone radial-velocity disagreement, surfaced via
  `track.radialVelocityStdDevMetersPerSecond()`.

A simple confidence proxy may multiply `track.confidence()` by a decay factor
derived from `Kalman2D.positionVariance()` after the prediction step. The
metric `decay(Δ)` defined in [`evaluation-metrics.md`](evaluation-metrics.md)
captures the spirit of this decay.

## Effects of noisy tracking

Prediction quality is bounded from below by tracking quality:

- A track whose position estimate jitters by `σ` meters per frame will show at
  least `σ` of error in any same-frame prediction; horizons make it worse, not
  better.
- A track whose velocity estimate jitters by `σ_v` m/s will accumulate roughly
  `Δ · σ_v` of additional position error per horizon.
- Identity flips (see *Tracking continuity* in
  [`evaluation-metrics.md`](evaluation-metrics.md)) invalidate predictions
  altogether for the affected source.

These limitations are summarised in
[`physics-limitations.md`](physics-limitations.md).

## Status and non-goals

- Status: exploratory. The per-frame Kalman state and the building blocks for
  prediction exist; the surface API and the benchmark harness do not yet.
- Non-goals: see *Explicit non-goals* in [`README.md`](README.md). Predictive
  tracking is investigated here for engineering reasons (latency, scheduling,
  smoothing) and is not intended to drive any biological, behavioural or
  pest-control application.

