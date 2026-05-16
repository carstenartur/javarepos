package org.hammer.audio.experimental.acoustic;

import org.hammer.audio.acquisition.Microphone;
import org.hammer.audio.acquisition.MicrophoneArray;
import org.hammer.audio.core.AudioBlock;

/** Normalized cross-correlation TDOA estimator for offline replay and deterministic experiments. */
public final class CrossCorrelationTdoaEstimator implements TdoaEstimator {

  private final double speedOfSoundMetersPerSecond;

  /** Create an estimator with a propagation speed. */
  public CrossCorrelationTdoaEstimator(double speedOfSoundMetersPerSecond) {
    if (!(speedOfSoundMetersPerSecond > 0.0) || !Double.isFinite(speedOfSoundMetersPerSecond)) {
      throw new IllegalArgumentException("speedOfSoundMetersPerSecond must be finite and > 0");
    }
    this.speedOfSoundMetersPerSecond = speedOfSoundMetersPerSecond;
  }

  @Override
  public TdoaEstimate estimate(AudioBlock block, MicrophoneArray array, int firstChannel, int secondChannel) {
    Microphone first = array.microphone(firstChannel);
    Microphone second = array.microphone(secondChannel);
    float[] a = block.channelView(firstChannel);
    float[] b = block.channelView(secondChannel);
    int frames = Math.min(a.length, b.length);
    int maxLag = Math.min(frames - 1, maxPhysicalLag(block, first, second));
    int bestLag = 0;
    double bestCorrelation = 0.0;
    for (int lag = -maxLag; lag <= maxLag; lag++) {
      double correlation = normalizedCorrelation(a, b, frames, lag);
      if (Math.abs(correlation) > Math.abs(bestCorrelation)) {
        bestCorrelation = correlation;
        bestLag = lag;
      }
    }
    double delaySeconds = bestLag / block.format().sampleRate();
    return new TdoaEstimate(
        first.id(),
        second.id(),
        bestLag,
        delaySeconds,
        delaySeconds * speedOfSoundMetersPerSecond,
        Math.min(1.0, Math.abs(bestCorrelation)));
  }

  private int maxPhysicalLag(AudioBlock block, Microphone first, Microphone second) {
    double spacing = first.positionMeters().distanceTo(second.positionMeters());
    return (int) Math.ceil(spacing * block.format().sampleRate() / speedOfSoundMetersPerSecond);
  }

  private static double normalizedCorrelation(float[] a, float[] b, int frames, int lag) {
    int aStart = Math.max(0, -lag);
    int bStart = Math.max(0, lag);
    int overlap = frames - Math.abs(lag);
    double sum = 0.0;
    double aEnergy = 0.0;
    double bEnergy = 0.0;
    for (int i = 0; i < overlap; i++) {
      double av = a[aStart + i];
      double bv = b[bStart + i];
      sum += av * bv;
      aEnergy += av * av;
      bEnergy += bv * bv;
    }
    double denom = Math.sqrt(aEnergy * bEnergy);
    return denom > 0.0 ? sum / denom : 0.0;
  }
}
