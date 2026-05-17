package org.hammer.audio.experimental.acoustic.doppler;

/** Doppler-derived radial velocity for one microphone channel. */
public record RadialVelocityEstimate(
    int channel,
    double observedFrequencyHz,
    double referenceFrequencyHz,
    double radialVelocityMetersPerSecond,
    double weight) {

  /** Validate fields. */
  public RadialVelocityEstimate {
    if (channel < 0) {
      throw new IllegalArgumentException("channel must be >= 0");
    }
    if (!Double.isFinite(observedFrequencyHz) || observedFrequencyHz < 0.0) {
      throw new IllegalArgumentException("observedFrequencyHz must be finite and >= 0");
    }
    if (!(referenceFrequencyHz > 0.0) || !Double.isFinite(referenceFrequencyHz)) {
      throw new IllegalArgumentException("referenceFrequencyHz must be finite and > 0");
    }
    if (!Double.isFinite(radialVelocityMetersPerSecond)) {
      throw new IllegalArgumentException("radialVelocityMetersPerSecond must be finite");
    }
    if (!Double.isFinite(weight) || weight < 0.0) {
      throw new IllegalArgumentException("weight must be finite and >= 0");
    }
  }
}
