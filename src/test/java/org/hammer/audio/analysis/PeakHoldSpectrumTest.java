package org.hammer.audio.analysis;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PeakHoldSpectrumTest {

  @Test
  void firstUpdate_seedsState() {
    PeakHoldSpectrum p = new PeakHoldSpectrum();
    p.update(new float[] {0.1f, 0.4f, 0.2f});
    assertArrayEquals(new float[] {0.1f, 0.4f, 0.2f}, p.peaks(), 1e-6f);
  }

  @Test
  void update_keepsMaximum() {
    PeakHoldSpectrum p = new PeakHoldSpectrum();
    p.update(new float[] {0.1f, 0.4f, 0.2f});
    p.update(new float[] {0.3f, 0.2f, 0.5f});
    assertArrayEquals(new float[] {0.3f, 0.4f, 0.5f}, p.peaks(), 1e-6f);
  }

  @Test
  void decayLowersHeldPeaks() {
    PeakHoldSpectrum p = new PeakHoldSpectrum(0.5f);
    p.update(new float[] {1.0f, 1.0f});
    p.update(new float[] {0.1f, 0.1f});
    float[] peaks = p.peaks();
    // After multiplicative decay of 0.5 the held peak (1.0) becomes 0.5, which is still > 0.1.
    assertEquals(0.5f, peaks[0], 1e-6f);
    assertEquals(0.5f, peaks[1], 1e-6f);
  }

  @Test
  void reset_clears() {
    PeakHoldSpectrum p = new PeakHoldSpectrum();
    p.update(new float[] {0.4f, 0.2f});
    p.reset();
    assertEquals(0, p.updates());
    assertArrayEquals(new float[] {0f, 0f}, p.peaks(), 1e-6f);
  }

  @Test
  void invalidDecay_throws() {
    assertThrows(IllegalArgumentException.class, () -> new PeakHoldSpectrum(-0.1f));
    assertThrows(IllegalArgumentException.class, () -> new PeakHoldSpectrum(1.5f));
  }

  @Test
  void averager_smoothsValues() {
    SpectrumAverager a = new SpectrumAverager(0.5f);
    a.update(new float[] {0f, 0f});
    a.update(new float[] {1f, 1f});
    assertArrayEquals(new float[] {0.5f, 0.5f}, a.average(), 1e-6f);
    a.update(new float[] {1f, 1f});
    assertArrayEquals(new float[] {0.75f, 0.75f}, a.average(), 1e-6f);
  }

  @Test
  void averager_alphaOneTracksImmediately() {
    SpectrumAverager a = new SpectrumAverager(1.0f);
    a.update(new float[] {0.2f, 0.4f});
    a.update(new float[] {0.6f, 0.8f});
    assertArrayEquals(new float[] {0.6f, 0.8f}, a.average(), 1e-6f);
  }

  @Test
  void averager_resetReseedsAverageOnNextUpdate() {
    SpectrumAverager a = new SpectrumAverager(0.5f);
    a.update(new float[] {0.4f, 0.4f});
    a.update(new float[] {0.4f, 0.4f});
    a.reset();
    a.update(new float[] {0.8f, 0.8f});
    // After reset, the first update should seed directly to the new magnitudes (no 0.5 * x).
    assertArrayEquals(new float[] {0.8f, 0.8f}, a.average(), 1e-6f);
  }
}
