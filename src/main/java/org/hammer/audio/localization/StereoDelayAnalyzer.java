package org.hammer.audio.localization;

import org.hammer.audio.analysis.AnalysisModule;
import org.hammer.audio.core.AudioBlock;

/** Estimates inter-channel delay in stereo audio using normalized cross-correlation. */
public final class StereoDelayAnalyzer implements AnalysisModule<StereoDelaySnapshot> {

  public static final double DEFAULT_MICROPHONE_SPACING_METERS = 0.20;
  public static final double DEFAULT_SPEED_OF_SOUND_METERS_PER_SECOND = 343.0;
  private static final double DEFAULT_MIN_CONFIDENCE = 0.35;
  private static final double SILENCE_RMS_THRESHOLD = 1.0e-4;

  private final double microphoneSpacingMeters;
  private final double speedOfSoundMetersPerSecond;
  private final double minimumConfidence;

  public StereoDelayAnalyzer() {
    this(
        DEFAULT_MICROPHONE_SPACING_METERS,
        DEFAULT_SPEED_OF_SOUND_METERS_PER_SECOND,
        DEFAULT_MIN_CONFIDENCE);
  }

  public StereoDelayAnalyzer(
      double microphoneSpacingMeters,
      double speedOfSoundMetersPerSecond,
      double minimumConfidence) {
    if (!(microphoneSpacingMeters > 0.0) || !Double.isFinite(microphoneSpacingMeters)) {
      throw new IllegalArgumentException(
          "microphoneSpacingMeters must be finite and > 0, was " + microphoneSpacingMeters);
    }
    if (!(speedOfSoundMetersPerSecond > 0.0) || !Double.isFinite(speedOfSoundMetersPerSecond)) {
      throw new IllegalArgumentException(
          "speedOfSoundMetersPerSecond must be finite and > 0, was " + speedOfSoundMetersPerSecond);
    }
    if (minimumConfidence < 0.0 || minimumConfidence > 1.0 || !Double.isFinite(minimumConfidence)) {
      throw new IllegalArgumentException("minimumConfidence must be finite and in [0,1]");
    }
    this.microphoneSpacingMeters = microphoneSpacingMeters;
    this.speedOfSoundMetersPerSecond = speedOfSoundMetersPerSecond;
    this.minimumConfidence = minimumConfidence;
  }

  @Override
  public StereoDelaySnapshot analyze(AudioBlock block) {
    if (block.channels() < 2) {
      return invalid(block, StereoDelayStatus.MONO_INPUT, 0, 0.0, new float[0], 0);
    }

    float[] left = block.channelView(0);
    float[] right = block.channelView(1);
    int frames = Math.min(left.length, right.length);
    if (frames == 0
        || rms(left, frames) < SILENCE_RMS_THRESHOLD
        || rms(right, frames) < SILENCE_RMS_THRESHOLD) {
      return invalid(block, StereoDelayStatus.SILENCE, 0, 0.0, new float[0], 0);
    }

    int maxLag = physicallyPossibleLagSamples(block.format().sampleRate(), frames);
    int minLag = -maxLag;
    float[] correlations = new float[maxLag - minLag + 1];
    int bestLag = 0;
    double bestCorrelation = 0.0;
    for (int lag = minLag; lag <= maxLag; lag++) {
      double correlation = normalizedCorrelation(left, right, frames, lag);
      correlations[lag - minLag] = (float) correlation;
      if (Math.abs(correlation) > Math.abs(bestCorrelation)) {
        bestCorrelation = correlation;
        bestLag = lag;
      }
    }

    double confidence = Math.abs(bestCorrelation);
    double delayMillis = samplesToMillis(bestLag, block.format().sampleRate());
    double pathDifference = samplesToPathDifference(bestLag, block.format().sampleRate());
    if (confidence < minimumConfidence) {
      return invalid(
          block, StereoDelayStatus.LOW_CORRELATION, bestLag, confidence, correlations, minLag);
    }
    if (Math.abs(pathDifference) > microphoneSpacingMeters) {
      return invalid(
          block,
          StereoDelayStatus.DELAY_OUTSIDE_PHYSICAL_RANGE,
          bestLag,
          confidence,
          correlations,
          minLag);
    }
    double ratio = clamp(pathDifference / microphoneSpacingMeters, -1.0, 1.0);
    double angleDegrees = Math.toDegrees(Math.asin(ratio));
    return new StereoDelaySnapshot(
        block.frameIndex(),
        block.timestampNanos(),
        StereoDelayStatus.VALID,
        bestLag,
        delayMillis,
        pathDifference,
        angleDegrees,
        confidence,
        microphoneSpacingMeters,
        speedOfSoundMetersPerSecond,
        minLag,
        correlations);
  }

  public double microphoneSpacingMeters() {
    return microphoneSpacingMeters;
  }

  public double speedOfSoundMetersPerSecond() {
    return speedOfSoundMetersPerSecond;
  }

  private StereoDelaySnapshot invalid(
      AudioBlock block,
      StereoDelayStatus status,
      int delaySamples,
      double confidence,
      float[] correlations,
      int minLag) {
    return new StereoDelaySnapshot(
        block.frameIndex(),
        block.timestampNanos(),
        status,
        delaySamples,
        samplesToMillis(delaySamples, block.format().sampleRate()),
        samplesToPathDifference(delaySamples, block.format().sampleRate()),
        Double.NaN,
        confidence,
        microphoneSpacingMeters,
        speedOfSoundMetersPerSecond,
        minLag,
        correlations);
  }

  private static double rms(float[] samples, int frames) {
    double sumSquares = 0.0;
    for (int i = 0; i < frames; i++) {
      sumSquares += samples[i] * samples[i];
    }
    return Math.sqrt(sumSquares / Math.max(1, frames));
  }

  private int physicallyPossibleLagSamples(float sampleRate, int frames) {
    double maxDelaySeconds = microphoneSpacingMeters / speedOfSoundMetersPerSecond;
    int physicalLag = (int) Math.ceil(maxDelaySeconds * sampleRate);
    return Math.max(0, Math.min(frames - 1, physicalLag));
  }

  private static double normalizedCorrelation(float[] left, float[] right, int frames, int lag) {
    int leftStart = Math.max(0, -lag);
    int rightStart = Math.max(0, lag);
    int overlap = frames - Math.abs(lag);
    if (overlap <= 1) {
      return 0.0;
    }
    double sum = 0.0;
    double leftEnergy = 0.0;
    double rightEnergy = 0.0;
    for (int i = 0; i < overlap; i++) {
      double leftSample = left[leftStart + i];
      double rightSample = right[rightStart + i];
      sum += leftSample * rightSample;
      leftEnergy += leftSample * leftSample;
      rightEnergy += rightSample * rightSample;
    }
    double denominator = Math.sqrt(leftEnergy * rightEnergy);
    return denominator > 0.0 ? sum / denominator : 0.0;
  }

  private double samplesToMillis(int samples, float sampleRate) {
    return 1000.0 * samples / sampleRate;
  }

  private double samplesToPathDifference(int samples, float sampleRate) {
    return speedOfSoundMetersPerSecond * samples / sampleRate;
  }

  private static double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }
}
