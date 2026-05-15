package org.hammer.audio.dsp;

import org.hammer.audio.core.AudioBlock;

/**
 * A single stage of the DSP processing pipeline.
 *
 * <p>A {@code DSPProcessor} consumes an {@link AudioBlock} and produces another {@link AudioBlock}.
 * Implementations may:
 *
 * <ul>
 *   <li>pass the block through unchanged ({@link #identity()})
 *   <li>transform the samples (e.g. high-pass filter, gain, normalization, downmix)
 *   <li>change the format (e.g. sample-rate conversion, mono mixdown) — the returned block's {@code
 *       format()} must reflect the post-processing format
 * </ul>
 *
 * <p>Processors should treat their input as immutable (see {@link AudioBlock#channelView(int)}) and
 * return a new {@link AudioBlock} for any modified output. Processors must be stateless or
 * internally synchronized — a single processor instance may be invoked from one DSP thread, but
 * pipelines composed of processors are reusable across threads if all stages are themselves
 * thread-safe.
 *
 * @author refactoring
 */
@FunctionalInterface
public interface DSPProcessor {

  /**
   * Process a single audio block.
   *
   * @param block the input block (treat as immutable); never {@code null}
   * @return the processed block; never {@code null}. May be the same instance as {@code block} for
   *     pass-through stages.
   */
  AudioBlock process(AudioBlock block);

  /**
   * @return a no-op processor that returns its input unchanged
   */
  static DSPProcessor identity() {
    return block -> block;
  }
}
