package org.hammer.audio.capture;

import org.hammer.audio.core.AudioFormatDescriptor;

/**
 * Stateless decoder of interleaved PCM byte buffers into normalized {@code float[channels][frames]}
 * sample arrays.
 *
 * <p>Supports the formats used by the platform's capture path:
 *
 * <ul>
 *   <li>signed/unsigned 8-bit PCM
 *   <li>signed/unsigned 16-bit PCM, little- or big-endian
 *   <li>generic 1..4-byte big-endian fallback for any other sample size
 * </ul>
 *
 * <p>Output samples are normalized to {@code [-1.0f, 1.0f]} for signed formats and to {@code [-1,
 * 1]} for unsigned formats (mapped from {@code [0, max]} to {@code [-1, 1]}).
 *
 * <p>This class is stateless and thread-safe.
 *
 * @author refactoring
 */
public final class SampleDecoder {

  private final AudioFormatDescriptor descriptor;
  private final int bytesPerSample;
  private final int frameSize;
  private final boolean signed;
  private final boolean bigEndian;
  private final float signedScale;
  private final float unsignedScale;
  private final int unsignedOffset;

  /**
   * @param descriptor audio format descriptor
   * @param signed true if source samples are signed
   * @param bigEndian true if source samples are big-endian (16-bit and wider)
   */
  public SampleDecoder(AudioFormatDescriptor descriptor, boolean signed, boolean bigEndian) {
    this.descriptor = descriptor;
    this.signed = signed;
    this.bigEndian = bigEndian;
    this.bytesPerSample = Math.max(1, descriptor.sourceSampleSizeInBits() / 8);
    this.frameSize = bytesPerSample * descriptor.channels();
    int bits = descriptor.sourceSampleSizeInBits();
    this.signedScale = 1f / ((1 << (bits - 1)) - 1);
    int unsignedMax = (1 << bits) - 1;
    this.unsignedScale = 2f / unsignedMax;
    this.unsignedOffset = unsignedMax / 2;
  }

  /** @return descriptor of the produced audio */
  public AudioFormatDescriptor descriptor() {
    return descriptor;
  }

  /** @return number of bytes per single-channel sample */
  public int bytesPerSample() {
    return bytesPerSample;
  }

  /** @return number of bytes per frame ({@code bytesPerSample * channels}) */
  public int frameSize() {
    return frameSize;
  }

  /**
   * @param byteCount number of bytes
   * @return number of complete frames in that byte count
   */
  public int framesIn(int byteCount) {
    return byteCount / frameSize;
  }

  /**
   * Decode raw bytes into a pre-allocated {@code float[channels][frames]} buffer. Existing samples
   * beyond the decoded range are not touched.
   *
   * @param data raw interleaved PCM bytes
   * @param byteCount number of valid bytes in {@code data}
   * @param dest destination buffer of shape {@code [channels][>=framesIn(byteCount)]}
   * @return number of decoded frames
   */
  public int decode(byte[] data, int byteCount, float[][] dest) {
    int frames = framesIn(byteCount);
    int channels = descriptor.channels();
    for (int frame = 0; frame < frames; frame++) {
      int frameOffset = frame * frameSize;
      for (int ch = 0; ch < channels; ch++) {
        int sampleOffset = frameOffset + ch * bytesPerSample;
        dest[ch][frame] = decodeOne(data, sampleOffset);
      }
    }
    return frames;
  }

  private float decodeOne(byte[] data, int offset) {
    if (bytesPerSample == 1) {
      int b = data[offset] & 0xFF;
      if (signed) {
        return (byte) b * signedScale;
      }
      return (b - unsignedOffset) * unsignedScale;
    }
    if (bytesPerSample == 2) {
      int hi = data[offset + (bigEndian ? 0 : 1)] & 0xFF;
      int lo = data[offset + (bigEndian ? 1 : 0)] & 0xFF;
      int raw = (hi << 8) | lo;
      if (signed) {
        return ((short) raw) * signedScale;
      }
      return ((raw & 0xFFFF) - unsignedOffset) * unsignedScale;
    }
    // Generic big-endian fallback for non-standard sizes.
    int sample = 0;
    for (int b = 0; b < bytesPerSample; b++) {
      sample = (sample << 8) | (data[offset + b] & 0xFF);
    }
    int bits = descriptor.sourceSampleSizeInBits();
    if (signed) {
      int shift = 32 - bits;
      sample = (sample << shift) >> shift;
      return sample * signedScale;
    }
    return (sample - unsignedOffset) * unsignedScale;
  }
}
