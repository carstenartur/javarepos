# Demo scenarios

Live demos that can be shown using the `acoustic-localization` plugin loaded into
`audio-app`. All demos use the deterministic scenarios from
`SimulationScenarios` so they look the same on every machine and on CI. The UI
side is the Swing overview `JPanel` that the plugin contributes via
`AcousticLocalizationPlugin#viewContributions()` (created by
`AcousticLocalizationPlugin::createOverviewPanel`); it consumes the immutable
debug model `AcousticDebugFrame` (a record built from
`AcousticLocalizationSnapshot`), not a JFrame.

For each demo we list the scenario, what the operator should highlight on screen
and the expected qualitative behaviour. Quantitative pass/fail belongs to
[`experiments.md`](experiments.md).

---

## Demo 1 — Single moving source

### Purpose

Show localization, Doppler velocity estimation and smooth identity-stable
tracking from end to end.

### Setup

- Scenario: `SimulationScenarios.movingSource()`.
- Source moves left-to-right across the room at 4 m/s.

### Visualization

- microphone-array overview panel with the live source position;
- the per-frame velocity vector reported by `VelocityReconstructor`;
- the frequency shift between observed and reference frequency, surfaced by the
  overview panel from the `AcousticDebugFrame` record.

### Expected

- a single persistent `TrackedSource` for the whole run;
- velocity vector roughly aligned with +x and ≈4 m/s magnitude;
- small Doppler shifts that flip sign as the source crosses the array centerline.

---

## Demo 2 — Two sources

### Purpose

Show frequency separation and independent tracking of two simultaneous emitters.

### Setup

- Scenario: `SimulationScenarios.twoMovingSources()` (220 Hz separation, ideal
  for stage demos) or `twoCloseFrequencies()` for the stress variant.

### Visualization

- two stable `TrackedSource#id` values, each rendered with its own color trail;
- two distinct cluster frequencies in the frequency panel.

### Expected

- no identity swaps during the scenario;
- separate trails crossing the room independently in the moving variant.

---

## Demo 3 — Reflection stress test

### Purpose

Show the limits of the current TDOA + beamforming stack under multipath
conditions.

### Setup

- Scenario: `SimulationScenarios.reflectedEnvironment()`.

### Visualization

- beamforming heatmap from `DelayAndSumBeamformer` showing secondary lobes
  caused by the reflected path;
- the resulting jitter on the tracked position.

### Expected

- visible secondary heatmap peaks near the reflective wall;
- moderate position jitter compared to the anechoic single-source demo, but the
  tracker should still recover the dominant peak each frame.

---

## Demo 4 — Noise stress test

### Purpose

Show graceful degradation as the noise floor approaches the source amplitude.

### Setup

- Scenario: `SimulationScenarios.noisyRoom()` as the starting point.
- For an interactive demo, the operator can wrap the scenario factory and sweep
  `Room2D.noiseAmplitude` over {0.0, 0.05, 0.10, 0.20}.

### Visualization

- the multi-peak spectrum panel filling with spurious peaks as noise rises;
- the tracked-source confidence indicator (frequency variance) growing.

### Expected

- stable tracking at low noise, increasingly intermittent matching at high
  noise, with the tracker eventually dropping the source instead of producing
  obviously wrong positions.

---

## Demo 5 — Synchronization failure (future)

### Purpose

Show why a shared sample clock matters for TDOA-based localization.

### Setup

Future demo — requires per-channel delay/drift injection that does not yet exist
in `SampleClock`. Once available, run `singleSource()` while injecting a slow
drift of e.g. 1 sample/second on one channel.

### Expected

- localization collapses to a wandering position even though the simulator
  produces a stationary source;
- the demo motivates the synchronization checklist in
  [`../synchronization.md`](../synchronization.md).

