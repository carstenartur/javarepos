package org.hammer.audio.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AudioFormatDescriptorTest {

  @Test
  void rejects_invalid_sample_rate() {
    assertThrows(IllegalArgumentException.class, () -> new AudioFormatDescriptor(0f, 1, 16));
    assertThrows(IllegalArgumentException.class, () -> new AudioFormatDescriptor(-1f, 1, 16));
    assertThrows(IllegalArgumentException.class, () -> new AudioFormatDescriptor(Float.NaN, 1, 16));
  }

  @Test
  void rejects_invalid_channels_and_bits() {
    assertThrows(IllegalArgumentException.class, () -> new AudioFormatDescriptor(44100f, 0, 16));
    assertThrows(IllegalArgumentException.class, () -> new AudioFormatDescriptor(44100f, 1, 0));
  }

  @Test
  void equals_and_hashCode_are_value_based() {
    AudioFormatDescriptor a = new AudioFormatDescriptor(44100f, 2, 16);
    AudioFormatDescriptor b = new AudioFormatDescriptor(44100f, 2, 16);
    AudioFormatDescriptor c = new AudioFormatDescriptor(48000f, 2, 16);

    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertNotEquals(a, c);
    assertNotEquals(null, a);
  }

  @Test
  void getters_return_constructor_values() {
    AudioFormatDescriptor d = new AudioFormatDescriptor(48000f, 2, 24);
    assertEquals(48000f, d.sampleRate());
    assertEquals(2, d.channels());
    assertEquals(24, d.sourceSampleSizeInBits());
  }
}
