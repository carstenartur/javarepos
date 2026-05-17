package org.hammer.audio.experimental.acoustic.doppler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.hammer.audio.experimental.acoustic.tracking.DetectedPeak;

/** Multi-microphone Doppler estimator with median absolute-deviation outlier rejection. */
public final class SimpleMultiSensorDopplerEstimator implements MultiSensorDopplerEstimator {

  private static final double MAD_OUTLIER_MULTIPLIER = 3.0;
  private static final double MAX_LOG_SNR_WEIGHT = Math.log1p(100.0);

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
    List<Double> rawWeights = new ArrayList<>();
    for (DetectedPeak peak : observation.perMicrophonePeaks()) {
      double radial =
          dopplerEstimator.estimateRadialVelocity(
              peak.frequencyHz(), observation.referenceFrequencyHz());
      rawWeights.add(rawWeight(peak));
      estimates.add(
          new RadialVelocityEstimate(
              peak.channel(),
              peak.frequencyHz(),
              observation.referenceFrequencyHz(),
              radial,
              0.0));
    }
    estimates = normalizeWeights(estimates, rawWeights);
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
    double threshold = Math.max(outlierThresholdMetersPerSecond, MAD_OUTLIER_MULTIPLIER * mad);
    List<RadialVelocityEstimate> filtered = new ArrayList<>();
    for (RadialVelocityEstimate estimate : estimates) {
      if (Math.abs(estimate.radialVelocityMetersPerSecond() - median) <= threshold) {
        filtered.add(estimate);
      }
    }
    return normalizeExistingWeights(filtered);
  }

  private static double rawWeight(DetectedPeak peak) {
    double snr = Math.max(0.0, peak.signalToNoiseRatio());
    double magnitude = Math.max(0.0, peak.magnitude());
    double raw = Math.log1p(snr > 0.0 ? snr : magnitude);
    return Math.min(MAX_LOG_SNR_WEIGHT, raw);
  }

  private static List<RadialVelocityEstimate> normalizeWeights(
      List<RadialVelocityEstimate> estimates, List<Double> rawWeights) {
    double total = rawWeights.stream().mapToDouble(Double::doubleValue).sum();
    double fallback = estimates.isEmpty() ? 0.0 : 1.0 / estimates.size();
    List<RadialVelocityEstimate> normalized = new ArrayList<>(estimates.size());
    for (int i = 0; i < estimates.size(); i++) {
      RadialVelocityEstimate estimate = estimates.get(i);
      double weight = total > 0.0 ? rawWeights.get(i) / total : fallback;
      normalized.add(
          new RadialVelocityEstimate(
              estimate.channel(),
              estimate.observedFrequencyHz(),
              estimate.referenceFrequencyHz(),
              estimate.radialVelocityMetersPerSecond(),
              weight));
    }
    return normalized;
  }

  private static List<RadialVelocityEstimate> normalizeExistingWeights(
      List<RadialVelocityEstimate> estimates) {
    double total = estimates.stream().mapToDouble(RadialVelocityEstimate::weight).sum();
    if (total <= 0.0) {
      return List.copyOf(estimates);
    }
    List<RadialVelocityEstimate> normalized = new ArrayList<>(estimates.size());
    for (RadialVelocityEstimate estimate : estimates) {
      normalized.add(
          new RadialVelocityEstimate(
              estimate.channel(),
              estimate.observedFrequencyHz(),
              estimate.referenceFrequencyHz(),
              estimate.radialVelocityMetersPerSecond(),
              estimate.weight() / total));
    }
    return List.copyOf(normalized);
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
