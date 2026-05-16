package org.hammer.audio.signal;

import org.hammer.audio.DemoSignalType;
import org.hammer.audio.core.AudioBlock;
import org.hammer.audio.core.AudioFormatDescriptor;

/** Synthetic stereo scenarios for reproducible measurement demos without audio hardware. */
public final class DemoPresetGenerator implements SignalGenerator {

  private static final double TWO_PI = 2.0 * Math.PI;
  private static final double DEFAULT_AMPLITUDE = 0.75;
  private static final int STEREO_DELAY_SAMPLES = 6;

  private final AudioFormatDescriptor format;
  private final DemoSignalType signalType;
  private long frameIndex;

  public DemoPresetGenerator(AudioFormatDescriptor format, DemoSignalType signalType) {
    this.format = format;
    this.signalType = signalType;
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
    float[][] samples = new float[format.channels()][frames];
    for (int frame = 0; frame < frames; frame++) {
      long absoluteFrame = frameIndex + frame;
      int delay = delaySamples(absoluteFrame);
      for (int channel = 0; channel < format.channels(); channel++) {
        long sourceFrame = channel == 1 ? absoluteFrame - delay : absoluteFrame;
        samples[channel][frame] = (float) sampleAt(sourceFrame, channel);
      }
    }
    AudioBlock block = AudioBlock.wrap(format, samples, frameIndex, System.nanoTime());
    frameIndex += frames;
    return block;
  }

  @Override
  public void reset() {
    frameIndex = 0L;
  }

  private int delaySamples(long absoluteFrame) {
    return switch (signalType) {
      case MOVING_CHIRP -> (int) Math.round(6.0 * Math.sin(TWO_PI * seconds(absoluteFrame) / 3.0));
      case MOSQUITO_BURST -> 4;
      case STEREO_DELAY_TEST -> STEREO_DELAY_SAMPLES;
      default -> 0;
    };
  }

  private double sampleAt(long absoluteFrame, int channel) {
    return switch (signalType) {
      case MOSQUITO_BURST -> mosquitoLikeBurst(absoluteFrame, channel);
      case MOVING_CHIRP -> movingChirp(absoluteFrame);
      case HUM_HARMONICS -> humWithHarmonics(absoluteFrame, channel);
      case CLIPPING_TEST -> clippingTest(absoluteFrame);
      case STEREO_DELAY_TEST -> stereoDelayProbe(absoluteFrame);
      default -> 0.0;
    };
  }

  private double mosquitoLikeBurst(long absoluteFrame, int channel) {
    double sampleRate = format.sampleRate();
    double burstPeriod = 0.42;
    double burstPosition = seconds(absoluteFrame) % burstPeriod;
    double burstLength = 0.055;
    double envelope =
        burstPosition < burstLength
            ? Math.sin(Math.PI * burstPosition / burstLength)
            : 0.0;
    double carrier = Math.sin(TWO_PI * 5_200.0 * absoluteFrame / sampleRate);
    double flutter = 0.65 + 0.35 * Math.sin(TWO_PI * 95.0 * absoluteFrame / sampleRate);
    double echo =
        0.18
            * envelope
            * Math.sin(TWO_PI * 5_200.0 * (absoluteFrame - 18) / sampleRate);
    double noise = 0.015 * deterministicNoise(absoluteFrame + channel * 17L);
    return 0.52 * envelope * flutter * carrier + echo + noise;
  }

  private double movingChirp(long absoluteFrame) {
    double cycle = 3.0;
    double position = (seconds(absoluteFrame) % cycle) / cycle;
    double frequency = 350.0 + position * 4_500.0;
    return 0.58 * Math.sin(TWO_PI * frequency * absoluteFrame / format.sampleRate());
  }

  private double humWithHarmonics(long absoluteFrame, int channel) {
    double t = seconds(absoluteFrame);
    double noise = 0.012 * deterministicNoise(absoluteFrame + channel * 31L);
    return 0.45 * Math.sin(TWO_PI * 50.0 * t)
        + 0.18 * Math.sin(TWO_PI * 100.0 * t)
        + 0.09 * Math.sin(TWO_PI * 150.0 * t)
        + noise;
  }

  private double clippingTest(long absoluteFrame) {
    double value = 1.6 * Math.sin(TWO_PI * 440.0 * seconds(absoluteFrame));
    return Math.max(-1.0, Math.min(1.0, value));
  }

  private double stereoDelayProbe(long absoluteFrame) {
    double t = seconds(absoluteFrame);
    double burst = Math.sin(TWO_PI * 880.0 * t) + 0.45 * Math.sin(TWO_PI * 2_700.0 * t);
    double gate = (absoluteFrame / 1_200) % 2 == 0 ? 1.0 : 0.25;
    return DEFAULT_AMPLITUDE * gate * burst / 1.45;
  }

  private double seconds(long absoluteFrame) {
    return absoluteFrame / (double) format.sampleRate();
  }

  private static double deterministicNoise(long value) {
    long mixed = value * 6_364_136_223_846_793_005L + 1_442_695_040_888_963_407L;
    mixed ^= mixed >>> 33;
    return ((mixed & 0xffffL) / 32767.5) - 1.0;
  }
}
