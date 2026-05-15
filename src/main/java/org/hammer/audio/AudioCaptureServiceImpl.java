package org.hammer.audio;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.TargetDataLine;
import org.hammer.audio.buffer.AudioRingBuffer;
import org.hammer.audio.capture.SampleDecoder;
import org.hammer.audio.core.AudioBlock;
import org.hammer.audio.core.AudioFormatDescriptor;
import org.hammer.audio.snapshot.WaveformSnapshot;
import org.hammer.audio.ui.WaveformRenderer;

/**
 * Audio capture service implementation: the bridge between the JavaSound input device and the
 * platform's audio-domain pipeline.
 *
 * <p><strong>Architecture (post-refactor)</strong>:
 *
 * <pre>{@code
 * TargetDataLine
 *   -> raw bytes
 *   -> SampleDecoder (-> normalized float[][])
 *   -> AudioBlock (immutable, with frame index + timestamp)
 *   -> AudioRingBuffer<AudioBlock>  (lock-free SPSC; downstream DSP/analysis polls asynchronously)
 *   -> latestBlock (volatile, for "give me the latest" UI consumers)
 *   -> WaveformModel (legacy compatibility view, built via WaveformRenderer)
 * }</pre>
 *
 * <p>The capture loop knows nothing about pixels, panel coordinates or Swing — pixel scaling has
 * moved into {@link WaveformRenderer}. The legacy {@link WaveformModel} is still produced for
 * existing UI consumers and tests; it is now derived from the same {@link AudioBlock} the rest of
 * the platform sees.
 *
 * <p>Thread-safety: all public methods are thread-safe. The capture worker thread is the sole
 * producer for the ring buffer; downstream DSP/analysis threads are the consumers.
 *
 * @author refactoring
 */
public class AudioCaptureServiceImpl implements AudioCaptureService {

  private static final Logger LOGGER = Logger.getLogger(AudioCaptureServiceImpl.class.getName());

  /** Tick distance in seconds (1 ms). */
  private static final float TICK_SECONDS = 1f / 1000f;

  /** Minimum buffer size in bytes to prevent overly small allocations. */
  private static final int MIN_BUFFER_SIZE = 256;

  /** Default ring-buffer capacity (in {@link AudioBlock}s). */
  private static final int RING_BUFFER_CAPACITY = 64;

  private final AtomicBoolean running = new AtomicBoolean(false);

  private volatile WaveformModel latestModel;
  private volatile AudioBlock latestBlock;

  // Audio configuration
  private final float sampleRate;
  private final int sampleSizeInBits;
  private final int channels;
  private final boolean signed;
  private final boolean bigEndian;
  private final AudioFormatDescriptor descriptor;
  private final SampleDecoder decoder;
  private final AudioRingBuffer<AudioBlock> ringBuffer;

  // Capture state
  private volatile int divisor;
  private volatile int panelWidth;
  private volatile int panelHeight;

  private TargetDataLine line;
  private AudioFormat format;
  private ExecutorService workerExecutor;

  // Capture buffers (mostly worker-thread owned; volatile for visibility on reconfiguration)
  private volatile byte[] datas;
  private volatile int datasize;
  private volatile int numberOfPoints;
  private final int tickEveryNSample;

  // Audio line provider (for testability)
  private final AudioLineProvider lineProvider;

  /**
   * Create a new AudioCaptureServiceImpl with specified audio parameters.
   *
   * @param sampleRate sample rate in Hz (e.g., 16000.0f)
   * @param sampleSizeInBits sample size in bits (e.g., 8 or 16)
   * @param channels number of audio channels (e.g., 1 for mono, 2 for stereo)
   * @param signed true if samples are signed
   * @param bigEndian true if samples are big-endian
   * @param divisor initial divisor for buffer size calculation
   */
  public AudioCaptureServiceImpl(
      float sampleRate,
      int sampleSizeInBits,
      int channels,
      boolean signed,
      boolean bigEndian,
      int divisor) {
    this(
        sampleRate,
        sampleSizeInBits,
        channels,
        signed,
        bigEndian,
        divisor,
        new DefaultAudioLineProvider());
  }

  /** Package-private constructor for testing with custom AudioLineProvider. */
  AudioCaptureServiceImpl(
      float sampleRate,
      int sampleSizeInBits,
      int channels,
      boolean signed,
      boolean bigEndian,
      int divisor,
      AudioLineProvider lineProvider) {
    this.sampleRate = sampleRate;
    this.sampleSizeInBits = sampleSizeInBits;
    this.channels = Math.max(1, channels);
    this.signed = signed;
    this.bigEndian = bigEndian;
    this.divisor = Math.max(1, divisor);
    this.tickEveryNSample = (int) (TICK_SECONDS * sampleRate);
    this.panelWidth = 640;
    this.panelHeight = 200;
    this.lineProvider = lineProvider;
    this.descriptor = new AudioFormatDescriptor(sampleRate, this.channels, sampleSizeInBits);
    this.decoder = new SampleDecoder(descriptor, signed, bigEndian);
    this.ringBuffer = new AudioRingBuffer<>(RING_BUFFER_CAPACITY);
  }

  @Override
  public void start() {
    if (running.get()) {
      LOGGER.warning("AudioCaptureService is already running");
      return;
    }
    try {
      initializeAudioLine();
      computeDataSize();
      running.set(true);
      workerExecutor =
          Executors.newSingleThreadExecutor(
              r -> {
                Thread t = new Thread(r, "AudioCaptureWorker");
                t.setDaemon(true);
                return t;
              });
      workerExecutor.submit(this::captureLoop);
      LOGGER.info("AudioCaptureService started successfully");
    } catch (Exception e) {
      running.set(false);
      LOGGER.log(Level.SEVERE, "Failed to start AudioCaptureService", e);
      throw new IllegalStateException("Failed to start audio capture", e);
    }
  }

  @Override
  public void stop() {
    if (!running.get()) {
      return;
    }
    running.set(false);
    if (workerExecutor != null) {
      workerExecutor.shutdownNow();
      try {
        if (!workerExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
          workerExecutor.shutdownNow();
        }
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
      }
      workerExecutor = null;
    }
    if (line != null) {
      try {
        line.stop();
        line.flush();
        line.close();
      } catch (Exception e) {
        LOGGER.log(Level.WARNING, "Error closing TargetDataLine", e);
      }
      line = null;
    }
    LOGGER.info("AudioCaptureService stopped");
  }

  @Override
  public boolean isRunning() {
    return running.get();
  }

  @Override
  public WaveformModel getLatestModel() {
    WaveformModel cached = latestModel;
    if (cached != null) {
      return cached;
    }
    return WaveformModel.EMPTY;
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
    if (line != null) {
      computeDataSize();
    }
  }

  @Override
  public int getDivisor() {
    return divisor;
  }

  @Override
  public void recomputeLayout(int width, int height) {
    this.panelWidth = width;
    this.panelHeight = height;
    // Re-render the latest block under the new layout so resized panels see fresh pixel
    // coordinates immediately, even before the next capture cycle.
    AudioBlock cached = latestBlock;
    if (cached != null) {
      latestModel = buildLegacyModel(cached);
    }
  }

  /** Initialize and open the audio line. */
  private void initializeAudioLine() {
    format = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    line = lineProvider.acquireLine(format);
    LOGGER.info("Opened audio line with format: " + format);
  }

  /** Compute byte buffer size and number of frames per block. */
  private void computeDataSize() {
    if (line == null) {
      throw new IllegalStateException("Line must be opened before computing buffer sizes.");
    }
    datasize = Math.max(MIN_BUFFER_SIZE, line.getBufferSize() / Math.max(1, divisor));
    int frameSize = decoder.frameSize();
    int points = datasize / Math.max(1, frameSize);
    if (points <= 0) {
      points = 1;
    }
    numberOfPoints = points;
    datas = new byte[datasize];
    LOGGER.fine(String.format("Computed data size: %d, points: %d", datasize, points));
  }

  /** Main capture loop running in worker thread. */
  private void captureLoop() {
    if (line == null) {
      LOGGER.warning("TargetDataLine is null, aborting capture loop.");
      return;
    }
    line.start();

    long frameIndex = 0L;
    int allocatedFrames = numberOfPoints;
    float[][] decodeBuffer = new float[channels][allocatedFrames];

    while (running.get() && !Thread.currentThread().isInterrupted()) {
      try {
        byte[] localData = datas;
        final int numBytesRead = line.read(localData, 0, localData.length);
        if (numBytesRead <= 0) {
          continue;
        }
        int currentPoints = numberOfPoints;
        if (allocatedFrames < currentPoints) {
          allocatedFrames = currentPoints;
          decodeBuffer = new float[channels][allocatedFrames];
        }

        final int decodedFrames = Math.min(currentPoints, decoder.framesIn(numBytesRead));
        if (decodedFrames <= 0) {
          continue;
        }
        decoder.decode(localData, decodedFrames * decoder.frameSize(), decodeBuffer);
        // Zero-pad the tail so block.frames() always equals the configured buffer size; this
        // preserves the legacy semantics where the model's numberOfPoints reflects the configured
        // buffer (driven by the divisor) rather than the partial bytes read in this iteration.
        for (int c = 0; c < channels; c++) {
          for (int i = decodedFrames; i < currentPoints; i++) {
            decodeBuffer[c][i] = 0f;
          }
        }

        // Build a fresh, exactly-sized float[channels][currentPoints] for the immutable block.
        float[][] blockSamples = new float[channels][currentPoints];
        for (int c = 0; c < channels; c++) {
          System.arraycopy(decodeBuffer[c], 0, blockSamples[c], 0, currentPoints);
        }
        AudioBlock block = AudioBlock.wrap(descriptor, blockSamples, frameIndex, System.nanoTime());
        frameIndex += decodedFrames;

        // Publish to ring buffer (plain offer + drop-on-full). The capture thread is the sole
        // producer; downstream DSP/analysis consumers may run on other threads, so we cannot
        // safely use offerOverwrite here (see AudioRingBuffer.offerOverwrite Javadoc). The
        // "latest wins" path is served by the volatile latestBlock pointer below, so dropping
        // the new block on overflow is preferable to corrupting the consumer's view.
        ringBuffer.offer(block);

        // Cache "latest" view for cheap polling consumers (UI, REST).
        latestBlock = block;

        // Build the legacy WaveformModel for backwards-compatible Swing rendering.
        latestModel = buildLegacyModel(block);

      } catch (Exception ex) {
        if (running.get()) {
          LOGGER.log(Level.SEVERE, "Error during audio capture loop", ex);
        }
      }
    }
    LOGGER.fine("Capture loop ended");
  }

  /** Build a legacy {@link WaveformModel} from a new {@link AudioBlock}. */
  private WaveformModel buildLegacyModel(AudioBlock block) {
    WaveformSnapshot snap =
        WaveformSnapshot.wrap(
            block.samples(),
            block.format().sampleRate(),
            block.frameIndex(),
            block.timestampNanos());
    int[] xPoints = WaveformRenderer.computeXPoints(snap.frames(), panelWidth);
    // Swing panels can transiently report height==0 before they are laid out; in that case we
    // emit empty per-channel arrays rather than asking WaveformRenderer to throw, since this is
    // a legitimate "nothing to draw yet" state, not a programming error.
    int h = panelHeight;
    int[][] yPoints;
    if (h <= 0) {
      yPoints = new int[snap.channels()][0];
    } else {
      yPoints = WaveformRenderer.computeYPointsAllChannels(snap, h);
    }
    return new WaveformModel(xPoints, yPoints, tickEveryNSample, datasize);
  }
}
