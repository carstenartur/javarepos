package org.hammer.audio.experimental.acoustic.tracking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hammer.audio.core.AudioBlock;
import org.hammer.audio.core.AudioFormatDescriptor;
import org.hammer.audio.experimental.acoustic.FrequencyBand;
import org.junit.jupiter.api.Test;

class MultiPeakDetectorTest {

  private static final float SAMPLE_RATE = 16_000.0f;

  @Test
  void detectsDualTonePeaksOnSingleChannel() {
    MultiPeakDetector detector =
        new MultiPeakDetector(1024, new FrequencyBand(200.0, 2_000.0), 4, 3.0);
    AudioBlock block = tonesBlock(1024, new double[] {1}, new double[] {600.0, 1_200.0});

    List<DetectedPeak> peaks = detector.detect(block, 0);

    assertTrue(peaks.size() >= 2, "expected at least two peaks, got " + peaks);
    List<DetectedPeak> byFrequency = new java.util.ArrayList<>(peaks);
    byFrequency.sort(java.util.Comparator.comparingDouble(DetectedPeak::frequencyHz));
    assertEquals(600.0, byFrequency.get(0).frequencyHz(), 12.0);
    assertEquals(1_200.0, byFrequency.get(1).frequencyHz(), 12.0);
  }

  @Test
  void honoursMaxPeaksCap() {
    MultiPeakDetector detector =
        new MultiPeakDetector(1024, new FrequencyBand(200.0, 4_000.0), 1, 1.0);
    AudioBlock block = tonesBlock(1024, new double[] {1}, new double[] {600.0, 1_200.0, 1_800.0});

    List<DetectedPeak> peaks = detector.detect(block, 0);

    assertEquals(1, peaks.size());
  }

  @Test
  void detectAllChannelsReturnsPerChannelLists() {
    MultiPeakDetector detector =
        new MultiPeakDetector(512, new FrequencyBand(200.0, 2_000.0), 2, 1.0);
    AudioBlock block = tonesBlock(512, new double[] {0.8, 0.0}, new double[] {500.0});
    // Channel 1 is silent; expect no peaks above noise.
    List<List<DetectedPeak>> all = detector.detectAllChannels(block);
    assertEquals(2, all.size());
    assertTrue(!all.get(0).isEmpty());
  }

  @Test
  void rejectsInvalidArguments() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new MultiPeakDetector(0, new FrequencyBand(100.0, 200.0), 1, 1.0));
    assertThrows(
        IllegalArgumentException.class,
        () -> new MultiPeakDetector(7, new FrequencyBand(100.0, 200.0), 1, 1.0));
    assertThrows(
        IllegalArgumentException.class,
        () -> new MultiPeakDetector(64, new FrequencyBand(100.0, 200.0), 0, 1.0));
    assertThrows(
        IllegalArgumentException.class,
        () -> new MultiPeakDetector(64, new FrequencyBand(100.0, 200.0), 1, -1.0));

    MultiPeakDetector detector = new MultiPeakDetector(64, new FrequencyBand(100.0, 200.0), 1, 1.0);
    AudioBlock block = tonesBlock(64, new double[] {1}, new double[] {150.0});
    assertThrows(IllegalArgumentException.class, () -> detector.detect(block, 5));
  }

  private static AudioBlock tonesBlock(int frames, double[] amplitudes, double[] frequencies) {
    int channels = amplitudes.length;
    float[][] samples = new float[channels][frames];
    for (int channel = 0; channel < channels; channel++) {
      double amplitude = amplitudes[channel];
      for (int frame = 0; frame < frames; frame++) {
        double value = 0.0;
        for (double frequency : frequencies) {
          value += amplitude * Math.sin(2.0 * Math.PI * frequency * frame / SAMPLE_RATE);
        }
        samples[channel][frame] = (float) (value / Math.max(1, frequencies.length));
      }
    }
    return new AudioBlock(new AudioFormatDescriptor(SAMPLE_RATE, channels, 32), samples, 0L, 0L);
  }
}
