package org.hammer.audio.experimental.acoustic.doppler;

import java.util.List;

/** Estimates per-microphone radial velocities from one source observation. */
public interface MultiSensorDopplerEstimator {

  /** Return outlier-filtered radial velocity estimates for contributing microphone channels. */
  List<RadialVelocityEstimate> estimateRadialVelocities(SourceObservation observation);
}
