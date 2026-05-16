package org.hammer.audio.experimental.acoustic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import org.hammer.audio.acquisition.Microphone;
import org.hammer.audio.acquisition.MicrophoneArray;
import org.hammer.audio.core.AudioBlock;
import org.hammer.audio.core.AudioFormatDescriptor;
import org.hammer.audio.geometry.Vector2;
import org.junit.jupiter.api.Test;

class GccPhatTdoaEstimatorTest {

  @Test
  void estimatesSyntheticDelayedSignal() {
    TdoaEstimate estimate =
        new GccPhatTdoaEstimator(343.0).estimate(delayedPulseBlock(5, 0.0, false), array(), 0, 1);

    assertEquals(5, estimate.delaySamples());
    assertTrue(estimate.confidence() > 0.2);
  }

  @Test
  void remainsUsableWithNoiseAndReflection() {
    TdoaEstimate estimate =
        new GccPhatTdoaEstimator(343.0).estimate(delayedPulseBlock(4, 0.03, true), array(), 0, 1);

    assertEquals(4, estimate.delaySamples());
    assertTrue(estimate.confidence() > 0.05);
  }

  private static AudioBlock delayedPulseBlock(int delay, double noiseAmplitude, boolean reflection) {
    int sampleRate = 48_000;
    int frames = 512;
    float[][] samples = new float[2][frames];
    Random random = new Random(1234L);
    for (int i = 80; i < 180; i++) {
      double pulse = Math.sin(2.0 * Math.PI * 1_200.0 * i / sampleRate);
      samples[0][i] += (float) pulse;
      samples[1][i + delay] += (float) pulse;
      if (reflection) {
        samples[0][i + 35] += (float) (0.25 * pulse);
        samples[1][i + delay + 35] += (float) (0.25 * pulse);
      }
    }
    for (int channel = 0; channel < samples.length; channel++) {
      for (int i = 0; i < frames; i++) {
        samples[channel][i] += (float) (noiseAmplitude * (random.nextDouble() * 2.0 - 1.0));
      }
    }
    return new AudioBlock(new AudioFormatDescriptor(sampleRate, 2, 32), samples, 0, 0);
  }

  private static MicrophoneArray array() {
    return new MicrophoneArray(
        List.of(
            new Microphone("left", new Vector2(0.0, 0.0), 0),
            new Microphone("right", new Vector2(0.10, 0.0), 1)));
  }
}
