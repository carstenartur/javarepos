package org.hammer.audio.experimental.acoustic.doppler;

/** Exact source-motion Doppler estimator for a stationary microphone and moving source. */
public final class ExactDopplerEstimator implements DopplerEstimator {

  private final double speedOfSoundMetersPerSecond;

  /** Create an estimator with the default speed of sound. */
  public ExactDopplerEstimator() {
    this(SimpleDopplerEstimator.DEFAULT_SPEED_OF_SOUND_METERS_PER_SECOND);
  }

  /** Create an estimator with a configurable speed of sound. */
  public ExactDopplerEstimator(double speedOfSoundMetersPerSecond) {
    if (!(speedOfSoundMetersPerSecond > 0.0) || !Double.isFinite(speedOfSoundMetersPerSecond)) {
      throw new IllegalArgumentException("speedOfSoundMetersPerSecond must be finite and > 0");
    }
    this.speedOfSoundMetersPerSecond = speedOfSoundMetersPerSecond;
  }

  @Override
  public double estimateRadialVelocity(double observedFrequencyHz, double referenceFrequencyHz) {
    if (!(observedFrequencyHz > 0.0) || !Double.isFinite(observedFrequencyHz)) {
      throw new IllegalArgumentException("observedFrequencyHz must be finite and > 0");
    }
    if (!(referenceFrequencyHz > 0.0) || !Double.isFinite(referenceFrequencyHz)) {
      throw new IllegalArgumentException("referenceFrequencyHz must be finite and > 0");
    }
    return speedOfSoundMetersPerSecond * (1.0 - referenceFrequencyHz / observedFrequencyHz);
  }

  /** Configured speed of sound in m/s. */
  public double speedOfSoundMetersPerSecond() {
    return speedOfSoundMetersPerSecond;
  }
}
