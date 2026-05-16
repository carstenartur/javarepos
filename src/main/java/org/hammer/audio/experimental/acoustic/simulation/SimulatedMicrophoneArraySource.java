package org.hammer.audio.experimental.acoustic.simulation;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.hammer.audio.acquisition.Microphone;
import org.hammer.audio.acquisition.MicrophoneArray;
import org.hammer.audio.acquisition.MultiChannelAudioSource;
import org.hammer.audio.acquisition.SampleClock;
import org.hammer.audio.core.AudioBlock;
import org.hammer.audio.core.AudioFormatDescriptor;
import org.hammer.audio.geometry.Vector2;

/** Deterministic synthetic synchronized multichannel source for virtual room experiments. */
public final class SimulatedMicrophoneArraySource implements MultiChannelAudioSource {

  /** Default speed of sound in air for experiments. */
  public static final double DEFAULT_SPEED_OF_SOUND_METERS_PER_SECOND = 343.0;

  private final Room2D room;
  private final MicrophoneArray array;
  private final List<SoundEmitter2D> emitters;
  private final AudioFormatDescriptor format;
  private final SampleClock clock;
  private final Random random;
  private final long totalFrames;
  private long nextFrameIndex;

  /** Create a deterministic simulation source. */
  public SimulatedMicrophoneArraySource(
      Room2D room,
      MicrophoneArray array,
      List<SoundEmitter2D> emitters,
      float sampleRate,
      double durationSeconds,
      long randomSeed) {
    if (emitters == null || emitters.isEmpty()) {
      throw new IllegalArgumentException("emitters must not be empty");
    }
    if (!(durationSeconds > 0.0) || !Double.isFinite(durationSeconds)) {
      throw new IllegalArgumentException("durationSeconds must be finite and > 0");
    }
    this.room = room;
    this.array = array;
    this.emitters = List.copyOf(emitters);
    this.format = new AudioFormatDescriptor(sampleRate, array.channels(), 32);
    this.clock = new SampleClock(sampleRate, 0L);
    this.random = new Random(randomSeed);
    this.totalFrames = Math.round(durationSeconds * sampleRate);
  }

  @Override
  public AudioFormatDescriptor format() {
    return format;
  }

  @Override
  public MicrophoneArray microphoneArray() {
    return array;
  }

  @Override
  public Optional<AudioBlock> readBlock(int frames) {
    if (frames <= 0) {
      throw new IllegalArgumentException("frames must be > 0");
    }
    if (nextFrameIndex >= totalFrames) {
      return Optional.empty();
    }
    int blockFrames = (int) Math.min(frames, totalFrames - nextFrameIndex);
    float[][] samples = new float[array.channels()][blockFrames];
    for (int frame = 0; frame < blockFrames; frame++) {
      long absoluteFrame = nextFrameIndex + frame;
      double receiverTime = absoluteFrame / format.sampleRate();
      for (Microphone mic : array.microphones()) {
        samples[mic.channel()][frame] = (float) sampleAt(mic.positionMeters(), receiverTime);
      }
    }
    AudioBlock block =
        AudioBlock.wrap(format, samples, nextFrameIndex, clock.timestampForFrame(nextFrameIndex));
    nextFrameIndex += blockFrames;
    return Optional.of(block);
  }

  private double sampleAt(Vector2 microphonePosition, double receiverTimeSeconds) {
    double sample = room.noiseAmplitude() * (random.nextDouble() * 2.0 - 1.0);
    for (SoundEmitter2D emitter : emitters) {
      Vector2 emitterPosition = emitter.positionAt(receiverTimeSeconds);
      double distance = Math.max(0.01, microphonePosition.distanceTo(emitterPosition));
      double travelSeconds = distance / DEFAULT_SPEED_OF_SOUND_METERS_PER_SECOND;
      sample += emitter.sampleAt(receiverTimeSeconds - travelSeconds) / distance;
      if (room.reflectionGain() > 0.0) {
        Vector2 reflected =
            new Vector2(room.widthMeters() - emitterPosition.x(), emitterPosition.y());
        double reflectedDistance = Math.max(0.01, microphonePosition.distanceTo(reflected));
        double reflectedTravel = reflectedDistance / DEFAULT_SPEED_OF_SOUND_METERS_PER_SECOND;
        sample +=
            room.reflectionGain()
                * emitter.sampleAt(receiverTimeSeconds - reflectedTravel)
                / reflectedDistance;
      }
    }
    return Math.max(-1.0, Math.min(1.0, sample));
  }
}
