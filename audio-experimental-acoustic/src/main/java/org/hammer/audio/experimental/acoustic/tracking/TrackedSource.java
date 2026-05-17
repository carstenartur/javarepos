package org.hammer.audio.experimental.acoustic.tracking;

import java.util.Objects;
import org.hammer.audio.geometry.Vector2;

/**
 * One acoustic source tracked over time.
 *
 * <p>{@code TrackedSource} carries the latest smoothed position and velocity estimate, the source
 * identity ({@link #id()}) that persists across frames, the most recently observed dominant
 * frequency and the frame index at which the source was last updated. The {@link #confidence()} is
 * a smoothed measure in {@code [0, 1]} that decays when the source is not observed for a frame.
 */
public record TrackedSource(
    int id,
    double frequencyHz,
    Vector2 positionMeters,
    Vector2 velocityMetersPerSecond,
    double confidence,
    long lastUpdatedFrameIndex,
    int observationCount) {

  /** Validate fields. */
  public TrackedSource {
    if (id < 0) {
      throw new IllegalArgumentException("id must be >= 0");
    }
    if (!Double.isFinite(frequencyHz) || frequencyHz < 0.0) {
      throw new IllegalArgumentException("frequencyHz must be finite and >= 0");
    }
    Objects.requireNonNull(positionMeters, "positionMeters");
    Objects.requireNonNull(velocityMetersPerSecond, "velocityMetersPerSecond");
    if (!Double.isFinite(confidence) || confidence < 0.0 || confidence > 1.0) {
      throw new IllegalArgumentException("confidence must be finite and in [0,1]");
    }
    if (observationCount < 1) {
      throw new IllegalArgumentException("observationCount must be >= 1");
    }
  }
}
