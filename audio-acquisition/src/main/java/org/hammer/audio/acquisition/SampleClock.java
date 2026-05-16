package org.hammer.audio.acquisition;

/** Converts frame indices to timestamps for offline and simulated synchronized audio sources. */
public record SampleClock(float sampleRate, long startTimestampNanos) {

  /** Create a sample clock for a fixed-rate stream. */
  public SampleClock {
    if (!(sampleRate > 0.0f) || Float.isNaN(sampleRate) || Float.isInfinite(sampleRate)) {
      throw new IllegalArgumentException("sampleRate must be finite and > 0");
    }
  }

  /** Timestamp for the first sample of {@code frameIndex}. */
  public long timestampForFrame(long frameIndex) {
    if (frameIndex < 0) {
      throw new IllegalArgumentException("frameIndex must be >= 0");
    }
    return startTimestampNanos + Math.round(1_000_000_000.0 * frameIndex / sampleRate);
  }
}
