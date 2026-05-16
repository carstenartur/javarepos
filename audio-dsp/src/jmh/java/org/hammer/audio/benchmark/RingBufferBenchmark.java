package org.hammer.audio.benchmark;

import java.util.concurrent.TimeUnit;
import org.hammer.audio.buffer.AudioRingBuffer;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

/**
 * JMH benchmark for the lock-free SPSC ring buffer.
 *
 * <p>Measures single-threaded round-trip throughput (offer + poll) at multiple capacities. The
 * SPSC contract means a multi-threaded benchmark would require a JMH async setup; this single-
 * thread benchmark is enough to track regressions in the hot-path math.
 *
 * <p>To run: {@code mvn clean verify -Pjmh} then execute the JMH jar.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
public class RingBufferBenchmark {

  @Param({"64", "1024"})
  private int capacity;

  private AudioRingBuffer<Integer> rb;
  private Integer payload;

  @Setup
  public void setup() {
    rb = new AudioRingBuffer<>(capacity);
    payload = 42;
  }

  @Benchmark
  public void offerPoll(Blackhole bh) {
    rb.offer(payload);
    bh.consume(rb.poll());
  }

  @Benchmark
  public void offerOverwrite(Blackhole bh) {
    bh.consume(rb.offerOverwrite(payload));
  }
}
