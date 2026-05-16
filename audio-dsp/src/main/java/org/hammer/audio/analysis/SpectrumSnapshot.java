package org.hammer.audio.analysis;

/**
 * Immutable spectrum-analysis snapshot produced by {@link SpectrumAnalyzer}.
 *
 * <p>Contains the one-sided magnitude spectrum (DC ... Nyquist) computed from one channel of an
 * audio block, the FFT size that produced it, and the sample rate (for bin → frequency conversion).
 *
 * @author refactoring
 */
public final class SpectrumSnapshot implements AnalysisSnapshot {

  private final long sourceFrameIndex;
  private final long sourceTimestampNanos;
  private final int fftSize;
  private final float sampleRate;
  private final int channel;
  private final float[] magnitudes;

  /**
   * Create a new spectrum snapshot. The magnitudes array is defensively copied.
   *
   * @param sourceFrameIndex frame index from the analyzed block
   * @param sourceTimestampNanos timestamp from the analyzed block
   * @param channel channel index that was analyzed
   * @param sampleRate sample rate of the source audio
   * @param fftSize FFT size that produced the magnitudes
   * @param magnitudes one-sided magnitude spectrum of length {@code fftSize/2 + 1}
   * @throws IllegalArgumentException if {@code magnitudes.length != fftSize/2 + 1}
   */
  public SpectrumSnapshot(
      long sourceFrameIndex,
      long sourceTimestampNanos,
      int channel,
      float sampleRate,
      int fftSize,
      float[] magnitudes) {
    if (magnitudes.length != fftSize / 2 + 1) {
      throw new IllegalArgumentException(
          "magnitudes.length must be fftSize/2+1, was " + magnitudes.length);
    }
    this.sourceFrameIndex = sourceFrameIndex;
    this.sourceTimestampNanos = sourceTimestampNanos;
    this.channel = channel;
    this.sampleRate = sampleRate;
    this.fftSize = fftSize;
    this.magnitudes = magnitudes.clone();
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
   * @return analyzed channel index
   */
  public int channel() {
    return channel;
  }

  /**
   * @return sample rate of the source audio (Hz)
   */
  public float sampleRate() {
    return sampleRate;
  }

  /**
   * @return the FFT size that produced the magnitudes
   */
  public int fftSize() {
    return fftSize;
  }

  /**
   * @return frequency resolution per bin in Hz, i.e. {@code sampleRate / fftSize}
   */
  public float binWidthHz() {
    return sampleRate / fftSize;
  }

  /**
   * @param bin bin index in {@code [0, magnitudes().length)}
   * @return centre frequency of the given bin in Hz
   */
  public float frequencyOfBin(int bin) {
    return bin * binWidthHz();
  }

  /**
   * @return defensive copy of the one-sided magnitude spectrum
   */
  public float[] magnitudes() {
    return magnitudes.clone();
  }

  /**
   * Read-only access to the internal magnitudes array. Callers must not mutate the returned array.
   * Intended for hot rendering paths and downstream analyzers that need to avoid per-frame
   * allocations.
   *
   * @return the internal magnitudes array (do not mutate)
   */
  public float[] magnitudesView() {
    return magnitudes;
  }

  /**
   * @return number of frequency bins in the one-sided spectrum
   */
  public int binCount() {
    return magnitudes.length;
  }

  /**
   * @param bin bin index
   * @return magnitude at that bin
   */
  public float magnitude(int bin) {
    return magnitudes[bin];
  }
}
