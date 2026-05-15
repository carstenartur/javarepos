package org.hammer.audio.analysis;

import static org.junit.jupiter.api.Assertions.*;

import org.hammer.audio.core.AudioBlock;
import org.hammer.audio.core.AudioFormatDescriptor;
import org.hammer.audio.signal.SineGenerator;
import org.junit.jupiter.api.Test;

class SpectrumAnalyzerTest {

  @Test
  void spectrum_peak_matches_input_frequency() {
    int sampleRate = 48000;
    int fftSize = 4096;
    double freq = 1000.0;
    AudioFormatDescriptor fmt = new AudioFormatDescriptor(sampleRate, 1, 16);
    SineGenerator gen = new SineGenerator(fmt, freq, 1f);
    AudioBlock block = gen.nextBlock(fftSize);

    SpectrumAnalyzer analyzer = new SpectrumAnalyzer(fftSize, 0, sampleRate);
    SpectrumSnapshot snap = analyzer.analyze(block);

    // Find the peak bin and check it corresponds to ~1 kHz within bin resolution.
    int peakBin = 0;
    float peakMag = 0f;
    for (int b = 0; b < snap.binCount(); b++) {
      if (snap.magnitude(b) > peakMag) {
        peakMag = snap.magnitude(b);
        peakBin = b;
      }
    }
    float peakHz = snap.frequencyOfBin(peakBin);
    float binWidth = snap.binWidthHz();
    assertTrue(
        Math.abs(peakHz - freq) <= binWidth,
        "peak at " + peakHz + " Hz should be within one bin (" + binWidth + " Hz) of " + freq);
  }

  @Test
  void empty_block_produces_zero_spectrum() {
    AudioFormatDescriptor fmt = new AudioFormatDescriptor(48000f, 1, 16);
    AudioBlock zeros = AudioBlock.wrap(fmt, new float[][] {new float[1024]}, 0L, 0L);
    SpectrumAnalyzer analyzer = new SpectrumAnalyzer(1024, 0, 48000f);
    SpectrumSnapshot snap = analyzer.analyze(zeros);

    for (int b = 0; b < snap.binCount(); b++) {
      assertEquals(0f, snap.magnitude(b), 1e-6f);
    }
  }

  @Test
  void rejects_invalid_parameters() {
    assertThrows(IllegalArgumentException.class, () -> new SpectrumAnalyzer(1024, -1, 48000f));
    assertThrows(IllegalArgumentException.class, () -> new SpectrumAnalyzer(1024, 0, -1f));
    assertThrows(IllegalArgumentException.class, () -> new SpectrumAnalyzer(1023, 0, 48000f));
  }

  @Test
  void snapshot_carries_source_metadata() {
    AudioFormatDescriptor fmt = new AudioFormatDescriptor(48000f, 1, 16);
    AudioBlock block = AudioBlock.wrap(fmt, new float[][] {new float[1024]}, 7777L, 8888L);
    SpectrumAnalyzer analyzer = new SpectrumAnalyzer(1024, 0, 48000f);
    SpectrumSnapshot snap = analyzer.analyze(block);
    assertEquals(7777L, snap.sourceFrameIndex());
    assertEquals(8888L, snap.sourceTimestampNanos());
    assertEquals(1024, snap.fftSize());
    assertEquals(0, snap.channel());
  }
}
