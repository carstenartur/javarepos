package org.hammer.audio.experimental.acoustic.tracking;

import java.util.List;
import java.util.Objects;

/**
 * A cluster of {@link DetectedPeak detected peaks} from different channels that share a common
 * frequency, representing one acoustic source observed across the microphone array in a single
 * frame.
 *
 * <p>The {@link #centerFrequencyHz()} is the magnitude-weighted average of the contributing peaks.
 * The {@link #totalMagnitude()} sums their magnitudes and is used as a rough activity estimate
 * downstream. The {@link #peaks()} list is immutable.
 */
public record FrequencyCluster(
    double centerFrequencyHz, double totalMagnitude, List<DetectedPeak> peaks) {

  /** Validate and defensively copy the peaks list. */
  public FrequencyCluster {
    if (!Double.isFinite(centerFrequencyHz) || centerFrequencyHz < 0.0) {
      throw new IllegalArgumentException("centerFrequencyHz must be finite and >= 0");
    }
    if (!Double.isFinite(totalMagnitude) || totalMagnitude < 0.0) {
      throw new IllegalArgumentException("totalMagnitude must be finite and >= 0");
    }
    Objects.requireNonNull(peaks, "peaks");
    if (peaks.isEmpty()) {
      throw new IllegalArgumentException("peaks must not be empty");
    }
    peaks = List.copyOf(peaks);
  }

  /** Number of channels that contributed a peak to this cluster. */
  public int channelCount() {
    return peaks.size();
  }
}
