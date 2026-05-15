package org.hammer.audio.analysis;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FftTest {

  private static final float TOLERANCE = 1e-4f;

  @Test
  void rejects_non_power_of_two() {
    assertThrows(IllegalArgumentException.class, () -> new Fft(3));
    assertThrows(IllegalArgumentException.class, () -> new Fft(0));
    assertThrows(IllegalArgumentException.class, () -> new Fft(1));
    assertThrows(IllegalArgumentException.class, () -> new Fft(7));
  }

  @Test
  void impulse_response_is_flat_magnitude() {
    int n = 16;
    Fft fft = new Fft(n);
    float[] re = new float[n];
    float[] im = new float[n];
    re[0] = 1f; // unit impulse

    fft.forward(re, im);

    // For a unit impulse at index 0, all FFT bins should have magnitude 1.
    for (int k = 0; k < n; k++) {
      double mag = Math.sqrt(re[k] * re[k] + im[k] * im[k]);
      assertEquals(1.0, mag, TOLERANCE, "bin " + k + " magnitude");
    }
  }

  @Test
  void dc_input_concentrates_energy_at_bin_zero() {
    int n = 32;
    Fft fft = new Fft(n);
    float[] re = new float[n];
    float[] im = new float[n];
    for (int i = 0; i < n; i++) {
      re[i] = 1f; // pure DC
    }

    fft.forward(re, im);

    // bin 0 should be N (sum of all 1s); all other bins should be ~0
    assertEquals(n, re[0], TOLERANCE);
    assertEquals(0f, im[0], TOLERANCE);
    for (int k = 1; k < n; k++) {
      double mag = Math.sqrt(re[k] * re[k] + im[k] * im[k]);
      assertTrue(mag < TOLERANCE, "bin " + k + " should be ~0, was " + mag);
    }
  }

  @Test
  void single_bin_sinusoid_concentrates_energy_in_correct_bin() {
    int n = 64;
    int targetBin = 8;
    Fft fft = new Fft(n);
    float[] re = new float[n];
    float[] im = new float[n];
    for (int i = 0; i < n; i++) {
      re[i] = (float) Math.cos(2.0 * Math.PI * targetBin * i / n);
    }

    fft.forward(re, im);

    float[] mags = new float[n];
    fft.magnitudes(re, im, mags);

    int peakBin = 0;
    float peakMag = 0f;
    for (int k = 0; k < n / 2 + 1; k++) {
      if (mags[k] > peakMag) {
        peakMag = mags[k];
        peakBin = k;
      }
    }
    assertEquals(targetBin, peakBin, "FFT should peak at bin " + targetBin);
  }

  @Test
  void magnitudes_length_validation() {
    Fft fft = new Fft(16);
    float[] re = new float[16];
    float[] im = new float[16];
    assertThrows(IllegalArgumentException.class, () -> fft.magnitudes(re, im, new float[10]));
  }

  @Test
  void rejects_mismatched_array_lengths_in_forward() {
    Fft fft = new Fft(8);
    assertThrows(IllegalArgumentException.class, () -> fft.forward(new float[7], new float[8]));
  }
}
