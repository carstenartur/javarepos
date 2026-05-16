package org.hammer.audio.analysis;

import java.util.Arrays;
import java.util.Objects;

/**
 * Peak-hold trace over a one-sided FFT magnitude spectrum.
 *
 * <p>For each frequency bin the stored value is the maximum magnitude observed across all updates
 * since the last {@link #reset()}, optionally decayed each update by an exponential factor so old
 * peaks slowly fade rather than persist forever.
 *
 * <p>Instances are <strong>not thread-safe</strong>; callers must synchronize externally if shared.
 */
public final class PeakHoldSpectrum {

  /** Default per-update multiplicative decay factor (no decay). */
  public static final float DEFAULT_DECAY_FACTOR = 1.0f;

  private float[] peaks;
  private float decayFactor;
  private int updates;

  /** Create a peak-hold trace with no decay (sticky peaks). */
  public PeakHoldSpectrum() {
    this(DEFAULT_DECAY_FACTOR);
  }

  /**
   * Create a peak-hold trace with the given per-update decay factor.
   *
   * @param decayFactor multiplicative factor applied to each held peak before max-merging the new
   *     spectrum, in {@code [0, 1]}. {@code 1.0} disables decay.
   */
  public PeakHoldSpectrum(float decayFactor) {
    setDecayFactor(decayFactor);
  }

  /**
   * @return current per-update decay factor in {@code [0, 1]}
   */
  public float decayFactor() {
    return decayFactor;
  }

  /**
   * Update the per-update decay factor.
   *
   * @param decayFactor in {@code [0, 1]}, where {@code 1.0} disables decay
   */
  public void setDecayFactor(float decayFactor) {
    if (!(decayFactor >= 0f) || !(decayFactor <= 1f) || Float.isNaN(decayFactor)) {
      throw new IllegalArgumentException("decayFactor must be in [0,1], was " + decayFactor);
    }
    this.decayFactor = decayFactor;
  }

  /**
   * @return number of accepted updates since the last {@link #reset()}
   */
  public int updates() {
    return updates;
  }

  /**
   * @return number of bins currently tracked, or {@code 0} if empty
   */
  public int binCount() {
    return peaks == null ? 0 : peaks.length;
  }

  /**
   * @return defensive copy of the held peak magnitudes, or an empty array if no updates yet
   */
  public float[] peaks() {
    return peaks == null ? new float[0] : peaks.clone();
  }

  /**
   * Read-only access to the internal peak magnitudes array. Callers must not mutate the returned
   * array. Intended for hot rendering paths that need to avoid per-frame allocations.
   *
   * @return the internal peak magnitudes array (do not mutate), or an empty array if no updates yet
   */
  public float[] peaksView() {
    return peaks == null ? new float[0] : peaks;
  }

  /**
   * Update the peak-hold trace from a new magnitude spectrum. If the bin count changes (e.g. FFT
   * size changed), the existing peaks are reset.
   *
   * @param magnitudes new one-sided magnitude spectrum; must not be {@code null}
   */
  public void update(float[] magnitudes) {
    Objects.requireNonNull(magnitudes, "magnitudes");
    if (peaks == null || peaks.length != magnitudes.length) {
      peaks = magnitudes.clone();
      updates = 1;
      return;
    }
    if (decayFactor < 1f) {
      for (int i = 0; i < peaks.length; i++) {
        peaks[i] *= decayFactor;
        if (magnitudes[i] > peaks[i]) {
          peaks[i] = magnitudes[i];
        }
      }
    } else {
      for (int i = 0; i < peaks.length; i++) {
        if (magnitudes[i] > peaks[i]) {
          peaks[i] = magnitudes[i];
        }
      }
    }
    updates++;
  }

  /** Reset the peak-hold trace, discarding all stored peaks. */
  public void reset() {
    if (peaks != null) {
      Arrays.fill(peaks, 0f);
    }
    updates = 0;
  }
}
