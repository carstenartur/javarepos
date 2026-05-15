package org.hammer.audio.signal;

import org.hammer.audio.core.AudioBlock;
import org.hammer.audio.core.AudioFormatDescriptor;

/**
 * Deterministic linear chirp / sweep generator.
 *
 * <p>Produces a sinusoidal signal whose instantaneous frequency increases linearly from {@code
 * startHz} to {@code endHz} over {@code durationSeconds}. After the sweep completes the signal
 * holds at {@code endHz} (or, if {@link #setLooping(boolean)} is enabled, restarts).
 *
 * @author refactoring
 */
public final class ChirpGenerator implements SignalGenerator {

  private final AudioFormatDescriptor format;
  private final double startHz;
  private final double endHz;
  private final double durationSeconds;
  private final float amplitude;
  private final double sampleRate;
  private boolean looping;
  private double timeSeconds;
  private double phase;
  private long frameIndex;

  /**
   * @param format output format descriptor
   * @param startHz starting frequency (Hz); must be {@code > 0}
   * @param endHz ending frequency (Hz); must be {@code > 0}
   * @param durationSeconds sweep duration in seconds; must be {@code > 0}
   * @param amplitude peak amplitude
   */
  public ChirpGenerator(
      AudioFormatDescriptor format,
      double startHz,
      double endHz,
      double durationSeconds,
      float amplitude) {
    if (!(startHz > 0.0)) {
      throw new IllegalArgumentException("startHz must be > 0");
    }
    if (!(endHz > 0.0)) {
      throw new IllegalArgumentException("endHz must be > 0");
    }
    if (!(durationSeconds > 0.0)) {
      throw new IllegalArgumentException("durationSeconds must be > 0");
    }
    this.format = format;
    this.startHz = startHz;
    this.endHz = endHz;
    this.durationSeconds = durationSeconds;
    this.amplitude = amplitude;
    this.sampleRate = format.sampleRate();
  }

  /**
   * Configure looping (the chirp restarts after {@code durationSeconds}).
   *
   * @param looping whether to loop
   */
  public void setLooping(boolean looping) {
    this.looping = looping;
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
    double dt = 1.0 / sampleRate;
    double t = timeSeconds;
    double p = phase;
    for (int i = 0; i < frames; i++) {
      double tEff = looping ? (t % durationSeconds) : Math.min(t, durationSeconds);
      double instFreq = startHz + (endHz - startHz) * (tEff / durationSeconds);
      // integrate frequency to advance phase
      p += 2.0 * Math.PI * instFreq * dt;
      float v = (float) (Math.sin(p) * amplitude);
      for (int c = 0; c < channels; c++) {
        samples[c][i] = v;
      }
      t += dt;
    }
    long index = frameIndex;
    timeSeconds = t;
    phase = p % (2.0 * Math.PI);
    frameIndex += frames;
    return AudioBlock.wrap(format, samples, index, System.nanoTime());
  }

  @Override
  public void reset() {
    timeSeconds = 0.0;
    phase = 0.0;
    frameIndex = 0L;
  }
}
