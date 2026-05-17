# Microphone array setup

Practical notes for building a real microphone array that mirrors the simulator
defaults used by [`SimulationScenarios.defaultArray()`](../../../../../audio-experimental-acoustic/src/main/java/org/hammer/audio/experimental/acoustic/simulation/SimulationScenarios.java).
These notes complement, and do not replace, the synchronization checklist in
[`../../synchronization.md`](../../synchronization.md).

## Reference geometry

The bundled simulator uses a planar 4-microphone array on a rigid frame:

| Mic | Position (m) | Channel |
|-----|--------------|---------|
| m0  | (1.35, 0.0)  | 0       |
| m1  | (1.65, 0.0)  | 1       |
| m2  | (1.35, 0.3)  | 2       |
| m3  | (1.65, 0.3)  | 3       |

- Inter-microphone spacing: 0.30 m in both axes.
- Array footprint: 30 × 30 cm square.
- Sample rate: 16 kHz.
- One sample at 16 kHz corresponds to ≈21.4 mm of path difference in air at
  c ≈ 343 m/s.

When you build the physical version, the coordinate frame must match the values
above so the simulator scenarios remain a valid ground truth for comparison
experiments.

## Hardware checklist

- **Synchronous capture device.** Use one multichannel audio interface with a
  single shared clock. Independent USB microphones drift and break TDOA before
  any other source of error.
- **Rigid frame.** Mount all microphones on a single rigid board; submillimetre
  position errors already swamp the integer-sample resolution of the current
  TDOA implementation.
- **Identical microphones.** Use the same model, batch and orientation; gain or
  phase mismatch directly maps to beamforming sidelobes.
- **Cable routing.** Match cable lengths and avoid mains/ground loops; hum at
  50/60 Hz and harmonics is loud relative to weak targets.
- **Stand and isolation.** Decouple the frame from the table or floor; vibration
  feeds into every channel coherently and corrupts beamforming.

## Calibration procedure

1. Measure the actual microphone coordinates with a caliper or laser distance
   tool and record them in meters, matching the scenario coordinate frame.
2. Play a known click or chirp from a fixed loudspeaker at a measured position.
3. Capture a short recording and feed it through `MosquitoLocalizationPipeline`
   with the measured geometry; the reported position should match the
   loudspeaker within the array spacing.
4. If it does not, re-check microphone polarity (channel order), the sign of the
   y-axis and per-channel sample offsets via cross-correlation of the calibration
   signal.

## Synchronization budget

A one-sample error at 16 kHz is ≈21 mm of equivalent path difference and at
48 kHz still ≈7 mm. For a 30 cm array that is roughly 7% of the maximum
geometric delay. Real microphone rigs should:

- capture calibration impulses before and after each session;
- bound clock drift to less than one sample over the longest recording;
- reject data when drift exceeds the localization error budget.

The current `SampleClock` only stores nominal timestamps and does not compensate
for drift, USB buffering jitter or per-channel latency. Drift compensation is
called out as future work in
[`../paper-outline.md`](../paper-outline.md#9-future-work).

## Mapping real recordings to simulator scenarios

Each scenario in [`../simulation-datasets/`](../simulation-datasets/) carries
explicit ground-truth positions, velocities and frequencies. A real-world rig
that reproduces the simulator geometry can be benchmarked by:

1. Replaying the same emitter geometry with a loudspeaker on a motion stage.
2. Recording with the synchronized array and saving the WAV to disk.
3. Running the offline benchmark harness against both the simulator output and
   the recorded WAV and computing the metrics in
   [`../evaluation-metrics.md`](../evaluation-metrics.md) for each.

This is the procedure that Experiment J in [`../experiments.md`](../experiments.md)
will eventually use.
