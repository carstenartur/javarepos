package org.hammer.audio.signal;

import org.hammer.audio.core.AudioBlock;
import org.hammer.audio.core.AudioFormatDescriptor;

/**
 * Deterministic mono square-wave generator (broadcast to all channels of {@code format}).
 *
 * <p>Produces values of {@code +amplitude} for the first half of each period and {@code -amplitude}
 * for the second half. Useful as a known harmonics-rich test signal for spectrum verification.
 *
 * @author refactoring
 */
public final class SquareGenerator implements SignalGenerator {

  private final AudioFormatDescriptor format;
  private final double frequencyHz;
  private final float amplitude;
  private final double phaseStep;
  private double phase;
  private long frameIndex;

  /**
   * @param format output format descriptor
   * @param frequencyHz oscillator frequency in Hz; must be {@code > 0}
   * @param amplitude peak amplitude
   */
  public SquareGenerator(AudioFormatDescriptor format, double frequencyHz, float amplitude) {
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
    final double twoPi = 2.0 * Math.PI;
    for (int i = 0; i < frames; i++) {
      float v = (Math.sin(p) >= 0.0) ? amplitude : -amplitude;
      for (int c = 0; c < channels; c++) {
        samples[c][i] = v;
      }
      p += phaseStep;
    }
    p = p % twoPi;
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

  /**
   * @return frequency of the oscillator in Hz
   */
  public double frequencyHz() {
    return frequencyHz;
  }
}
