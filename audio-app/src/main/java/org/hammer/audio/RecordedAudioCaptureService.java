package org.hammer.audio;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sound.sampled.AudioFormat;
import org.hammer.audio.buffer.AudioRingBuffer;
import org.hammer.audio.core.AudioBlock;
import org.hammer.audio.core.AudioFormatDescriptor;
import org.hammer.audio.recording.AudioBlockRecordingReader;
import org.hammer.audio.snapshot.WaveformSnapshot;
import org.hammer.audio.ui.WaveformRenderer;

/**
 * {@link AudioCaptureService} that replays a previously recorded {@code .aar} file. Blocks are
 * published at their natural sample-rate pace and exposed exactly like live or demo capture, so the
 * rest of the application (waveform panel, spectrum, diagnosis, evidence export, ...) is unaware of
 * the origin of the audio data.
 *
 * <p>When the recording is exhausted the service automatically stops, mirroring the behavior of
 * pressing "Stop" on a live capture.
 */
public final class RecordedAudioCaptureService implements AudioCaptureService {

  private static final int RING_BUFFER_CAPACITY = 64;

  private final List<AudioBlock> blocks;
  private final AudioFormatDescriptor descriptor;
  private final AudioFormat format;
  private final AudioRingBuffer<AudioBlock> ringBuffer =
      new AudioRingBuffer<>(RING_BUFFER_CAPACITY);
  private final AtomicBoolean running = new AtomicBoolean(false);
  private final boolean loop;

  private volatile WaveformModel latestModel = WaveformModel.EMPTY;
  private volatile AudioBlock latestBlock;
  private volatile int divisor = 1;
  private volatile int panelWidth = 640;
  private volatile int panelHeight = 200;

  private ExecutorService workerExecutor;

  /** Open a recording file and load its blocks into memory. */
  public static RecordedAudioCaptureService open(Path file, boolean loop) throws IOException {
    Objects.requireNonNull(file, "file");
    List<AudioBlock> blocks = AudioBlockRecordingReader.readAll(file);
    if (blocks.isEmpty()) {
      throw new IOException("recording contains no blocks: " + file);
    }
    return new RecordedAudioCaptureService(blocks, loop);
  }

  /**
   * @param blocks non-empty list of blocks to replay (all must share the same {@link
   *     AudioFormatDescriptor})
   * @param loop if true, replay continuously; otherwise stop after the last block
   */
  public RecordedAudioCaptureService(List<AudioBlock> blocks, boolean loop) {
    Objects.requireNonNull(blocks, "blocks");
    if (blocks.isEmpty()) {
      throw new IllegalArgumentException("blocks must be non-empty");
    }
    this.descriptor = blocks.get(0).format();
    for (AudioBlock b : blocks) {
      if (!descriptor.equals(b.format())) {
        throw new IllegalArgumentException(
            "all blocks must share the same format; first=" + descriptor + " block=" + b.format());
      }
    }
    this.blocks = List.copyOf(blocks);
    this.loop = loop;
    this.format =
        new AudioFormat(
            descriptor.sampleRate(),
            descriptor.sourceSampleSizeInBits(),
            descriptor.channels(),
            true,
            false);
  }

  /**
   * @return true if replay restarts at the beginning after reaching the end
   */
  public boolean isLooping() {
    return loop;
  }

  /**
   * @return the number of blocks in this recording
   */
  public int blockCount() {
    return blocks.size();
  }

  @Override
  public void start() {
    if (running.getAndSet(true)) {
      return;
    }
    workerExecutor =
        Executors.newSingleThreadExecutor(
            runnable -> {
              Thread worker = new Thread(runnable, "RecordedAudioCaptureWorker");
              worker.setDaemon(true);
              return worker;
            });
    workerExecutor.submit(this::replayLoop);
  }

  @Override
  public void stop() {
    if (!running.getAndSet(false)) {
      return;
    }
    if (workerExecutor != null) {
      workerExecutor.shutdownNow();
      try {
        if (!workerExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
          workerExecutor.shutdownNow();
        }
      } catch (InterruptedException interruptedException) {
        Thread.currentThread().interrupt();
      }
      workerExecutor = null;
    }
  }

  @Override
  public boolean isRunning() {
    return running.get();
  }

  @Override
  public WaveformModel getLatestModel() {
    return latestModel != null ? latestModel : WaveformModel.EMPTY;
  }

  @Override
  public AudioFormat getFormat() {
    return format;
  }

  @Override
  public AudioFormatDescriptor getDescriptor() {
    return descriptor;
  }

  @Override
  public AudioBlock getLatestBlock() {
    return latestBlock;
  }

  @Override
  public AudioRingBuffer<AudioBlock> getRingBuffer() {
    return ringBuffer;
  }

  @Override
  public void setDivisor(int divisor) {
    if (divisor < 1) {
      throw new IllegalArgumentException("Divisor must be >= 1");
    }
    this.divisor = divisor;
  }

  @Override
  public int getDivisor() {
    return divisor;
  }

  @Override
  public void recomputeLayout(int width, int height) {
    panelWidth = width;
    panelHeight = height;
    AudioBlock block = latestBlock;
    if (block != null) {
      latestModel = buildLegacyModel(block);
    }
  }

  private void replayLoop() {
    int index = 0;
    while (running.get() && !Thread.currentThread().isInterrupted()) {
      AudioBlock block = blocks.get(index);
      latestBlock = block;
      ringBuffer.offer(block);
      latestModel = buildLegacyModel(block);
      int sleepMillis =
          Math.max(5, Math.round((1000f * block.frames()) / Math.max(1f, descriptor.sampleRate())));
      try {
        Thread.sleep(sleepMillis);
      } catch (InterruptedException interruptedException) {
        Thread.currentThread().interrupt();
        return;
      }
      index++;
      if (index >= blocks.size()) {
        if (!loop) {
          running.set(false);
          return;
        }
        index = 0;
      }
    }
  }

  private WaveformModel buildLegacyModel(AudioBlock block) {
    WaveformSnapshot snapshot =
        WaveformSnapshot.wrap(
            block.samples(),
            block.format().sampleRate(),
            block.frameIndex(),
            block.timestampNanos());
    int[] xPoints = WaveformRenderer.computeXPoints(snapshot.frames(), panelWidth);
    int[][] yPoints;
    if (panelHeight <= 0) {
      yPoints = new int[snapshot.channels()][0];
    } else {
      yPoints = WaveformRenderer.computeYPointsAllChannels(snapshot, panelHeight);
    }
    int dataSizeBytes =
        snapshot.frames()
            * snapshot.channels()
            * Math.max(1, descriptor.sourceSampleSizeInBits() / 8);
    return new WaveformModel(xPoints, yPoints, 0, dataSizeBytes);
  }
}
