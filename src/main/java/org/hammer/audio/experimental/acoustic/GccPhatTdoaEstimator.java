package org.hammer.audio.experimental.acoustic;

import org.hammer.audio.acquisition.Microphone;
import org.hammer.audio.acquisition.MicrophoneArray;
import org.hammer.audio.core.AudioBlock;

/** Experimental GCC-PHAT-style TDOA estimator using phase-weighted direct correlation. */
public final class GccPhatTdoaEstimator implements TdoaEstimator {

  private static final double EPSILON = 1.0e-12;
  private final double speedOfSoundMetersPerSecond;

  /** Create a GCC-PHAT estimator with a propagation speed. */
  public GccPhatTdoaEstimator(double speedOfSoundMetersPerSecond) {
    if (!(speedOfSoundMetersPerSecond > 0.0) || !Double.isFinite(speedOfSoundMetersPerSecond)) {
      throw new IllegalArgumentException("speedOfSoundMetersPerSecond must be finite and > 0");
    }
    this.speedOfSoundMetersPerSecond = speedOfSoundMetersPerSecond;
  }

  @Override
  public TdoaEstimate estimate(
      AudioBlock block, MicrophoneArray array, int firstChannel, int secondChannel) {
    Microphone first = array.microphone(firstChannel);
    Microphone second = array.microphone(secondChannel);
    float[] a = block.channelView(firstChannel);
    float[] b = block.channelView(secondChannel);
    int frames = Math.min(a.length, b.length);
    int maxLag = Math.min(frames - 1, maxPhysicalLag(block, first, second));
    int bestLag = 0;
    double bestScore = Double.NEGATIVE_INFINITY;
    for (int lag = -maxLag; lag <= maxLag; lag++) {
      double score = phatScore(a, b, frames, lag);
      if (score > bestScore) {
        bestScore = score;
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
        Math.min(1.0, Math.max(0.0, bestScore)));
  }

  private int maxPhysicalLag(AudioBlock block, Microphone first, Microphone second) {
    double spacing = first.positionMeters().distanceTo(second.positionMeters());
    return (int) Math.ceil(spacing * block.format().sampleRate() / speedOfSoundMetersPerSecond);
  }

  private static double phatScore(float[] a, float[] b, int frames, int lag) {
    int aStart = Math.max(0, -lag);
    int bStart = Math.max(0, lag);
    int overlap = frames - Math.abs(lag);
    double sum = 0.0;
    for (int i = 0; i < overlap; i++) {
      double product = a[aStart + i] * b[bStart + i];
      sum += product / (Math.abs(product) + EPSILON);
    }
    return overlap > 0 ? Math.abs(sum / overlap) : 0.0;
  }
}
