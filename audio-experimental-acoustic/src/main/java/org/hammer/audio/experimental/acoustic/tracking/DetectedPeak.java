package org.hammer.audio.experimental.acoustic.tracking;

/**
 * One spectral peak detected on a specific input channel during multi-peak FFT analysis.
 *
 * <p>The frequency is given in Hz (interpolated to sub-bin accuracy when possible). The magnitude
 * is the linear FFT magnitude on the one-sided spectrum. The signal-to-noise ratio is the ratio of
 * the peak magnitude to the median band magnitude on the same channel, used to drop peaks below a
 * configurable noise floor.
 */
public record DetectedPeak(
    int channel, double frequencyHz, double magnitude, double signalToNoiseRatio) {

  /** Validate fields. */
  public DetectedPeak {
    if (channel < 0) {
      throw new IllegalArgumentException("channel must be >= 0");
    }
    if (!Double.isFinite(frequencyHz) || frequencyHz < 0.0) {
      throw new IllegalArgumentException("frequencyHz must be finite and >= 0");
    }
    if (!Double.isFinite(magnitude) || magnitude < 0.0) {
      throw new IllegalArgumentException("magnitude must be finite and >= 0");
    }
    if (!Double.isFinite(signalToNoiseRatio) || signalToNoiseRatio < 0.0) {
      throw new IllegalArgumentException("signalToNoiseRatio must be finite and >= 0");
    }
  }
}
