package org.hammer.audio.acquisition;

import java.io.IOException;
import java.util.Optional;
import org.hammer.audio.core.AudioBlock;
import org.hammer.audio.core.AudioFormatDescriptor;

/** API-neutral synchronized multichannel audio source for devices, replay files or simulators. */
public interface MultiChannelAudioSource extends AutoCloseable {

  /** Return the source format. */
  AudioFormatDescriptor format();

  /** Return microphone metadata for the synchronized channels. */
  MicrophoneArray microphoneArray();

  /** Read up to {@code frames} frames, or {@link Optional#empty()} when the source is exhausted. */
  Optional<AudioBlock> readBlock(int frames) throws IOException;

  /** Close the source. */
  @Override
  default void close() throws IOException {
    // Default for synthetic sources with no external handle.
  }
}
