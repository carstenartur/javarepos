package org.hammer.audio.buffer;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class AudioRingBufferTest {

  @Test
  void capacity_rounds_up_to_power_of_two() {
    assertEquals(1, new AudioRingBuffer<>(1).capacity());
    assertEquals(2, new AudioRingBuffer<>(2).capacity());
    assertEquals(4, new AudioRingBuffer<>(3).capacity());
    assertEquals(8, new AudioRingBuffer<>(5).capacity());
    assertEquals(16, new AudioRingBuffer<>(16).capacity());
  }

  @Test
  void rejects_invalid_capacity() {
    assertThrows(IllegalArgumentException.class, () -> new AudioRingBuffer<>(0));
    assertThrows(IllegalArgumentException.class, () -> new AudioRingBuffer<>(-1));
  }

  @Test
  void offer_and_poll_in_FIFO_order() {
    AudioRingBuffer<Integer> rb = new AudioRingBuffer<>(4);
    assertTrue(rb.offer(1));
    assertTrue(rb.offer(2));
    assertTrue(rb.offer(3));
    assertEquals(3, rb.size());

    assertEquals(1, rb.poll());
    assertEquals(2, rb.poll());
    assertEquals(3, rb.poll());
    assertNull(rb.poll());
    assertTrue(rb.isEmpty());
  }

  @Test
  void offer_returns_false_when_full() {
    AudioRingBuffer<Integer> rb = new AudioRingBuffer<>(2);
    assertTrue(rb.offer(1));
    assertTrue(rb.offer(2));
    assertFalse(rb.offer(3));
    assertTrue(rb.isFull());
  }

  @Test
  void offerOverwrite_drops_oldest_when_full() {
    AudioRingBuffer<Integer> rb = new AudioRingBuffer<>(2);
    rb.offer(1);
    rb.offer(2);
    Integer dropped = rb.offerOverwrite(3);
    assertEquals(1, dropped, "oldest element should be dropped");
    // After overwrite, the buffer should contain [2, 3] in FIFO order
    assertEquals(2, rb.poll());
    assertEquals(3, rb.poll());
  }

  @Test
  void wrap_around_works_correctly() {
    AudioRingBuffer<Integer> rb = new AudioRingBuffer<>(4);
    for (int i = 0; i < 100; i++) {
      assertTrue(rb.offer(i));
      assertEquals(i, rb.poll());
    }
    assertTrue(rb.isEmpty());
  }

  @Test
  void drainTo_drains_up_to_max() {
    AudioRingBuffer<Integer> rb = new AudioRingBuffer<>(8);
    for (int i = 0; i < 5; i++) {
      rb.offer(i);
    }
    Integer[] dest = new Integer[10];
    int drained = rb.drainTo(dest, 10);
    assertEquals(5, drained);
    for (int i = 0; i < drained; i++) {
      assertEquals(i, dest[i]);
    }
    assertTrue(rb.isEmpty());
  }

  @Test
  void clear_resets_to_empty() {
    AudioRingBuffer<Integer> rb = new AudioRingBuffer<>(4);
    rb.offer(1);
    rb.offer(2);
    rb.clear();
    assertTrue(rb.isEmpty());
    assertEquals(0, rb.size());
    assertNull(rb.poll());
  }

  @Test
  void rejects_null_elements() {
    AudioRingBuffer<Integer> rb = new AudioRingBuffer<>(4);
    assertThrows(NullPointerException.class, () -> rb.offer(null));
    assertThrows(NullPointerException.class, () -> rb.offerOverwrite(null));
  }

  @Test
  void spsc_concurrent_stress_no_loss() throws Exception {
    final int totalItems = 100_000;
    final AudioRingBuffer<Integer> rb = new AudioRingBuffer<>(64);
    final AtomicBoolean failed = new AtomicBoolean(false);
    final AtomicLong sumConsumed = new AtomicLong(0);

    Thread producer = new Thread(() -> {
      for (int i = 1; i <= totalItems; i++) {
        // Spin if full — back-pressure
        while (!rb.offer(i)) {
          Thread.onSpinWait();
        }
      }
    }, "producer");

    Thread consumer = new Thread(() -> {
      int received = 0;
      int lastValue = 0;
      while (received < totalItems) {
        Integer v = rb.poll();
        if (v == null) {
          Thread.onSpinWait();
          continue;
        }
        if (v <= lastValue) {
          failed.set(true);
        }
        lastValue = v;
        sumConsumed.addAndGet(v);
        received++;
      }
    }, "consumer");

    consumer.start();
    producer.start();
    producer.join(10_000);
    consumer.join(10_000);

    assertFalse(failed.get(), "values must be received in FIFO order");
    long expected = (long) totalItems * (totalItems + 1) / 2;
    assertEquals(expected, sumConsumed.get(), "no items should be lost or duplicated");
  }
}
