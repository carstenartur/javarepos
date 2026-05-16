package org.hammer.audio.spectrogram;

import org.hammer.audio.analysis.AnalysisModule;
import org.hammer.audio.analysis.SpectrumAnalyzer;
import org.hammer.audio.analysis.SpectrumSnapshot;
import org.hammer.audio.core.AudioBlock;

/**
 * Analyzer that computes a {@link SpectrogramFrame} per {@link AudioBlock} and appends it to an
 * internal {@link SpectrogramHistory}.
 *
 * <p>Implemented on top of {@link SpectrumAnalyzer}; the per-block FFT result is also exposed as a
 * {@link SpectrumSnapshot} via {@link #lastSpectrum()} for callers that need both views without
 * paying the cost of a second FFT.
 *
 * <p>Instances are <strong>not thread-safe</strong> and are intended to be driven by a single
 * analysis / UI thread.
 */
public final class SpectrogramAnalyzer implements AnalysisModule<SpectrogramFrame> {

  private final SpectrumAnalyzer spectrumAnalyzer;
  private final SpectrogramHistory history;
  private SpectrumSnapshot lastSpectrum;

  /**
   * Create a spectrogram analyzer.
   *
   * @param fftSize FFT size; must be a power of two and {@code >= 2}
   * @param channel channel index of the source block to analyze (0 for mono)
   * @param sampleRate sample rate of the source audio in Hz
   * @param historyFrames maximum number of retained frames; must be {@code >= 1}
   */
  public SpectrogramAnalyzer(int fftSize, int channel, float sampleRate, int historyFrames) {
    this(new SpectrumAnalyzer(fftSize, channel, sampleRate), new SpectrogramHistory(historyFrames));
  }

  /**
   * Create a spectrogram analyzer with externally supplied dependencies.
   *
   * @param spectrumAnalyzer FFT-based spectrum analyzer
   * @param history rolling history of frames
   */
  public SpectrogramAnalyzer(SpectrumAnalyzer spectrumAnalyzer, SpectrogramHistory history) {
    this.spectrumAnalyzer = spectrumAnalyzer;
    this.history = history;
  }

  @Override
  public SpectrogramFrame analyze(AudioBlock block) {
    SpectrumSnapshot snapshot = spectrumAnalyzer.analyze(block);
    // Use the non-copying view + adopting factory to avoid an extra defensive clone in the
    // per-block hot path (the SpectrogramFrame still ends up with its own backing array).
    SpectrogramFrame frame =
        SpectrogramFrame.adopting(
            snapshot.sourceFrameIndex(),
            snapshot.sourceTimestampNanos(),
            snapshot.sampleRate(),
            snapshot.fftSize(),
            snapshot.magnitudesView());
    history.append(frame);
    lastSpectrum = snapshot;
    return frame;
  }

  /**
   * @return the spectrogram history backing this analyzer
   */
  public SpectrogramHistory history() {
    return history;
  }

  /**
   * @return the most recent {@link SpectrumSnapshot}, or {@code null} if {@link
   *     #analyze(AudioBlock)} has not yet been called
   */
  public SpectrumSnapshot lastSpectrum() {
    return lastSpectrum;
  }

  /**
   * @return configured FFT size
   */
  public int fftSize() {
    return spectrumAnalyzer.fftSize();
  }
}
