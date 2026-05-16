package org.hammer.audio.experimental.acoustic;

import org.hammer.audio.acquisition.Microphone;
import org.hammer.audio.acquisition.MicrophoneArray;
import org.hammer.audio.analysis.Fft;
import org.hammer.audio.core.AudioBlock;

/**
 * Experimental frequency-domain GCC-PHAT TDOA estimator.
 *
 * <p>This implementation zero-pads both channels, computes the cross-power spectrum, applies PHAT
 * weighting, and searches the inverse transform for the strongest physically plausible lag.
 * Sub-sample interpolation and robust reverberation rejection remain experimental future work.
 */
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
    double[] correlation = gccPhatCorrelation(a, b, frames);
    LagScore lagScore = strongestLag(correlation, maxLag);
    int bestLag = lagScore.lag();
    double delaySeconds = bestLag / block.format().sampleRate();
    return new TdoaEstimate(
        first.id(),
        second.id(),
        bestLag,
        delaySeconds,
        delaySeconds * speedOfSoundMetersPerSecond,
        lagScore.confidence());
  }

  private int maxPhysicalLag(AudioBlock block, Microphone first, Microphone second) {
    double spacing = first.positionMeters().distanceTo(second.positionMeters());
    return (int) Math.ceil(spacing * block.format().sampleRate() / speedOfSoundMetersPerSecond);
  }

  private static double[] gccPhatCorrelation(float[] a, float[] b, int frames) {
    int fftSize = nextPowerOfTwo(frames * 2);
    float[] aRe = new float[fftSize];
    float[] aIm = new float[fftSize];
    float[] bRe = new float[fftSize];
    float[] bIm = new float[fftSize];
    System.arraycopy(a, 0, aRe, 0, frames);
    System.arraycopy(b, 0, bRe, 0, frames);

    Fft fft = new Fft(fftSize);
    fft.forward(aRe, aIm);
    fft.forward(bRe, bIm);

    float[] crossRe = new float[fftSize];
    float[] crossIm = new float[fftSize];
    for (int bin = 0; bin < fftSize; bin++) {
      double real = bRe[bin] * aRe[bin] + bIm[bin] * aIm[bin];
      double imaginary = bIm[bin] * aRe[bin] - bRe[bin] * aIm[bin];
      double magnitude = Math.hypot(real, imaginary);
      if (magnitude > EPSILON) {
        crossRe[bin] = (float) (real / magnitude);
        crossIm[bin] = (float) (imaginary / magnitude);
      }
    }

    inverse(fft, crossRe, crossIm);
    double[] correlation = new double[fftSize];
    for (int i = 0; i < fftSize; i++) {
      correlation[i] = crossRe[i];
    }
    return correlation;
  }

  private static LagScore strongestLag(double[] correlation, int maxLag) {
    int bestLag = 0;
    double bestScore = Double.NEGATIVE_INFINITY;
    double totalScore = 0.0;
    for (int lag = -maxLag; lag <= maxLag; lag++) {
      int index = lag >= 0 ? lag : correlation.length + lag;
      double score = Math.abs(correlation[index]);
      totalScore += score;
      if (score > bestScore) {
        bestScore = score;
        bestLag = lag;
      }
    }
    double confidence = totalScore > 0.0 ? bestScore / totalScore : 0.0;
    return new LagScore(bestLag, Math.min(1.0, Math.max(0.0, confidence)));
  }

  private static void inverse(Fft fft, float[] re, float[] im) {
    for (int i = 0; i < im.length; i++) {
      im[i] = -im[i];
    }
    fft.forward(re, im);
    for (int i = 0; i < re.length; i++) {
      re[i] /= re.length;
      im[i] = -im[i] / re.length;
    }
  }

  private static int nextPowerOfTwo(int value) {
    int result = 1;
    while (result < value) {
      result <<= 1;
    }
    return Math.max(2, result);
  }

  private record LagScore(int lag, double confidence) {}
}
