# Event-driven sensing

> Exploratory notes on using the acoustic tracker as the timing source for an
> event-driven sensing pipeline that may, in turn, trigger external
> acquisition systems (for example optical capture devices). Nothing in this
> document references a specific downstream project; the description is
> deliberately generic and decoupled.

## Motivation

A reactive sensing pipeline waits for an external system to report an event and
then analyses it. An event-driven pipeline does the inverse: it uses its own
estimates to *predict* a future event and to *trigger* an external system at
the right moment.

In the context of `audio-experimental-acoustic`, the acoustic tracker can
contribute:

- a continuously updated source-position estimate from `TrackingPipeline`;
- a constant-velocity prediction of the future position
  (see [`predictive-tracking.md`](predictive-tracking.md));
- a confidence proxy that a downstream scheduler can use to gate triggers.

## Reference architecture

The conceptual data flow is:

```text
AudioBlock (synchronized multichannel frame)
  → TrackingPipeline
  → TrackedSource (position, velocity, confidence)
  → trajectory estimation
  → future-position prediction (Δ horizon)
  → trigger decision (threshold on confidence and predicted state)
  → synchronized external capture (optical or other modality)
```

Every stage is implemented or planned inside this subproject:

|           Stage            |         Status          |                                  Code anchor                                  |
|----------------------------|-------------------------|-------------------------------------------------------------------------------|
| Audio acquisition          | Implemented             | `MultiChannelAudioSource`, `SimulatedMicrophoneArraySource`                   |
| Tracking                   | Implemented             | `TrackingPipeline`, `SourceTracker`, `Kalman2D`                               |
| Future-position prediction | Building blocks present | `Kalman2D.predict(Δ)`, see [`predictive-tracking.md`](predictive-tracking.md) |
| Trigger decision           | Future work             | not implemented                                                               |
| External capture           | Out of scope here       | belongs to the consuming system                                               |

The trigger decision and the external-capture interface are intentionally left
as a generic interface. The acoustic subproject's responsibility ends at
emitting a predicted future state with an associated confidence; whether and
how that state is turned into an external trigger is the consumer's choice.

## Timing budget

A meaningful trigger has to account for the full pipeline latency:

```text
t_trigger_decision = t_capture_block
                   + processing_pipeline_latency
                   + scheduler_overhead
                   + external_trigger_propagation
```

`TrackingSnapshot` exposes the per-frame processing time via
`snapshot.processingNanos()` and the original block timestamp via
`snapshot.sourceTimestampNanos()`, which together let the consumer compute a
realistic prediction horizon `Δ` that compensates for the pipeline path. The
overall latency formula is the one used by the *Latency* metric in
[`evaluation-metrics.md`](evaluation-metrics.md).

## Synchronization assumptions

External capture systems typically have their own clock. Treating the acoustic
tracker as a trigger source therefore inherits the synchronization concerns
from [`../synchronization.md`](../synchronization.md) and from
[`hardware/microphone-array-setup.md`](hardware/microphone-array-setup.md):

- the microphone array needs a shared sample clock or a validated calibrated virtual time base;
- the trigger-issuing component must know its own latency to the external
  device with sufficient precision;
- if the two clocks drift, calibration impulses (or a periodic resync signal)
  may be needed.

This is exactly the regime in which Experiment I (synchronization stress) in
[`experiments.md`](experiments.md) becomes interesting.

## Confidence-gated triggering

To avoid spurious triggers under noise or reflections, an event-driven pipeline
may gate trigger emission on a confidence threshold computed from:

- `track.confidence()` reported by the tracker;
- the decay factor `decay(Δ)` introduced in
  [`evaluation-metrics.md`](evaluation-metrics.md);
- consistency of per-microphone radial velocities, surfaced via
  `track.radialVelocityStdDevMetersPerSecond()`.

A reasonable default is to refuse to issue a trigger when any of these signals
crosses a configured limit. The exact policy is consumer-specific and is not
shipped here.

## Sensor fusion at the trigger boundary

An external sensor (for example an optical capture device) may itself observe
the same event after being triggered. Comparing the acoustic prediction with
the external observation enables:

- per-event trigger-timing error measurement (see *Trigger timing error* in
  [`evaluation-metrics.md`](evaluation-metrics.md));
- offline calibration of the latency model;
- cross-modal sensor fusion in a downstream system, which is again out of
  scope here.

## Status and non-goals

- Status: exploratory. The acoustic subproject does not ship a trigger
  scheduler or any external-capture integration today.
- Non-goals: this work explicitly does not target the manipulation of any
  biological subject, does not provide a pest-control system, and does not
  describe an acoustic weapon. See *Explicit non-goals* in
  [`README.md`](README.md).

