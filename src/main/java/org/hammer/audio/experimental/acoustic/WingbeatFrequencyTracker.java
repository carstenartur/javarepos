package org.hammer.audio.experimental.acoustic;

import org.hammer.audio.analysis.Fft;
import org.hammer.audio.core.AudioBlock;

/** Experimental narrow-band frequency tracker for wingbeat-like tonal sources. */
public final class WingbeatFrequencyTracker {

  private final int fftSize;
  private final FrequencyBand searchBand;
  private final Fft fft;

  /** Create a tracker with a power-of-two FFT size and a search band. */
  public WingbeatFrequencyTracker(int fftSize, FrequencyBand searchBand) {
    this.fftSize = fftSize;
    this.searchBand = searchBand;
    this.fft = new Fft(fftSize);
  }

  /** Track the strongest peak in {@code channel}. */
  public SpectralPeak track(AudioBlock block, int channel) {
    float[] samples = block.channelView(channel);
    float[] re = new float[fftSize];
    float[] im = new float[fftSize];
    int copied = Math.min(samples.length, fftSize);
    System.arraycopy(samples, 0, re, 0, copied);
    applyHannWindow(re, copied);
    fft.forward(re, im);
    float[] magnitudes = new float[fftSize / 2 + 1];
    fft.magnitudesOneSided(re, im, magnitudes);

    int lowBin =
        Math.max(0, (int) Math.ceil(searchBand.lowHz() * fftSize / block.format().sampleRate()));
    int highBin =
        Math.min(
            magnitudes.length - 1,
            (int) Math.floor(searchBand.highHz() * fftSize / block.format().sampleRate()));
    int bestBin = lowBin;
    double bestMagnitude = 0.0;
    double bandEnergy = 0.0;
    for (int bin = lowBin; bin <= highBin; bin++) {
      double magnitude = magnitudes[bin];
      bandEnergy += magnitude;
      if (magnitude > bestMagnitude) {
        bestMagnitude = magnitude;
        bestBin = bin;
      }
    }
    double frequency = bestBin * block.format().sampleRate() / fftSize;
    double confidence = bandEnergy > 0.0 ? Math.min(1.0, bestMagnitude / bandEnergy) : 0.0;
    return new SpectralPeak(frequency, bestMagnitude, confidence);
  }

  private static void applyHannWindow(float[] samples, int frames) {
    if (frames <= 1) {
      return;
    }
    for (int i = 0; i < frames; i++) {
      samples[i] *= (float) (0.5 - 0.5 * Math.cos(2.0 * Math.PI * i / (frames - 1)));
    }
  }
}
