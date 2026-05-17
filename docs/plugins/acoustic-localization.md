# Experimental Acoustic Localization Plugin

## Summary

This plugin adds experimental acoustic localization workflows for weak, intermittent or
insect-like sound sources. It is not a production mosquito tracker; it is a research and demo
plugin for testing frequency tracking, TDOA estimation, microphone-array geometry and
visualization.

## Plugin status

Experimental.

|   Property   |                                         Value                                          |
|--------------|----------------------------------------------------------------------------------------|
| Plugin id    | `acoustic-localization`                                                                |
| Module       | `audio-experimental-acoustic`                                                          |
| Discovery    | Java `ServiceLoader` (`META-INF/services/org.hammer.audio.plugin.AudioAnalyzerPlugin`) |
| Plugin class | `org.hammer.audio.experimental.acoustic.plugin.AcousticLocalizationPlugin`             |

## Provided functionality

- insect-like demo signal presets;
- per-channel multi-peak frequency detection (`MultiPeakDetector`) and stable cross-channel
  frequency clustering (`FrequencyClusterer`);
- TDOA / GCC-PHAT based localization experiments (`CrossCorrelationTdoaEstimator`,
  `GccPhatTdoaEstimator`);
- delay-and-sum beamforming (`DelayAndSumBeamformer`);
- Doppler velocity estimation (`DopplerEstimator`, `SimpleDopplerEstimator`),
  per-microphone fusion (`MultiSensorDopplerEstimator`) and geometry-aware velocity
  reconstruction (`VelocityReconstructor`);
- microphone-array visualization and 2D source-position overview panel contributed via the
  plugin API;
- coherent real-time multi-source tracking via `TrackingPipeline`, `SourceTracker` and
  `Kalman2D` — see [`tracking.md`](acoustic-localization/tracking.md) for the pipeline
  design;
- reproducible validation scenarios via `SimulationScenarios` (single source, two close
  frequencies, noisy room, moving source, moving toward the array, moving across the array, two
  moving sources, reflected environment);
- bounded per-frame real-time budget (`FrameSchedule`, `ProcessingBudget`).

The legacy `MosquitoLocalizationPipeline` is still provided for one-shot per-frame
analyses; new work should use `TrackingPipeline`.

## Doppler based velocity estimation

The tracking pipeline stabilizes each detected source frequency over a bounded multi-frame
`FrequencyTrack` before computing Doppler velocity. For low source speeds relative to the speed of
sound it uses the small-velocity approximation:

```text
v_r ~= c * (f_observed - f_reference) / f_reference
```

`c` is configurable and defaults to 343 m/s. `f_reference` is the stabilized base-frequency
estimate, while `f_observed` is the current per-frame peak frequency. Positive radial velocity is
treated as motion toward the microphone, matching the sign of a positive frequency shift in the
plugin's simulator and reconstructor.

Per-microphone frequency peaks produce individual radial velocities. The default multi-sensor
estimator applies median absolute-deviation outlier rejection, then `VelocityReconstructor` solves a
weighted least-squares system from the microphone line-of-sight vectors to estimate a global
horizontal velocity vector. If the geometry is under-constrained, it falls back to the strongest
available radial direction.

## Combining Doppler and TDOA

The processing order is explicit:

1. audio frame input;
2. FFT peak detection;
3. cross-channel frequency clustering;
4. frequency stabilization;
5. TDOA consistency estimation;
6. delay-and-sum position estimation;
7. Doppler radial-velocity estimation and multi-sensor fusion;
8. source tracking.

TDOA and beamforming provide direction/position, Doppler provides radial motion, and the
`SourceTracker` blends Doppler-derived velocity with frame-to-frame position changes from its
Kalman filter. Each `TrackedSource` exposes stable frequency, observed frequency, position, 2D/3D
velocity, radial velocity, confidence and frequency-variance metrics for visualization.

## Validation metrics

Simulation and tests track:

- frequency stability over time (`FrequencyTrack.variance()`);
- Doppler error against known synthetic source velocity;
- reconstructed velocity-vector error from multi-microphone radial observations.

## UI integration

The plugin appears under the application's **Plugins** menu. The main application does not
contain any acoustic-localization-specific UI code: it only knows the
`AudioAnalyzerPlugin` contract from `audio-plugin-api` and renders contributions (views,
menu entries, descriptors) generically.

## What belongs to the core product

Reusable DSP, acquisition and geometry primitives (FFT, RMS/peak, spectrum, ring buffer,
microphone arrays, sample clocks, 2D geometry helpers) remain in the stable product
modules `audio-core`, `audio-dsp`, `audio-acquisition` and `audio-geometry`.

## What belongs to this plugin

Domain-specific presets, frequency-band defaults, mosquito/insect-style workflows,
visualizations and experiment logic remain inside this plugin.

## Hardware notes

TDOA-based localization requires a single shared sample clock across all microphones; see
the dedicated [synchronization document](acoustic-localization/synchronization.md) for the
mandatory requirements, the limitations of independent USB microphones and the per-array
timing-precision math. The detailed microphone setup discussion is in
[`docs/plugins/acoustic-localization/README.md`](acoustic-localization/README.md).

## Limitations

- no guaranteed mosquito detection;
- no species classification;
- synthetic validation does not prove real-world reliability;
- room reflections and noise can dominate the target signal;
- multiple weak sources are difficult to separate;
- accurate localization requires calibrated geometry and synchronization.
- unstable source frequencies (for example, insect wingbeat modulation) can masquerade as Doppler
  shift;
- multipath reflections can create contradictory per-microphone radial velocities;
- very small velocities may fall below FFT frequency resolution for short frames or low SNR.

## Roadmap

- ship the plugin as an external JAR loaded from a `plugins/` directory;
- add plugin settings (microphone geometry, frequency band, TDOA method);
- richer experiment workflows and visualizations contributed via the plugin API;
- optional heatmap / confidence display contributed as additional views.
