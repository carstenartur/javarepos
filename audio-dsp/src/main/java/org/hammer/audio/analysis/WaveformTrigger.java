package org.hammer.audio.analysis;

import java.util.Objects;
import java.util.Optional;
import org.hammer.audio.core.AudioBlock;

/**
 * Oscilloscope-style trigger that aligns repeated displays of a periodic or transient signal so
 * each frame begins at a stable reference (e.g. a rising zero crossing).
 *
 * <p>The trigger maintains a small rolling history of recent samples for one selected channel.
 * Every {@link #process(AudioBlock, int)} call appends the new samples to that history and then:
 *
 * <ol>
 *   <li>Scans forward (respecting {@link #holdoffFrames() holdoff}) for the first sample crossing
 *       the configured {@link #level() level} with the configured {@link Slope slope}.
 *   <li>If a trigger is found <em>and</em> at least {@link #viewFrames() viewFrames} samples are
 *       available after it, publishes a {@link TriggeredView snapshot} starting at the trigger
 *       sample and returns it.
 *   <li>If {@link Mode#AUTO} is selected and no trigger has fired for more than {@link
 *       #autoTimeoutFrames() autoTimeoutFrames}, publishes the most recent {@code viewFrames}
 *       samples instead (so non-periodic signals still produce a display).
 * </ol>
 *
 * <p>The published snapshot is independent of pixel space: it carries normalized {@code float}
 * samples plus the originating frame index/timestamp. UI code is responsible for any rendering.
 *
 * <p>Instances are <strong>not thread-safe</strong>; intended to be driven from a single consumer
 * thread (typically the UI refresh thread or a DSP worker).
 */
public final class WaveformTrigger {

  /** Trigger slope. */
  public enum Slope {
    /** Trigger when the signal rises through {@link #level()}. */
    RISING,
    /** Trigger when the signal falls through {@link #level()}. */
    FALLING
  }

  /** Trigger mode. */
  public enum Mode {
    /**
     * Only publish a snapshot when a trigger event is actually found within the configured holdoff
     * window. Silence or non-periodic signals produce no output.
     */
    NORMAL,
    /**
     * Like {@link #NORMAL}, but if no trigger fires for {@link #autoTimeoutFrames()} samples
     * publish the latest {@link #viewFrames()} samples anyway so the display does not freeze.
     */
    AUTO
  }

  /** Default view length used when the caller does not supply one. */
  public static final int DEFAULT_VIEW_FRAMES = 1024;

  /** Default holdoff used when the caller does not supply one. */
  public static final int DEFAULT_HOLDOFF_FRAMES = 64;

  private static final int MIN_HISTORY_FRAMES = 4 * 1024;

  private final int viewFrames;
  private final int historyCapacity;
  private final float[] history;

  private int historySize;
  private long firstSampleFrameIndex;
  private long firstSampleTimestampNanos;
  private float sampleRate;

  private float level;
  private Slope slope;
  private Mode mode;
  private int holdoffFrames;
  private int autoTimeoutFrames;

  private long framesSinceLastTrigger;
  private float prevSample;
  private boolean prevSampleValid;

  /** Create a trigger with sensible defaults: level 0, rising slope, AUTO mode. */
  public WaveformTrigger() {
    this(DEFAULT_VIEW_FRAMES);
  }

  /**
   * Create a trigger with the supplied view length and default parameters.
   *
   * @param viewFrames number of samples published per triggered snapshot; must be {@code > 0}
   */
  public WaveformTrigger(int viewFrames) {
    if (viewFrames <= 0) {
      throw new IllegalArgumentException("viewFrames must be > 0, was " + viewFrames);
    }
    this.viewFrames = viewFrames;
    this.historyCapacity = Math.max(MIN_HISTORY_FRAMES, viewFrames * 4);
    this.history = new float[historyCapacity];
    this.historySize = 0;
    this.firstSampleFrameIndex = 0L;
    this.firstSampleTimestampNanos = 0L;
    this.level = 0f;
    this.slope = Slope.RISING;
    this.mode = Mode.AUTO;
    this.holdoffFrames = DEFAULT_HOLDOFF_FRAMES;
    this.autoTimeoutFrames = Math.max(viewFrames, MIN_HISTORY_FRAMES / 2);
    this.framesSinceLastTrigger = Long.MAX_VALUE / 2;
    this.prevSample = 0f;
    this.prevSampleValid = false;
  }

  /**
   * @return number of samples published per snapshot
   */
  public int viewFrames() {
    return viewFrames;
  }

  /**
   * @return current trigger level in {@code [-1, 1]}
   */
  public float level() {
    return level;
  }

  /**
   * Set the trigger level. Must be a finite value in {@code [-1, 1]}.
   *
   * @param level new trigger level
   */
  public void setLevel(float level) {
    if (Float.isNaN(level) || Float.isInfinite(level) || level < -1f || level > 1f) {
      throw new IllegalArgumentException("level must be a finite value in [-1, 1], was " + level);
    }
    this.level = level;
  }

  /**
   * @return current trigger slope (rising or falling)
   */
  public Slope slope() {
    return slope;
  }

  /**
   * Set the trigger slope.
   *
   * @param slope rising or falling; must not be {@code null}
   */
  public void setSlope(Slope slope) {
    this.slope = Objects.requireNonNull(slope, "slope");
  }

  /**
   * @return current trigger mode
   */
  public Mode mode() {
    return mode;
  }

  /**
   * Set the trigger mode.
   *
   * @param mode {@link Mode#NORMAL} or {@link Mode#AUTO}; must not be {@code null}
   */
  public void setMode(Mode mode) {
    this.mode = Objects.requireNonNull(mode, "mode");
  }

  /**
   * @return current holdoff in samples (minimum spacing between triggers)
   */
  public int holdoffFrames() {
    return holdoffFrames;
  }

  /**
   * Set the holdoff: minimum number of samples between two triggers. Useful to skip multiple
   * crossings within a single waveform period.
   *
   * @param holdoffFrames non-negative number of samples
   */
  public void setHoldoffFrames(int holdoffFrames) {
    if (holdoffFrames < 0) {
      throw new IllegalArgumentException("holdoffFrames must be >= 0, was " + holdoffFrames);
    }
    this.holdoffFrames = holdoffFrames;
  }

  /**
   * @return number of samples after which AUTO mode publishes an untriggered view
   */
  public int autoTimeoutFrames() {
    return autoTimeoutFrames;
  }

  /**
   * Set the AUTO-mode timeout in samples.
   *
   * @param autoTimeoutFrames number of samples without a trigger before AUTO publishes anyway; must
   *     be {@code >= viewFrames}
   */
  public void setAutoTimeoutFrames(int autoTimeoutFrames) {
    if (autoTimeoutFrames < viewFrames) {
      throw new IllegalArgumentException(
          "autoTimeoutFrames must be >= viewFrames (" + viewFrames + "), was " + autoTimeoutFrames);
    }
    this.autoTimeoutFrames = autoTimeoutFrames;
  }

  /** Clear the rolling history and prior-sample state. */
  public void reset() {
    historySize = 0;
    firstSampleFrameIndex = 0L;
    firstSampleTimestampNanos = 0L;
    sampleRate = 0f;
    framesSinceLastTrigger = Long.MAX_VALUE / 2;
    prevSample = 0f;
    prevSampleValid = false;
  }

  /**
   * Feed an {@link AudioBlock} into the trigger.
   *
   * @param block the audio block; must not be {@code null}
   * @param channel zero-based channel index to trigger on
   * @return a triggered snapshot if a trigger fired (or AUTO mode timed out), otherwise {@link
   *     Optional#empty()}
   */
  public Optional<TriggeredView> process(AudioBlock block, int channel) {
    Objects.requireNonNull(block, "block");
    if (channel < 0 || channel >= block.channels()) {
      throw new IllegalArgumentException(
          "channel " + channel + " out of range [0, " + block.channels() + ")");
    }
    float[] in = block.channelView(channel);
    int frames = block.frames();
    if (frames <= 0) {
      return Optional.empty();
    }
    sampleRate = block.format().sampleRate();
    int firstNewIndex = appendToHistory(in, frames, block.frameIndex(), block.timestampNanos());
    return findAndPublish(firstNewIndex, block);
  }

  /** Append samples into the rolling history; returns the history index of the first new sample. */
  private int appendToHistory(float[] in, int frames, long blockFrameIndex, long blockTimestamp) {
    if (frames >= historyCapacity) {
      // Block bigger than capacity: keep only the tail.
      int tail = historyCapacity;
      System.arraycopy(in, frames - tail, history, 0, tail);
      historySize = tail;
      long droppedFrames = (long) frames - tail;
      firstSampleFrameIndex = blockFrameIndex + droppedFrames;
      firstSampleTimestampNanos =
          blockTimestamp + (long) (droppedFrames * 1_000_000_000.0d / Math.max(1.0d, sampleRate));
      // The previous sample crossing the boundary is the sample right before the kept tail.
      prevSample = in[frames - tail - 1 < 0 ? 0 : frames - tail - 1];
      prevSampleValid = tail > 0;
      return 0;
    }
    int free = historyCapacity - historySize;
    if (frames > free) {
      // Shift old data out, advance frame-index/timestamp accordingly.
      int shift = frames - free;
      System.arraycopy(history, shift, history, 0, historySize - shift);
      historySize -= shift;
      firstSampleFrameIndex += shift;
      firstSampleTimestampNanos += (long) (shift * 1_000_000_000.0d / Math.max(1.0d, sampleRate));
    }
    int firstNewIndex = historySize;
    if (historySize == 0) {
      firstSampleFrameIndex = blockFrameIndex;
      firstSampleTimestampNanos = blockTimestamp;
    }
    System.arraycopy(in, 0, history, historySize, frames);
    historySize += frames;
    return firstNewIndex;
  }

  private Optional<TriggeredView> findAndPublish(int firstNewIndex, AudioBlock source) {
    int triggerIndex = findTrigger(firstNewIndex);
    int newFrames = historySize - firstNewIndex;
    if (triggerIndex >= 0 && historySize - triggerIndex >= viewFrames) {
      framesSinceLastTrigger = 0L;
      return Optional.of(buildSnapshot(triggerIndex, true, source));
    }
    framesSinceLastTrigger = saturatedAdd(framesSinceLastTrigger, newFrames);
    if (mode == Mode.AUTO
        && framesSinceLastTrigger >= autoTimeoutFrames
        && historySize >= viewFrames) {
      framesSinceLastTrigger = 0L;
      int start = historySize - viewFrames;
      return Optional.of(buildSnapshot(start, false, source));
    }
    return Optional.empty();
  }

  private int findTrigger(int firstNewIndex) {
    int searchStart = firstNewIndex;
    int holdoffStart = computeHoldoffSearchStart();
    if (holdoffStart > searchStart) {
      searchStart = holdoffStart;
    }
    if (searchStart < 1) {
      // Need at least one earlier sample for slope detection. Use prevSample seeded from last call.
      if (!prevSampleValid) {
        searchStart = 1;
      }
    }
    for (int i = Math.max(searchStart, 0); i < historySize; i++) {
      float curr = history[i];
      float prev;
      if (i == 0) {
        if (!prevSampleValid) {
          continue;
        }
        prev = prevSample;
      } else {
        prev = history[i - 1];
      }
      boolean fired =
          slope == Slope.RISING ? (prev < level && curr >= level) : (prev > level && curr <= level);
      if (fired) {
        // Remember the last sample for next call before this position is overwritten.
        prevSample = history[historySize - 1];
        prevSampleValid = true;
        return i;
      }
    }
    // Update prev sample for next call.
    if (historySize > 0) {
      prevSample = history[historySize - 1];
      prevSampleValid = true;
    }
    return -1;
  }

  private int computeHoldoffSearchStart() {
    if (framesSinceLastTrigger >= holdoffFrames) {
      return 0;
    }
    long need = holdoffFrames - framesSinceLastTrigger;
    long start = historySize - need;
    return start < 0 ? 0 : (int) start;
  }

  private TriggeredView buildSnapshot(int start, boolean triggered, AudioBlock source) {
    float[] view = new float[viewFrames];
    System.arraycopy(history, start, view, 0, viewFrames);
    long sourceOffsetFrames = (long) start;
    long viewFrameIndex = firstSampleFrameIndex + sourceOffsetFrames;
    long viewTimestamp =
        firstSampleTimestampNanos
            + (long) (sourceOffsetFrames * 1_000_000_000.0d / Math.max(1.0d, sampleRate));
    return new TriggeredView(
        view,
        sampleRate,
        triggered,
        level,
        slope,
        viewFrameIndex,
        viewTimestamp,
        source.frameIndex(),
        source.timestampNanos());
  }

  private static long saturatedAdd(long a, long b) {
    long r = a + b;
    if (((a ^ r) & (b ^ r)) < 0L) {
      return Long.MAX_VALUE;
    }
    return r;
  }

  /**
   * Immutable triggered waveform view returned by {@link WaveformTrigger#process(AudioBlock, int)}.
   *
   * @param samples view samples starting at the trigger position (length = {@link #viewFrames()})
   * @param sampleRate sample rate in Hz
   * @param triggered {@code true} if a real trigger event fired; {@code false} if AUTO timeout
   * @param level trigger level used to produce this view
   * @param slope trigger slope used to produce this view
   * @param viewFrameIndex frame index of the first sample in {@link #samples()}
   * @param viewTimestampNanos timestamp of the first sample in {@link #samples()}
   * @param sourceFrameIndex frame index of the {@link AudioBlock} that produced this view
   * @param sourceTimestampNanos timestamp of the {@link AudioBlock} that produced this view
   */
  public record TriggeredView(
      float[] samples,
      float sampleRate,
      boolean triggered,
      float level,
      Slope slope,
      long viewFrameIndex,
      long viewTimestampNanos,
      long sourceFrameIndex,
      long sourceTimestampNanos) {

    /** Compact constructor that defensively clones {@code samples}. */
    public TriggeredView {
      Objects.requireNonNull(samples, "samples");
      Objects.requireNonNull(slope, "slope");
      samples = samples.clone();
    }

    /**
     * @return a defensive copy of the view samples
     */
    @Override
    public float[] samples() {
      return samples.clone();
    }

    /**
     * @return read-only access to the internal samples array (do not mutate)
     */
    public float[] samplesView() {
      return samples;
    }
  }
}
