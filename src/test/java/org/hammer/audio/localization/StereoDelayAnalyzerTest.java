package org.hammer.audio.localization;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
import org.hammer.audio.core.AudioBlock;
import org.hammer.audio.core.AudioFormatDescriptor;
import org.junit.jupiter.api.Test;

class StereoDelayAnalyzerTest {

  private static final float SAMPLE_RATE = 48_000f;
  private static final AudioFormatDescriptor STEREO_FORMAT =
      new AudioFormatDescriptor(SAMPLE_RATE, 2, 16);

  @Test
  void detects_known_stereo_delay_within_tolerance() {
    AudioBlock block = delayedStereoBlock(2_048, 12);
    StereoDelayAnalyzer analyzer = new StereoDelayAnalyzer(0.20, 343.0, 0.25);

    StereoDelaySnapshot snapshot = analyzer.analyze(block);

    assertTrue(snapshot.valid());
    assertEquals(12, snapshot.delaySamples(), 1);
    assertEquals(0.25, snapshot.delayMillis(), 0.03);
    assertTrue(snapshot.confidence() > 0.9);
  }

  @Test
  void estimates_angle_for_known_microphone_spacing() {
    AudioBlock block = delayedStereoBlock(2_048, 14);
    StereoDelayAnalyzer analyzer = new StereoDelayAnalyzer(0.20, 343.0, 0.25);

    StereoDelaySnapshot snapshot = analyzer.analyze(block);

    double expectedAngle =
        Math.toDegrees(Math.asin((343.0 * snapshot.delaySamples() / SAMPLE_RATE) / 0.20));
    assertTrue(snapshot.valid());
    assertEquals(expectedAngle, snapshot.angleDegrees(), 0.001);
  }

  @Test
  void marks_impossible_delay_invalid() {
    AudioBlock block = delayedImpulseStereoBlock(2_048, 80);
    StereoDelayAnalyzer analyzer = new StereoDelayAnalyzer(0.10, 343.0, 0.25);

    StereoDelaySnapshot snapshot = analyzer.analyze(block);

    assertFalse(snapshot.valid());
    assertEquals(StereoDelayStatus.LOW_CORRELATION, snapshot.status());
    assertTrue(Double.isNaN(snapshot.angleDegrees()));
  }

  @Test
  void rejects_non_finite_constructor_parameters() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new StereoDelayAnalyzer(Double.POSITIVE_INFINITY, 343.0, 0.25));
    assertThrows(
        IllegalArgumentException.class,
        () -> new StereoDelayAnalyzer(0.20, Double.NEGATIVE_INFINITY, 0.25));
    assertThrows(
        IllegalArgumentException.class, () -> new StereoDelayAnalyzer(0.20, 343.0, Double.NaN));
  }

  @Test
  void correlation_window_is_limited_to_physical_delay_range() {
    AudioBlock block = delayedImpulseStereoBlock(2_048, 12);
    StereoDelayAnalyzer analyzer = new StereoDelayAnalyzer(0.10, 343.0, 0.25);

    StereoDelaySnapshot snapshot = analyzer.analyze(block);

    int maxPhysicalLag = (int) Math.ceil(0.10 / 343.0 * SAMPLE_RATE);
    assertEquals(-maxPhysicalLag, snapshot.minCorrelationLagSamples());
    assertEquals(maxPhysicalLag * 2 + 1, snapshot.correlationByLag().length);
  }

  @Test
  void silence_does_not_produce_fake_localization() {
    AudioBlock block =
        AudioBlock.wrap(STEREO_FORMAT, new float[][] {new float[512], new float[512]}, 0, 0);
    StereoDelayAnalyzer analyzer = new StereoDelayAnalyzer();

    StereoDelaySnapshot snapshot = analyzer.analyze(block);

    assertFalse(snapshot.valid());
    assertEquals(StereoDelayStatus.SILENCE, snapshot.status());
  }

  @Test
  void mono_input_is_invalid() {
    AudioFormatDescriptor monoFormat = new AudioFormatDescriptor(SAMPLE_RATE, 1, 16);
    AudioBlock block = AudioBlock.wrap(monoFormat, new float[][] {new float[512]}, 0, 0);
    StereoDelayAnalyzer analyzer = new StereoDelayAnalyzer();

    StereoDelaySnapshot snapshot = analyzer.analyze(block);

    assertFalse(snapshot.valid());
    assertEquals(StereoDelayStatus.MONO_INPUT, snapshot.status());
  }

  @Test
  void synthetic_moving_source_delay_changes_are_detected() {
    StereoDelayAnalyzer analyzer = new StereoDelayAnalyzer(0.20, 343.0, 0.25);

    StereoDelaySnapshot first = analyzer.analyze(delayedStereoBlock(2_048, -8));
    StereoDelaySnapshot second = analyzer.analyze(delayedStereoBlock(2_048, 0));
    StereoDelaySnapshot third = analyzer.analyze(delayedStereoBlock(2_048, 8));

    assertTrue(first.valid());
    assertTrue(second.valid());
    assertTrue(third.valid());
    assertEquals(-8, first.delaySamples(), 1);
    assertEquals(0, second.delaySamples(), 1);
    assertEquals(8, third.delaySamples(), 1);
  }

  private static AudioBlock delayedStereoBlock(int frames, int rightDelaySamples) {
    int margin = Math.abs(rightDelaySamples) + 32;
    float[] source = deterministicWidebandSignal(frames + margin * 2);
    float[][] samples = new float[2][frames];
    int leftOffset = margin;
    int rightOffset = margin - rightDelaySamples;
    for (int i = 0; i < frames; i++) {
      samples[0][i] = source[leftOffset + i];
      samples[1][i] = source[rightOffset + i];
    }
    return AudioBlock.wrap(STEREO_FORMAT, samples, 0L, 0L);
  }

  private static AudioBlock delayedImpulseStereoBlock(int frames, int rightDelaySamples) {
    float[][] samples = new float[2][frames];
    int leftImpulse = frames / 3;
    int rightImpulse = leftImpulse + rightDelaySamples;
    samples[0][leftImpulse] = 1.0f;
    if (rightImpulse >= 0 && rightImpulse < frames) {
      samples[1][rightImpulse] = 1.0f;
    }
    return AudioBlock.wrap(STEREO_FORMAT, samples, 0L, 0L);
  }

  private static float[] deterministicWidebandSignal(int frames) {
    float[] signal = new float[frames];
    Random random = new Random(0x5EED_1234L);
    for (int i = 0; i < frames; i++) {
      double noise = random.nextDouble(-1.0, 1.0);
      double burstEnvelope = Math.exp(-Math.pow((i - frames / 2.0) / (frames / 8.0), 2.0));
      signal[i] =
          (float)
              (0.6
                  * burstEnvelope
                  * (0.6 * noise + 0.4 * Math.sin(2.0 * Math.PI * 3137.0 * i / SAMPLE_RATE)));
    }
    return signal;
  }
}
