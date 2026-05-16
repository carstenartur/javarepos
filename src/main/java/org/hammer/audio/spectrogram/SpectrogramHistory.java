package org.hammer.audio.spectrogram;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Bounded rolling history of {@link SpectrogramFrame}s used to back the waterfall display and drift
 * / burst analyses.
 *
 * <p>Internally backed by a fixed-size ring buffer. Once {@link #capacity()} frames are stored,
 * appending a new frame evicts the oldest. The history rejects frames whose {@link
 * SpectrogramFrame#binCount()} differs from the established bin count and starts fresh after a
 * {@link #clear()} or a {@link #reset()} (the latter is also called automatically if the FFT size
 * or sample rate changes).
 *
 * <p>Instances are <strong>not thread-safe</strong>; callers must externally synchronize if shared.
 */
public final class SpectrogramHistory {

  private final int capacity;
  private final SpectrogramFrame[] frames;
  private int head;
  private int size;
  private int activeBinCount = -1;
  private int activeFftSize = -1;
  private float activeSampleRate = -1f;

  /**
   * Create a history with the given maximum number of retained frames.
   *
   * @param capacity maximum number of retained frames; must be {@code >= 1}
   */
  public SpectrogramHistory(int capacity) {
    if (capacity < 1) {
      throw new IllegalArgumentException("capacity must be >= 1, was " + capacity);
    }
    this.capacity = capacity;
    this.frames = new SpectrogramFrame[capacity];
  }

  /**
   * @return maximum number of retained frames
   */
  public int capacity() {
    return capacity;
  }

  /**
   * @return current number of stored frames (between 0 and {@link #capacity()})
   */
  public int size() {
    return size;
  }

  /**
   * @return true if no frames are currently stored
   */
  public boolean isEmpty() {
    return size == 0;
  }

  /**
   * @return number of one-sided frequency bins of the stored frames, or {@code -1} if empty
   */
  public int binCount() {
    return activeBinCount;
  }

  /**
   * @return FFT size of the stored frames, or {@code -1} if empty
   */
  public int fftSize() {
    return activeFftSize;
  }

  /**
   * @return sample rate of the stored frames in Hz, or {@code -1f} if empty
   */
  public float sampleRate() {
    return activeSampleRate;
  }

  /**
   * Append a frame to the history. If the frame's bin count, FFT size or sample rate differs from
   * the established values, the history is cleared first so the new geometry can take over.
   *
   * @param frame frame to append; must not be {@code null}
   */
  public void append(SpectrogramFrame frame) {
    Objects.requireNonNull(frame, "frame");
    if (activeBinCount != -1
        && (frame.binCount() != activeBinCount
            || frame.fftSize() != activeFftSize
            || Math.abs(frame.sampleRate() - activeSampleRate) > 0.0001f)) {
      clear();
    }
    if (activeBinCount == -1) {
      activeBinCount = frame.binCount();
      activeFftSize = frame.fftSize();
      activeSampleRate = frame.sampleRate();
    }
    int writeIndex;
    if (size < capacity) {
      writeIndex = (head + size) % capacity;
      size++;
    } else {
      writeIndex = head;
      head = (head + 1) % capacity;
    }
    frames[writeIndex] = frame;
  }

  /** Reset the history to empty, releasing references to retained frames. */
  public void clear() {
    Arrays.fill(frames, null);
    head = 0;
    size = 0;
    activeBinCount = -1;
    activeFftSize = -1;
    activeSampleRate = -1f;
  }

  /**
   * @return the most recently appended frame, or {@code null} if empty
   */
  public SpectrogramFrame latest() {
    if (size == 0) {
      return null;
    }
    int idx = (head + size - 1) % capacity;
    return frames[idx];
  }

  /**
   * @param indexFromOldest zero-based index from the oldest stored frame
   * @return the frame at that position
   * @throws IndexOutOfBoundsException if the index is out of range
   */
  public SpectrogramFrame frameAt(int indexFromOldest) {
    if (indexFromOldest < 0 || indexFromOldest >= size) {
      throw new IndexOutOfBoundsException("indexFromOldest=" + indexFromOldest + ", size=" + size);
    }
    return frames[(head + indexFromOldest) % capacity];
  }

  /**
   * @return an immutable copy of the stored frames in order from oldest to newest
   */
  public List<SpectrogramFrame> snapshot() {
    List<SpectrogramFrame> out = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      out.add(frames[(head + i) % capacity]);
    }
    return List.copyOf(out);
  }
}
