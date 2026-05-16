# Stereo delay and sound direction estimation

Audio Analyzer can estimate the time delay between the left and right channels of a stereo
microphone pair. With a known microphone spacing, that delay can be converted into an approximate
path-length difference and angle of arrival.

With a stereo microphone pair, Audio Analyzer estimates the inter-channel time delay and derives an
approximate angle of arrival. This can help investigate short, localized sounds such as clicks,
insects, fans or mechanical noises, but room reflections and microphone geometry strongly affect
accuracy.

## What it can do

- Detect whether the right channel appears delayed or advanced relative to the left channel.
- Report delay in samples and milliseconds.
- Convert delay to path-length difference using the configured speed of sound.
- Estimate an approximate angle of arrival when the microphone spacing is known.
- Reject silence, mono input, low-confidence correlations and delays that are physically impossible
  for the configured microphone spacing.

## What it cannot do

- It does not provide full 3D room localization from two microphones.
- It cannot reliably separate direct sound from reflections in reverberant rooms.
- It does not identify animal or insect species. The mosquito-like demo is only a synthetic
  localized high-frequency intermittent sound source.
- It assumes the left/right channels are synchronized and that the microphone spacing setting
  matches the real setup.

## How it works

`StereoDelayAnalyzer` reads stereo `AudioBlock` input and computes normalized cross-correlation
between the left and right channels across candidate lags. The strongest correlation determines the
estimated delay:

- positive delay: the right channel lags the left channel
- negative delay: the right channel leads the left channel

The delay is converted with:

- `delayMs = 1000 * delaySamples / sampleRate`
- `pathDifferenceMeters = speedOfSound * delaySamples / sampleRate`
- `angleDegrees = asin(pathDifference / microphoneSpacing)`

The default microphone spacing is `0.20 m`; the default speed of sound is `343 m/s`.

## Demo scenarios

Run the application, switch to **Demo mode**, and choose one of these presets:

- **Stereo delay test** — deterministic stereo signal with a fixed inter-channel delay.
- **Mosquito-like high-frequency burst** — intermittent high-frequency bursts with noise and a small
  echo component.
- **Moving chirp source** — chirp signal whose inter-channel delay changes over time.
- **50 Hz hum + harmonics** — stable mains-hum style bands for spectrum diagnosis.
- **Clipping test** — intentionally clipped waveform for level diagnostics.

Use the **Mic spacing m** field to match your microphone pair spacing. The UI displays delay,
confidence and approximate angle. Treat the result as a transparent diagnostic estimate, not a
precise position measurement.
