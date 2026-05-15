package org.hammer.audio.analysis;

import org.hammer.audio.core.AudioBlock;

/**
 * Module that analyzes an {@link AudioBlock} and produces an immutable {@link AnalysisSnapshot}.
 *
 * <p>Analysis modules typically compute aggregate or transformed measurements that are cheaper to
 * consume than the raw audio (RMS, peak, FFT, correlation, loudness, ...). They are the natural
 * extension point for plugin-based DSP analyzers.
 *
 * <p>Modules must accept input as immutable, must not mutate the input block, and may safely cache
 * scratch state across invocations only if they document and enforce single-threaded use, or
 * synchronize internally.
 *
 * @param <S> the snapshot type produced by this module
 * @author refactoring
 */
@FunctionalInterface
public interface AnalysisModule<S extends AnalysisSnapshot> {

  /**
   * Analyze a block and return an immutable snapshot of the result.
   *
   * @param block the input block; never {@code null}
   * @return analysis snapshot; never {@code null}
   */
  S analyze(AudioBlock block);
}
