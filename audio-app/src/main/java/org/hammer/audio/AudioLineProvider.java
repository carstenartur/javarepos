package org.hammer.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.TargetDataLine;

/**
 * Package-private interface for obtaining audio input lines.
 *
 * <p>This interface enables dependency injection and testability by abstracting the acquisition of
 * TargetDataLine instances. Production code uses the default implementation that calls
 * AudioSystem.getLine(), while tests can inject mock implementations.
 *
 * @author refactoring
 */
interface AudioLineProvider {

  /**
   * Acquire a TargetDataLine for the given audio format.
   *
   * @param format the desired audio format
   * @return a TargetDataLine configured for the format
   * @throws IllegalStateException if the line cannot be obtained or opened
   */
  TargetDataLine acquireLine(AudioFormat format);
}
