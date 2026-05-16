package org.hammer.audio.compare;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.hammer.audio.analysis.MeasurementSnapshot;
import org.hammer.audio.analysis.SpectrumSnapshot;
import org.hammer.audio.diagnosis.DiagnosisFinding;

/**
 * Renders a {@link ComparisonReport} as a Markdown document. The format is intentionally simple
 * (one table per analyzed dimension) so the output can be pasted into QA notes, GitHub issues or a
 * regression log.
 */
public final class MarkdownComparisonReportRenderer {

  /** Render the given comparison report to a Markdown string. */
  public String render(ComparisonReport report) {
    Objects.requireNonNull(report, "report");
    StringBuilder sb = new StringBuilder(2048);
    sb.append("# A/B comparison report\n\n");
    sb.append("| Side | Label | Format | Duration | Frames |\n");
    sb.append("|------|-------|--------|----------|--------|\n");
    appendSideRow(sb, "A", report.a());
    appendSideRow(sb, "B", report.b());
    sb.append('\n');

    appendMeasurementsTable(sb, report);
    sb.append('\n');

    appendSpectrumSummary(sb, report);
    sb.append('\n');

    appendDiagnosisSummary(sb, report);
    return sb.toString();
  }

  private static void appendSideRow(StringBuilder sb, String tag, ComparisonReport.Side s) {
    sb.append("| ")
        .append(tag)
        .append(" | ")
        .append(escapePipes(s.label()))
        .append(" | ")
        .append(s.format().sampleRate())
        .append(" Hz / ")
        .append(s.format().channels())
        .append(" ch / ")
        .append(s.format().sourceSampleSizeInBits())
        .append(" bit | ")
        .append(String.format(Locale.ROOT, "%.3f s", s.durationSeconds()))
        .append(" | ")
        .append(s.totalFrames())
        .append(" |\n");
  }

  private static void appendMeasurementsTable(StringBuilder sb, ComparisonReport report) {
    MeasurementSnapshot a = report.a().measurement();
    MeasurementSnapshot b = report.b().measurement();
    sb.append("## Measurements\n\n");
    sb.append("| Metric | A | B | abs Δ |\n");
    sb.append("|--------|---|---|-------|\n");
    appendDoubleRow(sb, "RMS", a.rms(), b.rms(), "%.4f");
    appendDoubleRow(sb, "Peak level", a.peakLevel(), b.peakLevel(), "%.4f");
    appendDoubleRow(
        sb, "Dominant freq (Hz)", a.dominantFrequencyHz(), b.dominantFrequencyHz(), "%.1f");
    if (a.stereoCorrelationAvailable() || b.stereoCorrelationAvailable()) {
      double sa = a.stereoCorrelationAvailable() ? a.stereoCorrelation() : Double.NaN;
      double sb2 = b.stereoCorrelationAvailable() ? b.stereoCorrelation() : Double.NaN;
      appendDoubleRow(sb, "Stereo correlation", sa, sb2, "%.3f");
    }
    sb.append("| Clipping | ")
        .append(a.clipping() ? "YES" : "no")
        .append(" | ")
        .append(b.clipping() ? "YES" : "no")
        .append(" | ")
        .append(a.clipping() == b.clipping() ? "same" : "**changed**")
        .append(" |\n");
  }

  private static void appendDoubleRow(
      StringBuilder sb, String label, double a, double b, String fmt) {
    sb.append("| ").append(label).append(" | ");
    sb.append(formatDouble(a, fmt)).append(" | ");
    sb.append(formatDouble(b, fmt)).append(" | ");
    double delta = ComparisonReport.absDelta(a, b);
    sb.append(formatDouble(delta, fmt)).append(" |\n");
  }

  private static String formatDouble(double v, String fmt) {
    if (Double.isNaN(v)) {
      return "n/a";
    }
    return String.format(Locale.ROOT, fmt, v);
  }

  private static void appendSpectrumSummary(StringBuilder sb, ComparisonReport report) {
    SpectrumSnapshot a = report.a().spectrum();
    SpectrumSnapshot b = report.b().spectrum();
    sb.append("## Spectrum summary\n\n");
    if (a == null || b == null) {
      sb.append("_One or both recordings produced no spectrum snapshot._\n");
      return;
    }
    sb.append("| Metric | A | B | abs Δ |\n");
    sb.append("|--------|---|---|-------|\n");
    appendDoubleRow(
        sb, "FFT bin count", a.magnitudesView().length, b.magnitudesView().length, "%.0f");
    appendDoubleRow(sb, "Peak magnitude", peakMagnitude(a), peakMagnitude(b), "%.4f");
    appendDoubleRow(sb, "Spectral centroid (Hz)", centroid(a), centroid(b), "%.1f");
  }

  private static double peakMagnitude(SpectrumSnapshot s) {
    float[] m = s.magnitudesView();
    double peak = 0.0;
    for (float v : m) {
      double abs = Math.abs(v);
      if (abs > peak) {
        peak = abs;
      }
    }
    return peak;
  }

  private static double centroid(SpectrumSnapshot s) {
    float[] m = s.magnitudesView();
    if (m.length == 0) {
      return Double.NaN;
    }
    double binHz = m.length > 1 ? (double) s.sampleRate() / (2.0d * (double) (m.length - 1)) : 0.0d;
    double sum = 0.0;
    double weighted = 0.0;
    for (int i = 0; i < m.length; i++) {
      double mag = Math.abs(m[i]);
      sum += mag;
      weighted += mag * (i * binHz);
    }
    return sum > 0.0 ? weighted / sum : Double.NaN;
  }

  private static void appendDiagnosisSummary(StringBuilder sb, ComparisonReport report) {
    sb.append("## Diagnosis findings\n\n");
    appendFindings(sb, "A", report.a().findings());
    sb.append('\n');
    appendFindings(sb, "B", report.b().findings());
  }

  private static void appendFindings(StringBuilder sb, String tag, List<DiagnosisFinding> list) {
    sb.append("**").append(tag).append("**\n\n");
    if (list.isEmpty()) {
      sb.append("- _no findings_\n");
      return;
    }
    for (DiagnosisFinding f : list) {
      sb.append("- `")
          .append(f.severity())
          .append("` ")
          .append(f.type())
          .append(" — ")
          .append(escapePipes(f.message()))
          .append(" (conf ")
          .append(String.format(Locale.ROOT, "%.2f", f.confidence()))
          .append(")\n");
    }
  }

  private static String escapePipes(String s) {
    return s == null ? "" : s.replace("|", "\\|");
  }
}
