package org.hammer;

/**
 * UI constants for consistent configuration across components.
 *
 * @author refactoring
 */
public final class UiConstants {

  /**
   * Refresh interval in milliseconds for UI component repainting.
   *
   * <p>This interval is used by timers in WaveformPanel, PhaseDiagramCanvas, and AudioAnalyseFrame
   * to periodically update the display. A value of 200ms provides smooth visual updates (5 FPS)
   * while minimizing CPU usage.
   */
  public static final int REFRESH_INTERVAL_MS = 200;

  private UiConstants() {
    // Utility class, prevent instantiation
  }
}
