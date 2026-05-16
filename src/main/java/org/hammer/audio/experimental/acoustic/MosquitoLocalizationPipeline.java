package org.hammer.audio.experimental.acoustic;

import java.util.ArrayList;
import java.util.List;
import org.hammer.audio.acquisition.MicrophoneArray;
import org.hammer.audio.core.AudioBlock;
import org.hammer.audio.experimental.acoustic.DelayAndSumBeamformer.BeamformingPoint;
import org.hammer.audio.geometry.LocalizationConstraint2D;
import org.hammer.audio.geometry.Vector2;

/** Example experimental pipeline combining frequency tracking, TDOA and beamforming. */
public final class MosquitoLocalizationPipeline {

  private final WingbeatFrequencyTracker frequencyTracker;
  private final TdoaEstimator tdoaEstimator;
  private final DelayAndSumBeamformer beamformer;
  private final List<Vector2> candidateGrid;

  /** Create a pipeline from interchangeable experimental stages. */
  public MosquitoLocalizationPipeline(
      WingbeatFrequencyTracker frequencyTracker,
      TdoaEstimator tdoaEstimator,
      DelayAndSumBeamformer beamformer,
      List<Vector2> candidateGrid) {
    this.frequencyTracker = frequencyTracker;
    this.tdoaEstimator = tdoaEstimator;
    this.beamformer = beamformer;
    this.candidateGrid = List.copyOf(candidateGrid);
    if (this.candidateGrid.isEmpty()) {
      throw new IllegalArgumentException("candidateGrid must not be empty");
    }
  }

  /** Analyze one synchronized multichannel block. */
  public AcousticLocalizationSnapshot analyze(AudioBlock block, MicrophoneArray array) {
    SpectralPeak peak = frequencyTracker.track(block, 0);
    List<TdoaEstimate> estimates = new ArrayList<>();
    List<LocalizationConstraint2D> constraints = new ArrayList<>();
    for (int channel = 1; channel < array.channels(); channel++) {
      TdoaEstimate estimate = tdoaEstimator.estimate(block, array, 0, channel);
      estimates.add(estimate);
      constraints.add(estimate.asConstraint());
    }
    List<BeamformingPoint> heatmap = beamformer.scan(block, array, candidateGrid);
    Vector2 bestPosition = beamformer.best(block, array, candidateGrid).positionMeters();
    return new AcousticLocalizationSnapshot(
        block.frameIndex(),
        block.timestampNanos(),
        peak,
        estimates,
        constraints,
        heatmap,
        bestPosition);
  }
}
