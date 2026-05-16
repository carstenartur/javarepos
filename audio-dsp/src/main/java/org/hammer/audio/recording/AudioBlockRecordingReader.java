package org.hammer.audio.recording;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.hammer.audio.core.AudioBlock;
import org.hammer.audio.core.AudioFormatDescriptor;

/**
 * Reads {@link AudioBlock}s from a binary recording file written by {@link
 * AudioBlockRecordingWriter}.
 *
 * <p>Instances are <strong>not thread-safe</strong>.
 */
public final class AudioBlockRecordingReader implements Closeable {

  private final DataInputStream in;
  private final AudioFormatDescriptor format;
  private boolean closed;

  /** Open a reader for the given file and parse its header. */
  public static AudioBlockRecordingReader open(Path file) throws IOException {
    Objects.requireNonNull(file, "file");
    return new AudioBlockRecordingReader(Files.newInputStream(file));
  }

  /**
   * Read the entire recording into memory. Suitable for short recordings used by replay or A/B
   * comparison; long recordings should stream via repeated {@link #next()} calls instead.
   */
  public static List<AudioBlock> readAll(Path file) throws IOException {
    try (AudioBlockRecordingReader reader = open(file)) {
      List<AudioBlock> blocks = new ArrayList<>();
      Optional<AudioBlock> next;
      while ((next = reader.next()).isPresent()) {
        blocks.add(next.get());
      }
      return Collections.unmodifiableList(blocks);
    }
  }

  /**
   * Wrap an existing input stream. The stream's header is read immediately.
   *
   * @throws IOException if the header is missing, truncated or uses an unsupported format
   */
  public AudioBlockRecordingReader(InputStream stream) throws IOException {
    this.in = new DataInputStream(Objects.requireNonNull(stream, "stream"));
    this.format = readHeader();
  }

  private AudioFormatDescriptor readHeader() throws IOException {
    int magic = in.readInt();
    if (magic != AudioBlockRecordingFormat.MAGIC) {
      throw new IOException(
          String.format(
              "not an audio block recording: bad magic 0x%08x (expected 0x%08x)",
              magic, AudioBlockRecordingFormat.MAGIC));
    }
    int version = in.readUnsignedShort();
    if (version != AudioBlockRecordingFormat.VERSION) {
      throw new IOException(
          "unsupported recording version "
              + version
              + " (this build supports "
              + AudioBlockRecordingFormat.VERSION
              + ")");
    }
    int channels = in.readUnsignedShort();
    float sampleRate = in.readFloat();
    int sourceBits = in.readUnsignedShort();
    in.readUnsignedShort(); // reserved
    if (channels < 1 || sourceBits < 1 || !(sampleRate > 0f) || Float.isNaN(sampleRate)) {
      throw new IOException(
          "invalid header values: channels="
              + channels
              + " sampleRate="
              + sampleRate
              + " sourceBits="
              + sourceBits);
    }
    return new AudioFormatDescriptor(sampleRate, channels, sourceBits);
  }

  /**
   * @return the audio format descriptor parsed from the file header
   */
  public AudioFormatDescriptor format() {
    return format;
  }

  /**
   * Read the next block or {@link Optional#empty()} at end-of-file.
   *
   * @throws IOException if the file is truncated in the middle of a block
   */
  public Optional<AudioBlock> next() throws IOException {
    if (closed) {
      return Optional.empty();
    }
    int frames;
    try {
      frames = in.readInt();
    } catch (EOFException eof) {
      return Optional.empty();
    }
    if (frames < 0) {
      throw new IOException("negative frame count: " + frames);
    }
    long frameIndex = in.readLong();
    long timestampNanos = in.readLong();
    int channels = format.channels();
    float[][] samples = new float[channels][frames];
    for (int ch = 0; ch < channels; ch++) {
      for (int i = 0; i < frames; i++) {
        samples[ch][i] = in.readFloat();
      }
    }
    return Optional.of(new AudioBlock(format, samples, frameIndex, timestampNanos));
  }

  @Override
  public void close() throws IOException {
    if (closed) {
      return;
    }
    closed = true;
    in.close();
  }
}
