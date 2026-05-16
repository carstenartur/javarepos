package org.hammer.audio.diagnosis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.hammer.audio.analysis.AnalysisSnapshot;

/**
 * Immutable result of one diagnosis run: ordered list of {@link DiagnosisFinding}s plus the source
 * frame/timestamp of the analyzed block.
 *
 * <p>Findings are stored in the order produced by {@link DiagnosisAnalyzer}, with the most
 * informative finding first (typically the highest severity / confidence).
 */
public final class DiagnosisSnapshot implements AnalysisSnapshot {

  private static final DiagnosisSnapshot EMPTY =
      new DiagnosisSnapshot(0L, 0L, Collections.emptyList());

  private final long sourceFrameIndex;
  private final long sourceTimestampNanos;
  private final List<DiagnosisFinding> findings;

  /**
   * Create a diagnosis snapshot. The finding list is defensively copied to an unmodifiable list.
   *
   * @param sourceFrameIndex frame index of the originating audio block
   * @param sourceTimestampNanos timestamp of the originating audio block
   * @param findings list of findings; must not be {@code null} (may be empty)
   */
  public DiagnosisSnapshot(
      long sourceFrameIndex, long sourceTimestampNanos, List<DiagnosisFinding> findings) {
    this.sourceFrameIndex = sourceFrameIndex;
    this.sourceTimestampNanos = sourceTimestampNanos;
    this.findings = List.copyOf(Objects.requireNonNull(findings, "findings"));
  }

  /**
   * @return shared empty snapshot
   */
  public static DiagnosisSnapshot empty() {
    return EMPTY;
  }

  @Override
  public long sourceFrameIndex() {
    return sourceFrameIndex;
  }

  @Override
  public long sourceTimestampNanos() {
    return sourceTimestampNanos;
  }

  /**
   * @return unmodifiable list of findings, in display order (most informative first)
   */
  public List<DiagnosisFinding> findings() {
    return findings;
  }

  /**
   * @return true if the snapshot contains no findings
   */
  public boolean isEmpty() {
    return findings.isEmpty();
  }

  /**
   * @return findings of the given severity, never {@code null}
   */
  public List<DiagnosisFinding> findingsOfSeverity(DiagnosisSeverity severity) {
    List<DiagnosisFinding> out = new ArrayList<>();
    for (DiagnosisFinding finding : findings) {
      if (finding.severity() == severity) {
        out.add(finding);
      }
    }
    return List.copyOf(out);
  }
}
