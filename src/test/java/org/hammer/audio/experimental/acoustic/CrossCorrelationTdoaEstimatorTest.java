package org.hammer.audio.experimental.acoustic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hammer.audio.acquisition.Microphone;
import org.hammer.audio.acquisition.MicrophoneArray;
import org.hammer.audio.core.AudioBlock;
import org.hammer.audio.core.AudioFormatDescriptor;
import org.hammer.audio.geometry.Vector2;
import org.junit.jupiter.api.Test;

class CrossCorrelationTdoaEstimatorTest {

  @Test
  void estimatesIntegerSampleDelay() {
    int sampleRate = 48_000;
    int frames = 256;
    int delay = 4;
    float[][] samples = new float[2][frames];
    for (int i = 20; i < 80; i++) {
      samples[0][i] = 1.0f;
      samples[1][i + delay] = 1.0f;
    }
    AudioBlock block = new AudioBlock(new AudioFormatDescriptor(sampleRate, 2, 32), samples, 0, 0);
    MicrophoneArray array =
        new MicrophoneArray(
            List.of(
                new Microphone("left", new Vector2(0.0, 0.0), 0),
                new Microphone("right", new Vector2(0.10, 0.0), 1)));

    TdoaEstimate estimate = new CrossCorrelationTdoaEstimator(343.0).estimate(block, array, 0, 1);

    assertEquals(delay, estimate.delaySamples());
    assertTrue(estimate.confidence() > 0.9);
  }
}
