package org.hammer.audio.experimental.acoustic.doppler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.hammer.audio.experimental.acoustic.tracking.DetectedPeak;

/** Multi-microphone Doppler estimator with median absolute-deviation outlier rejection. */
public final class SimpleMultiSensorDopplerEstimator implements MultiSensorDopplerEstimator {

  private final DopplerEstimator dopplerEstimator;
  private final double outlierThresholdMetersPerSecond;

  /** Create a multi-sensor estimator. */
  public SimpleMultiSensorDopplerEstimator(
      DopplerEstimator dopplerEstimator, double outlierThresholdMetersPerSecond) {
    this.dopplerEstimator = Objects.requireNonNull(dopplerEstimator, "dopplerEstimator");
    if (!Double.isFinite(outlierThresholdMetersPerSecond)
        || outlierThresholdMetersPerSecond < 0.0) {
      throw new IllegalArgumentException("outlierThresholdMetersPerSecond must be finite and >= 0");
    }
    this.outlierThresholdMetersPerSecond = outlierThresholdMetersPerSecond;
  }

  /** Default configuration for insect-scale acoustic tracking. */
  public static SimpleMultiSensorDopplerEstimator withDefaults() {
    return new SimpleMultiSensorDopplerEstimator(new SimpleDopplerEstimator(), 8.0);
  }

  @Override
  public List<RadialVelocityEstimate> estimateRadialVelocities(SourceObservation observation) {
    Objects.requireNonNull(observation, "observation");
    List<RadialVelocityEstimate> estimates = new ArrayList<>();
    for (DetectedPeak peak : observation.perMicrophonePeaks()) {
      double radial =
          dopplerEstimator.estimateRadialVelocity(
              peak.frequencyHz(), observation.referenceFrequencyHz());
      estimates.add(
          new RadialVelocityEstimate(
              peak.channel(),
              peak.frequencyHz(),
              observation.referenceFrequencyHz(),
              radial,
              Math.max(peak.magnitude(), peak.signalToNoiseRatio())));
    }
    if (estimates.size() < 3 || outlierThresholdMetersPerSecond == 0.0) {
      return List.copyOf(estimates);
    }
    double median =
        median(
            estimates.stream()
                .mapToDouble(RadialVelocityEstimate::radialVelocityMetersPerSecond)
                .toArray());
    double mad =
        median(
            estimates.stream()
                .mapToDouble(e -> Math.abs(e.radialVelocityMetersPerSecond() - median))
                .toArray());
    double threshold = Math.max(outlierThresholdMetersPerSecond, 3.0 * mad);
    List<RadialVelocityEstimate> filtered = new ArrayList<>();
    for (RadialVelocityEstimate estimate : estimates) {
      if (Math.abs(estimate.radialVelocityMetersPerSecond() - median) <= threshold) {
        filtered.add(estimate);
      }
    }
    return List.copyOf(filtered);
  }

  private static double median(double[] values) {
    List<Double> sorted = new ArrayList<>(values.length);
    for (double value : values) {
      sorted.add(value);
    }
    sorted.sort(Comparator.naturalOrder());
    int middle = sorted.size() / 2;
    if (sorted.size() % 2 == 1) {
      return sorted.get(middle);
    }
    return (sorted.get(middle - 1) + sorted.get(middle)) * 0.5;
  }
}
