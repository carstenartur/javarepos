package org.hammer.audio.signal;

import static org.junit.jupiter.api.Assertions.*;

import org.hammer.audio.core.AudioBlock;
import org.hammer.audio.core.AudioFormatDescriptor;
import org.junit.jupiter.api.Test;

class SignalGeneratorTest {

  private static final AudioFormatDescriptor MONO = new AudioFormatDescriptor(48000f, 1, 16);

  @Test
  void sine_is_deterministic_after_reset() {
    SineGenerator g = new SineGenerator(MONO, 440.0, 1f);
    AudioBlock first = g.nextBlock(128);
    g.reset();
    AudioBlock second = g.nextBlock(128);
    assertArrayEquals(first.channelView(0), second.channelView(0));
    assertEquals(0L, second.frameIndex());
  }

  @Test
  void sine_phase_is_continuous_across_blocks() {
    SineGenerator g = new SineGenerator(MONO, 100.0, 1f);
    g.nextBlock(100);
    g.reset();
    AudioBlock big = g.nextBlock(200);
    g.reset();
    AudioBlock first = g.nextBlock(100);
    AudioBlock second = g.nextBlock(100);
    for (int i = 0; i < 100; i++) {
      assertEquals(big.channelView(0)[i], first.channelView(0)[i], 1e-5);
      assertEquals(big.channelView(0)[100 + i], second.channelView(0)[i], 1e-5);
    }
    assertEquals(100L, second.frameIndex());
  }

  @Test
  void sine_amplitude_is_bounded() {
    SineGenerator g = new SineGenerator(MONO, 1000.0, 0.5f);
    AudioBlock block = g.nextBlock(1024);
    for (float s : block.channelView(0)) {
      assertTrue(s <= 0.5f + 1e-6 && s >= -0.5f - 1e-6);
    }
  }

  @Test
  void square_is_two_valued() {
    SquareGenerator g = new SquareGenerator(MONO, 100.0, 0.7f);
    AudioBlock block = g.nextBlock(2048);
    for (float s : block.channelView(0)) {
      assertTrue(s == 0.7f || s == -0.7f, "unexpected square sample " + s);
    }
  }

  @Test
  void chirp_starts_and_ends_at_configured_frequencies_via_local_period_check() {
    // A linear chirp from 100 Hz to 1000 Hz over 1 second.
    AudioFormatDescriptor fmt = new AudioFormatDescriptor(48000f, 1, 16);
    ChirpGenerator g = new ChirpGenerator(fmt, 100.0, 1000.0, 1.0, 1f);
    AudioBlock block = g.nextBlock(48000);

    // Count zero-crossings in a small early window vs. a small late window.
    float[] s = block.channelView(0);
    int earlyCrossings = countZeroCrossings(s, 0, 4800); // first 0.1 s
    int lateCrossings = countZeroCrossings(s, 43200, 48000); // last 0.1 s

    // At 100 Hz over 0.1 s ≈ 10 cycles ≈ 20 zero crossings (roughly)
    // At ~1000 Hz over 0.1 s ≈ 100 cycles ≈ 200 zero crossings
    assertTrue(lateCrossings > earlyCrossings * 4,
        "chirp should accelerate (early=" + earlyCrossings + ", late=" + lateCrossings + ")");
  }

  @Test
  void rejects_invalid_frequency() {
    assertThrows(IllegalArgumentException.class, () -> new SineGenerator(MONO, 0.0, 1f));
    assertThrows(IllegalArgumentException.class, () -> new SquareGenerator(MONO, -1.0, 1f));
    assertThrows(IllegalArgumentException.class,
        () -> new ChirpGenerator(MONO, 0.0, 1000.0, 1.0, 1f));
  }

  private static int countZeroCrossings(float[] signal, int start, int end) {
    int count = 0;
    for (int i = start + 1; i < end; i++) {
      if ((signal[i - 1] >= 0f && signal[i] < 0f) || (signal[i - 1] < 0f && signal[i] >= 0f)) {
        count++;
      }
    }
    return count;
  }
}
