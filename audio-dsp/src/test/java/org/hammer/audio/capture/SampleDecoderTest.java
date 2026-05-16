package org.hammer.audio.capture;

import static org.junit.jupiter.api.Assertions.*;

import org.hammer.audio.core.AudioFormatDescriptor;
import org.junit.jupiter.api.Test;

class SampleDecoderTest {

  @Test
  void decodes_signed_16bit_little_endian() {
    AudioFormatDescriptor fmt = new AudioFormatDescriptor(48000f, 1, 16);
    SampleDecoder dec = new SampleDecoder(fmt, true, false);
    // 32767 (0x7FFF) -> ~+1.0, -32768 (0x8000) -> ~-1.0, 0 -> 0
    byte[] data = new byte[] {(byte) 0xFF, (byte) 0x7F, (byte) 0x00, (byte) 0x80, 0, 0};
    float[][] out = new float[1][3];
    int frames = dec.decode(data, 6, out);
    assertEquals(3, frames);
    assertEquals(1f, out[0][0], 1e-3);
    assertTrue(out[0][1] < -0.99f);
    assertEquals(0f, out[0][2], 1e-6);
  }

  @Test
  void decodes_unsigned_8bit() {
    AudioFormatDescriptor fmt = new AudioFormatDescriptor(48000f, 1, 8);
    SampleDecoder dec = new SampleDecoder(fmt, false, false);
    byte[] data = new byte[] {0, (byte) 255, (byte) 127};
    float[][] out = new float[1][3];
    dec.decode(data, 3, out);
    // 0 -> -1.0, 255 -> ~+1.0, 127 -> ~0.0 (mid)
    assertEquals(-1f, out[0][0], 0.02);
    assertEquals(1f, out[0][1], 0.02);
    assertEquals(0f, out[0][2], 0.02);
  }

  @Test
  void decodes_stereo_interleaved() {
    AudioFormatDescriptor fmt = new AudioFormatDescriptor(48000f, 2, 8);
    SampleDecoder dec = new SampleDecoder(fmt, true, false);
    // Left=10, Right=20, Left=-10, Right=-20
    byte[] data = new byte[] {10, 20, -10, -20};
    float[][] out = new float[2][2];
    int frames = dec.decode(data, 4, out);
    assertEquals(2, frames);
    assertEquals(10f / 127f, out[0][0], 1e-4);
    assertEquals(20f / 127f, out[1][0], 1e-4);
    assertEquals(-10f / 127f, out[0][1], 1e-4);
    assertEquals(-20f / 127f, out[1][1], 1e-4);
  }

  @Test
  void framesIn_uses_frame_size() {
    AudioFormatDescriptor fmt = new AudioFormatDescriptor(48000f, 2, 16);
    SampleDecoder dec = new SampleDecoder(fmt, true, false);
    // 2 channels * 2 bytes = 4 bytes per frame
    assertEquals(4, dec.frameSize());
    assertEquals(10, dec.framesIn(40));
    assertEquals(10, dec.framesIn(43)); // floor
  }
}
