package org.hammer.audio.snapshot;

import org.hammer.audio.core.AudioBlock;

/**
 * Immutable, UI-friendly snapshot of one block of waveform data.
 *
 * <p>Unlike the legacy {@link org.hammer.audio.WaveformModel}, which carried pre-computed pixel
 * coordinates and thus mixed audio and rendering concerns, a {@code WaveformSnapshot} holds only
 * normalized {@code float} samples plus the source metadata. Pixel scaling for any specific
 * canvas is performed by the rendering layer (e.g. {@link org.hammer.audio.ui.WaveformRenderer}).
 *
 * <p>Snapshots are cheap to consume by any UI toolkit (Swing, JavaFX, Web), and are
 * serializable-friendly for export and remote APIs.
 *
 * @author refactoring
 */
public final class WaveformSnapshot {

  private final float[][] samples; // [channel][frame]
  private final int frames;
  private final float sampleRate;
  private final long sourceFrameIndex;
  private final long sourceTimestampNanos;

  /**
   * Build a waveform snapshot from a freshly-allocated {@code float[channels][frames]} array. The
   * caller transfers ownership of the array and must not mutate it after the call.
   *
   * @param samples per-channel sample arrays of equal length (ownership transferred)
   * @param sampleRate source sample rate in Hz
   * @param sourceFrameIndex frame index of the source block
   * @param sourceTimestampNanos timestamp of the source block (nanos)
   * @return a new immutable snapshot
   */
  public static WaveformSnapshot wrap(float[][] samples, float sampleRate, long sourceFrameIndex,
      long sourceTimestampNanos) {
    return new WaveformSnapshot(samples, sampleRate, sourceFrameIndex, sourceTimestampNanos, false);
  }

  /**
   * Convenience factory that builds a snapshot from the contents of an {@link AudioBlock}.
   *
   * <p>Performs a defensive copy so that the snapshot is fully detached from the block's internal
   * arrays. Suitable for low-rate UI updates.
   */
  public static WaveformSnapshot fromBlock(AudioBlock block) {
    return new WaveformSnapshot(block.samples(), block.format().sampleRate(), block.frameIndex(),
        block.timestampNanos(), false);
  }

  /** Empty snapshot constant. */
  public static final WaveformSnapshot EMPTY = new WaveformSnapshot(new float[0][], 0f, 0L, 0L,
      false);

  private WaveformSnapshot(float[][] samples, float sampleRate, long sourceFrameIndex,
      long sourceTimestampNanos, boolean copy) {
    if (copy) {
      float[][] cp = new float[samples.length][];
      for (int c = 0; c < samples.length; c++) {
        cp[c] = samples[c].clone();
      }
      this.samples = cp;
    } else {
      this.samples = samples;
    }
    this.frames = samples.length == 0 || samples[0] == null ? 0 : samples[0].length;
    this.sampleRate = sampleRate;
    this.sourceFrameIndex = sourceFrameIndex;
    this.sourceTimestampNanos = sourceTimestampNanos;
  }

  /** @return number of channels */
  public int channels() {
    return samples.length;
  }

  /** @return number of frames per channel */
  public int frames() {
    return frames;
  }

  /** @return source sample rate in Hz */
  public float sampleRate() {
    return sampleRate;
  }

  /** @return source block frame index */
  public long sourceFrameIndex() {
    return sourceFrameIndex;
  }

  /** @return source block timestamp in nanos */
  public long sourceTimestampNanos() {
    return sourceTimestampNanos;
  }

  /**
   * Read-only view of one channel. The returned array is the snapshot's internal storage and
   * <strong>must not be mutated</strong>.
   *
   * @param channel channel index
   * @return internal sample array (do not mutate)
   */
  public float[] channelView(int channel) {
    return samples[channel];
  }
}
