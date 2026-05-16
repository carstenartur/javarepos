package org.hammer.audio.experimental.acoustic;

import java.util.List;
import org.hammer.audio.experimental.acoustic.DelayAndSumBeamformer.BeamformingPoint;
import org.hammer.audio.geometry.LocalizationConstraint2D;
import org.hammer.audio.geometry.Vector2;

/** Visualization-ready output from the experimental acoustic localization pipeline. */
public record AcousticLocalizationSnapshot(
    long sourceFrameIndex,
    long sourceTimestampNanos,
    SpectralPeak trackedFrequency,
    List<TdoaEstimate> tdoaEstimates,
    List<LocalizationConstraint2D> constraints,
    List<BeamformingPoint> heatmap,
    Vector2 estimatedPositionMeters) {

  /** Create an immutable acoustic-localization snapshot. */
  public AcousticLocalizationSnapshot {
    if (trackedFrequency == null || estimatedPositionMeters == null) {
      throw new IllegalArgumentException(
          "trackedFrequency and estimatedPositionMeters must not be null");
    }
    tdoaEstimates = List.copyOf(tdoaEstimates);
    constraints = List.copyOf(constraints);
    heatmap = List.copyOf(heatmap);
  }
}
