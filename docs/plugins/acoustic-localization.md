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
- frequency tracking workflow (`WingbeatFrequencyTracker`);
- TDOA / GCC-PHAT based localization experiments (`CrossCorrelationTdoaEstimator`,
  `GccPhatTdoaEstimator`);
- delay-and-sum beamforming (`DelayAndSumBeamformer`);
- microphone-array visualization and 2D source-position overview panel contributed via the
  plugin API;
- experiment-oriented orchestration through `MosquitoLocalizationPipeline`.

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

TDOA-based localization requires synchronized channels. Unsynchronized USB microphones are
useful for demonstrations but not reliable for precise localization. See
[`docs/plugins/acoustic-localization/README.md`](acoustic-localization/README.md) for the
detailed microphone setup and synchronization discussion.

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

