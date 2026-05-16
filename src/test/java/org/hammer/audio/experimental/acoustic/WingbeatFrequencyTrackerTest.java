package org.hammer.audio.experimental.acoustic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hammer.audio.core.AudioBlock;
import org.hammer.audio.core.AudioFormatDescriptor;
import org.junit.jupiter.api.Test;

class WingbeatFrequencyTrackerTest {

  @Test
  void tracksStrongestFrequencyInsideBand() {
    int sampleRate = 8_192;
    int frames = 1_024;
    float[][] samples = new float[1][frames];
    for (int i = 0; i < frames; i++) {
      samples[0][i] = (float) Math.sin(2.0 * Math.PI * 440.0 * i / sampleRate);
    }
    AudioBlock block = new AudioBlock(new AudioFormatDescriptor(sampleRate, 1, 32), samples, 0, 0);
    WingbeatFrequencyTracker tracker = new WingbeatFrequencyTracker(1_024, new FrequencyBand(300.0, 600.0));

    SpectralPeak peak = tracker.track(block, 0);

    assertEquals(440.0, peak.frequencyHz(), sampleRate / 1_024.0);
    assertTrue(peak.magnitude() > 100.0);
    assertTrue(peak.confidence() > 0.2);
  }
}
