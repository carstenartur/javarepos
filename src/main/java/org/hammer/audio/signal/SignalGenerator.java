package org.hammer.audio.signal;

import org.hammer.audio.core.AudioBlock;
import org.hammer.audio.core.AudioFormatDescriptor;

/**
 * Deterministic synthetic signal source.
 *
 * <p>Signal generators produce a stream of {@link AudioBlock}s without any audio hardware. They
 * are intended for:
 *
 * <ul>
 *   <li>Deterministic unit tests of the DSP and analysis modules
 *   <li>Headless demos and CI smoke tests
 *   <li>Verifying spectrum/correlation/level analyzers against known inputs
 * </ul>
 *
 * <p>Generators are stateful — they advance a frame counter / phase across calls — but a single
 * generator may be advanced from a single thread. Calling {@link #reset()} returns the generator
 * to its initial state for reproducible test runs.
 *
 * @author refactoring
 */
public interface SignalGenerator {

  /** @return the format descriptor of the produced blocks */
  AudioFormatDescriptor format();

  /**
   * Generate the next {@code frames}-frame block of audio.
   *
   * @param frames number of frames to generate; must be {@code >= 1}
   * @return a freshly-allocated immutable block
   */
  AudioBlock nextBlock(int frames);

  /** Reset the generator to its initial state (frame index 0, initial phase). */
  void reset();
}
