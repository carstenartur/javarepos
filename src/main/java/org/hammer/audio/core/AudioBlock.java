package org.hammer.audio.core;

import java.util.Objects;

/**
 * Immutable block of normalized audio frames flowing through the platform.
 *
 * <p>An {@code AudioBlock} is the canonical unit of audio data exchanged between the capture, ring
 * buffer, DSP, analysis and snapshot layers. It carries:
 *
 * <ul>
 *   <li>{@link #format()} — descriptor of the underlying stream
 *   <li>{@link #samples()} — non-interleaved {@code float[channels][frames]} samples in the
 *       normalized range {@code [-1.0f, 1.0f]}
 *   <li>{@link #frameIndex()} — running frame counter from stream start (monotonically increasing)
 *   <li>{@link #timestampNanos()} — capture timestamp in nanoseconds (relative to {@link
 *       System#nanoTime()})
 * </ul>
 *
 * <p>Blocks are immutable: the underlying {@code float[][]} is defensively copied on construction
 * and on access. This makes them safe to hand out to multiple consumers without coordination, at
 * the cost of an extra allocation per snapshot. Hot DSP paths that consume blocks should use {@link
 * #channelView(int)} which exposes the internal array read-only by contract (the array is still
 * shared, do not mutate).
 *
 * <p>Thread-safety: instances are immutable and safely publishable.
 *
 * @author refactoring
 */
public final class AudioBlock {

  private final AudioFormatDescriptor format;
  private final float[][] samples; // [channel][frame]
  private final int frames;
  private final long frameIndex;
  private final long timestampNanos;

  /**
   * Create an audio block by defensively copying the supplied samples.
   *
   * @param format audio format descriptor; must not be {@code null}
   * @param samples non-interleaved {@code float[channels][frames]} buffer; must match {@code
   *     format.channels()} and have a uniform per-channel length. Must not be {@code null}
   * @param frameIndex monotonically increasing frame counter from stream start
   * @param timestampNanos capture timestamp (nanoseconds, e.g. {@link System#nanoTime()})
   * @throws IllegalArgumentException if {@code samples} layout does not match {@code format}
   */
  public AudioBlock(AudioFormatDescriptor format, float[][] samples, long frameIndex,
      long timestampNanos) {
    this(format, samples, frameIndex, timestampNanos, true);
  }

  /**
   * Internal constructor that may skip the defensive copy. Used by {@link #wrap} for hot-path
   * production code that has just allocated a fresh array and is willing to surrender ownership.
   */
  private AudioBlock(AudioFormatDescriptor format, float[][] samples, long frameIndex,
      long timestampNanos, boolean copy) {
    this.format = Objects.requireNonNull(format, "format");
    Objects.requireNonNull(samples, "samples");
    if (samples.length != format.channels()) {
      throw new IllegalArgumentException(
          "samples.length (" + samples.length + ") != format.channels (" + format.channels() + ")");
    }
    int len = samples.length == 0 ? 0 : (samples[0] == null ? 0 : samples[0].length);
    for (int c = 0; c < samples.length; c++) {
      if (samples[c] == null) {
        throw new IllegalArgumentException("samples[" + c + "] is null");
      }
      if (samples[c].length != len) {
        throw new IllegalArgumentException(
            "All channels must have the same number of frames; channel "
                + c
                + " has "
                + samples[c].length
                + " expected "
                + len);
      }
    }
    if (copy) {
      float[][] cp = new float[samples.length][];
      for (int c = 0; c < samples.length; c++) {
        cp[c] = samples[c].clone();
      }
      this.samples = cp;
    } else {
      this.samples = samples;
    }
    this.frames = len;
    this.frameIndex = frameIndex;
    this.timestampNanos = timestampNanos;
  }

  /**
   * Wrap an already-owned {@code float[channels][frames]} array as an {@code AudioBlock} without
   * copying. The caller transfers ownership of {@code samples} and must not mutate it after the
   * call.
   *
   * @param format audio format descriptor; must not be {@code null}
   * @param samples non-interleaved per-channel buffer (ownership transferred); must not be {@code
   *     null}
   * @param frameIndex monotonically increasing frame counter from stream start
   * @param timestampNanos capture timestamp (nanoseconds)
   * @return a new immutable {@code AudioBlock} referencing the supplied array
   */
  public static AudioBlock wrap(AudioFormatDescriptor format, float[][] samples, long frameIndex,
      long timestampNanos) {
    return new AudioBlock(format, samples, frameIndex, timestampNanos, false);
  }

  /** @return the audio format descriptor */
  public AudioFormatDescriptor format() {
    return format;
  }

  /**
   * @return a defensive deep copy of the per-channel sample arrays. For zero-allocation read access
   *     in hot paths use {@link #channelView(int)}.
   */
  public float[][] samples() {
    float[][] cp = new float[samples.length][];
    for (int c = 0; c < samples.length; c++) {
      cp[c] = samples[c].clone();
    }
    return cp;
  }

  /**
   * Read-only view of one channel's sample array. The returned array is the block's internal
   * storage and <strong>must not be mutated</strong> by the caller. This exists for hot DSP loops
   * that must avoid per-block allocations.
   *
   * @param channel channel index, in {@code [0, channels)}
   * @return the internal per-channel sample array (do not mutate)
   * @throws IndexOutOfBoundsException if {@code channel} is out of range
   */
  public float[] channelView(int channel) {
    return samples[channel];
  }

  /** @return number of audio frames in this block (samples per channel) */
  public int frames() {
    return frames;
  }

  /** @return number of channels (convenience alias of {@code format().channels()}) */
  public int channels() {
    return format.channels();
  }

  /** @return monotonically increasing frame counter from stream start */
  public long frameIndex() {
    return frameIndex;
  }

  /** @return capture timestamp in nanoseconds (cf. {@link System#nanoTime()}) */
  public long timestampNanos() {
    return timestampNanos;
  }

  @Override
  public String toString() {
    return "AudioBlock[" + format + ", frames=" + frames + ", frameIndex=" + frameIndex + "]";
  }
}
