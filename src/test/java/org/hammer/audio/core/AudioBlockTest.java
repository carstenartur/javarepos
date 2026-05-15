package org.hammer.audio.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AudioBlockTest {

  private static final AudioFormatDescriptor STEREO = new AudioFormatDescriptor(48000f, 2, 16);

  @Test
  void constructor_makes_defensive_copy() {
    float[][] s = {{0.1f, 0.2f}, {0.3f, 0.4f}};
    AudioBlock block = new AudioBlock(STEREO, s, 0L, 1L);

    s[0][0] = 99f; // mutate the source array
    assertEquals(0.1f, block.channelView(0)[0], "block must be detached from caller's array");
  }

  @Test
  void wrap_does_not_copy() {
    float[][] s = {{0.1f, 0.2f}, {0.3f, 0.4f}};
    AudioBlock block = AudioBlock.wrap(STEREO, s, 0L, 1L);
    assertSame(s[0], block.channelView(0));
  }

  @Test
  void rejects_mismatched_channel_count() {
    float[][] s = {{0.1f, 0.2f}}; // only 1 channel but format says 2
    assertThrows(IllegalArgumentException.class, () -> new AudioBlock(STEREO, s, 0L, 1L));
  }

  @Test
  void rejects_mismatched_per_channel_lengths() {
    float[][] s = {{0.1f, 0.2f}, {0.3f}};
    assertThrows(IllegalArgumentException.class, () -> new AudioBlock(STEREO, s, 0L, 1L));
  }

  @Test
  void samples_returns_defensive_copy() {
    float[][] s = {{0.1f, 0.2f}, {0.3f, 0.4f}};
    AudioBlock block = new AudioBlock(STEREO, s, 0L, 1L);

    float[][] copy = block.samples();
    copy[0][0] = 99f;
    assertEquals(0.1f, block.channelView(0)[0]);
  }

  @Test
  void frameIndex_and_timestamp_round_trip() {
    AudioBlock block = AudioBlock.wrap(STEREO, new float[][] {{0f}, {0f}}, 12345L, 6789L);
    assertEquals(12345L, block.frameIndex());
    assertEquals(6789L, block.timestampNanos());
    assertEquals(1, block.frames());
    assertEquals(2, block.channels());
    assertEquals(STEREO, block.format());
  }
}
