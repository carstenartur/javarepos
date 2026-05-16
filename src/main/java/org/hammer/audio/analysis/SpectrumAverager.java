package org.hammer.audio.analysis;

import java.util.Objects;

/**
 * Exponential moving average over a one-sided FFT magnitude spectrum.
 *
 * <p>For each bin the displayed value is updated with {@code avg' = (1 - alpha) * avg + alpha * x}.
 * Smaller alpha values yield slower averaging and more flicker suppression; alpha {@code = 1.0}
 * disables averaging.
 *
 * <p>Instances are <strong>not thread-safe</strong>; callers must synchronize externally if shared.
 */
public final class SpectrumAverager {

  /** Default smoothing factor (moderate). */
  public static final float DEFAULT_ALPHA = 0.3f;

  private static final float[] EMPTY = new float[0];

  private float[] average;
  private float alpha;
  private int updates;

  /** Create an averager with the {@link #DEFAULT_ALPHA default alpha}. */
  public SpectrumAverager() {
    this(DEFAULT_ALPHA);
  }

  /**
   * Create an averager with the given smoothing factor.
   *
   * @param alpha smoothing factor in {@code (0, 1]}, where larger values track the input faster
   */
  public SpectrumAverager(float alpha) {
    setAlpha(alpha);
  }

  /**
   * @return current smoothing factor in {@code (0, 1]}
   */
  public float alpha() {
    return alpha;
  }

  /**
   * Update the smoothing factor.
   *
   * @param alpha in {@code (0, 1]}; {@code 1.0} disables averaging
   */
  public void setAlpha(float alpha) {
    if (!(alpha > 0f) || !(alpha <= 1f) || Float.isNaN(alpha)) {
      throw new IllegalArgumentException("alpha must be in (0,1], was " + alpha);
    }
    this.alpha = alpha;
  }

  /**
   * @return number of bins currently tracked, or {@code 0} if empty
   */
  public int binCount() {
    return average == null ? 0 : average.length;
  }

  /**
   * @return number of accepted updates since the last {@link #reset()}
   */
  public int updates() {
    return updates;
  }

  /**
   * @return defensive copy of the current averaged spectrum, or an empty array if no updates yet
   */
  public float[] average() {
    return average == null ? EMPTY : average.clone();
  }

  /**
   * Read-only access to the internal averaged spectrum. Callers must not mutate the returned array.
   * Intended for hot rendering paths that need to avoid per-frame allocations.
   *
   * @return the internal averaged spectrum (do not mutate), or an empty array if no updates yet
   */
  public float[] averageView() {
    return average == null ? EMPTY : average;
  }

  /**
   * Update the averaged spectrum with a new measurement. The first measurement initializes the
   * state. If the bin count changes (e.g. FFT size changed), the state is reset.
   *
   * @param magnitudes new one-sided magnitude spectrum; must not be {@code null}
   */
  public void update(float[] magnitudes) {
    Objects.requireNonNull(magnitudes, "magnitudes");
    if (average == null || average.length != magnitudes.length) {
      average = magnitudes.clone();
      updates = 1;
      return;
    }
    float oneMinusAlpha = 1f - alpha;
    for (int i = 0; i < average.length; i++) {
      average[i] = oneMinusAlpha * average[i] + alpha * magnitudes[i];
    }
    updates++;
  }

  /**
   * Reset the averaged spectrum, discarding all accumulated state. The next {@link
   * #update(float[])} call will re-seed the average from the new measurement.
   */
  public void reset() {
    average = null;
    updates = 0;
  }
}
