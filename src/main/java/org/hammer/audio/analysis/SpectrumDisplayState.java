package org.hammer.audio.analysis;

/**
 * Aggregated display state for the spectrum panel: combines the live spectrum with optional
 * peak-hold and exponential-average traces.
 *
 * <p>{@link #update(SpectrumSnapshot)} feeds the latest snapshot into all enabled traces. The
 * traces are exposed through accessor methods; callers decide whether and how to render them.
 *
 * <p>Instances are <strong>not thread-safe</strong>; intended to be driven by the Swing EDT.
 */
public final class SpectrumDisplayState {

  private final SpectrumAverager averager;
  private final PeakHoldSpectrum peakHold;

  private boolean averagingEnabled;
  private boolean peakHoldEnabled;
  private SpectrumSnapshot latest;

  /** Create a display state with averaging and peak-hold disabled by default. */
  public SpectrumDisplayState() {
    this(new SpectrumAverager(), new PeakHoldSpectrum());
  }

  /**
   * Create a display state with the supplied averager and peak-hold trace.
   *
   * @param averager spectrum averager; must not be {@code null}
   * @param peakHold peak-hold trace; must not be {@code null}
   */
  public SpectrumDisplayState(SpectrumAverager averager, PeakHoldSpectrum peakHold) {
    if (averager == null) {
      throw new IllegalArgumentException("averager must not be null");
    }
    if (peakHold == null) {
      throw new IllegalArgumentException("peakHold must not be null");
    }
    this.averager = averager;
    this.peakHold = peakHold;
  }

  /**
   * @return true if exponential averaging is enabled
   */
  public boolean isAveragingEnabled() {
    return averagingEnabled;
  }

  /**
   * Enable or disable exponential averaging. Disabling also resets the averager state so it does
   * not display a stale frozen trace next time it is re-enabled.
   *
   * @param enabled true to enable averaging
   */
  public void setAveragingEnabled(boolean enabled) {
    if (!enabled && averagingEnabled) {
      averager.reset();
    }
    this.averagingEnabled = enabled;
  }

  /**
   * @return true if peak-hold display is enabled
   */
  public boolean isPeakHoldEnabled() {
    return peakHoldEnabled;
  }

  /**
   * Enable or disable the peak-hold trace. Disabling also resets the held peaks.
   *
   * @param enabled true to enable peak hold
   */
  public void setPeakHoldEnabled(boolean enabled) {
    if (!enabled && peakHoldEnabled) {
      peakHold.reset();
    }
    this.peakHoldEnabled = enabled;
  }

  /**
   * @return the underlying averager (never {@code null})
   */
  public SpectrumAverager averager() {
    return averager;
  }

  /**
   * @return the underlying peak-hold trace (never {@code null})
   */
  public PeakHoldSpectrum peakHold() {
    return peakHold;
  }

  /**
   * @return the most recent snapshot pushed via {@link #update(SpectrumSnapshot)}, or {@code null}
   *     if none
   */
  public SpectrumSnapshot latestSnapshot() {
    return latest;
  }

  /**
   * Feed a new spectrum snapshot into the enabled traces.
   *
   * @param snapshot latest spectrum snapshot; may be {@code null} (no-op)
   */
  public void update(SpectrumSnapshot snapshot) {
    if (snapshot == null) {
      return;
    }
    this.latest = snapshot;
    float[] magnitudes = snapshot.magnitudes();
    if (averagingEnabled) {
      averager.update(magnitudes);
    }
    if (peakHoldEnabled) {
      peakHold.update(magnitudes);
    }
  }

  /** Reset the peak-hold trace without disabling it. */
  public void resetPeakHold() {
    peakHold.reset();
  }

  /** Reset the averager state without disabling it. */
  public void resetAverager() {
    averager.reset();
  }

  /** Reset both auxiliary traces and forget the latest snapshot. */
  public void clear() {
    averager.reset();
    peakHold.reset();
    latest = null;
  }
}
