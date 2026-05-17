package org.hammer.audio.experimental.acoustic.tracking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class FrameScheduleAndBudgetTest {

  @Test
  void frameScheduleComputesDurationAndDeadline() {
    FrameSchedule schedule = new FrameSchedule(16_000.0, 512, 0.5);

    assertEquals(0.032, schedule.blockDurationSeconds(), 1.0e-9);
    assertEquals(16_000_000L, schedule.maxProcessingNanos());
  }

  @Test
  void frameScheduleValidatesArguments() {
    assertThrows(IllegalArgumentException.class, () -> new FrameSchedule(0.0, 256, 0.5));
    assertThrows(IllegalArgumentException.class, () -> new FrameSchedule(16_000.0, 0, 0.5));
    assertThrows(IllegalArgumentException.class, () -> new FrameSchedule(16_000.0, 256, 0.0));
    assertThrows(IllegalArgumentException.class, () -> new FrameSchedule(16_000.0, 256, 1.1));
  }

  @Test
  void processingBudgetDetectsExceededDeadline() {
    FrameSchedule schedule = new FrameSchedule(16_000.0, 512, 0.5);
    AtomicLong virtualTime = new AtomicLong(0L);
    ProcessingBudget budget = new ProcessingBudget(schedule, virtualTime::get);

    budget.start();
    virtualTime.set(1_000_000L);
    long delta = budget.checkpoint("peak-detection");
    assertEquals(1_000_000L, delta);
    assertEquals("peak-detection", budget.lastStage());
    assertFalse(budget.exceeded());

    virtualTime.set(20_000_000L);
    assertTrue(budget.exceeded());
  }
}
