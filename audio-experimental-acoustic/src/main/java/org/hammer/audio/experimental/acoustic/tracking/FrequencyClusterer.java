package org.hammer.audio.experimental.acoustic.tracking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Groups {@link DetectedPeak detected peaks} across microphone channels into stable {@link
 * FrequencyCluster frequency clusters}.
 *
 * <p>Peaks are sorted by magnitude and assigned greedily to the nearest existing cluster (in Hz),
 * subject to a configurable matching tolerance. The tolerance can be specified either in absolute
 * Hz or in cents (1200 cents = one octave); both are honoured and a peak matches when it falls
 * within either tolerance. This combined matching gives stable behaviour when two sources sit close
 * in frequency: the matching tolerance keeps each cluster narrow enough to keep them apart while
 * the cents tolerance avoids over-splitting at high frequencies where the absolute spacing
 * naturally grows.
 *
 * <p>Each cluster must collect peaks from at least {@code minPeaksPerCluster} distinct channels to
 * be emitted; transient single-channel artefacts are dropped. The output is ordered by total
 * magnitude (descending) and capped at {@code maxClusters}.
 */
public final class FrequencyClusterer {

  private final double matchToleranceHz;
  private final double matchToleranceCents;
  private final int minPeaksPerCluster;
  private final int maxClusters;

  /**
   * Configure a clusterer.
   *
   * @param matchToleranceHz absolute matching tolerance in Hz (&gt;= 0)
   * @param matchToleranceCents relative matching tolerance in cents (&gt;= 0)
   * @param minPeaksPerCluster minimum number of channels that must contribute a peak (&gt;= 1)
   * @param maxClusters maximum number of clusters returned (&gt;= 1)
   */
  public FrequencyClusterer(
      double matchToleranceHz,
      double matchToleranceCents,
      int minPeaksPerCluster,
      int maxClusters) {
    if (!Double.isFinite(matchToleranceHz) || matchToleranceHz < 0.0) {
      throw new IllegalArgumentException("matchToleranceHz must be finite and >= 0");
    }
    if (!Double.isFinite(matchToleranceCents) || matchToleranceCents < 0.0) {
      throw new IllegalArgumentException("matchToleranceCents must be finite and >= 0");
    }
    if (minPeaksPerCluster < 1) {
      throw new IllegalArgumentException("minPeaksPerCluster must be >= 1");
    }
    if (maxClusters < 1) {
      throw new IllegalArgumentException("maxClusters must be >= 1");
    }
    this.matchToleranceHz = matchToleranceHz;
    this.matchToleranceCents = matchToleranceCents;
    this.minPeaksPerCluster = minPeaksPerCluster;
    this.maxClusters = maxClusters;
  }

  /** Cluster peaks supplied as one list per channel. */
  public List<FrequencyCluster> clusterPerChannel(
      Collection<? extends Collection<DetectedPeak>> perChannel) {
    if (perChannel == null) {
      throw new IllegalArgumentException("perChannel must not be null");
    }
    List<DetectedPeak> all = new ArrayList<>();
    for (Collection<DetectedPeak> peaks : perChannel) {
      if (peaks != null) {
        all.addAll(peaks);
      }
    }
    return cluster(all);
  }

  /** Cluster a flat collection of peaks from possibly several channels. */
  public List<FrequencyCluster> cluster(Collection<DetectedPeak> peaks) {
    if (peaks == null) {
      throw new IllegalArgumentException("peaks must not be null");
    }
    List<DetectedPeak> sorted = new ArrayList<>(peaks);
    sorted.sort(Comparator.comparingDouble(DetectedPeak::magnitude).reversed());

    List<List<DetectedPeak>> buckets = new ArrayList<>();
    List<Double> bucketCenters = new ArrayList<>();
    for (DetectedPeak peak : sorted) {
      int matched = findMatch(bucketCenters, peak.frequencyHz());
      if (matched >= 0 && hasChannel(buckets.get(matched), peak.channel())) {
        // Already have a peak for this channel in the cluster; skip the weaker one.
        continue;
      }
      if (matched < 0) {
        List<DetectedPeak> bucket = new ArrayList<>();
        bucket.add(peak);
        buckets.add(bucket);
        bucketCenters.add(peak.frequencyHz());
      } else {
        buckets.get(matched).add(peak);
        bucketCenters.set(matched, weightedCentre(buckets.get(matched)));
      }
    }

    List<FrequencyCluster> clusters = new ArrayList<>();
    for (int i = 0; i < buckets.size(); i++) {
      List<DetectedPeak> bucket = buckets.get(i);
      if (countDistinctChannels(bucket) < minPeaksPerCluster) {
        continue;
      }
      double totalMagnitude = 0.0;
      for (DetectedPeak peak : bucket) {
        totalMagnitude += peak.magnitude();
      }
      clusters.add(new FrequencyCluster(bucketCenters.get(i), totalMagnitude, bucket));
    }
    clusters.sort(Comparator.comparingDouble(FrequencyCluster::totalMagnitude).reversed());
    if (clusters.size() > maxClusters) {
      return Collections.unmodifiableList(new ArrayList<>(clusters.subList(0, maxClusters)));
    }
    return Collections.unmodifiableList(clusters);
  }

  private int findMatch(List<Double> centres, double frequencyHz) {
    int best = -1;
    double bestDelta = Double.POSITIVE_INFINITY;
    for (int i = 0; i < centres.size(); i++) {
      double centre = centres.get(i);
      double delta = Math.abs(centre - frequencyHz);
      if (matches(centre, frequencyHz) && delta < bestDelta) {
        bestDelta = delta;
        best = i;
      }
    }
    return best;
  }

  private boolean matches(double centre, double frequencyHz) {
    if (Math.abs(centre - frequencyHz) <= matchToleranceHz) {
      return true;
    }
    if (matchToleranceCents <= 0.0 || centre <= 0.0 || frequencyHz <= 0.0) {
      return false;
    }
    double cents = 1200.0 * Math.log(frequencyHz / centre) / Math.log(2.0);
    return Math.abs(cents) <= matchToleranceCents;
  }

  private static boolean hasChannel(List<DetectedPeak> bucket, int channel) {
    for (DetectedPeak peak : bucket) {
      if (peak.channel() == channel) {
        return true;
      }
    }
    return false;
  }

  private static int countDistinctChannels(List<DetectedPeak> bucket) {
    int mask = 0;
    int count = 0;
    for (DetectedPeak peak : bucket) {
      int bit = 1 << Math.min(peak.channel(), 30);
      if ((mask & bit) == 0) {
        mask |= bit;
        count++;
      }
    }
    return count;
  }

  private static double weightedCentre(List<DetectedPeak> bucket) {
    double sum = 0.0;
    double weight = 0.0;
    for (DetectedPeak peak : bucket) {
      sum += peak.frequencyHz() * peak.magnitude();
      weight += peak.magnitude();
    }
    return weight > 0.0 ? sum / weight : bucket.get(0).frequencyHz();
  }
}
