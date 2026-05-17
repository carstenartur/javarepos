# Synchronization requirements

TDOA-based localization is fundamentally an inter-channel timing measurement. The accuracy of
every downstream stage in the [tracking pipeline](tracking.md) is bounded by the
sample-accurate alignment of the channels feeding it. This document captures the
synchronization assumptions the plugin makes and the practical implications for hardware
selection.

## Mandatory: a shared sample clock

All microphones must share a single sample clock. The plugin assumes that frame `n` on
channel A and frame `n` on channel B are captured at the same instant. Sources that satisfy
this requirement include:

- a single multi-channel audio interface (Focusrite Scarlett, RME Babyface, MOTU M-series,
  Behringer UMC, etc.) where every input is sampled from the same crystal;
- multiple interfaces locked together via word-clock or ADAT in a single ASIO / CoreAudio
  aggregate driven by one master;
- a dedicated multi-channel field recorder used in line-in mode.

In all cases the host audio framework must deliver the channels as a single multi-channel
stream so that the platform receives one `AudioBlock` per frame with `channels = N`.

## Independent USB microphones: experimental only with calibration

USB microphones each contain their own crystal and ADC. Even nominally identical devices
drift relative to each other at typical rates of a few PPM (parts per million), which
accumulates to:

- ~50 µs of drift over 50 seconds at 1 PPM (≈ 2 samples at 48 kHz);
- multi-millisecond drift over a few minutes — orders of magnitude larger than the
  inter-microphone delays the pipeline tries to estimate.

Two arbitrary USB microphones are therefore unsuitable for **precise** TDOA work unless repeated
calibration or explicit synchronization is added. They remain useful for non-TDOA demos
(frequency tracking on a single channel, sample-rate calibration of the simulator), and they can be
used experimentally for TDOA only if calibration beacons, drift estimation and residual-error checks
are part of the workflow. Without those controls, `TdoaEstimator` and downstream beamforming may
produce arbitrary results.

See [Physics and latency limits](physics-and-latency-limits.md) for the underlying path-difference,
drift, ambiguity and AR-latency bounds that motivate this guidance.

## Timing precision requirements

For a planar array of side `d`, a wavefront from a distant source crosses the array in at
most `d / c` seconds (`c` ≈ 343 m/s). The pipeline can only resolve TDOA at integer-sample
granularity (with sub-sample interpolation in the GCC-PHAT branch). Useful resolution
therefore requires the smaller of:

- `1 / sampleRate` ≤ desired-angular-resolution × `d / c`;
- `interChannelClockJitter < 0.5 / sampleRate`.

At 48 kHz this gives a per-sample resolution of ~20 µs and an angular resolution at the
broadside that grows linearly with `1 / d`. Concretely:

| Array spacing | Time-of-flight | Resolvable angles at 48 kHz |
|---------------|----------------|-----------------------------|
| 5 cm          | 146 µs         | ~7° steps                   |
| 15 cm         | 437 µs         | ~2.5° steps                 |
| 30 cm         | 875 µs         | ~1.3° steps                 |

These figures are upper bounds; reverberation, noise and channel mismatch reduce the
achievable accuracy further.

## Capture-side checklist

When evaluating a setup against this plugin, verify:

1. The audio framework exposes a single multi-channel device (not separate per-microphone
   devices wrapped into one). On Linux check `arecord -l`; on macOS check the Aggregate
   Device's clock source; on Windows pick a driver that exposes the device as one
   multi-channel interface.
2. The microphones are mechanically rigid relative to each other and their positions are
   measured to within a few millimeters; uncertainty in geometry directly translates into
   localization error.
3. The capture chain's per-channel gain mismatch is below ~3 dB and the channel order is
   known and stable across reboots.
4. The simulator (`SimulationScenarios`) is used as the ground-truth reference: the
   `singleSource()` and `twoCloseFrequencies()` scenarios are bit-exact for a fixed seed
   and provide the upper bound on what the pipeline can possibly achieve on the host.

If any of these conditions cannot be met, treat the localization output as
demonstration-grade only.
