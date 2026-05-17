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
- microphone-array visualization and 2D source-position overview panel contributed via the
  plugin API;
- coherent real-time multi-source tracking via `TrackingPipeline`, `SourceTracker` and
  `Kalman2D` — see [`tracking.md`](acoustic-localization/tracking.md) for the pipeline
  design;
- reproducible validation scenarios via `SimulationScenarios` (single source, two close
  frequencies, noisy room, moving source, reflected environment);
- bounded per-frame real-time budget (`FrameSchedule`, `ProcessingBudget`).

The legacy `MosquitoLocalizationPipeline` is still provided for one-shot per-frame
analyses; new work should use `TrackingPipeline`.

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

## Roadmap

- ship the plugin as an external JAR loaded from a `plugins/` directory;
- add plugin settings (microphone geometry, frequency band, TDOA method);
- richer experiment workflows and visualizations contributed via the plugin API;
- optional heatmap / confidence display contributed as additional views.

