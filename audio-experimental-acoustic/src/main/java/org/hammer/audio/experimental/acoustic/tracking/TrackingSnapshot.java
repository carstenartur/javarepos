package org.hammer.audio.experimental.acoustic.tracking;

import java.util.List;
import java.util.Objects;

/**
 * Visualization-ready output of one {@link TrackingPipeline} step.
 *
 * @param sourceFrameIndex frame index of the analysed audio block
 * @param sourceTimestampNanos capture timestamp of the analysed audio block
 * @param clusters frequency clusters detected in this frame (after multi-peak + clustering),
 *     ordered by total magnitude (descending)
 * @param tracks currently active tracked sources (immutable, sorted by id)
 * @param processingNanos wall-clock processing time of the pipeline for this block
 */
public record TrackingSnapshot(
    long sourceFrameIndex,
    long sourceTimestampNanos,
    List<FrequencyCluster> clusters,
    List<TrackedSource> tracks,
    long processingNanos) {

  /** Validate and defensively copy lists. */
  public TrackingSnapshot {
    Objects.requireNonNull(clusters, "clusters");
    Objects.requireNonNull(tracks, "tracks");
    if (processingNanos < 0L) {
      throw new IllegalArgumentException("processingNanos must be >= 0");
    }
    clusters = List.copyOf(clusters);
    tracks = List.copyOf(tracks);
  }
}
