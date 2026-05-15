package org.hammer.audio.analysis;

import java.util.Arrays;

/**
 * Immutable per-channel RMS/peak measurement snapshot produced by {@link RmsPeakAnalyzer}.
 *
 * <p>RMS and peak are reported in normalized linear units (the same units as
 * {@link org.hammer.audio.core.AudioBlock} samples, i.e. nominally {@code [0.0, 1.0]} for the peak
 * magnitude and {@code [0.0, 1.0]} for RMS of a unit-amplitude signal).
 *
 * @author refactoring
 */
public final class RmsPeakSnapshot implements AnalysisSnapshot {

  private final long sourceFrameIndex;
  private final long sourceTimestampNanos;
  private final float[] rms;
  private final float[] peak;

  /**
   * Create a new immutable snapshot. Arrays are defensively copied.
   *
   * @param sourceFrameIndex frame index from the analyzed block
   * @param sourceTimestampNanos timestamp from the analyzed block
   * @param rms per-channel RMS values
   * @param peak per-channel peak (max absolute value)
   */
  public RmsPeakSnapshot(long sourceFrameIndex, long sourceTimestampNanos, float[] rms,
      float[] peak) {
    if (rms.length != peak.length) {
      throw new IllegalArgumentException("rms and peak length must match");
    }
    this.sourceFrameIndex = sourceFrameIndex;
    this.sourceTimestampNanos = sourceTimestampNanos;
    this.rms = rms.clone();
    this.peak = peak.clone();
  }

  @Override
  public long sourceFrameIndex() {
    return sourceFrameIndex;
  }

  @Override
  public long sourceTimestampNanos() {
    return sourceTimestampNanos;
  }

  /** @return number of channels in this snapshot */
  public int channels() {
    return rms.length;
  }

  /** @return defensive copy of the per-channel RMS values */
  public float[] rms() {
    return rms.clone();
  }

  /** @return defensive copy of the per-channel peak values */
  public float[] peak() {
    return peak.clone();
  }

  /**
   * @param channel channel index
   * @return RMS of the given channel
   */
  public float rms(int channel) {
    return rms[channel];
  }

  /**
   * @param channel channel index
   * @return peak of the given channel
   */
  public float peak(int channel) {
    return peak[channel];
  }

  @Override
  public String toString() {
    return "RmsPeakSnapshot[frame=" + sourceFrameIndex + ", rms=" + Arrays.toString(rms) + ", peak="
        + Arrays.toString(peak) + "]";
  }
}
