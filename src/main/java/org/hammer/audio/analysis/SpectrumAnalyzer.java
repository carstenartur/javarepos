package org.hammer.audio.analysis;

import org.hammer.audio.core.AudioBlock;

/**
 * FFT-based spectrum analyzer.
 *
 * <p>Takes one channel of an {@link AudioBlock}, applies a Hann window of the configured FFT size,
 * computes the forward FFT and produces a {@link SpectrumSnapshot} containing the one-sided
 * magnitude spectrum.
 *
 * <p>If the input block contains fewer frames than the FFT size, the remaining samples are
 * zero-padded. If it contains more, only the first {@code fftSize} frames are analyzed.
 *
 * <p>Internally this analyzer caches per-instance scratch buffers; instances are <strong>not
 * thread-safe</strong>. Create one per analysis thread or guard externally.
 *
 * @author refactoring
 */
public final class SpectrumAnalyzer implements AnalysisModule<SpectrumSnapshot> {

  private final int fftSize;
  private final int channel;
  private final float sampleRate;
  private final Fft fft;
  private final float[] window;
  private final float[] re;
  private final float[] im;
  private final float[] magnitudes;

  /**
   * Create a new spectrum analyzer.
   *
   * @param fftSize FFT size; must be a power of two and {@code >= 2}
   * @param channel channel index of the source block to analyze (0 for mono)
   * @param sampleRate sample rate of the source audio in Hz
   * @throws IllegalArgumentException if any parameter is invalid
   */
  public SpectrumAnalyzer(int fftSize, int channel, float sampleRate) {
    if (channel < 0) {
      throw new IllegalArgumentException("channel must be >= 0, was " + channel);
    }
    if (!(sampleRate > 0f)) {
      throw new IllegalArgumentException("sampleRate must be > 0, was " + sampleRate);
    }
    this.fftSize = fftSize;
    this.channel = channel;
    this.sampleRate = sampleRate;
    this.fft = new Fft(fftSize);
    this.window = hannWindow(fftSize);
    this.re = new float[fftSize];
    this.im = new float[fftSize];
    this.magnitudes = new float[fftSize / 2 + 1];
  }

  @Override
  public SpectrumSnapshot analyze(AudioBlock block) {
    float[] samples = block.channelView(channel);
    int n = Math.min(samples.length, fftSize);

    // Apply Hann window and zero-pad remainder.
    for (int i = 0; i < n; i++) {
      re[i] = samples[i] * window[i];
      im[i] = 0f;
    }
    for (int i = n; i < fftSize; i++) {
      re[i] = 0f;
      im[i] = 0f;
    }

    fft.forward(re, im);
    fft.magnitudes(re, im, magnitudes);

    return new SpectrumSnapshot(block.frameIndex(), block.timestampNanos(), channel, sampleRate,
        fftSize, magnitudes);
  }

  /** @return the configured FFT size */
  public int fftSize() {
    return fftSize;
  }

  /** Compute a Hann window of length {@code n}. */
  private static float[] hannWindow(int n) {
    float[] w = new float[n];
    if (n <= 1) {
      if (n == 1) {
        w[0] = 1f;
      }
      return w;
    }
    double scale = 2.0 * Math.PI / (n - 1);
    for (int i = 0; i < n; i++) {
      w[i] = (float) (0.5 * (1.0 - Math.cos(scale * i)));
    }
    return w;
  }
}
