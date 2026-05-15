package org.hammer.audio.signal;

import org.hammer.audio.core.AudioBlock;
import org.hammer.audio.core.AudioFormatDescriptor;

/**
 * Deterministic mono sine-wave generator.
 *
 * <p>Produces {@code amplitude * sin(2π * frequency * t)} samples, sampled at {@code
 * format.sampleRate()}. Phase is tracked across calls in double precision to avoid drift over
 * long runs.
 *
 * <p>Although mono internally, the generator can be configured with a multi-channel format; in
 * that case the same signal is broadcast on every channel.
 *
 * @author refactoring
 */
public final class SineGenerator implements SignalGenerator {

  private final AudioFormatDescriptor format;
  private final double frequencyHz;
  private final float amplitude;
  private final double phaseStep;
  private double phase;
  private long frameIndex;

  /**
   * Create a new sine generator.
   *
   * @param format output format descriptor
   * @param frequencyHz oscillator frequency in Hz; must be {@code > 0}
   * @param amplitude peak amplitude in normalized units (typically in {@code (0, 1]})
   */
  public SineGenerator(AudioFormatDescriptor format, double frequencyHz, float amplitude) {
    if (!(frequencyHz > 0.0)) {
      throw new IllegalArgumentException("frequencyHz must be > 0, was " + frequencyHz);
    }
    this.format = format;
    this.frequencyHz = frequencyHz;
    this.amplitude = amplitude;
    this.phaseStep = 2.0 * Math.PI * frequencyHz / format.sampleRate();
    this.phase = 0.0;
    this.frameIndex = 0L;
  }

  @Override
  public AudioFormatDescriptor format() {
    return format;
  }

  @Override
  public AudioBlock nextBlock(int frames) {
    if (frames < 1) {
      throw new IllegalArgumentException("frames must be >= 1");
    }
    int channels = format.channels();
    float[][] samples = new float[channels][frames];
    double p = phase;
    for (int i = 0; i < frames; i++) {
      float v = (float) (Math.sin(p) * amplitude);
      for (int c = 0; c < channels; c++) {
        samples[c][i] = v;
      }
      p += phaseStep;
    }
    // Wrap phase to keep precision over long generations.
    p = p % (2.0 * Math.PI);
    long index = frameIndex;
    phase = p;
    frameIndex += frames;
    return AudioBlock.wrap(format, samples, index, System.nanoTime());
  }

  @Override
  public void reset() {
    phase = 0.0;
    frameIndex = 0L;
  }

  /** @return frequency of the oscillator in Hz */
  public double frequencyHz() {
    return frequencyHz;
  }

  /** @return peak amplitude */
  public float amplitude() {
    return amplitude;
  }
}
