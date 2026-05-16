package org.hammer.audio.compare;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.hammer.audio.analysis.MeasurementCalculator;
import org.hammer.audio.analysis.MeasurementSnapshot;
import org.hammer.audio.analysis.SpectrumAnalyzer;
import org.hammer.audio.analysis.SpectrumSnapshot;
import org.hammer.audio.core.AudioBlock;
import org.hammer.audio.core.AudioFormatDescriptor;
import org.hammer.audio.diagnosis.DiagnosisAnalyzer;
import org.hammer.audio.diagnosis.DiagnosisSnapshot;
import org.hammer.audio.recording.AudioBlockRecordingReader;
import org.hammer.audio.spectrogram.SpectrogramAnalyzer;

/**
 * Replays two {@code .aar} recordings (or two in-memory block lists), runs the standard analyzer
 * stack on each, and returns a {@link ComparisonReport}.
 *
 * <p>The recording is analyzed block-by-block exactly as the live UI would, so the resulting
 * snapshots reflect the end state at the last block (matching the "freeze and inspect" workflow).
 */
public final class RecordingComparator {

  private static final int DEFAULT_FFT_SIZE = 1024;
  private static final int DEFAULT_SPECTROGRAM_FRAMES = 128;

  private final int fftSize;

  /** Create a comparator with the default {@value #DEFAULT_FFT_SIZE} FFT size. */
  public RecordingComparator() {
    this(DEFAULT_FFT_SIZE);
  }

  /**
   * @param fftSize FFT size used by the spectrum / spectrogram analyzers
   */
  public RecordingComparator(int fftSize) {
    if (fftSize <= 0 || Integer.bitCount(fftSize) != 1) {
      throw new IllegalArgumentException("fftSize must be a positive power of two, was " + fftSize);
    }
    this.fftSize = fftSize;
  }

  /**
   * Compare two recordings on disk.
   *
   * @param fileA recording A
   * @param fileB recording B
   * @param labelA human-readable label for A (e.g. the filename)
   * @param labelB human-readable label for B
   */
  public ComparisonReport compareFiles(Path fileA, Path fileB, String labelA, String labelB)
      throws IOException {
    Objects.requireNonNull(fileA, "fileA");
    Objects.requireNonNull(fileB, "fileB");
    List<AudioBlock> blocksA = AudioBlockRecordingReader.readAll(fileA);
    List<AudioBlock> blocksB = AudioBlockRecordingReader.readAll(fileB);
    return compareBlocks(blocksA, blocksB, labelA, labelB);
  }

  /** Compare two pre-loaded block sequences. */
  public ComparisonReport compareBlocks(
      List<AudioBlock> blocksA, List<AudioBlock> blocksB, String labelA, String labelB) {
    Objects.requireNonNull(blocksA, "blocksA");
    Objects.requireNonNull(blocksB, "blocksB");
    if (blocksA.isEmpty()) {
      throw new IllegalArgumentException("blocksA must be non-empty");
    }
    if (blocksB.isEmpty()) {
      throw new IllegalArgumentException("blocksB must be non-empty");
    }
    return new ComparisonReport(analyze(blocksA, labelA), analyze(blocksB, labelB));
  }

  private ComparisonReport.Side analyze(List<AudioBlock> blocks, String label) {
    AudioFormatDescriptor format = blocks.get(0).format();
    SpectrumAnalyzer spectrumAnalyzer = new SpectrumAnalyzer(fftSize, 0, format.sampleRate());
    MeasurementCalculator measurementCalculator = new MeasurementCalculator();
    SpectrogramAnalyzer spectrogramAnalyzer =
        new SpectrogramAnalyzer(fftSize, 0, format.sampleRate(), DEFAULT_SPECTROGRAM_FRAMES);
    DiagnosisAnalyzer diagnosisAnalyzer = new DiagnosisAnalyzer();

    SpectrumSnapshot lastSpectrum = null;
    AudioBlock lastBlock = null;
    long totalFrames = 0L;
    for (AudioBlock block : blocks) {
      if (block.frames() <= 0) {
        continue;
      }
      lastBlock = block;
      totalFrames += block.frames();
      if (block.channels() > 0 && block.frames() >= fftSize) {
        lastSpectrum = spectrumAnalyzer.analyze(block);
        spectrogramAnalyzer.analyze(block);
      }
    }
    MeasurementSnapshot measurement = measurementCalculator.calculate(lastBlock, lastSpectrum);
    DiagnosisSnapshot diagnosis =
        diagnosisAnalyzer.analyze(lastBlock, lastSpectrum, spectrogramAnalyzer.history(), null);
    return new ComparisonReport.Side(
        label, format, totalFrames, measurement, lastSpectrum, diagnosis);
  }
}
