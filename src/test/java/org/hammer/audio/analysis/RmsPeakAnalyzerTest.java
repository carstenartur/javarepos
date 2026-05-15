package org.hammer.audio.analysis;

import static org.junit.jupiter.api.Assertions.*;

import org.hammer.audio.core.AudioBlock;
import org.hammer.audio.core.AudioFormatDescriptor;
import org.hammer.audio.signal.SineGenerator;
import org.junit.jupiter.api.Test;

class RmsPeakAnalyzerTest {

  @Test
  void zero_input_yields_zero_rms_and_peak() {
    AudioFormatDescriptor fmt = new AudioFormatDescriptor(48000f, 1, 16);
    AudioBlock zeros = AudioBlock.wrap(fmt, new float[][] {new float[1024]}, 0L, 0L);
    RmsPeakSnapshot snap = new RmsPeakAnalyzer().analyze(zeros);
    assertEquals(0f, snap.rms(0));
    assertEquals(0f, snap.peak(0));
  }

  @Test
  void unit_amplitude_sine_has_rms_of_one_over_sqrt2() {
    int sampleRate = 48000;
    AudioFormatDescriptor fmt = new AudioFormatDescriptor(sampleRate, 1, 16);
    SineGenerator gen = new SineGenerator(fmt, 1000.0, 1f);
    AudioBlock block = gen.nextBlock(8192);

    RmsPeakSnapshot snap = new RmsPeakAnalyzer().analyze(block);
    assertEquals(1f / Math.sqrt(2.0), snap.rms(0), 0.01,
        "RMS of unit sine should be 1/sqrt(2)");
    assertEquals(1f, snap.peak(0), 0.01, "peak of unit sine should be ~1");
  }

  @Test
  void per_channel_rms_is_independent() {
    AudioFormatDescriptor fmt = new AudioFormatDescriptor(48000f, 2, 16);
    float[][] s = new float[2][4];
    s[0][0] = 1f;
    s[0][1] = -1f;
    s[0][2] = 1f;
    s[0][3] = -1f;
    // channel 1 silent
    AudioBlock block = AudioBlock.wrap(fmt, s, 0L, 0L);
    RmsPeakSnapshot snap = new RmsPeakAnalyzer().analyze(block);
    assertEquals(1f, snap.rms(0), 1e-6);
    assertEquals(1f, snap.peak(0), 1e-6);
    assertEquals(0f, snap.rms(1), 1e-6);
    assertEquals(0f, snap.peak(1), 1e-6);
    assertEquals(2, snap.channels());
  }

  @Test
  void snapshot_arrays_are_defensive_copies() {
    AudioFormatDescriptor fmt = new AudioFormatDescriptor(48000f, 1, 16);
    AudioBlock block = AudioBlock.wrap(fmt, new float[][] {{0.5f, -0.5f}}, 0L, 0L);
    RmsPeakSnapshot snap = new RmsPeakAnalyzer().analyze(block);
    float[] rms1 = snap.rms();
    rms1[0] = 99f;
    assertNotEquals(99f, snap.rms()[0]);
  }
}
