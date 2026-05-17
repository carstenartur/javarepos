package org.hammer.audio.experimental.acoustic.tracking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class FrequencyClustererTest {

  @Test
  void groupsClosePeaksFromMultipleChannelsIntoOneCluster() {
    FrequencyClusterer clusterer = new FrequencyClusterer(5.0, 0.0, 2, 4);

    List<FrequencyCluster> clusters =
        clusterer.cluster(
            List.of(
                new DetectedPeak(0, 600.5, 1.0, 10.0),
                new DetectedPeak(1, 599.5, 0.9, 9.0),
                new DetectedPeak(2, 1_201.0, 0.8, 8.0),
                new DetectedPeak(0, 1_200.0, 0.7, 7.0)));

    assertEquals(2, clusters.size());
    assertEquals(600.0, clusters.get(0).centerFrequencyHz(), 1.0);
    assertEquals(1_200.5, clusters.get(1).centerFrequencyHz(), 1.0);
    assertEquals(2, clusters.get(0).channelCount());
  }

  @Test
  void dropsSingleChannelClustersWhenMinPeaksRequired() {
    FrequencyClusterer clusterer = new FrequencyClusterer(5.0, 0.0, 2, 4);

    List<FrequencyCluster> clusters =
        clusterer.cluster(List.of(new DetectedPeak(0, 600.0, 1.0, 10.0)));

    assertTrue(clusters.isEmpty());
  }

  @Test
  void clusterPerChannelMatchesFlatCluster() {
    FrequencyClusterer clusterer = new FrequencyClusterer(5.0, 0.0, 2, 4);
    List<DetectedPeak> channel0 =
        List.of(new DetectedPeak(0, 600.0, 1.0, 10.0), new DetectedPeak(0, 800.0, 0.9, 9.0));
    List<DetectedPeak> channel1 =
        List.of(new DetectedPeak(1, 601.0, 0.95, 10.0), new DetectedPeak(1, 802.0, 0.85, 9.0));

    List<FrequencyCluster> flat =
        clusterer.cluster(
            List.of(channel0.get(0), channel0.get(1), channel1.get(0), channel1.get(1)));
    List<FrequencyCluster> perChannel = clusterer.clusterPerChannel(List.of(channel0, channel1));

    assertEquals(flat.size(), perChannel.size());
    assertEquals(flat.get(0).centerFrequencyHz(), perChannel.get(0).centerFrequencyHz(), 1.0e-9);
  }

  @Test
  void respectsCentsToleranceForHighFrequencies() {
    FrequencyClusterer clusterer = new FrequencyClusterer(0.0, 50.0, 2, 4);

    // 8000 and 8030 Hz differ by ~6.5 cents, far below 50 cents tolerance.
    List<FrequencyCluster> clusters =
        clusterer.cluster(
            List.of(
                new DetectedPeak(0, 8_000.0, 1.0, 10.0), new DetectedPeak(1, 8_030.0, 0.9, 10.0)));
    assertEquals(1, clusters.size());
  }

  @Test
  void respectsMaxClusters() {
    FrequencyClusterer clusterer = new FrequencyClusterer(5.0, 0.0, 1, 1);

    List<FrequencyCluster> clusters =
        clusterer.cluster(
            List.of(new DetectedPeak(0, 600.0, 1.0, 10.0), new DetectedPeak(0, 1_200.0, 0.5, 9.0)));

    assertEquals(1, clusters.size());
    assertEquals(600.0, clusters.get(0).centerFrequencyHz(), 1.0e-9);
  }

  @Test
  void validatesConstructorArguments() {
    assertThrows(IllegalArgumentException.class, () -> new FrequencyClusterer(-1.0, 0.0, 1, 1));
    assertThrows(IllegalArgumentException.class, () -> new FrequencyClusterer(1.0, -1.0, 1, 1));
    assertThrows(IllegalArgumentException.class, () -> new FrequencyClusterer(1.0, 0.0, 0, 1));
    assertThrows(IllegalArgumentException.class, () -> new FrequencyClusterer(1.0, 0.0, 1, 0));
  }
}
