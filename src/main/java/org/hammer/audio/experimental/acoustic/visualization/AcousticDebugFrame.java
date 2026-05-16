package org.hammer.audio.experimental.acoustic.visualization;

import java.util.List;
import org.hammer.audio.experimental.acoustic.AcousticLocalizationSnapshot;
import org.hammer.audio.experimental.acoustic.DelayAndSumBeamformer.BeamformingPoint;
import org.hammer.audio.experimental.acoustic.TdoaEstimate;
import org.hammer.audio.geometry.Vector2;

/** UI-agnostic debug model for spectrogram, geometry, heatmap and TDOA renderers. */
public record AcousticDebugFrame(
    long sourceFrameIndex,
    double trackedFrequencyHz,
    Vector2 estimatedPositionMeters,
    List<TdoaEstimate> tdoaEstimates,
    List<BeamformingPoint> heatmap) {

  /** Create a debug frame from an analysis snapshot. */
  public static AcousticDebugFrame from(AcousticLocalizationSnapshot snapshot) {
    return new AcousticDebugFrame(
        snapshot.sourceFrameIndex(),
        snapshot.trackedFrequency().frequencyHz(),
        snapshot.estimatedPositionMeters(),
        snapshot.tdoaEstimates(),
        snapshot.heatmap());
  }

  /** Create an immutable debug frame. */
  public AcousticDebugFrame {
    if (estimatedPositionMeters == null) {
      throw new IllegalArgumentException("estimatedPositionMeters must not be null");
    }
    tdoaEstimates = List.copyOf(tdoaEstimates);
    heatmap = List.copyOf(heatmap);
  }
}
