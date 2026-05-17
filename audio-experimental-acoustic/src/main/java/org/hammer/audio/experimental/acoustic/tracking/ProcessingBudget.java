package org.hammer.audio.experimental.acoustic.tracking;

import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * Lightweight per-frame deadline checker for the real-time tracking pipeline.
 *
 * <p>A {@code ProcessingBudget} is created with a {@link FrameSchedule}; callers invoke {@link
 * #start()} at the beginning of a block and {@link #checkpoint(String)} after each pipeline stage.
 * {@link #elapsedNanos()} reports the time spent so far and {@link #exceeded()} is {@code true}
 * once the configured per-block deadline is past.
 *
 * <p>The budget intentionally does not abort processing; it is observational and lets the host
 * decide whether to skip stages, downsample, or drop output. A {@link LongSupplier} time source is
 * injectable so deterministic tests can advance virtual time.
 */
public final class ProcessingBudget {

  private final FrameSchedule schedule;
  private final LongSupplier nanoTimeSource;
  private long startNanos;
  private long lastCheckpointNanos;
  private String lastStage;

  /** Create a budget tied to {@code schedule} that uses {@link System#nanoTime()}. */
  public ProcessingBudget(FrameSchedule schedule) {
    this(schedule, System::nanoTime);
  }

  /** Create a budget with an injectable time source for deterministic testing. */
  public ProcessingBudget(FrameSchedule schedule, LongSupplier nanoTimeSource) {
    this.schedule = Objects.requireNonNull(schedule, "schedule");
    this.nanoTimeSource = Objects.requireNonNull(nanoTimeSource, "nanoTimeSource");
  }

  /** Mark the beginning of a block. Must be called before {@link #checkpoint(String)}. */
  public void start() {
    long now = nanoTimeSource.getAsLong();
    this.startNanos = now;
    this.lastCheckpointNanos = now;
    this.lastStage = null;
  }

  /** Record the completion of a pipeline stage and return the time it took. */
  public long checkpoint(String stage) {
    Objects.requireNonNull(stage, "stage");
    long now = nanoTimeSource.getAsLong();
    long delta = now - lastCheckpointNanos;
    lastCheckpointNanos = now;
    lastStage = stage;
    return delta;
  }

  /** Total elapsed time since {@link #start()} in nanoseconds. */
  public long elapsedNanos() {
    return nanoTimeSource.getAsLong() - startNanos;
  }

  /** Whether the elapsed time has exceeded the per-block deadline. */
  public boolean exceeded() {
    return elapsedNanos() > schedule.maxProcessingNanos();
  }

  /** Underlying {@link FrameSchedule}. */
  public FrameSchedule schedule() {
    return schedule;
  }

  /** Name of the last stage that was checkpointed, or {@code null} if none. */
  public String lastStage() {
    return lastStage;
  }
}
