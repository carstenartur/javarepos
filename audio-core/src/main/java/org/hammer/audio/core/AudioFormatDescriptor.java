package org.hammer.audio.core;

import java.util.Objects;

/**
 * Immutable, audio-domain-only description of an audio stream.
 *
 * <p>This is the platform's canonical format descriptor. Unlike {@link
 * javax.sound.sampled.AudioFormat}, it is:
 *
 * <ul>
 *   <li>UI/JavaSound-independent (no pixels, no Swing, no platform types)
 *   <li>immutable and thread-safe (all fields {@code final})
 *   <li>cheap to share between the capture, DSP, analysis and UI layers
 * </ul>
 *
 * <p>It always describes a stream of <em>normalized floating-point</em> samples. The original
 * device sample size in bits is retained for diagnostics and back-conversion only; downstream DSP
 * and analysis modules must operate on the normalized {@code float} representation.
 *
 * @author refactoring
 */
public final class AudioFormatDescriptor {

  private final float sampleRate;
  private final int channels;
  private final int sourceSampleSizeInBits;

  /**
   * Create a new immutable audio format descriptor.
   *
   * @param sampleRate sample rate in Hz; must be {@code > 0}
   * @param channels number of channels (e.g. 1 for mono, 2 for stereo); must be {@code >= 1}
   * @param sourceSampleSizeInBits source device sample size in bits (e.g. 8 or 16); must be {@code
   *     >= 1}. Retained for diagnostics, not used in DSP math.
   * @throws IllegalArgumentException if any argument is invalid
   */
  public AudioFormatDescriptor(float sampleRate, int channels, int sourceSampleSizeInBits) {
    if (!(sampleRate > 0f) || Float.isNaN(sampleRate) || Float.isInfinite(sampleRate)) {
      throw new IllegalArgumentException("sampleRate must be > 0, was " + sampleRate);
    }
    if (channels < 1) {
      throw new IllegalArgumentException("channels must be >= 1, was " + channels);
    }
    if (sourceSampleSizeInBits < 1) {
      throw new IllegalArgumentException(
          "sourceSampleSizeInBits must be >= 1, was " + sourceSampleSizeInBits);
    }
    this.sampleRate = sampleRate;
    this.channels = channels;
    this.sourceSampleSizeInBits = sourceSampleSizeInBits;
  }

  /**
   * @return the sample rate in Hz (e.g. {@code 44100.0f})
   */
  public float sampleRate() {
    return sampleRate;
  }

  /**
   * @return the number of channels (e.g. 1 for mono, 2 for stereo)
   */
  public int channels() {
    return channels;
  }

  /**
   * @return the source device sample size in bits (typically 8, 16 or 24). Provided for diagnostics
   *     only; DSP code should not depend on this.
   */
  public int sourceSampleSizeInBits() {
    return sourceSampleSizeInBits;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AudioFormatDescriptor other)) {
      return false;
    }
    return Float.compare(other.sampleRate, sampleRate) == 0
        && other.channels == channels
        && other.sourceSampleSizeInBits == sourceSampleSizeInBits;
  }

  @Override
  public int hashCode() {
    return Objects.hash(sampleRate, channels, sourceSampleSizeInBits);
  }

  @Override
  public String toString() {
    return "AudioFormatDescriptor[sampleRate="
        + sampleRate
        + "Hz, channels="
        + channels
        + ", sourceBits="
        + sourceSampleSizeInBits
        + "]";
  }
}
