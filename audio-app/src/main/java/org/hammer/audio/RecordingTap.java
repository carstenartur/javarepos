package org.hammer.audio;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Timer;
import org.hammer.audio.core.AudioBlock;
import org.hammer.audio.recording.AudioBlockRecordingWriter;

/**
 * Best-effort recorder that polls an {@link AudioCaptureService} on the Swing EDT and persists
 * every newly observed {@link AudioBlock} to an {@code .aar} file via {@link
 * AudioBlockRecordingWriter}.
 *
 * <p>Blocks are deduplicated by {@link AudioBlock#frameIndex()}. The poll interval should be at
 * least as fast as the capture service produces blocks; otherwise blocks may be missed. With the
 * default capture/demo configuration (≥10 ms per block) the standard UI refresh of a few tens of
 * milliseconds is fast enough for diagnostic recordings.
 *
 * <p>This class lives in {@code audio-app} because it relies on the Swing {@link Timer}; the
 * underlying file format and writer are in {@code audio-dsp}.
 */
public final class RecordingTap {

  private static final Logger LOGGER = Logger.getLogger(RecordingTap.class.getName());

  private final AudioCaptureService service;
  private final AudioBlockRecordingWriter writer;
  private final Timer pollTimer;
  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final Path file;

  private long lastSeenFrameIndex = Long.MIN_VALUE;
  private long blocksWritten;
  private boolean firstBlockSeen;

  /**
   * Start recording from {@code service} into {@code file}.
   *
   * @param service capture service to poll
   * @param file destination file (will be created or truncated)
   * @param pollIntervalMs how often to poll for new blocks
   */
  public static RecordingTap start(AudioCaptureService service, Path file, int pollIntervalMs)
      throws IOException {
    Objects.requireNonNull(service, "service");
    Objects.requireNonNull(file, "file");
    if (pollIntervalMs < 1) {
      throw new IllegalArgumentException("pollIntervalMs must be >= 1, was " + pollIntervalMs);
    }
    AudioBlockRecordingWriter writer = AudioBlockRecordingWriter.open(file);
    RecordingTap tap = new RecordingTap(service, writer, file, pollIntervalMs);
    tap.pollTimer.start();
    return tap;
  }

  private RecordingTap(
      AudioCaptureService service,
      AudioBlockRecordingWriter writer,
      Path file,
      int pollIntervalMs) {
    this.service = service;
    this.writer = writer;
    this.file = file;
    this.pollTimer = new Timer(pollIntervalMs, e -> pollOnce());
    this.pollTimer.setRepeats(true);
  }

  /**
   * @return the destination file passed to {@link #start}
   */
  public Path file() {
    return file;
  }

  /**
   * @return number of blocks written so far
   */
  public long blocksWritten() {
    return blocksWritten;
  }

  /**
   * @return true if {@link #stop()} has been invoked
   */
  public boolean isClosed() {
    return closed.get();
  }

  private void pollOnce() {
    if (closed.get()) {
      return;
    }
    AudioBlock block = service.getLatestBlock();
    if (block == null) {
      return;
    }
    long frameIndex = block.frameIndex();
    if (firstBlockSeen && frameIndex == lastSeenFrameIndex) {
      return;
    }
    if (firstBlockSeen && frameIndex < lastSeenFrameIndex) {
      // Source restarted (e.g. service was stopped/started). Accept it.
      LOGGER.fine(() -> "frame index moved backwards; treating as restart");
    }
    try {
      writer.write(block);
      blocksWritten++;
      lastSeenFrameIndex = frameIndex;
      firstBlockSeen = true;
    } catch (IOException ex) {
      LOGGER.log(Level.WARNING, "failed to write block to recording, stopping tap", ex);
      stopQuietly();
    } catch (RuntimeException ex) {
      // Format mismatch (capture service reconfigured mid-recording), writer already closed,
      // etc. Stop the tap rather than letting the Swing Timer spam the EDT.
      LOGGER.log(Level.WARNING, "runtime error while writing block, stopping tap", ex);
      stopQuietly();
    }
  }

  /** Stop the tap and close the underlying writer. Safe to call multiple times. */
  public void stop() throws IOException {
    if (closed.getAndSet(true)) {
      return;
    }
    pollTimer.stop();
    writer.close();
  }

  private void stopQuietly() {
    try {
      stop();
    } catch (IOException ignored) {
      // already logged above
    }
  }
}
