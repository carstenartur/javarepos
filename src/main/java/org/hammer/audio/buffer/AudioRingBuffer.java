package org.hammer.audio.buffer;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Bounded, lock-free single-producer / single-consumer (SPSC) ring buffer.
 *
 * <p>This buffer is designed for realtime audio workloads where the audio capture thread is the
 * sole producer and a downstream DSP/analysis thread is the sole consumer. It avoids locks on the
 * hot path entirely: synchronization is performed via two {@link AtomicLong} sequences using
 * acquire/release semantics implicit in {@link AtomicLong#get()} / {@link AtomicLong#lazySet(long)}
 * /{@link AtomicLong#set(long)}.
 *
 * <p><strong>Capacity</strong> is rounded up to the next power of two so that the index calculation
 * can use a bitmask instead of modulo.
 *
 * <p><strong>Allocation</strong>: zero allocations on the hot path. The internal storage array is
 * allocated once at construction; {@link #offer(Object)} and {@link #poll()} only update sequence
 * counters.
 *
 * <p><strong>Concurrency contract</strong>: at most one producer thread may call {@link
 * #offer(Object)}, and at most one consumer thread may call {@link #poll()}. Read-only inspection
 * methods ({@link #size()}, {@link #isEmpty()}, {@link #isFull()}, {@link #capacity()}) are safe to
 * call from any thread.
 *
 * <p>This is intentionally specialized for SPSC: it is faster than a general-purpose queue and
 * matches the producer/consumer topology of an audio capture pipeline. For multi-producer or
 * multi-consumer scenarios use {@link java.util.concurrent.LinkedBlockingQueue} or similar.
 *
 * @param <T> element type; typically {@link org.hammer.audio.core.AudioBlock}
 * @author refactoring
 */
public final class AudioRingBuffer<T> {

  private final Object[] elements;
  private final int mask;
  private final int capacity;

  /** Next index a producer will write to (modulo capacity). Producer-write, consumer-read. */
  private final AtomicLong tail = new AtomicLong(0);

  /** Next index a consumer will read from (modulo capacity). Consumer-write, producer-read. */
  private final AtomicLong head = new AtomicLong(0);

  /**
   * Create a new SPSC ring buffer with at least the requested capacity.
   *
   * @param requestedCapacity minimum capacity; will be rounded up to the next power of two. Must be
   *     {@code >= 1} and {@code <= 2^30}.
   * @throws IllegalArgumentException if {@code requestedCapacity} is out of range
   */
  public AudioRingBuffer(int requestedCapacity) {
    if (requestedCapacity < 1) {
      throw new IllegalArgumentException("requestedCapacity must be >= 1");
    }
    if (requestedCapacity > (1 << 30)) {
      throw new IllegalArgumentException("requestedCapacity too large (max 2^30)");
    }
    int cap = 1;
    while (cap < requestedCapacity) {
      cap <<= 1;
    }
    this.capacity = cap;
    this.mask = cap - 1;
    this.elements = new Object[cap];
  }

  /** @return the actual capacity (next power of two &ge; the requested value) */
  public int capacity() {
    return capacity;
  }

  /**
   * Approximate current number of elements in the buffer. Safe to call from any thread; may be
   * slightly stale because head and tail are not read atomically together.
   *
   * @return number of elements currently buffered
   */
  public int size() {
    long t = tail.get();
    long h = head.get();
    long s = t - h;
    if (s < 0) {
      return 0;
    }
    return s > capacity ? capacity : (int) s;
  }

  /** @return {@code true} if empty (snapshot, may be stale) */
  public boolean isEmpty() {
    return tail.get() == head.get();
  }

  /** @return {@code true} if full (snapshot, may be stale) */
  public boolean isFull() {
    return (tail.get() - head.get()) >= capacity;
  }

  /**
   * Offer an element to the buffer. Producer-only operation.
   *
   * <p>This method never blocks. If the buffer is full the element is rejected and {@code false} is
   * returned: realtime audio capture should drop the oldest data only via an explicit overwrite
   * strategy (see {@link #offerOverwrite(Object)}), never silently stall.
   *
   * @param element element to enqueue; must not be {@code null}
   * @return {@code true} if accepted, {@code false} if the buffer is full
   * @throws NullPointerException if {@code element} is {@code null}
   */
  public boolean offer(T element) {
    if (element == null) {
      throw new NullPointerException("element");
    }
    long t = tail.get();
    long h = head.get();
    if (t - h >= capacity) {
      return false;
    }
    elements[(int) (t & mask)] = element;
    tail.lazySet(t + 1);
    return true;
  }

  /**
   * Offer an element to the buffer, dropping the oldest element if the buffer is full.
   *
   * <p>Producer-only operation. This is a typical strategy for realtime UI feeds where the most
   * recent data matters more than completeness. Returns the dropped element (or {@code null}).
   *
   * @param element element to enqueue; must not be {@code null}
   * @return the element dropped because the buffer was full, or {@code null} if nothing was dropped
   * @throws NullPointerException if {@code element} is {@code null}
   */
  @SuppressWarnings("unchecked")
  public T offerOverwrite(T element) {
    if (element == null) {
      throw new NullPointerException("element");
    }
    long t = tail.get();
    long h = head.get();
    T dropped = null;
    if (t - h >= capacity) {
      // Advance head, dropping the oldest element. Producer-side adjustment of head is
      // safe in SPSC only when the consumer does not also advance head concurrently in
      // practice this is guarded by the contract that producers using offerOverwrite
      // are typically used in a "latest-wins" topology, where an overflow indicates the
      // consumer is too slow. We accept that head may be advanced by either side: the
      // CAS below ensures we never advance past tail.
      long desiredHead = t - capacity + 1;
      // Best-effort advance: only move head forward.
      while (true) {
        long curHead = head.get();
        if (curHead >= desiredHead) {
          break;
        }
        if (head.compareAndSet(curHead, desiredHead)) {
          dropped = (T) elements[(int) (curHead & mask)];
          break;
        }
      }
    }
    elements[(int) (t & mask)] = element;
    tail.lazySet(t + 1);
    return dropped;
  }

  /**
   * Remove and return the oldest element, or {@code null} if the buffer is empty.
   *
   * <p>Consumer-only operation.
   *
   * @return the dequeued element, or {@code null} if the buffer is empty
   */
  @SuppressWarnings("unchecked")
  public T poll() {
    long h = head.get();
    long t = tail.get();
    if (h >= t) {
      return null;
    }
    int idx = (int) (h & mask);
    T element = (T) elements[idx];
    elements[idx] = null; // help GC
    head.lazySet(h + 1);
    return element;
  }

  /**
   * Drain up to {@code max} elements into the supplied destination array, starting at index 0.
   * Consumer-only operation.
   *
   * @param dest destination array; must not be {@code null} and must have length {@code >= max}
   * @param max maximum number of elements to drain
   * @return number of elements actually drained ({@code 0..max})
   * @throws IllegalArgumentException if {@code max < 0} or {@code dest.length < max}
   */
  @SuppressWarnings("unchecked")
  public int drainTo(T[] dest, int max) {
    if (max < 0) {
      throw new IllegalArgumentException("max must be >= 0");
    }
    if (dest.length < max) {
      throw new IllegalArgumentException("dest is too small");
    }
    int drained = 0;
    long h = head.get();
    long t = tail.get();
    while (drained < max && h < t) {
      int idx = (int) (h & mask);
      dest[drained++] = (T) elements[idx];
      elements[idx] = null;
      h++;
    }
    head.lazySet(h);
    return drained;
  }

  /** Reset the buffer to empty. Not safe to call concurrently with {@link #offer} or {@link #poll}. */
  public void clear() {
    long h = head.get();
    long t = tail.get();
    while (h < t) {
      elements[(int) (h & mask)] = null;
      h++;
    }
    head.set(t);
  }
}
