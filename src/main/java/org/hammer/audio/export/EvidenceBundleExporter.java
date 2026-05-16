package org.hammer.audio.export;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import org.hammer.audio.analysis.SpectrumSnapshot;
import org.hammer.audio.core.AudioBlock;
import org.hammer.audio.diagnosis.DiagnosisFinding;
import org.hammer.audio.diagnosis.DiagnosisSnapshot;
import org.hammer.audio.localization.StereoDelaySnapshot;
import org.hammer.audio.spectrogram.SpectrogramFrame;
import org.hammer.audio.spectrogram.SpectrogramHistory;

/**
 * Self-contained evidence bundle writer.
 *
 * <p>Given a parent directory and an immutable {@link EvidenceData} payload, writes a {@code
 * measurement-YYYYMMDD-HHMMSS} sub-directory containing:
 *
 * <ul>
 *   <li>{@code screenshot.png} (if screenshot supplied)
 *   <li>{@code samples.csv} per-frame, per-channel sample table
 *   <li>{@code spectrum.csv} one-sided FFT magnitudes
 *   <li>{@code spectrogram.csv} time-major spectrogram frames
 *   <li>{@code stereo-delay.csv} stereo delay measurement
 *   <li>{@code diagnosis.txt} human-readable diagnosis findings
 *   <li>{@code metadata.json} bundle metadata (timestamp, formats, sizes, etc.)
 * </ul>
 *
 * <p>Any optional artifact whose source data is {@code null} is omitted; an empty bundle (no
 * artifacts at all) is rejected.
 */
public final class EvidenceBundleExporter {

  private static final DateTimeFormatter DIR_TIMESTAMP =
      DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT);

  private final ZoneId zoneId;

  /** Create an exporter using the system default time zone. */
  public EvidenceBundleExporter() {
    this(ZoneId.systemDefault());
  }

  /**
   * Create an exporter with a fixed time zone.
   *
   * @param zoneId zone used to format the bundle directory name
   */
  public EvidenceBundleExporter(ZoneId zoneId) {
    this.zoneId = zoneId;
  }

  /**
   * Write an evidence bundle.
   *
   * @param parentDirectory directory under which the bundle directory is created
   * @param data evidence payload; must not be entirely empty
   * @return the path to the created bundle directory
   * @throws IOException if writing any artifact fails
   * @throws IllegalArgumentException if the payload contains no usable data
   */
  public Path export(Path parentDirectory, EvidenceData data) throws IOException {
    if (data == null) {
      throw new IllegalArgumentException("data must not be null");
    }
    if (!data.hasAnything()) {
      throw new IllegalArgumentException("Evidence payload contains no data to export");
    }
    Files.createDirectories(parentDirectory);
    Instant now = data.timestamp() != null ? data.timestamp() : Instant.now();
    String baseName = "measurement-" + LocalDateTime.ofInstant(now, zoneId).format(DIR_TIMESTAMP);
    Path bundleDir = parentDirectory.resolve(baseName);
    int suffix = 1;
    while (Files.exists(bundleDir)) {
      bundleDir = parentDirectory.resolve(baseName + "-" + suffix);
      suffix++;
    }
    Files.createDirectory(bundleDir);
    String name = bundleDir.getFileName().toString();

    if (data.screenshot() != null) {
      writeScreenshot(bundleDir.resolve("screenshot.png"), data.screenshot());
    }
    if (data.block() != null) {
      writeSamples(bundleDir.resolve("samples.csv"), data.block());
    }
    if (data.spectrum() != null) {
      writeSpectrum(bundleDir.resolve("spectrum.csv"), data.spectrum());
    }
    if (data.spectrogram() != null && !data.spectrogram().isEmpty()) {
      writeSpectrogram(bundleDir.resolve("spectrogram.csv"), data.spectrogram());
    }
    if (data.stereoDelay() != null) {
      writeStereoDelay(bundleDir.resolve("stereo-delay.csv"), data.stereoDelay());
    }
    if (data.diagnosis() != null) {
      writeDiagnosis(bundleDir.resolve("diagnosis.txt"), data.diagnosis());
    }
    writeMetadata(bundleDir.resolve("metadata.json"), name, now, data);
    return bundleDir;
  }

  private static void writeScreenshot(Path file, BufferedImage image) throws IOException {
    if (!ImageIO.write(image, "png", file.toFile())) {
      throw new IOException("No PNG writer available for screenshot");
    }
  }

  private static void writeSamples(Path file, AudioBlock block) throws IOException {
    try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(file, StandardCharsets.UTF_8))) {
      w.print("frame");
      for (int c = 0; c < block.channels(); c++) {
        w.print(",channel");
        w.print(c);
      }
      w.println();
      float[][] samples = block.samples();
      for (int frame = 0; frame < block.frames(); frame++) {
        w.print(frame);
        for (int c = 0; c < block.channels(); c++) {
          w.printf(Locale.ROOT, ",%.9f", samples[c][frame]);
        }
        w.println();
      }
    }
  }

  private static void writeSpectrum(Path file, SpectrumSnapshot spectrum) throws IOException {
    try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(file, StandardCharsets.UTF_8))) {
      w.println("bin,frequencyHz,magnitude");
      for (int bin = 0; bin < spectrum.binCount(); bin++) {
        w.printf(
            Locale.ROOT,
            "%d,%.6f,%.9f%n",
            bin,
            spectrum.frequencyOfBin(bin),
            spectrum.magnitude(bin));
      }
    }
  }

  private static void writeSpectrogram(Path file, SpectrogramHistory history) throws IOException {
    List<SpectrogramFrame> frames = history.snapshot();
    try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(file, StandardCharsets.UTF_8))) {
      w.print("frameIndex,timestampNanos");
      int bins = history.binCount();
      for (int bin = 0; bin < bins; bin++) {
        w.printf(Locale.ROOT, ",bin%d", bin);
      }
      w.println();
      for (SpectrogramFrame f : frames) {
        w.print(f.sourceFrameIndex());
        w.print(",");
        w.print(f.sourceTimestampNanos());
        float[] view = f.magnitudesView();
        for (int bin = 0; bin < bins; bin++) {
          float m = bin < view.length ? view[bin] : 0f;
          w.printf(Locale.ROOT, ",%.9f", m);
        }
        w.println();
      }
    }
  }

  private static void writeStereoDelay(Path file, StereoDelaySnapshot delay) throws IOException {
    try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(file, StandardCharsets.UTF_8))) {
      w.println("key,value");
      w.printf(Locale.ROOT, "status,%s%n", delay.status());
      w.printf(Locale.ROOT, "valid,%s%n", delay.valid());
      w.printf(Locale.ROOT, "delaySamples,%d%n", delay.delaySamples());
      w.printf(Locale.ROOT, "delayMillis,%.6f%n", delay.delayMillis());
      w.printf(
          Locale.ROOT, "pathLengthDifferenceMeters,%.6f%n", delay.pathLengthDifferenceMeters());
      w.printf(Locale.ROOT, "angleDegrees,%.3f%n", delay.angleDegrees());
      w.printf(Locale.ROOT, "confidence,%.4f%n", delay.confidence());
      w.printf(Locale.ROOT, "microphoneSpacingMeters,%.4f%n", delay.microphoneSpacingMeters());
      w.printf(
          Locale.ROOT, "speedOfSoundMetersPerSecond,%.3f%n", delay.speedOfSoundMetersPerSecond());
      w.println();
      w.println("lagSamples,correlation");
      float[] correlations = delay.correlationByLag();
      for (int i = 0; i < correlations.length; i++) {
        w.printf(Locale.ROOT, "%d,%.6f%n", delay.correlationLagForIndex(i), correlations[i]);
      }
    }
  }

  private static void writeDiagnosis(Path file, DiagnosisSnapshot diagnosis) throws IOException {
    try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(file, StandardCharsets.UTF_8))) {
      w.printf(Locale.ROOT, "sourceFrameIndex: %d%n", diagnosis.sourceFrameIndex());
      w.printf(Locale.ROOT, "sourceTimestampNanos: %d%n", diagnosis.sourceTimestampNanos());
      w.printf(Locale.ROOT, "findings: %d%n", diagnosis.findings().size());
      w.println();
      if (diagnosis.findings().isEmpty()) {
        w.println("No findings.");
        return;
      }
      for (DiagnosisFinding finding : diagnosis.findings()) {
        w.printf(
            Locale.ROOT,
            "[%s] %s (type=%s, confidence=%.2f",
            finding.severity(),
            finding.message(),
            finding.type(),
            finding.confidence());
        if (!Double.isNaN(finding.frequencyHz())) {
          w.printf(Locale.ROOT, ", frequencyHz=%.3f", finding.frequencyHz());
        }
        if (!Double.isNaN(finding.value())) {
          w.printf(Locale.ROOT, ", value=%.6f", finding.value());
        }
        w.println(")");
      }
    }
  }

  private static void writeMetadata(
      Path file, String bundleName, Instant timestamp, EvidenceData data) throws IOException {
    try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(file, StandardCharsets.UTF_8))) {
      w.println("{");
      w.printf(Locale.ROOT, "  \"bundleName\": \"%s\",%n", escape(bundleName));
      w.printf(Locale.ROOT, "  \"createdAt\": \"%s\",%n", timestamp.toString());
      w.printf(Locale.ROOT, "  \"hasScreenshot\": %s,%n", data.screenshot() != null);
      AudioBlock block = data.block();
      if (block != null) {
        w.println("  \"audio\": {");
        w.printf(Locale.ROOT, "    \"sampleRate\": %.3f,%n", block.format().sampleRate());
        w.printf(Locale.ROOT, "    \"channels\": %d,%n", block.channels());
        w.printf(Locale.ROOT, "    \"frames\": %d,%n", block.frames());
        w.printf(Locale.ROOT, "    \"frameIndex\": %d%n", block.frameIndex());
        w.println("  },");
      }
      SpectrumSnapshot spectrum = data.spectrum();
      if (spectrum != null) {
        w.println("  \"spectrum\": {");
        w.printf(Locale.ROOT, "    \"fftSize\": %d,%n", spectrum.fftSize());
        w.printf(Locale.ROOT, "    \"bins\": %d,%n", spectrum.binCount());
        w.printf(Locale.ROOT, "    \"binWidthHz\": %.6f%n", spectrum.binWidthHz());
        w.println("  },");
      }
      SpectrogramHistory history = data.spectrogram();
      if (history != null && !history.isEmpty()) {
        w.println("  \"spectrogram\": {");
        w.printf(Locale.ROOT, "    \"frames\": %d,%n", history.size());
        w.printf(Locale.ROOT, "    \"capacity\": %d,%n", history.capacity());
        w.printf(Locale.ROOT, "    \"bins\": %d,%n", history.binCount());
        w.printf(Locale.ROOT, "    \"sampleRate\": %.3f%n", history.sampleRate());
        w.println("  },");
      }
      DiagnosisSnapshot diagnosis = data.diagnosis();
      if (diagnosis != null) {
        w.printf(Locale.ROOT, "  \"diagnosisFindings\": %d,%n", diagnosis.findings().size());
      }
      w.printf(
          Locale.ROOT, "  \"notes\": \"%s\"%n", escape(data.notes() == null ? "" : data.notes()));
      w.println("}");
    }
  }

  private static String escape(String s) {
    StringBuilder out = new StringBuilder(s.length() + 8);
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '\\' -> out.append("\\\\");
        case '"' -> out.append("\\\"");
        case '\b' -> out.append("\\b");
        case '\f' -> out.append("\\f");
        case '\n' -> out.append("\\n");
        case '\r' -> out.append("\\r");
        case '\t' -> out.append("\\t");
        default -> {
          if (c < 0x20) {
            out.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
          } else {
            out.append(c);
          }
        }
      }
    }
    return out.toString();
  }
}
