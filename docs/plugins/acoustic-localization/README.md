# Acoustic localization plugin

This package is an experimental research subsystem for real-time localization and tracking of small
flying sound sources such as mosquitoes. It is not a production mosquito detector.

## Pipeline overview

```text
MultiChannelAudioSource
  -> AudioBlock
  -> WingbeatFrequencyTracker
  -> TDOA estimator (cross-correlation or GCC-PHAT)
  -> DelayAndSumBeamformer
  -> AcousticLocalizationSnapshot
  -> AcousticDebugFrame / renderer
```

## Practical microphone setup

- Use a synchronized multi-channel interface; independent USB microphones usually drift and need
  calibration before TDOA is meaningful.
- Start with 2D arrays on a rigid frame and known coordinates in meters.
- Keep microphone spacing large enough for measurable delay but below room-reflection dominance.
- Record calibration clicks or chirps to estimate channel polarity, gain and sample offsets.

## Synchronization requirements

TDOA assumes one sample clock across channels. A one-sample error at 48 kHz is roughly 7.1 mm of
path difference in air, so clock drift and buffering jitter quickly dominate small arrays.

## DSP concepts

- **STFT / frequency analysis:** inspect short windows to find narrow-band wingbeat energy.
- **Harmonic detection:** insects often create harmonics; experiments should track fundamental and
  harmonics independently instead of hardcoding a species range.
- **Frequency tracking:** `WingbeatFrequencyTracker` finds a dominant peak in a configurable band.
- **Cross-correlation:** robust for clean delayed copies but weak under reflections and multi-source
  mixtures.
- **GCC-PHAT:** emphasizes phase alignment and is a candidate for reverberant rooms.
- **Beamforming:** `DelayAndSumBeamformer` scores candidate positions and returns a heatmap.

## Room acoustics considerations

Reflections, standing waves, air absorption, microphone frequency response and fan noise can be
larger than the target signal. The simulator includes configurable reflection gain and noise so
algorithms can be validated before real recordings are available.

## Simulation

`SimulatedMicrophoneArraySource` generates timestamped multi-channel `AudioBlock` values from:

- `Room2D` dimensions, reflection gain and noise;
- one or more `SoundEmitter2D` instances with position, velocity, frequency and amplitude;
- a deterministic random seed for repeatable tests.

Use it to evaluate localization precision, robustness and multi-source separation before collecting
real insect recordings.

## Visualization outputs

`AcousticLocalizationSnapshot` and `AcousticDebugFrame` expose:

- tracked frequency;
- TDOA estimates and path-difference constraints;
- beamforming heatmap points;
- estimated source position.

They are UI-agnostic and can drive Swing panels, web dashboards or offline notebooks.

## Limitations and non-goals

- No species classifier or production mosquito tracker is implemented.
- 2D geometry is supported first; 3D arrays are future work.
- The GCC-PHAT implementation is intentionally simple and should be replaced with a fuller
  frequency-domain implementation when needed.
- No GPU, distributed processing or real-time scheduler integration is included.
- No Python bridge is added; future interoperability should remain behind stable interfaces.

## Future research directions

- Multi-source separation using harmonic grouping and probabilistic frequency tracks.
- Full frequency-domain GCC-PHAT with sub-sample interpolation.
- 3D geometry and calibrated array files.
- Better reflection models and measured room impulse responses.
- Benchmark corpus with real and synthetic mosquito-like recordings.
