package org.hammer.audio;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.TargetDataLine;

/**
 * Implementation of AudioCaptureService.
 *
 * <p>This class handles audio input capture, sample processing, and model generation. It runs a
 * background thread to continuously read audio data and update the waveform model.
 *
 * <p>Thread-safety: All public methods are thread-safe. Internal state is protected by locks and
 * atomic variables.
 *
 * @author refactoring
 */
public class AudioCaptureServiceImpl implements AudioCaptureService {

  private static final Logger LOGGER = Logger.getLogger(AudioCaptureServiceImpl.class.getName());

  /** Tick distance in seconds (1 ms). */
  private static final float TICK_SECONDS = 1f / 1000f;

  /** Minimum buffer size in bytes to prevent overly small allocations. */
  private static final int MIN_BUFFER_SIZE = 256;

  private final AtomicBoolean running = new AtomicBoolean(false);
  private final Lock modelLock = new ReentrantLock();

  // Cached latest model (volatile for thread-safety, immutable so no defensive copy needed)
  private volatile WaveformModel latestModel;

  // Audio configuration
  private final float sampleRate;
  private final int sampleSizeInBits;
  private final int channels;
  private final boolean signed;
  private final boolean bigEndian;

  // Capture state
  private volatile int divisor;
  private volatile int panelWidth;
  private volatile int panelHeight;

  private TargetDataLine line;
  private AudioFormat format;
  private ExecutorService workerExecutor;

  // Model data (protected by modelLock)
  private byte[] dataBuffer;
  private int[] xPoints;
  private int[][] yPoints;
  private int[][] workingYPoints; // Reusable buffer to avoid per-iteration allocations
  private int tickEveryNSample;
  private int datasize;
  private int pointCount;

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

  /**
   * Package-private constructor for testing with custom AudioLineProvider.
   *
   * @param sampleRate sample rate in Hz (e.g., 16000.0f)
   * @param sampleSizeInBits sample size in bits (e.g., 8 or 16)
   * @param channels number of audio channels (e.g., 1 for mono, 2 for stereo)
   * @param signed true if samples are signed
   * @param bigEndian true if samples are big-endian
   * @param divisor initial divisor for buffer size calculation
   * @param lineProvider provider for acquiring audio lines
   */
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
    // Ensure at least 1 channel (mono) - invalid values are normalized
    this.channels = Math.max(1, channels);
    this.signed = signed;
    this.bigEndian = bigEndian;
    this.divisor = Math.max(1, divisor);
    this.tickEveryNSample = (int) (TICK_SECONDS * sampleRate);
    this.panelWidth = 640;
    this.panelHeight = 200;
    this.lineProvider = lineProvider;
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
    // Return empty model if no data captured yet
    return WaveformModel.EMPTY;
  }

  @Override
  public AudioFormat getFormat() {
    return format;
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

  /**
   * Recompute the X-axis layout based on new panel dimensions.
   *
   * <p>This method recalculates x-coordinate positions when the display panel is resized,
   * distributing points evenly across the new width. It does not affect Y-axis calculations or
   * trigger new audio capture; only the horizontal layout is updated.
   *
   * @param width new panel width in pixels
   * @param height new panel height in pixels
   */
  @Override
  public void recomputeLayout(int width, int height) {
    this.panelWidth = width;
    this.panelHeight = height;
    recomputeXValues();
  }

  /** Initialize and open the audio line. */
  private void initializeAudioLine() {
    format = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    line = lineProvider.acquireLine(format);
    LOGGER.info("Opened audio line with format: " + format);
  }

  /** Compute buffer sizes and allocate arrays. */
  private void computeDataSize() {
    if (line == null) {
      throw new IllegalStateException("Line must be opened before computing buffer sizes.");
    }

    modelLock.lock();
    try {
      final int frameSize = Math.max(1, (sampleSizeInBits / 8) * channels);

      // Calculate desired datasize, then align to frame boundaries to prevent partial frames
      int desiredSize = Math.max(MIN_BUFFER_SIZE, line.getBufferSize() / Math.max(1, divisor));
      datasize = (desiredSize / frameSize) * frameSize; // Truncate to nearest frame multiple
      if (datasize < frameSize) {
        datasize = frameSize; // Ensure at least one frame
      }

      pointCount = datasize / frameSize;
      if (pointCount <= 0) {
        pointCount = 1;
      }

      dataBuffer = new byte[datasize];
      xPoints = new int[pointCount];
      yPoints = new int[channels][pointCount];
      workingYPoints = new int[channels][pointCount]; // Allocate reusable working buffer
      recomputeXValues();

      LOGGER.fine(String.format("Computed data size: %d, points: %d", datasize, pointCount));
    } finally {
      modelLock.unlock();
    }
  }

  /** Recompute X coordinates for drawing based on panel width. */
  private void recomputeXValues() {
    modelLock.lock();
    try {
      if (xPoints == null || pointCount <= 1) {
        if (xPoints != null && xPoints.length > 0) {
          xPoints[0] = 0;
        }
        return;
      }

      for (int i = 0; i < pointCount; i++) {
        xPoints[i] = Math.round((panelWidth - 1) * (i / (float) (pointCount - 1)));
      }
    } finally {
      modelLock.unlock();
    }
  }

  /** Main capture loop running in worker thread. */
  private void captureLoop() {
    if (line == null) {
      LOGGER.warning("TargetDataLine is null, aborting capture loop.");
      return;
    }

    line.start();

    final int bytesPerSample = Math.max(1, sampleSizeInBits / 8);
    final int frameSize = bytesPerSample * channels;

    while (running.get() && !Thread.currentThread().isInterrupted()) {
      try {
        int numBytesRead = line.read(dataBuffer, 0, dataBuffer.length);
        if (numBytesRead <= 0 || numBytesRead < frameSize) {
          continue; // Guard against partial frames
        }

        final int framesRead = numBytesRead / frameSize;
        final int points = Math.min(pointCount, framesRead);

        // Use reusable working buffer instead of allocating per iteration
        for (int frame = 0; frame < points; frame++) {
          final int frameOffset = frame * frameSize;
          for (int ch = 0; ch < channels; ch++) {
            final int sampleOffset = frameOffset + ch * bytesPerSample;
            final int sample = readSample(sampleOffset, bytesPerSample);
            workingYPoints[ch][frame] = scaleToPixel(sample);
          }
        }

        // Update model atomically
        modelLock.lock();
        try {
          if (yPoints == null || yPoints.length != channels || yPoints[0].length != pointCount) {
            yPoints = new int[channels][pointCount];
          }

          // Copy from working buffer to model, filling remainder with 0 if partial read
          for (int ch = 0; ch < channels; ch++) {
            System.arraycopy(workingYPoints[ch], 0, yPoints[ch], 0, points);
            if (points < pointCount) {
              // Fill remaining points with 0 for partial reads
              Arrays.fill(yPoints[ch], points, pointCount, 0);
            }
          }

          // Cache the latest model (WaveformModel is immutable, no defensive copy needed)
          latestModel = new WaveformModel(xPoints, yPoints, tickEveryNSample, datasize);
        } finally {
          modelLock.unlock();
        }

      } catch (Exception ex) {
        if (running.get()) {
          LOGGER.log(Level.SEVERE, "Error during audio capture loop", ex);
        }
      }
    }

    LOGGER.fine("Capture loop ended");
  }

  /** Read a sample from the data buffer with bounds checking. */
  private int readSample(final int offset, final int bytesPerSample) {
    // Defensive bounds check to prevent IndexOutOfBounds with partial frames
    if (offset + bytesPerSample > dataBuffer.length) {
      return 0; // Return silence for out-of-bounds reads
    }

    int sample = 0;

    if (bytesPerSample == 1) {
      final int b = dataBuffer[offset] & 0xFF;
      if (signed) {
        sample = (byte) b; // sign extend
      } else {
        sample = b;
      }
    } else if (bytesPerSample == 2) {
      final int hi = dataBuffer[offset + (bigEndian ? 0 : 1)] & 0xFF;
      final int lo = dataBuffer[offset + (bigEndian ? 1 : 0)] & 0xFF;
      final int raw = (hi << 8) | lo;
      if (signed) {
        sample = (short) raw; // sign extend to int
      } else {
        sample = raw & 0xFFFF;
      }
    } else {
      // Support for other sample sizes (assumes big-endian byte order)
      // This is a fallback for non-standard sample sizes
      for (int b = 0; b < bytesPerSample; b++) {
        sample = (sample << 8) | (dataBuffer[offset + b] & 0xFF);
      }
    }

    return sample;
  }

  /** Scale a sample value to pixel coordinates. */
  private int scaleToPixel(final int sample) {
    int y = 0;

    if (panelHeight > 0) {
      if (signed) {
        final int maxAbs = (1 << (sampleSizeInBits - 1)) - 1;
        final float norm = (float) sample / (float) maxAbs; // -1..1
        y = Math.round((panelHeight / 2f) - norm * (panelHeight / 2f));
      } else {
        final int max = (1 << sampleSizeInBits) - 1;
        final float norm = (float) sample / (float) max; // 0..1
        y = Math.round(panelHeight - norm * panelHeight);
      }
    }

    return y;
  }
}
