package org.hammer.audio.experimental.acoustic.tracking;

/**
 * Bounded per-frame timing budget for a real-time tracking pipeline.
 *
 * <p>{@code FrameSchedule} captures the contract that the tracking pipeline imposes on its host:
 * the host must deliver one {@link org.hammer.audio.core.AudioBlock} of {@code blockFrames} every
 * {@code blockFrames / sampleRate} seconds, and the pipeline must complete its processing within a
 * fraction ({@code maxLoadFraction}) of that period to stay real-time.
 *
 * <p>This record is informational; it does not schedule anything itself. It exists so callers have
 * a single, validated source of truth for the timing budget that documentation, monitoring and
 * {@link ProcessingBudget} can rely on.
 *
 * @param sampleRate sample rate in Hz, &gt; 0
 * @param blockFrames frames per processed block, &gt; 0
 * @param maxLoadFraction maximum fraction of one block period that pipeline processing may consume
 *     (between 0 exclusive and 1 inclusive); typical values are 0.5 for a comfortable budget and
 *     0.8 for an aggressive one
 */
public record FrameSchedule(double sampleRate, int blockFrames, double maxLoadFraction) {

  /** Validate sample rate, block size and load fraction. */
  public FrameSchedule {
    if (!(sampleRate > 0.0) || !Double.isFinite(sampleRate)) {
      throw new IllegalArgumentException("sampleRate must be finite and > 0");
    }
    if (blockFrames <= 0) {
      throw new IllegalArgumentException("blockFrames must be > 0");
    }
    if (!(maxLoadFraction > 0.0) || maxLoadFraction > 1.0 || !Double.isFinite(maxLoadFraction)) {
      throw new IllegalArgumentException("maxLoadFraction must be in (0,1]");
    }
  }

  /** Duration of one block in seconds. */
  public double blockDurationSeconds() {
    return blockFrames / sampleRate;
  }

  /** Maximum allowed processing time per block in nanoseconds. */
  public long maxProcessingNanos() {
    return Math.round(blockDurationSeconds() * 1.0e9 * maxLoadFraction);
  }
}
