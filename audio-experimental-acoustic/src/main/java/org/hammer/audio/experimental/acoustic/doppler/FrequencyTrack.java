package org.hammer.audio.experimental.acoustic.doppler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Stabilizes a source-frequency estimate over a bounded history of frames. */
public final class FrequencyTrack {

  private final int maxHistory;
  private final double smoothingAlpha;
  private final Deque<Double> history = new ArrayDeque<>();
  private double stableFrequency;

  /** Create a frequency stabilizer. */
  public FrequencyTrack(int maxHistory, double smoothingAlpha) {
    if (maxHistory < 1) {
      throw new IllegalArgumentException("maxHistory must be >= 1");
    }
    if (!Double.isFinite(smoothingAlpha) || smoothingAlpha <= 0.0 || smoothingAlpha > 1.0) {
      throw new IllegalArgumentException("smoothingAlpha must be in (0,1]");
    }
    this.maxHistory = maxHistory;
    this.smoothingAlpha = smoothingAlpha;
  }

  /** Add one observed frequency and return the updated stable frequency. */
  public double update(double observedFrequencyHz) {
    if (!Double.isFinite(observedFrequencyHz) || observedFrequencyHz < 0.0) {
      throw new IllegalArgumentException("observedFrequencyHz must be finite and >= 0");
    }
    if (history.isEmpty()) {
      stableFrequency = observedFrequencyHz;
    } else {
      stableFrequency =
          smoothingAlpha * observedFrequencyHz + (1.0 - smoothingAlpha) * stableFrequency;
    }
    history.addLast(observedFrequencyHz);
    while (history.size() > maxHistory) {
      history.removeFirst();
    }
    return stableFrequency;
  }

  /** Current reference frequency in Hz. */
  public double stableFrequency() {
    return stableFrequency;
  }

  /** Immutable history of observed frequencies in Hz. */
  public List<Double> history() {
    return List.copyOf(history);
  }

  /** Sample variance of the bounded frequency history. */
  public double variance() {
    if (history.size() < 2) {
      return 0.0;
    }
    double mean = 0.0;
    for (double value : history) {
      mean += value;
    }
    mean /= history.size();
    double sumSquares = 0.0;
    for (double value : history) {
      double diff = value - mean;
      sumSquares += diff * diff;
    }
    return sumSquares / (history.size() - 1);
  }

  /** Snapshot copy preserving the stable frequency. */
  public FrequencyTrackSnapshot snapshot() {
    return new FrequencyTrackSnapshot(stableFrequency, new ArrayList<>(history), variance());
  }

  /** Immutable frequency-track state for metrics and visualization. */
  public record FrequencyTrackSnapshot(
      double stableFrequency, List<Double> history, double variance) {

    /** Validate and copy fields. */
    public FrequencyTrackSnapshot {
      if (!Double.isFinite(stableFrequency) || stableFrequency < 0.0) {
        throw new IllegalArgumentException("stableFrequency must be finite and >= 0");
      }
      history = List.copyOf(history);
      if (!Double.isFinite(variance) || variance < 0.0) {
        throw new IllegalArgumentException("variance must be finite and >= 0");
      }
    }
  }
}
