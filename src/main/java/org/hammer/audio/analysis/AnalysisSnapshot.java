package org.hammer.audio.analysis;

/**
 * Marker interface for immutable analysis results published by {@link AnalysisModule}s.
 *
 * <p>Snapshot implementations carry the analyzed values, the source frame index / timestamp from
 * the originating {@link org.hammer.audio.core.AudioBlock}, and any per-analyzer metadata. They are
 * detached from realtime mutable state and safe to serialize for export, transport over an API, or
 * render in any UI toolkit.
 *
 * <p>Implementations <strong>must</strong> be immutable and thread-safe.
 *
 * @author refactoring
 */
public interface AnalysisSnapshot {

  /**
   * @return the {@link org.hammer.audio.core.AudioBlock#frameIndex()} of the analyzed block
   */
  long sourceFrameIndex();

  /**
   * @return the {@link org.hammer.audio.core.AudioBlock#timestampNanos()} of the analyzed block
   */
  long sourceTimestampNanos();
}
