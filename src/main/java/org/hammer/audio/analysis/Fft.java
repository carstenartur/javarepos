package org.hammer.audio.analysis;

/**
 * Pure-Java in-place radix-2 Cooley-Tukey FFT.
 *
 * <p>This is a deliberately small, dependency-free FFT implementation suitable as the default
 * spectrum backend. It supports power-of-two sizes only; callers are expected to pad or window
 * input as needed.
 *
 * <p><strong>Performance characteristics</strong>: O(N log N), O(N) extra memory (twiddle tables
 * and bit-reversed index table cached per instance for reuse across calls). The implementation is
 * single-threaded; create one {@code Fft} per thread or guard externally.
 *
 * <p>The architecture is more important than absolute FFT performance here: callers wanting more
 * speed should plug in a different {@link AnalysisModule} backed by a native or vectorized FFT
 * library while keeping the rest of the platform unchanged.
 *
 * @author refactoring
 */
public final class Fft {

  private final int size;
  private final int log2Size;
  private final float[] cosTable;
  private final float[] sinTable;
  private final int[] bitReverseIndex;

  /**
   * Create a new FFT of the given size.
   *
   * @param size FFT size; must be a power of two and {@code >= 2}
   * @throws IllegalArgumentException if {@code size} is not a positive power of two
   */
  public Fft(int size) {
    if (size < 2 || (size & (size - 1)) != 0) {
      throw new IllegalArgumentException("size must be a power of two >= 2, was " + size);
    }
    this.size = size;
    this.log2Size = Integer.numberOfTrailingZeros(size);

    // Precompute twiddle factors for sub-FFTs of every power-of-two stride up to size/2.
    cosTable = new float[size / 2];
    sinTable = new float[size / 2];
    for (int i = 0; i < size / 2; i++) {
      double angle = -2.0 * Math.PI * i / size;
      cosTable[i] = (float) Math.cos(angle);
      sinTable[i] = (float) Math.sin(angle);
    }

    // Precompute bit-reverse permutation.
    bitReverseIndex = new int[size];
    for (int i = 0; i < size; i++) {
      bitReverseIndex[i] = Integer.reverse(i) >>> (32 - log2Size);
    }
  }

  /**
   * @return the FFT size
   */
  public int size() {
    return size;
  }

  /**
   * Forward in-place FFT on real input; produces complex output in {@code (re, im)} form.
   *
   * @param re real part array of length {@link #size()}; on return contains real components of the
   *     transform
   * @param im imaginary part array of length {@link #size()}; treated as zero on input (caller may
   *     pass a zero-filled array). On return contains imaginary components of the transform.
   * @throws IllegalArgumentException if array lengths do not match {@link #size()}
   */
  public void forward(float[] re, float[] im) {
    if (re.length != size || im.length != size) {
      throw new IllegalArgumentException(
          "re/im length must equal FFT size "
              + size
              + " (got "
              + re.length
              + "/"
              + im.length
              + ")");
    }

    // Bit-reverse permutation
    for (int i = 0; i < size; i++) {
      int j = bitReverseIndex[i];
      if (j > i) {
        float tmpRe = re[i];
        re[i] = re[j];
        re[j] = tmpRe;
        float tmpIm = im[i];
        im[i] = im[j];
        im[j] = tmpIm;
      }
    }

    // Butterfly stages
    for (int stageSize = 2; stageSize <= size; stageSize <<= 1) {
      int halfStage = stageSize >> 1;
      int step = size / stageSize;
      for (int k = 0; k < size; k += stageSize) {
        int twiddleIdx = 0;
        for (int j = 0; j < halfStage; j++) {
          float wRe = cosTable[twiddleIdx];
          float wIm = sinTable[twiddleIdx];
          int idxA = k + j;
          int idxB = idxA + halfStage;
          float aRe = re[idxA];
          float aIm = im[idxA];
          float bRe = re[idxB];
          float bIm = im[idxB];
          float tRe = wRe * bRe - wIm * bIm;
          float tIm = wRe * bIm + wIm * bRe;
          re[idxA] = aRe + tRe;
          im[idxA] = aIm + tIm;
          re[idxB] = aRe - tRe;
          im[idxB] = aIm - tIm;
          twiddleIdx += step;
        }
      }
    }
  }

  /**
   * Compute magnitudes from complex output. Convenience method.
   *
   * @param re real array of length {@link #size()}
   * @param im imaginary array of length {@link #size()}
   * @param magnitudes output array of length {@code size/2 + 1} (one-sided spectrum) or {@link
   *     #size()} (two-sided). Length determines what gets returned.
   */
  public void magnitudes(float[] re, float[] im, float[] magnitudes) {
    int n = magnitudes.length;
    if (n != size && n != size / 2 + 1) {
      throw new IllegalArgumentException("magnitudes length must be size or size/2+1, was " + n);
    }
    for (int i = 0; i < n; i++) {
      float r = re[i];
      float ii = im[i];
      magnitudes[i] = (float) Math.sqrt(r * r + ii * ii);
    }
  }
}
