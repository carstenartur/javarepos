package org.hammer.audio.experimental.acoustic.doppler;

/** Estimates radial source velocity from Doppler frequency shift. */
public interface DopplerEstimator {

  /** Estimate radial velocity in m/s from observed and reference frequencies in Hz. */
  double estimateRadialVelocity(double observedFrequencyHz, double referenceFrequencyHz);
}
