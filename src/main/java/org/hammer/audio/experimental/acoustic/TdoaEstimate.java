package org.hammer.audio.experimental.acoustic;

import org.hammer.audio.geometry.LocalizationConstraint2D;

/** Time-difference-of-arrival estimate for one microphone pair. */
public record TdoaEstimate(
    String firstMicrophoneId,
    String secondMicrophoneId,
    int delaySamples,
    double delaySeconds,
    double pathDifferenceMeters,
    double confidence) {

  /** Create a TDOA estimate. */
  public TdoaEstimate {
    if (firstMicrophoneId == null || firstMicrophoneId.isBlank()) {
      throw new IllegalArgumentException("firstMicrophoneId must not be blank");
    }
    if (secondMicrophoneId == null || secondMicrophoneId.isBlank()) {
      throw new IllegalArgumentException("secondMicrophoneId must not be blank");
    }
    if (!Double.isFinite(delaySeconds) || !Double.isFinite(pathDifferenceMeters)) {
      throw new IllegalArgumentException("delay and path difference must be finite");
    }
    if (!Double.isFinite(confidence) || confidence < 0.0 || confidence > 1.0) {
      throw new IllegalArgumentException("confidence must be finite and in [0,1]");
    }
  }

  /** Convert to a reusable 2D localization constraint. */
  public LocalizationConstraint2D asConstraint() {
    return new LocalizationConstraint2D(firstMicrophoneId, secondMicrophoneId, pathDifferenceMeters, confidence);
  }
}
