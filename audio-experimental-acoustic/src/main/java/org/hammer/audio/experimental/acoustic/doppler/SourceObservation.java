package org.hammer.audio.experimental.acoustic.doppler;

import java.util.List;
import java.util.Objects;
import org.hammer.audio.experimental.acoustic.tracking.DetectedPeak;
import org.hammer.audio.geometry.Vector2;

/** Per-frame source observation used by Doppler fusion and velocity reconstruction. */
public record SourceObservation(
    double observedFrequencyHz,
    double referenceFrequencyHz,
    Vector2 positionMeters,
    List<DetectedPeak> perMicrophonePeaks) {

  /** Validate and copy fields. */
  public SourceObservation {
    if (!Double.isFinite(observedFrequencyHz) || observedFrequencyHz < 0.0) {
      throw new IllegalArgumentException("observedFrequencyHz must be finite and >= 0");
    }
    if (!(referenceFrequencyHz > 0.0) || !Double.isFinite(referenceFrequencyHz)) {
      throw new IllegalArgumentException("referenceFrequencyHz must be finite and > 0");
    }
    Objects.requireNonNull(positionMeters, "positionMeters");
    Objects.requireNonNull(perMicrophonePeaks, "perMicrophonePeaks");
    perMicrophonePeaks = List.copyOf(perMicrophonePeaks);
  }
}
