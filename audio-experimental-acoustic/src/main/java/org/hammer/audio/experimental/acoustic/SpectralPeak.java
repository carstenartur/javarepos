package org.hammer.audio.experimental.acoustic;

/** Peak detected in a frequency band. */
public record SpectralPeak(double frequencyHz, double magnitude, double confidence) {

  /** Create a spectral peak. */
  public SpectralPeak {
    if (!Double.isFinite(frequencyHz) || frequencyHz < 0.0) {
      throw new IllegalArgumentException("frequencyHz must be finite and >= 0");
    }
    if (!Double.isFinite(magnitude) || magnitude < 0.0) {
      throw new IllegalArgumentException("magnitude must be finite and >= 0");
    }
    if (!Double.isFinite(confidence) || confidence < 0.0 || confidence > 1.0) {
      throw new IllegalArgumentException("confidence must be finite and in [0,1]");
    }
  }
}
