package org.hammer.audio.geometry;

/**
 * Hyperbola-like 2D TDOA constraint between two sensors, represented by a path-length difference.
 */
public record LocalizationConstraint2D(
    String firstSensorId, String secondSensorId, double pathDifferenceMeters, double confidence) {

  /** Create a reusable localization constraint without source-specific assumptions. */
  public LocalizationConstraint2D {
    if (firstSensorId == null || firstSensorId.isBlank()) {
      throw new IllegalArgumentException("firstSensorId must not be blank");
    }
    if (secondSensorId == null || secondSensorId.isBlank()) {
      throw new IllegalArgumentException("secondSensorId must not be blank");
    }
    if (firstSensorId.equals(secondSensorId)) {
      throw new IllegalArgumentException("sensor ids must be distinct");
    }
    if (!Double.isFinite(pathDifferenceMeters)) {
      throw new IllegalArgumentException("pathDifferenceMeters must be finite");
    }
    if (confidence < 0.0 || confidence > 1.0 || !Double.isFinite(confidence)) {
      throw new IllegalArgumentException("confidence must be finite and in [0,1]");
    }
  }
}
