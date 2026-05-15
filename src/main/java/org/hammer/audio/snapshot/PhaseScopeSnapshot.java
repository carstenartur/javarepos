package org.hammer.audio.snapshot;

import org.hammer.audio.core.AudioBlock;

/**
 * Immutable snapshot for stereo phase-scope (X-Y) visualization.
 *
 * <p>Carries the left and right channel sample arrays from one block, normalized to {@code [-1,
 * 1]}. The renderer is responsible for any pixel scaling.
 *
 * @author refactoring
 */
public final class PhaseScopeSnapshot {

  private final float[] left;
  private final float[] right;
  private final long sourceFrameIndex;
  private final long sourceTimestampNanos;

  /**
   * Build a phase-scope snapshot from an {@link AudioBlock}. Defensive copies are made of the
   * channel arrays.
   *
   * @param block the source block; must have at least 2 channels. If not, an empty snapshot is
   *     produced.
   * @return immutable snapshot
   */
  public static PhaseScopeSnapshot fromBlock(AudioBlock block) {
    if (block.channels() < 2) {
      return new PhaseScopeSnapshot(new float[0], new float[0], block.frameIndex(),
          block.timestampNanos());
    }
    return new PhaseScopeSnapshot(block.channelView(0).clone(), block.channelView(1).clone(),
        block.frameIndex(), block.timestampNanos());
  }

  /** Empty snapshot constant. */
  public static final PhaseScopeSnapshot EMPTY = new PhaseScopeSnapshot(new float[0], new float[0],
      0L, 0L);

  private PhaseScopeSnapshot(float[] left, float[] right, long sourceFrameIndex,
      long sourceTimestampNanos) {
    this.left = left;
    this.right = right;
    this.sourceFrameIndex = sourceFrameIndex;
    this.sourceTimestampNanos = sourceTimestampNanos;
  }

  /** @return number of frames in this snapshot */
  public int frames() {
    return left.length;
  }

  /** @return read-only view of the left channel samples (do not mutate) */
  public float[] leftView() {
    return left;
  }

  /** @return read-only view of the right channel samples (do not mutate) */
  public float[] rightView() {
    return right;
  }

  /** @return source block frame index */
  public long sourceFrameIndex() {
    return sourceFrameIndex;
  }

  /** @return source block timestamp in nanos */
  public long sourceTimestampNanos() {
    return sourceTimestampNanos;
  }
}
