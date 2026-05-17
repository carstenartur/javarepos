package org.hammer.audio.experimental.acoustic.doppler;

/** Small-velocity Doppler estimator using v_r ~= c * (f_observed - f_reference) / f_reference. */
public final class SimpleDopplerEstimator implements DopplerEstimator {

  /** Default speed of sound in air in m/s. */
  public static final double DEFAULT_SPEED_OF_SOUND_METERS_PER_SECOND = 343.0;

  private final double speedOfSoundMetersPerSecond;

  /** Create an estimator with the default speed of sound. */
  public SimpleDopplerEstimator() {
    this(DEFAULT_SPEED_OF_SOUND_METERS_PER_SECOND);
  }

  /** Create an estimator with a configurable speed of sound. */
  public SimpleDopplerEstimator(double speedOfSoundMetersPerSecond) {
    if (!(speedOfSoundMetersPerSecond > 0.0) || !Double.isFinite(speedOfSoundMetersPerSecond)) {
      throw new IllegalArgumentException("speedOfSoundMetersPerSecond must be finite and > 0");
    }
    this.speedOfSoundMetersPerSecond = speedOfSoundMetersPerSecond;
  }

  @Override
  public double estimateRadialVelocity(double observedFrequencyHz, double referenceFrequencyHz) {
    if (!Double.isFinite(observedFrequencyHz) || observedFrequencyHz < 0.0) {
      throw new IllegalArgumentException("observedFrequencyHz must be finite and >= 0");
    }
    if (!(referenceFrequencyHz > 0.0) || !Double.isFinite(referenceFrequencyHz)) {
      throw new IllegalArgumentException("referenceFrequencyHz must be finite and > 0");
    }
    return speedOfSoundMetersPerSecond * (observedFrequencyHz - referenceFrequencyHz) / referenceFrequencyHz;
  }

  /** Configured speed of sound in m/s. */
  public double speedOfSoundMetersPerSecond() {
    return speedOfSoundMetersPerSecond;
  }
}
