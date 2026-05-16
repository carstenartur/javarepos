package org.hammer.audio.compare;

import java.util.List;
import java.util.Objects;
import org.hammer.audio.analysis.MeasurementSnapshot;
import org.hammer.audio.analysis.SpectrumSnapshot;
import org.hammer.audio.core.AudioFormatDescriptor;
import org.hammer.audio.diagnosis.DiagnosisFinding;
import org.hammer.audio.diagnosis.DiagnosisSnapshot;

/**
 * Immutable A/B comparison of two analyzed audio recordings (or live sessions). Each side captures
 * the format and the three core snapshots produced by the analyzer chain.
 *
 * <p>Use {@link MarkdownComparisonReportRenderer} to format a report for diagnostics, QA notes or
 * bug tickets.
 */
public final class ComparisonReport {

  private final Side a;
  private final Side b;

  /** Create a report from two analyzed sides. */
  public ComparisonReport(Side a, Side b) {
    this.a = Objects.requireNonNull(a, "a");
    this.b = Objects.requireNonNull(b, "b");
  }

  public Side a() {
    return a;
  }

  public Side b() {
    return b;
  }

  /**
   * @return the absolute difference {@code |a - b|} treating {@link Double#NaN} as missing on
   *     either side (returns {@link Double#NaN} in that case)
   */
  public static double absDelta(double aValue, double bValue) {
    if (Double.isNaN(aValue) || Double.isNaN(bValue)) {
      return Double.NaN;
    }
    return Math.abs(aValue - bValue);
  }

  /**
   * One side of the comparison: label, format and the analyzed snapshots.
   *
   * @param label human-readable label (filename, "before", "after", ...)
   * @param format audio format descriptor of the recording
   * @param totalFrames total frame count consumed during analysis
   * @param measurement aggregate measurement snapshot
   * @param spectrum spectrum snapshot (may be {@code null} if not produced)
   * @param diagnosis diagnosis snapshot
   */
  public record Side(
      String label,
      AudioFormatDescriptor format,
      long totalFrames,
      MeasurementSnapshot measurement,
      SpectrumSnapshot spectrum,
      DiagnosisSnapshot diagnosis) {

    /** Compact constructor with null checks for required fields. */
    public Side {
      Objects.requireNonNull(label, "label");
      Objects.requireNonNull(format, "format");
      Objects.requireNonNull(measurement, "measurement");
      Objects.requireNonNull(diagnosis, "diagnosis");
    }

    /**
     * @return findings list (never {@code null}; possibly empty)
     */
    public List<DiagnosisFinding> findings() {
      return diagnosis.findings();
    }

    /**
     * @return duration in seconds derived from {@code totalFrames} and the format sample rate
     */
    public double durationSeconds() {
      float sampleRate = format.sampleRate();
      if (sampleRate <= 0f) {
        return Double.NaN;
      }
      return totalFrames / (double) sampleRate;
    }
  }
}
