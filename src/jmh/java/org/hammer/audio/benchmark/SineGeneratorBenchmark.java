package org.hammer.audio.benchmark;

import java.util.concurrent.TimeUnit;
import org.hammer.audio.core.AudioBlock;
import org.hammer.audio.core.AudioFormatDescriptor;
import org.hammer.audio.signal.SineGenerator;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

/**
 * JMH benchmark for synthetic signal generators.
 *
 * <p>Tracks per-block allocation behavior and throughput of {@link SineGenerator}, which is the
 * fastest path through the {@link org.hammer.audio.core.AudioBlock} construction pipeline. A
 * regression here would point at a hot-path allocation issue in {@code AudioBlock.wrap} or in the
 * generator's per-frame loop.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
public class SineGeneratorBenchmark {

  @Param({"512", "4096"})
  private int frames;

  private SineGenerator generator;

  @Setup
  public void setup() {
    AudioFormatDescriptor fmt = new AudioFormatDescriptor(48000f, 2, 16);
    generator = new SineGenerator(fmt, 1000.0, 1f);
  }

  @Benchmark
  public void generateBlock(Blackhole bh) {
    AudioBlock block = generator.nextBlock(frames);
    bh.consume(block);
  }
}
