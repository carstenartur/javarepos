package org.hammer.audio.experimental.acoustic.tracking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.hammer.audio.analysis.Fft;
import org.hammer.audio.core.AudioBlock;
import org.hammer.audio.experimental.acoustic.FrequencyBand;

/**
 * Multi-peak detector replacing the single-peak {@code WingbeatFrequencyTracker} as the entry stage
 * of the tracking pipeline.
 *
 * <p>For each channel of an {@link AudioBlock} the detector performs a Hann-windowed real FFT,
 * restricts the spectrum to the configured {@link FrequencyBand}, finds at most {@code maxPeaks}
 * local maxima ordered by magnitude, refines each peak frequency by parabolic interpolation across
 * its three adjacent bins, and rejects peaks whose magnitude is below a configurable signal-to-
 * noise multiple of the band median.
 *
 * <p>The detector is allocation-aware: it reuses internal scratch arrays for the FFT and magnitudes
 * between calls, which makes it safe to use inside a bounded per-frame budget. Each detector
 * instance is single-threaded; create one per processing thread.
 */
public final class MultiPeakDetector {

  private final int fftSize;
  private final FrequencyBand band;
  private final int maxPeaks;
  private final double minSnr;
  private final Fft fft;

  private final float[] re;
  private final float[] im;
  private final float[] magnitudes;

  /**
   * Create a detector with explicit limits.
   *
   * @param fftSize power-of-two FFT length
   * @param band frequency search band
   * @param maxPeaks maximum number of peaks returned per channel (must be &gt;= 1)
   * @param minSnr minimum ratio of peak magnitude to band median; peaks below are dropped (&gt;= 0)
   */
  public MultiPeakDetector(int fftSize, FrequencyBand band, int maxPeaks, double minSnr) {
    if (fftSize <= 0 || (fftSize & (fftSize - 1)) != 0) {
      throw new IllegalArgumentException("fftSize must be a positive power of two");
    }
    Objects.requireNonNull(band, "band");
    if (maxPeaks < 1) {
      throw new IllegalArgumentException("maxPeaks must be >= 1");
    }
    if (!Double.isFinite(minSnr) || minSnr < 0.0) {
      throw new IllegalArgumentException("minSnr must be finite and >= 0");
    }
    this.fftSize = fftSize;
    this.band = band;
    this.maxPeaks = maxPeaks;
    this.minSnr = minSnr;
    this.fft = new Fft(fftSize);
    this.re = new float[fftSize];
    this.im = new float[fftSize];
    this.magnitudes = new float[fftSize / 2 + 1];
  }

  /** Detect peaks on a single channel. The returned list is immutable. */
  public List<DetectedPeak> detect(AudioBlock block, int channel) {
    Objects.requireNonNull(block, "block");
    if (channel < 0 || channel >= block.channels()) {
      throw new IllegalArgumentException("channel out of range: " + channel);
    }
    float[] samples = block.channelView(channel);
    int copied = Math.min(samples.length, fftSize);
    Arrays.fill(re, 0.0f);
    Arrays.fill(im, 0.0f);
    System.arraycopy(samples, 0, re, 0, copied);
    applyHannWindow(re, copied);
    fft.forward(re, im);
    fft.magnitudesOneSided(re, im, magnitudes);

    double sampleRate = block.format().sampleRate();
    int lowBin = Math.max(1, (int) Math.ceil(band.lowHz() * fftSize / sampleRate));
    int highBin =
        Math.min(magnitudes.length - 2, (int) Math.floor(band.highHz() * fftSize / sampleRate));
    if (highBin < lowBin) {
      return List.of();
    }
    double median = bandMedian(lowBin, highBin);

    List<DetectedPeak> peaks = new ArrayList<>();
    for (int bin = lowBin; bin <= highBin; bin++) {
      double mag = magnitudes[bin];
      if (mag <= magnitudes[bin - 1] || mag < magnitudes[bin + 1]) {
        continue;
      }
      double snr = median > 0.0 ? mag / median : Double.POSITIVE_INFINITY;
      if (snr < minSnr) {
        continue;
      }
      double refinedBin = parabolicRefine(bin);
      double frequency = refinedBin * sampleRate / fftSize;
      peaks.add(new DetectedPeak(channel, frequency, mag, snr));
    }
    peaks.sort((a, b) -> Double.compare(b.magnitude(), a.magnitude()));
    if (peaks.size() > maxPeaks) {
      return List.copyOf(peaks.subList(0, maxPeaks));
    }
    return List.copyOf(peaks);
  }

  /** Detect peaks on every channel and return per-channel results in channel order. */
  public List<List<DetectedPeak>> detectAllChannels(AudioBlock block) {
    Objects.requireNonNull(block, "block");
    List<List<DetectedPeak>> result = new ArrayList<>(block.channels());
    for (int channel = 0; channel < block.channels(); channel++) {
      result.add(detect(block, channel));
    }
    return List.copyOf(result);
  }

  private double bandMedian(int lowBin, int highBin) {
    int length = highBin - lowBin + 1;
    float[] bandCopy = new float[length];
    System.arraycopy(magnitudes, lowBin, bandCopy, 0, length);
    Arrays.sort(bandCopy);
    return bandCopy[length / 2];
  }

  private double parabolicRefine(int bin) {
    double left = magnitudes[bin - 1];
    double centre = magnitudes[bin];
    double right = magnitudes[bin + 1];
    double denom = left - 2.0 * centre + right;
    if (denom == 0.0) {
      return bin;
    }
    double offset = 0.5 * (left - right) / denom;
    if (!Double.isFinite(offset) || Math.abs(offset) > 1.0) {
      return bin;
    }
    return bin + offset;
  }

  private static void applyHannWindow(float[] samples, int frames) {
    if (frames <= 1) {
      return;
    }
    double denom = frames - 1.0;
    for (int i = 0; i < frames; i++) {
      samples[i] *= (float) (0.5 - 0.5 * Math.cos(2.0 * Math.PI * i / denom));
    }
  }

  /** Read-only access to the search band for callers wiring downstream stages. */
  public FrequencyBand band() {
    return band;
  }

  /** Read-only access to the maximum number of peaks per channel. */
  public int maxPeaks() {
    return maxPeaks;
  }

  /** Returned for use as an empty-result sentinel where a {@link List#of()} is preferred. */
  static List<DetectedPeak> emptyPeaks() {
    return Collections.emptyList();
  }
}
