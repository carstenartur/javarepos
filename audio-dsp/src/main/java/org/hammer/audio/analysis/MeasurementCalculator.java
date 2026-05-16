package org.hammer.audio.analysis;

import org.hammer.audio.core.AudioBlock;

/** Computes robust aggregate UI measurements for the latest block/spectrum. */
public final class MeasurementCalculator {

  private static final double CLIPPING_THRESHOLD = 0.999;
  private static final double EPSILON = 1e-12;

  /**
   * Calculate current measurements.
   *
   * @param block latest audio block (may be {@code null})
   * @param spectrum latest spectrum (may be {@code null})
   * @return immutable measurement snapshot
   */
  public MeasurementSnapshot calculate(AudioBlock block, SpectrumSnapshot spectrum) {
    double dominantFrequency = dominantFrequencyHz(spectrum);
    if (block == null || block.channels() <= 0 || block.frames() <= 0) {
      return new MeasurementSnapshot(0.0, 0.0, dominantFrequency, Double.NaN, false, false);
    }

    double sumSquares = 0.0;
    double peak = 0.0;
    long sampleCount = 0L;
    boolean clipping = false;

    for (int channel = 0; channel < block.channels(); channel++) {
      float[] samples = block.channelView(channel);
      for (int i = 0; i < block.frames(); i++) {
        double sample = samples[i];
        double absSample = Math.abs(sample);
        sumSquares += sample * sample;
        peak = Math.max(peak, absSample);
        clipping |= absSample >= CLIPPING_THRESHOLD;
        sampleCount++;
      }
    }

    double rms = sampleCount == 0 ? 0.0 : Math.sqrt(sumSquares / sampleCount);
    double stereoCorrelation = stereoCorrelation(block);
    boolean stereoCorrelationAvailable = !Double.isNaN(stereoCorrelation);
    return new MeasurementSnapshot(
        rms, peak, dominantFrequency, stereoCorrelation, stereoCorrelationAvailable, clipping);
  }

  private static double dominantFrequencyHz(SpectrumSnapshot spectrum) {
    if (spectrum == null || spectrum.binCount() <= 1) {
      return Double.NaN;
    }
    int peakBin = -1;
    float peakMagnitude = 0f;
    for (int bin = 1; bin < spectrum.binCount(); bin++) {
      float magnitude = spectrum.magnitude(bin);
      if (magnitude > peakMagnitude) {
        peakMagnitude = magnitude;
        peakBin = bin;
      }
    }
    return peakBin < 0 ? Double.NaN : spectrum.frequencyOfBin(peakBin);
  }

  private static double stereoCorrelation(AudioBlock block) {
    if (block.channels() < 2 || block.frames() <= 0) {
      return Double.NaN;
    }
    float[] left = block.channelView(0);
    float[] right = block.channelView(1);
    int frames = block.frames();

    double meanLeft = 0.0;
    double meanRight = 0.0;
    for (int i = 0; i < frames; i++) {
      meanLeft += left[i];
      meanRight += right[i];
    }
    meanLeft /= frames;
    meanRight /= frames;

    double covariance = 0.0;
    double varianceLeft = 0.0;
    double varianceRight = 0.0;
    for (int i = 0; i < frames; i++) {
      double leftCentered = left[i] - meanLeft;
      double rightCentered = right[i] - meanRight;
      covariance += leftCentered * rightCentered;
      varianceLeft += leftCentered * leftCentered;
      varianceRight += rightCentered * rightCentered;
    }

    double denominator = Math.sqrt(varianceLeft * varianceRight);
    if (denominator <= EPSILON) {
      return Double.NaN;
    }
    double correlation = covariance / denominator;
    if (correlation > 1.0) {
      return 1.0;
    }
    if (correlation < -1.0) {
      return -1.0;
    }
    return correlation;
  }
}
