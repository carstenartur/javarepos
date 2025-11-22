package org.hammer.audio.benchmark;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

/**
 * JMH benchmark for sample decoding performance.
 *
 * <p>This benchmark measures the throughput of audio sample decoding on synthetic buffers without
 * requiring an audio device. It exercises the critical path of sample reading and scaling.
 *
 * <p>To run: mvn clean verify -Pjmh exec:java
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
public class SampleDecodingBenchmark {

  private byte[] buffer16BitSigned;
  private byte[] buffer8BitSigned;
  private byte[] buffer16BitUnsigned;
  private byte[] buffer8BitUnsigned;
  private static final int BUFFER_SIZE = 8192;
  private static final int SAMPLE_SIZE_8 = 8;
  private static final int SAMPLE_SIZE_16 = 16;
  private static final int CHANNELS = 2;
  private static final int PANEL_HEIGHT = 200;

  @Setup
  public void setup() {
    // Create synthetic audio buffers for different formats
    buffer16BitSigned = new byte[BUFFER_SIZE];
    buffer8BitSigned = new byte[BUFFER_SIZE];
    buffer16BitUnsigned = new byte[BUFFER_SIZE];
    buffer8BitUnsigned = new byte[BUFFER_SIZE];

    // Fill with synthetic waveform data
    for (int i = 0; i < BUFFER_SIZE; i++) {
      buffer16BitSigned[i] = (byte) (Math.sin(i / 10.0) * 127);
      buffer8BitSigned[i] = (byte) (Math.sin(i / 10.0) * 127);
      buffer16BitUnsigned[i] = (byte) ((Math.sin(i / 10.0) * 127) + 128);
      buffer8BitUnsigned[i] = (byte) ((Math.sin(i / 10.0) * 127) + 128);
    }
  }

  @Benchmark
  public void decode16BitSignedLittleEndian(Blackhole bh) {
    decodeAndScale(buffer16BitSigned, SAMPLE_SIZE_16, true, false, bh);
  }

  @Benchmark
  public void decode16BitSignedBigEndian(Blackhole bh) {
    decodeAndScale(buffer16BitSigned, SAMPLE_SIZE_16, true, true, bh);
  }

  @Benchmark
  public void decode8BitSigned(Blackhole bh) {
    decodeAndScale(buffer8BitSigned, SAMPLE_SIZE_8, true, false, bh);
  }

  @Benchmark
  public void decode16BitUnsignedLittleEndian(Blackhole bh) {
    decodeAndScale(buffer16BitUnsigned, SAMPLE_SIZE_16, false, false, bh);
  }

  @Benchmark
  public void decode8BitUnsigned(Blackhole bh) {
    decodeAndScale(buffer8BitUnsigned, SAMPLE_SIZE_8, false, false, bh);
  }

  private void decodeAndScale(
      byte[] buffer, int sampleSizeInBits, boolean signed, boolean bigEndian, Blackhole bh) {
    final int bytesPerSample = sampleSizeInBits / 8;
    final int frameSize = bytesPerSample * CHANNELS;
    final int frames = buffer.length / frameSize;

    for (int frame = 0; frame < frames; frame++) {
      final int frameOffset = frame * frameSize;
      for (int ch = 0; ch < CHANNELS; ch++) {
        final int sampleOffset = frameOffset + ch * bytesPerSample;
        int sample = readSample(buffer, sampleOffset, bytesPerSample, signed, bigEndian);
        int y = scaleToPixel(sample, sampleSizeInBits, signed);
        bh.consume(y);
      }
    }
  }

  private int readSample(
      byte[] data, int offset, int bytesPerSample, boolean signed, boolean bigEndian) {
    int sample = 0;

    if (bytesPerSample == 1) {
      final int b = data[offset] & 0xFF;
      if (signed) {
        sample = (byte) b; // sign extend
      } else {
        sample = b;
      }
    } else if (bytesPerSample == 2) {
      final int hi = data[offset + (bigEndian ? 0 : 1)] & 0xFF;
      final int lo = data[offset + (bigEndian ? 1 : 0)] & 0xFF;
      final int raw = (hi << 8) | lo;
      if (signed) {
        sample = (short) raw; // sign extend to int
      } else {
        sample = raw & 0xFFFF;
      }
    }

    return sample;
  }

  private int scaleToPixel(int sample, int sampleSizeInBits, boolean signed) {
    int y = 0;

    if (PANEL_HEIGHT > 0) {
      if (signed) {
        int maxAbs = (1 << (sampleSizeInBits - 1)) - 1;
        float norm = (float) sample / (float) maxAbs; // -1..1
        y = Math.round((PANEL_HEIGHT / 2f) - norm * (PANEL_HEIGHT / 2f));
      } else {
        int max = (1 << sampleSizeInBits) - 1;
        float norm = (float) sample / (float) max; // 0..1
        y = Math.round(PANEL_HEIGHT - norm * PANEL_HEIGHT);
      }
    }

    return y;
  }
}
