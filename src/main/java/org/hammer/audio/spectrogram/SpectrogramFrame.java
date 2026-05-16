package org.hammer.audio.spectrogram;

import java.util.Objects;
import org.hammer.audio.analysis.AnalysisSnapshot;

/**
 * Immutable single frame of a spectrogram / waterfall: the one-sided magnitude spectrum captured at
 * a specific {@link #sourceTimestampNanos()} / {@link #sourceFrameIndex()}, together with the
 * sample-rate and FFT size required to interpret it.
 *
 * <p>The magnitude array is defensively copied on construction. {@link #magnitudes()} returns a
 * defensive copy; {@link #magnitudesView()} returns the internal array directly for hot rendering
 * paths and must not be mutated.
 */
public final class SpectrogramFrame implements AnalysisSnapshot {

  private final long sourceFrameIndex;
  private final long sourceTimestampNanos;
  private final float sampleRate;
  private final int fftSize;
  private final float[] magnitudes;

  /**
   * Create a new spectrogram frame. The magnitude array is defensively copied.
   *
   * @param sourceFrameIndex frame index of the originating audio block
   * @param sourceTimestampNanos timestamp of the originating audio block in nanoseconds
   * @param sampleRate sample rate of the source audio in Hz; must be {@code > 0}
   * @param fftSize FFT size that produced the magnitudes; must be a positive even number
   * @param magnitudes one-sided magnitude spectrum of length {@code fftSize/2 + 1}; must not be
   *     {@code null}
   * @throws IllegalArgumentException if any parameter is invalid
   */
  public SpectrogramFrame(
      long sourceFrameIndex,
      long sourceTimestampNanos,
      float sampleRate,
      int fftSize,
      float[] magnitudes) {
    if (!(sampleRate > 0f)) {
      throw new IllegalArgumentException("sampleRate must be > 0, was " + sampleRate);
    }
    if (fftSize < 2 || (fftSize & 1) != 0) {
      throw new IllegalArgumentException("fftSize must be a positive even number, was " + fftSize);
    }
    Objects.requireNonNull(magnitudes, "magnitudes");
    int expected = fftSize / 2 + 1;
    if (magnitudes.length != expected) {
      throw new IllegalArgumentException(
          "magnitudes.length must be fftSize/2+1=" + expected + ", was " + magnitudes.length);
    }
    this.sourceFrameIndex = sourceFrameIndex;
    this.sourceTimestampNanos = sourceTimestampNanos;
    this.sampleRate = sampleRate;
    this.fftSize = fftSize;
    this.magnitudes = magnitudes.clone();
  }

  /**
   * Internal constructor used by {@link org.hammer.audio.spectrogram.SpectrogramAnalyzer} to adopt
   * an already-validated magnitudes array without copying. The supplied array becomes the frame's
   * backing buffer and must not be mutated by the caller after the call.
   */
  static SpectrogramFrame adopting(
      long sourceFrameIndex,
      long sourceTimestampNanos,
      float sampleRate,
      int fftSize,
      float[] magnitudes) {
    SpectrogramFrame frame =
        new SpectrogramFrame(sourceFrameIndex, sourceTimestampNanos, sampleRate, fftSize);
    frame.adoptMagnitudes(magnitudes);
    return frame;
  }

  private SpectrogramFrame(
      long sourceFrameIndex, long sourceTimestampNanos, float sampleRate, int fftSize) {
    this.sourceFrameIndex = sourceFrameIndex;
    this.sourceTimestampNanos = sourceTimestampNanos;
    this.sampleRate = sampleRate;
    this.fftSize = fftSize;
    this.magnitudes = new float[fftSize / 2 + 1];
  }

  private void adoptMagnitudes(float[] source) {
    // The constructor has already allocated the backing array; a single arraycopy here avoids
    // the second full-array allocation that the public constructor's defensive clone would do
    // on top of the clone SpectrumSnapshot already performed.
    System.arraycopy(source, 0, this.magnitudes, 0, this.magnitudes.length);
  }

  /**
   * @return frame index of the originating audio block
   */
  @Override
  public long sourceFrameIndex() {
    return sourceFrameIndex;
  }

  /**
   * @return timestamp of the originating audio block in nanoseconds
   */
  @Override
  public long sourceTimestampNanos() {
    return sourceTimestampNanos;
  }

  /**
   * @return sample rate of the source audio in Hz
   */
  public float sampleRate() {
    return sampleRate;
  }

  /**
   * @return FFT size that produced the magnitudes
   */
  public int fftSize() {
    return fftSize;
  }

  /**
   * @return number of one-sided frequency bins (DC ... Nyquist)
   */
  public int binCount() {
    return magnitudes.length;
  }

  /**
   * @return bin width in Hz, i.e. {@code sampleRate / fftSize}
   */
  public float binWidthHz() {
    return sampleRate / fftSize;
  }

  /**
   * @param bin bin index in {@code [0, binCount())}
   * @return centre frequency of the bin in Hz
   */
  public float frequencyOfBin(int bin) {
    return bin * binWidthHz();
  }

  /**
   * @param bin bin index in {@code [0, binCount())}
   * @return magnitude at the bin
   */
  public float magnitude(int bin) {
    return magnitudes[bin];
  }

  /**
   * @return defensive copy of the magnitude spectrum
   */
  public float[] magnitudes() {
    return magnitudes.clone();
  }

  /**
   * Read-only access to the internal magnitudes array. Callers must not mutate the returned array.
   * Intended for hot rendering paths that need to avoid per-frame allocations.
   *
   * @return the internal magnitudes array (do not mutate)
   */
  public float[] magnitudesView() {
    return magnitudes;
  }
}
