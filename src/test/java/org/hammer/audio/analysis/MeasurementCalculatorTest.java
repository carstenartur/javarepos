package org.hammer.audio.analysis;

import static org.junit.jupiter.api.Assertions.*;

import org.hammer.audio.core.AudioBlock;
import org.hammer.audio.core.AudioFormatDescriptor;
import org.junit.jupiter.api.Test;

class MeasurementCalculatorTest {

  private final MeasurementCalculator calculator = new MeasurementCalculator();

  @Test
  void empty_input_returns_zero_and_na_values() {
    MeasurementSnapshot snapshot = calculator.calculate(null, null);
    assertEquals(0.0, snapshot.rms(), 1e-9);
    assertEquals(0.0, snapshot.peakLevel(), 1e-9);
    assertTrue(Double.isNaN(snapshot.dominantFrequencyHz()));
    assertFalse(snapshot.stereoCorrelationAvailable());
    assertFalse(snapshot.clipping());
  }

  @Test
  void clipping_and_correlation_are_computed_for_stereo_data() {
    AudioFormatDescriptor format = new AudioFormatDescriptor(48_000f, 2, 16);
    float[][] samples =
        new float[][] {
          new float[] {1.0f, -1.0f, 0.5f, -0.5f},
          new float[] {1.0f, -1.0f, 0.5f, -0.5f}
        };
    AudioBlock block = AudioBlock.wrap(format, samples, 0L, 0L);
    MeasurementSnapshot snapshot = calculator.calculate(block, null);

    assertTrue(snapshot.clipping(), "peak sample of 1.0 should trigger clipping");
    assertEquals(1.0, snapshot.peakLevel(), 1e-9);
    assertTrue(snapshot.stereoCorrelationAvailable());
    assertEquals(1.0, snapshot.stereoCorrelation(), 1e-9);
  }

  @Test
  void mono_input_has_no_stereo_correlation() {
    AudioFormatDescriptor format = new AudioFormatDescriptor(48_000f, 1, 16);
    AudioBlock block = AudioBlock.wrap(format, new float[][] {new float[] {0.1f, -0.1f}}, 0L, 0L);
    MeasurementSnapshot snapshot = calculator.calculate(block, null);

    assertFalse(snapshot.stereoCorrelationAvailable());
    assertTrue(Double.isNaN(snapshot.stereoCorrelation()));
  }

  @Test
  void dominant_frequency_comes_from_spectrum() {
    float[] magnitudes = new float[] {0f, 0.1f, 0.9f, 0.2f};
    SpectrumSnapshot spectrum = new SpectrumSnapshot(0L, 0L, 0, 48_000f, 6, magnitudes);
    MeasurementSnapshot snapshot = calculator.calculate(null, spectrum);

    assertEquals(spectrum.frequencyOfBin(2), snapshot.dominantFrequencyHz(), 1e-9);
  }
}
