package org.hammer.audio.recording;

import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.hammer.audio.core.AudioBlock;
import org.hammer.audio.core.AudioFormatDescriptor;

/**
 * Writes {@link AudioBlock}s to a binary recording file as documented in {@link
 * AudioBlockRecordingFormat}.
 *
 * <p>The writer derives its format header from the first {@link #write(AudioBlock)} call. All
 * subsequent blocks must use the same {@link AudioFormatDescriptor}.
 *
 * <p>Instances are <strong>not thread-safe</strong>.
 */
public final class AudioBlockRecordingWriter implements Closeable {

  private final DataOutputStream out;
  private AudioFormatDescriptor format;
  private long blocksWritten;
  private boolean closed;

  /** Open a writer that writes to the given file (creating/truncating it). */
  public static AudioBlockRecordingWriter open(Path file) throws IOException {
    Objects.requireNonNull(file, "file");
    return new AudioBlockRecordingWriter(Files.newOutputStream(file));
  }

  /** Wrap an existing output stream. The stream will be closed by {@link #close()}. */
  public AudioBlockRecordingWriter(OutputStream stream) {
    this.out = new DataOutputStream(Objects.requireNonNull(stream, "stream"));
  }

  /**
   * @return the format header that was written (or {@code null} if no block was written yet)
   */
  public AudioFormatDescriptor format() {
    return format;
  }

  /**
   * @return number of blocks successfully written so far
   */
  public long blocksWritten() {
    return blocksWritten;
  }

  /**
   * Append one block to the recording. The first call writes the file header.
   *
   * @param block block to write; must not be {@code null}
   * @throws IOException if the underlying stream fails
   * @throws IllegalStateException if the block's format differs from a previously written block
   */
  public void write(AudioBlock block) throws IOException {
    Objects.requireNonNull(block, "block");
    if (closed) {
      throw new IllegalStateException("writer is closed");
    }
    if (format == null) {
      writeHeader(block.format());
      format = block.format();
    } else if (!format.equals(block.format())) {
      throw new IllegalStateException(
          "format mismatch: expected " + format + " but block was " + block.format());
    }
    int frames = block.frames();
    int channels = block.channels();
    out.writeInt(frames);
    out.writeLong(block.frameIndex());
    out.writeLong(block.timestampNanos());
    for (int ch = 0; ch < channels; ch++) {
      float[] samples = block.channelView(ch);
      for (int i = 0; i < frames; i++) {
        out.writeFloat(samples[i]);
      }
    }
    blocksWritten++;
  }

  private void writeHeader(AudioFormatDescriptor fmt) throws IOException {
    out.writeInt(AudioBlockRecordingFormat.MAGIC);
    out.writeShort(AudioBlockRecordingFormat.VERSION);
    out.writeShort(fmt.channels());
    out.writeFloat(fmt.sampleRate());
    out.writeShort(fmt.sourceSampleSizeInBits());
    out.writeShort(0); // reserved
  }

  @Override
  public void close() throws IOException {
    if (closed) {
      return;
    }
    closed = true;
    out.close();
  }
}
