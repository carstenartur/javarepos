package org.hammer.audio.experimental.acoustic.doppler;

import java.util.List;
import java.util.Objects;
import org.hammer.audio.acquisition.Microphone;
import org.hammer.audio.acquisition.MicrophoneArray;
import org.hammer.audio.geometry.Vector2;

/** Reconstructs a global velocity vector from microphone radial velocities and array geometry. */
public final class VelocityReconstructor {

  private static final double SINGULAR_GEOMETRY_DETERMINANT_EPSILON = 1.0e-9;

  /**
   * Reconstruct horizontal velocity by weighted least squares over microphone line-of-sight axes.
   */
  public Vector3 reconstruct(
      List<RadialVelocityEstimate> radialVelocities,
      MicrophoneArray geometry,
      Vector2 sourcePositionMeters) {
    Objects.requireNonNull(radialVelocities, "radialVelocities");
    Objects.requireNonNull(geometry, "geometry");
    Objects.requireNonNull(sourcePositionMeters, "sourcePositionMeters");
    if (radialVelocities.isEmpty()) {
      return Vector3.ZERO;
    }

    double a00 = 0.0;
    double a01 = 0.0;
    double a11 = 0.0;
    double b0 = 0.0;
    double b1 = 0.0;
    for (RadialVelocityEstimate radial : radialVelocities) {
      Microphone microphone = geometry.microphone(radial.channel());
      Vector2 direction =
          sourcePositionMeters.minus(microphone.positionMeters()).normalized().scale(-1.0);
      double weight = radial.weight() > 0.0 ? radial.weight() : 1.0;
      a00 += weight * direction.x() * direction.x();
      a01 += weight * direction.x() * direction.y();
      a11 += weight * direction.y() * direction.y();
      b0 += weight * direction.x() * radial.radialVelocityMetersPerSecond();
      b1 += weight * direction.y() * radial.radialVelocityMetersPerSecond();
    }
    double determinant = a00 * a11 - a01 * a01;
    if (Math.abs(determinant) < SINGULAR_GEOMETRY_DETERMINANT_EPSILON) {
      double radial = fusedRadialVelocity(radialVelocities);
      Microphone microphone = geometry.microphone(radialVelocities.get(0).channel());
      return Vector3.from(
          sourcePositionMeters.minus(microphone.positionMeters()).normalized().scale(-radial));
    }
    double vx = (b0 * a11 - b1 * a01) / determinant;
    double vy = (a00 * b1 - a01 * b0) / determinant;
    return new Vector3(vx, vy, 0.0);
  }

  /** Weighted average radial velocity. */
  public double fusedRadialVelocity(List<RadialVelocityEstimate> radialVelocities) {
    Objects.requireNonNull(radialVelocities, "radialVelocities");
    double weighted = 0.0;
    double totalWeight = 0.0;
    for (RadialVelocityEstimate radial : radialVelocities) {
      double weight = radial.weight() > 0.0 ? radial.weight() : 1.0;
      weighted += radial.radialVelocityMetersPerSecond() * weight;
      totalWeight += weight;
    }
    return totalWeight > 0.0 ? weighted / totalWeight : 0.0;
  }
}
