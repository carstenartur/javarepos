# Physics and latency limits for acoustic localization

This page explains the hard limits that apply to acoustic localization, Doppler/velocity estimation
and AR visualization for weak flying sound sources such as insect-like emitters. It is intentionally
conservative: it describes what can be improved, what can only be calibrated, and what no
implementation can overcome.

## Four classes of limits

1. **Hard physical limits:** finite speed of sound, finite aperture, finite observation time,
   reflections and weak-signal SNR limits. No implementation can remove these.
2. **Practical consumer-hardware limits:** phone microphones, independent USB microphones, small
   microphone spacing, OS buffering and display latency. Better hardware can reduce these.
3. **Calibration-reducible limits:** geometry error, channel gain mismatch, fixed offsets and some
   drift terms. Calibration can reduce them, but not eliminate residual error.
4. **AR rendering limits:** capture, processing, tracking, rendering and display all add latency. A
   predicted overlay is only as good as the motion model and confidence estimate behind it.

Nothing on this page implies guaranteed mosquito tracking or guaranteed exact AR overlay.

## Speed of sound and information latency

Acoustic localization always works on information that arrived at the microphones after the sound
propagated through air. That propagation delay is itself a hard lower bound.

- Speed of sound in air is approximately `c ≈ 343 m/s` at room temperature.
- A source 1 m away cannot be observed acoustically before roughly `1 / 343 s ≈ 2.9 ms`.
- A source 3 m away already adds roughly `8.7 ms` of propagation delay before capture and
  processing are considered.

This means the system never knows where a moving source is **now**. It only knows where the source
was when the wavefront reached the microphones.

## Sampling rate, time resolution and TDOA error

Time-difference-of-arrival (TDOA) estimation is quantized by the sample period unless sub-sample
interpolation is both implemented and reliable.

- Sample period is `Δt = 1 / sampleRate`.
- At 48 kHz, `Δt ≈ 20.8 µs`.
- One sample at 48 kHz corresponds to about `c / 48000 ≈ 7.1 mm` acoustic path difference.

That 7.1 mm figure is a **hard discretization scale** for integer-sample TDOA. Sub-sample fitting
may reduce average error in good conditions, but it does not remove the need for sufficient SNR,
bandwidth and synchronization.

Practical implications:

- small arrays can have true delays of only a few samples, so one wrong sample is already a large
  angular error;
- integer-sample TDOA is often too coarse for precise localization of weak moving sources;
- sub-sample interpolation is sensitive to noise, multipath, spectral leakage and model mismatch.

## Microphone spacing and array geometry

Array geometry sets another hard limit: the larger the microphone spacing, the larger the maximum
measurable path difference. But larger spacing also increases spatial aliasing risk, packaging
difficulty and sensitivity to reflections.

- Small spacing reduces the maximum TDOA and makes delay estimation harder.
- Large spacing improves delay leverage but can increase ambiguity and multipath sensitivity.
- Poorly measured geometry directly becomes localization bias.

Calibration can reduce geometry error, but it cannot make a badly chosen aperture behave like a
larger or better-conditioned array. For weak flying sources, rigid mounting and millimeter-scale
measurement matter because geometry errors can be comparable to the path-difference resolution.

## SNR, frequency resolution and Doppler velocity limits

Doppler estimation is limited by observation time, frequency resolution and SNR.

- Frequency resolution is `delta_f = sampleRate / FFT_size`.
- Larger FFT windows improve `delta_f`, but increase latency and assume the signal stays coherent
  over the window.
- Weak sources, blade/wingbeat modulation, harmonics and clutter often broaden peaks enough that
  the practical frequency error is worse than one FFT bin.

An approximate radial-velocity resolution is:

`delta_v ≈ c * delta_f / f_reference`

where `f_reference` is the relevant acoustic carrier or narrow-band feature.

Examples:

- At `sampleRate = 48 kHz` and `FFT_size = 4096`, `delta_f ≈ 11.7 Hz`.
- For `f_reference = 1 kHz`, this gives `delta_v ≈ 343 * 11.7 / 1000 ≈ 4.0 m/s`.
- For `f_reference = 10 kHz`, the same FFT gives `delta_v ≈ 0.40 m/s`.

So Doppler velocity estimation for insect-like sources is usually limited less by formula elegance
than by whether there is a stable narrow-band reference with enough SNR and enough observation time.

## Reflections, multipath and ambiguity

Reflections and multipath are hard environmental limits, not just implementation bugs.

- A reflected path can be stronger than the direct path.
- Multiple candidate TDOA peaks may fit the same data.
- Delay-and-sum or GCC-PHAT peaks can represent room geometry, not source truth.
- Weak flying sources near walls, tables, windows or the user can create unstable or mirrored
  solutions.

Calibration can characterize some rooms or reject some states, but it cannot guarantee unique
localization in reflective scenes. Ambiguity must therefore be treated as part of the output.

## Independent USB microphones, clock drift and calibration beacons

Independent USB microphones are not suitable for **precise** TDOA unless synchronization or repeated
calibration is added.

Each USB microphone normally has its own ADC clock. Even small parts-per-million drift accumulates
into timing error over time, and the resulting error can exceed the true inter-microphone delay of a
small array.

Nuanced guidance:

- independent USB microphones are usually unsuitable for precise TDOA out of the box;
- they can still be used experimentally if repeated calibration beacons, drift estimation and
  residual-error checks are part of the workflow;
- the system should reject or down-rank estimates when the residual synchronization error exceeds the
  localization error budget.

Calibration beacons can help estimate relative offsets and drift before, during or after a run, but
they do not create perfect synchronization. Residual error remains.

## Shared-clock multi-channel interfaces as preferred hardware

A shared-clock multi-channel interface is the preferred hardware because all channels are sampled
from one clock domain.

This does not eliminate every error source, but it removes one of the most damaging ones:
independent inter-device drift. Shared-clock capture is therefore the right baseline for serious
TDOA, beamforming and Doppler experiments.

Even with preferred hardware, calibration is still needed for:

- microphone positions;
- polarity and gain mismatch;
- fixed channel delay offsets;
- mechanical rigidity and repeatability.

## End-to-end latency: audio capture → processing → tracking → AR rendering → display

For AR, the relevant latency is not only acoustic propagation. The user sees an overlay after the
whole stack has finished:

`audio capture → buffering → processing → tracking/filtering → AR rendering → display scanout`

Practical consumer systems can easily accumulate tens of milliseconds across that chain. That means
the displayed overlay is almost always behind the real moving source unless prediction is applied.

Typical contributors:

- audio block size and driver buffering;
- FFT window length and overlap;
- CPU/GPU scheduling and OS jitter;
- tracking/filter update cadence;
- AR compositor and display refresh latency.

No implementation can make end-to-end latency disappear entirely.

## Prediction/extrapolation for AR overlays

Prediction can reduce apparent lag, but only under motion-model assumptions.

- A constant-velocity predictor works only while that approximation stays valid.
- Acceleration, turns, hovering, occlusion or identity swaps quickly invalidate extrapolation.
- A visually stable overlay can still be wrong.

Concrete scale examples:

- `1 ms` latency at `1 m/s` motion requires `1 mm` prediction.
- `50 ms` latency at `1 m/s` requires `5 cm` prediction.

Those numbers become larger at higher source speed, and they apply **in addition** to localization
error and synchronization error. Predictions are therefore not guaranteed truth; they are model-based
guesses for a display time.

## Why AR should display uncertainty regions, not exact points

AR should not imply more precision than the sensing and prediction stack can justify. An exact point
marker can falsely suggest certainty when the actual solution is a region, a ridge or multiple
hypotheses.

For research, debugging and calibration, explicit uncertainty regions are often the clearest choice.
For AR-assisted use, permanent ellipses can reduce usability, so practical overlays may encode
confidence more implicitly through marker stability, color, opacity, halo size, trail behaviour or
automatic suppression under low-confidence conditions.

Recommended interpretation:

- **high confidence:** small stable reticle;
- **medium confidence:** subtle halo, pulse or transparency change;
- **low confidence:** larger diffuse region or multiple hypotheses;
- **very low confidence:** hide the overlay instead of pretending exactness.

So the principle is not “always draw the same ellipse”. The principle is “never pretend exactness
when latency, ambiguity or tracking confidence say otherwise”.

## Practical error-budget examples

These examples show how different limits add up.

### Example 1: integer-sample TDOA at 48 kHz

- 1-sample delay error corresponds to about `7.1 mm` path-difference error.
- If geometry calibration is off by another few millimeters, the total spatial bias can already be on
  the order of centimeters depending on array layout and source direction.
- Weak-source SNR or multipath can easily add more error than the nominal sample quantization.

### Example 2: Doppler velocity resolution

- `delta_f = sampleRate / FFT_size`.
- With `48 kHz / 4096`, `delta_f ≈ 11.7 Hz`.
- Using `delta_v ≈ c * delta_f / f_reference`, the achievable radial-velocity resolution depends
  strongly on the tracked reference frequency.
- In practice, SNR, spectral leakage and non-stationary source motion usually widen the error beyond
  the bin-limited approximation.

### Example 3: AR prediction budget

- Suppose acoustic propagation is `3 ms`, audio/processing/tracking adds `20 ms`, and AR rendering
  plus display adds `27 ms`.
- End-to-end latency is then about `50 ms`.
- At `1 m/s`, that already requires `5 cm` of prediction just to compensate latency.
- If tracking confidence is poor, the correct UI response may be to enlarge/simplify the overlay or
  suppress it rather than show a precise-looking dot.

## Bottom line

Acoustic localization of weak flying sound sources is bounded by physics first, hardware second and
calibration quality third. AR visualization adds a separate latency-and-prediction problem on top of
the sensing problem. The right engineering goal is therefore not “exact guaranteed tracking”, but a
conservative system that communicates confidence honestly and avoids false precision.
