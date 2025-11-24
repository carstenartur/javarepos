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
  private byte[] datas;
  private int[] xPoints;
  private int[][] yPoints;
  private int tickEveryNSample;
  private int datasize;
  private int numberOfPoints;

  // Precomputed audio format fields (immutable after line is opened)
  private int bytesPerSample;
  private int frameSize;

  // Reusable working buffer for capture loop (avoids per-iteration allocation)
  private int[][] workingYPoints;

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

  @Override
  public void recomputeLayout(int width, int height) {
    this.panelWidth = width;
    this.panelHeight = height;
    recomputeXValues();
  }

  /** Initialize and open the audio line. */
  private void initializeAudioLine() {
    format = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

    if (!AudioSystem.isLineSupported(info)) {
      throw new IllegalStateException("TargetDataLine not supported for format: " + format);
    }

    try {
      line = (TargetDataLine) AudioSystem.getLine(info);
      line.open(format);
      // Precompute audio format constants to avoid recalculation in hot path
      this.bytesPerSample = Math.max(1, sampleSizeInBits / 8);
      this.frameSize = bytesPerSample * channels;
      LOGGER.info("Opened audio line with format: " + format);
    } catch (LineUnavailableException ex) {
      throw new IllegalStateException("Unable to open TargetDataLine", ex);
    }
  }

  /** Compute buffer sizes and allocate arrays. */
  private void computeDataSize() {
    if (line == null) {
      throw new IllegalStateException("Line must be opened before computing buffer sizes.");
    }

    modelLock.lock();
    try {
      datasize = Math.max(MIN_BUFFER_SIZE, line.getBufferSize() / Math.max(1, divisor));
      numberOfPoints = datasize / Math.max(1, frameSize);
      if (numberOfPoints <= 0) numberOfPoints = 1;

      datas = new byte[datasize];
      xPoints = new int[numberOfPoints];
      yPoints = new int[channels][numberOfPoints];
      // Allocate reusable working buffer to eliminate per-iteration allocation
      workingYPoints = new int[channels][numberOfPoints];
      recomputeXValues();

      LOGGER.fine(String.format("Computed data size: %d, points: %d", datasize, numberOfPoints));
    } finally {
      modelLock.unlock();
    }
  }

  /** Recompute X coordinates for drawing based on panel width. */
  private void recomputeXValues() {
    modelLock.lock();
    try {
      if (xPoints == null || numberOfPoints <= 1) {
        if (xPoints != null && xPoints.length > 0) xPoints[0] = 0;
        return;
      }

      // Use integer arithmetic to avoid float conversions and rounding overhead
      final int panelW = panelWidth - 1;
      final int pointsM1 = numberOfPoints - 1;
      for (int i = 0; i < numberOfPoints; i++) {
        xPoints[i] = (int) ((long) panelW * i / pointsM1);
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

    // Use precomputed format constants
    final int bps = this.bytesPerSample;
    final int fsize = this.frameSize;

    while (running.get() && !Thread.currentThread().isInterrupted()) {
      try {
        // Read audio data outside lock (IO should not be inside critical section)
        final int numBytesRead = line.read(datas, 0, datas.length);
        if (numBytesRead <= 0) {
          continue;
        }

        final int framesRead = numBytesRead / fsize;
        final int points = Math.min(numberOfPoints, framesRead);

        // Decode samples outside lock into reusable working buffer
        for (int frame = 0; frame < points; frame++) {
          final int frameOffset = frame * fsize;
          for (int ch = 0; ch < channels; ch++) {
            final int sampleOffset = frameOffset + ch * bps;
            final int sample = readSample(sampleOffset, bps);
            workingYPoints[ch][frame] = scaleToPixel(sample);
          }
        }

        // Clear tail region if needed (only the portion not written)
        if (points < numberOfPoints) {
          for (int ch = 0; ch < channels; ch++) {
            Arrays.fill(workingYPoints[ch], points, numberOfPoints, 0);
          }
        }

        // Update model atomically - minimal critical section
        modelLock.lock();
        try {
          if (yPoints == null
              || yPoints.length != channels
              || yPoints[0].length != numberOfPoints) {
            yPoints = new int[channels][numberOfPoints];
          }

          // Copy working buffer to yPoints
          for (int ch = 0; ch < channels; ch++) {
            System.arraycopy(workingYPoints[ch], 0, yPoints[ch], 0, numberOfPoints);
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

  /** Read a sample from the data buffer. */
  private int readSample(int offset, int bytesPerSample) {
    int sample = 0;

    // Fast paths for common formats (hoisted branching)
    if (bytesPerSample == 1) {
      final int b = datas[offset] & 0xFF;
      if (signed) {
        sample = (byte) b; // sign extend
      } else {
        sample = b;
      }
    } else if (bytesPerSample == 2) {
      final int hi = datas[offset + (bigEndian ? 0 : 1)] & 0xFF;
      final int lo = datas[offset + (bigEndian ? 1 : 0)] & 0xFF;
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
        sample = (sample << 8) | (datas[offset + b] & 0xFF);
      }
    }

    return sample;
  }

  /** Scale a sample value to pixel coordinates. */
  private int scaleToPixel(int sample) {
    int y = 0;

    if (panelHeight > 0) {
      if (signed) {
        int maxAbs = (1 << (sampleSizeInBits - 1)) - 1;
        float norm = (float) sample / (float) maxAbs; // -1..1
        y = Math.round((panelHeight / 2f) - norm * (panelHeight / 2f));
      } else {
        int max = (1 << sampleSizeInBits) - 1;
        float norm = (float) sample / (float) max; // 0..1
        y = Math.round(panelHeight - norm * panelHeight);
      }
    }

    return y;
  }
}
