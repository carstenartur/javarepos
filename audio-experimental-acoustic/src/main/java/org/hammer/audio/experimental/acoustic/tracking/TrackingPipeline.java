package org.hammer.audio.experimental.acoustic.tracking;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.hammer.audio.acquisition.MicrophoneArray;
import org.hammer.audio.core.AudioBlock;
import org.hammer.audio.experimental.acoustic.DelayAndSumBeamformer;
import org.hammer.audio.experimental.acoustic.TdoaEstimator;
import org.hammer.audio.geometry.Vector2;

/**
 * Coherent real-time multi-source tracking pipeline.
 *
 * <p>For each input {@link AudioBlock} the pipeline executes the following stages, in order:
 *
 * <ol>
 *   <li>Per-channel multi-peak FFT detection ({@link MultiPeakDetector}).
 *   <li>Cross-channel frequency clustering ({@link FrequencyClusterer}).
 *   <li>Optional per-cluster TDOA estimation across all microphone pairs (currently informational;
 *       used as a consistency input for downstream tools).
 *   <li>Localization via delay-and-sum beamforming ({@link DelayAndSumBeamformer}) over a
 *       caller-supplied 2D candidate grid; one best position per cluster.
 *   <li>Temporal tracking via {@link SourceTracker} (identity persistence + Kalman smoothing).
 * </ol>
 *
 * <p>Real-time readiness is supported via {@link FrameSchedule} and {@link ProcessingBudget}. The
 * pipeline never allocates unbounded data structures per frame; the only per-frame allocations are
 * the small lists carried in the produced {@link TrackingSnapshot}.
 *
 * <p>Instances are single-threaded and stateful: the {@link SourceTracker} carries information
 * between frames. Create one pipeline per processing thread.
 */
public final class TrackingPipeline {

  private final MultiPeakDetector peakDetector;
  private final FrequencyClusterer clusterer;
  private final TdoaEstimator tdoaEstimator;
  private final DelayAndSumBeamformer beamformer;
  private final SourceTracker tracker;
  private final List<Vector2> candidateGrid;
  private final FrameSchedule schedule;

  /** Configure a pipeline. {@code schedule} may be {@code null} to disable budget reporting. */
  public TrackingPipeline(
      MultiPeakDetector peakDetector,
      FrequencyClusterer clusterer,
      TdoaEstimator tdoaEstimator,
      DelayAndSumBeamformer beamformer,
      SourceTracker tracker,
      List<Vector2> candidateGrid,
      FrameSchedule schedule) {
    this.peakDetector = Objects.requireNonNull(peakDetector, "peakDetector");
    this.clusterer = Objects.requireNonNull(clusterer, "clusterer");
    this.tdoaEstimator = Objects.requireNonNull(tdoaEstimator, "tdoaEstimator");
    this.beamformer = Objects.requireNonNull(beamformer, "beamformer");
    this.tracker = Objects.requireNonNull(tracker, "tracker");
    Objects.requireNonNull(candidateGrid, "candidateGrid");
    if (candidateGrid.isEmpty()) {
      throw new IllegalArgumentException("candidateGrid must not be empty");
    }
    this.candidateGrid = List.copyOf(candidateGrid);
    this.schedule = schedule;
  }

  /** Process one synchronized multichannel block and emit a tracking snapshot. */
  public TrackingSnapshot process(AudioBlock block, MicrophoneArray array) {
    Objects.requireNonNull(block, "block");
    Objects.requireNonNull(array, "array");
    if (block.channels() != array.channels()) {
      throw new IllegalArgumentException("block channel count must match microphone array");
    }
    long startNanos = System.nanoTime();
    List<List<DetectedPeak>> perChannel = peakDetector.detectAllChannels(block);
    List<FrequencyCluster> clusters = clusterer.clusterPerChannel(perChannel);

    List<SourceTracker.Observation> observations = new ArrayList<>(clusters.size());
    for (FrequencyCluster cluster : clusters) {
      // Run TDOA across all pairs purely for consistency reporting; the beamformer is the
      // primary localizer. Future stages can use these estimates as additional constraints.
      for (int first = 0; first < array.channels(); first++) {
        for (int second = first + 1; second < array.channels(); second++) {
          tdoaEstimator.estimate(block, array, first, second);
        }
      }
      DelayAndSumBeamformer.BeamformingPoint best = beamformer.best(block, array, candidateGrid);
      observations.add(
          new SourceTracker.Observation(cluster.centerFrequencyHz(), best.positionMeters()));
    }

    double timestampSeconds = block.timestampNanos() / 1.0e9;
    List<TrackedSource> tracks = tracker.update(block.frameIndex(), timestampSeconds, observations);
    long processingNanos = System.nanoTime() - startNanos;
    return new TrackingSnapshot(
        block.frameIndex(), block.timestampNanos(), clusters, tracks, processingNanos);
  }

  /** Snapshot the underlying tracker without consuming a new block. */
  public List<TrackedSource> currentTracks() {
    return tracker.snapshot();
  }

  /** Reset internal tracking state. */
  public void reset() {
    tracker.reset();
  }

  /** Frame schedule the pipeline was configured against, or {@code null} when unspecified. */
  public FrameSchedule schedule() {
    return schedule;
  }
}
