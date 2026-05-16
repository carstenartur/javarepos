package org.hammer.audio.localization;

import java.util.Objects;
import org.hammer.audio.analysis.AnalysisSnapshot;

/** Immutable result of stereo inter-channel delay analysis. */
public final class StereoDelaySnapshot implements AnalysisSnapshot {

  private final long sourceFrameIndex;
  private final long sourceTimestampNanos;
  private final StereoDelayStatus status;
  private final int delaySamples;
  private final double delayMillis;
  private final double pathLengthDifferenceMeters;
  private final double angleDegrees;
  private final double confidence;
  private final double microphoneSpacingMeters;
  private final double speedOfSoundMetersPerSecond;
  private final int minCorrelationLagSamples;
  private final float[] correlationByLag;

  /**
   * Create a stereo delay snapshot.
   *
   * @param sourceFrameIndex source block frame index
   * @param sourceTimestampNanos source block timestamp
   * @param status validity status
   * @param delaySamples estimated right-channel delay relative to left, in samples
   * @param delayMillis estimated delay in milliseconds
   * @param pathLengthDifferenceMeters path-length difference implied by delay
   * @param angleDegrees approximate angle of arrival from broadside, or NaN
   * @param confidence normalized cross-correlation confidence
   * @param microphoneSpacingMeters microphone spacing used for physical checks
   * @param speedOfSoundMetersPerSecond speed of sound used for conversion
   * @param minCorrelationLagSamples lag represented by index zero of correlationByLag
   * @param correlationByLag normalized cross-correlation curve, defensively copied
   */
  public StereoDelaySnapshot(
      long sourceFrameIndex,
      long sourceTimestampNanos,
      StereoDelayStatus status,
      int delaySamples,
      double delayMillis,
      double pathLengthDifferenceMeters,
      double angleDegrees,
      double confidence,
      double microphoneSpacingMeters,
      double speedOfSoundMetersPerSecond,
      int minCorrelationLagSamples,
      float[] correlationByLag) {
    this.sourceFrameIndex = sourceFrameIndex;
    this.sourceTimestampNanos = sourceTimestampNanos;
    this.status = Objects.requireNonNull(status, "status");
    this.delaySamples = delaySamples;
    this.delayMillis = delayMillis;
    this.pathLengthDifferenceMeters = pathLengthDifferenceMeters;
    this.angleDegrees = angleDegrees;
    this.confidence = confidence;
    this.microphoneSpacingMeters = microphoneSpacingMeters;
    this.speedOfSoundMetersPerSecond = speedOfSoundMetersPerSecond;
    this.minCorrelationLagSamples = minCorrelationLagSamples;
    this.correlationByLag = correlationByLag == null ? new float[0] : correlationByLag.clone();
  }

  @Override
  public long sourceFrameIndex() {
    return sourceFrameIndex;
  }

  @Override
  public long sourceTimestampNanos() {
    return sourceTimestampNanos;
  }

  public StereoDelayStatus status() {
    return status;
  }

  public boolean valid() {
    return status == StereoDelayStatus.VALID;
  }

  public int delaySamples() {
    return delaySamples;
  }

  public double delayMillis() {
    return delayMillis;
  }

  public double pathLengthDifferenceMeters() {
    return pathLengthDifferenceMeters;
  }

  public double angleDegrees() {
    return angleDegrees;
  }

  public double confidence() {
    return confidence;
  }

  public double microphoneSpacingMeters() {
    return microphoneSpacingMeters;
  }

  public double speedOfSoundMetersPerSecond() {
    return speedOfSoundMetersPerSecond;
  }

  public int minCorrelationLagSamples() {
    return minCorrelationLagSamples;
  }

  public int correlationLagForIndex(int index) {
    return minCorrelationLagSamples + index;
  }

  public float[] correlationByLag() {
    return correlationByLag.clone();
  }
}
