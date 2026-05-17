# Research questions

> Open research questions for the `audio-experimental-acoustic` subproject. The
> questions are intentionally framed as open-ended ("can…", "how far…") rather
> than as design statements. Each is anchored on building blocks that already
> exist in the code or on clearly marked future work in
> [`experiments.md`](experiments.md) and [`predictive-tracking.md`](predictive-tracking.md).

## RQ1 — Predictive accuracy under typical pipeline latency

**Question.** Can future source positions, derived from `TrackedSource` and
`Kalman2D.predict(Δ)`, be predicted accurately enough to compensate for
realistic pipeline latency (typically tens of milliseconds for the bundled
scenarios)?

**Why it matters.** This is the precondition for any latency compensation or
event-driven sensing pipeline described in
[`event-driven-sensing.md`](event-driven-sensing.md).

**Method (proposed).** Run the prediction experiments described in
[`experiments.md`](experiments.md) on the moving-source scenarios in
`SimulationScenarios` and report the *future-position prediction error* metric
from [`evaluation-metrics.md`](evaluation-metrics.md) at horizons 10 ms, 50 ms,
100 ms and 250 ms.

## RQ2 — Usable prediction horizon for weak narrowband sources

**Question.** How far into the future can a weak moving narrowband source be
extrapolated before prediction uncertainty dominates the position estimate?

**Why it matters.** The answer bounds the useful prediction horizon for any
downstream scheduler and informs the choice of `Δ` in
[`predictive-tracking.md`](predictive-tracking.md).

**Method (proposed).** Sweep `Δ` over a fine grid and report both
*future-position prediction error* and *prediction drift over time*. The
crossover point at which drift exceeds an application-defined budget defines
the usable horizon for that scenario.

## RQ3 — Doppler-assisted short-term prediction

**Question.** Can per-microphone radial-velocity estimates produced by
`SimpleMultiSensorDopplerEstimator` and reconciled by `VelocityReconstructor`
improve short-term trajectory prediction compared to position-only Kalman
prediction?

**Why it matters.** A measured radial-velocity signal is, in principle,
independent of the discrete-time position update and may stabilise the velocity
component of the Kalman state.

**Method (proposed).** Compare the prediction error of:

- a position-only Kalman state updated without Doppler fusion;
- the current pipeline, which optionally blends Doppler velocity into the
  Kalman update via `dopplerVelocityWeight`.

Use the same moving-source scenarios in both runs and report the difference in
*future-position prediction error*.

## RQ4 — Acoustic sensing as an early-warning stage

**Question.** Can the acoustic tracker act as an early-warning stage for an
external sensing system that has higher fidelity but a smaller field of view
(for example, an optical capture device)?

**Why it matters.** This is the use case behind the architecture sketched in
[`event-driven-sensing.md`](event-driven-sensing.md). A positive answer would
motivate a stable trigger API; a negative answer would either bound the
scenarios in which the approach is viable or send us back to the prediction
metrics.

**Method (proposed).** Combine the prediction metrics above with the *trigger
timing error* metric from [`evaluation-metrics.md`](evaluation-metrics.md) and
report the fraction of frames in which the predicted trigger arrives within an
acceptance window relative to the realised ground-truth event time.

## RQ5 — Robustness ceiling for noise and reflections

**Question.** Up to what level of background noise and reflective energy does
the tracker maintain stable identity and acceptable localization error on the
bundled scenarios?

**Why it matters.** This is the ceiling that gates every downstream use case.
Today the answer is qualitative (Experiments G and H in
[`experiments.md`](experiments.md)); turning it quantitative is a prerequisite
for everything else.

**Method (proposed).** Sweep `Room2D.noiseAmplitude` and `reflectionGain` and
report *localization error*, *frequency stability* and *tracking continuity*
from [`evaluation-metrics.md`](evaluation-metrics.md).

## RQ6 — Synchronization sensitivity

**Question.** How much per-channel clock drift can the TDOA stage tolerate
before localization collapses?

**Why it matters.** This drives both the hardware checklist in
[`hardware/microphone-array-setup.md`](hardware/microphone-array-setup.md) and
the scope of Experiment I.

**Method (proposed).** Once a per-channel sample-delay/drift hook is added to
`SampleClock` (currently future work), repeat Experiment A with controlled
drift and report the same metrics.

## Status

All questions above are open. No claim is made that the subproject answers any
of them yet; the questions exist to guide future experiments and to keep the
documentation honest about what has and has not been measured.
