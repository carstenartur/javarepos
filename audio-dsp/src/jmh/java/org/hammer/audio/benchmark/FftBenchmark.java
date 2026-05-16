package org.hammer.audio.benchmark;

import java.util.concurrent.TimeUnit;
import org.hammer.audio.analysis.Fft;
import org.hammer.audio.analysis.SpectrumAnalyzer;
import org.hammer.audio.core.AudioBlock;
import org.hammer.audio.core.AudioFormatDescriptor;
import org.hammer.audio.signal.SineGenerator;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

/**
 * JMH benchmark for the pure-Java FFT and the windowed {@link SpectrumAnalyzer}.
 *
 * <p>Measures throughput of forward transforms at typical realtime sizes (1024, 4096, 16384).
 * Caller-side allocations (re/im arrays) are reused across invocations to isolate the FFT cost
 * itself, so this also indirectly benchmarks the analyzer's allocation behavior.
 *
 * <p>To run: {@code mvn clean verify -Pjmh} then execute the JMH jar.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
public class FftBenchmark {

  @Param({"1024", "4096", "16384"})
  private int size;

  private Fft fft;
  private float[] re;
  private float[] im;
  private float[] mag;
  private SpectrumAnalyzer analyzer;
  private AudioBlock block;

  @Setup
  public void setup() {
    fft = new Fft(size);
    re = new float[size];
    im = new float[size];
    mag = new float[size / 2 + 1];
    for (int i = 0; i < size; i++) {
      re[i] = (float) Math.sin(2.0 * Math.PI * 5 * i / size);
    }
    AudioFormatDescriptor fmt = new AudioFormatDescriptor(48000f, 1, 16);
    SineGenerator gen = new SineGenerator(fmt, 1000.0, 1f);
    block = gen.nextBlock(size);
    analyzer = new SpectrumAnalyzer(size, 0, 48000f);
  }

  @Benchmark
  public void rawForwardFft(Blackhole bh) {
    // Reset im to zeros each iteration; re holds reusable input we overwrite.
    for (int i = 0; i < size; i++) {
      im[i] = 0f;
    }
    fft.forward(re, im);
    fft.magnitudes(re, im, mag);
    bh.consume(mag);
  }

  @Benchmark
  public void analyzerEndToEnd(Blackhole bh) {
    bh.consume(analyzer.analyze(block));
  }
}
