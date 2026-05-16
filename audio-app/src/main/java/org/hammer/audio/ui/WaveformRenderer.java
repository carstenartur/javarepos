package org.hammer.audio.ui;

import org.hammer.audio.snapshot.WaveformSnapshot;

/**
 * Pure-function pixel scaling for waveform rendering.
 *
 * <p>This is the only place in the platform that knows about panel dimensions and pixel
 * coordinates. Audio capture, ring buffering, DSP and analysis all stay in normalized {@code float}
 * space; renderers in this package convert immutable {@link WaveformSnapshot}s into the {@code (x,
 * y)} integer arrays a Swing or JavaFX canvas can {@code drawPolyline}.
 *
 * <p>Renderers are stateless: the same instance can be invoked from any thread and reused across
 * snapshots. They allocate a fresh result on every call to keep the data immutable from the
 * caller's perspective; for high-rate rendering paths a future variant can accept caller-owned
 * scratch arrays.
 *
 * @author refactoring
 */
public final class WaveformRenderer {

  private WaveformRenderer() {
    // Utility class
  }

  /**
   * Compute X-coordinates that span {@code [0, panelWidth - 1]} for {@code points} samples.
   *
   * @param points number of samples on the X axis; must be {@code >= 0}
   * @param panelWidth target panel width in pixels
   * @return integer array of length {@code points}; values are monotonically non-decreasing
   */
  public static int[] computeXPoints(int points, int panelWidth) {
    int[] xs = new int[Math.max(0, points)];
    if (xs.length == 0) {
      return xs;
    }
    if (xs.length == 1) {
      xs[0] = 0;
      return xs;
    }
    final int panelW = panelWidth - 1;
    final int pointsM1 = xs.length - 1;
    for (int i = 0; i < xs.length; i++) {
      xs[i] = (int) ((long) panelW * i / pointsM1);
    }
    return xs;
  }

  /**
   * Convert one channel of a {@link WaveformSnapshot} into pixel-space Y-coordinates.
   *
   * <p>Samples in the snapshot are normalized to {@code [-1, 1]} (the audio domain). This method
   * maps {@code +1.0f → 0} (top) and {@code -1.0f → panelHeight} (bottom), so the centre of the
   * panel corresponds to silence.
   *
   * <p>If the snapshot legitimately has zero frames an empty array is returned. Contract violations
   * on the inputs throw eagerly so that misconfigured rendering code surfaces as a clear failure
   * rather than silently drawing nothing.
   *
   * @param snapshot waveform snapshot; never {@code null}
   * @param channel channel index, in {@code [0, snapshot.channels())}
   * @param panelHeight target panel height in pixels; must be {@code > 0}
   * @return integer Y-coordinates of length {@code snapshot.frames()}
   * @throws IllegalArgumentException if {@code panelHeight <= 0}
   * @throws IndexOutOfBoundsException if {@code channel} is outside {@code [0,
   *     snapshot.channels())}
   */
  public static int[] computeYPoints(WaveformSnapshot snapshot, int channel, int panelHeight) {
    if (panelHeight <= 0) {
      throw new IllegalArgumentException("panelHeight must be > 0, was " + panelHeight);
    }
    if (channel < 0 || channel >= snapshot.channels()) {
      throw new IndexOutOfBoundsException(
          "channel " + channel + " out of range [0, " + snapshot.channels() + ")");
    }
    float[] samples = snapshot.channelView(channel);
    int[] ys = new int[samples.length];
    final float halfH = panelHeight / 2f;
    for (int i = 0; i < samples.length; i++) {
      float n = samples[i];
      if (n > 1f) {
        n = 1f;
      } else if (n < -1f) {
        n = -1f;
      }
      ys[i] = Math.round(halfH - n * halfH);
    }
    return ys;
  }

  /**
   * Convert all channels at once.
   *
   * <p>Returns an empty outer array when the snapshot has zero channels (e.g. {@link
   * WaveformSnapshot#EMPTY}); otherwise delegates to {@link #computeYPoints} per channel and
   * propagates any contract violation it raises.
   *
   * @param snapshot waveform snapshot
   * @param panelHeight target panel height in pixels; must be {@code > 0}
   * @return integer Y-coordinate arrays, one per channel
   * @throws IllegalArgumentException if {@code panelHeight <= 0} and the snapshot has at least one
   *     channel
   */
  public static int[][] computeYPointsAllChannels(WaveformSnapshot snapshot, int panelHeight) {
    int[][] result = new int[snapshot.channels()][];
    for (int c = 0; c < snapshot.channels(); c++) {
      result[c] = computeYPoints(snapshot, c, panelHeight);
    }
    return result;
  }
}
