package org.hammer.audio.analysis;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import org.hammer.audio.core.AudioBlock;
import org.hammer.audio.core.AudioFormatDescriptor;
import org.junit.jupiter.api.Test;

class WaveformTriggerTest {

  private static final AudioFormatDescriptor MONO_44K = new AudioFormatDescriptor(44100f, 1, 16);

  @Test
  void firesOnRisingZeroCrossing() {
    WaveformTrigger trigger = new WaveformTrigger(8);
    trigger.setMode(WaveformTrigger.Mode.NORMAL);
    trigger.setHoldoffFrames(0);

    // -0.5 then 0.5 -> rising crossing at index 1.
    float[] samples = new float[] {-0.5f, 0.5f, 0.4f, 0.3f, 0.2f, 0.1f, 0.0f, -0.1f, -0.2f, -0.3f};
    AudioBlock block = new AudioBlock(MONO_44K, new float[][] {samples}, 0L, 0L);

    Optional<WaveformTrigger.TriggeredView> view = trigger.process(block, 0);
    assertTrue(view.isPresent());
    WaveformTrigger.TriggeredView v = view.get();
    assertTrue(v.triggered());
    assertEquals(0.5f, v.samplesView()[0], 1e-6f);
    assertEquals(8, v.samplesView().length);
    assertEquals(1L, v.viewFrameIndex());
  }

  @Test
  void doesNotFireOnFallingWhenRisingSelected() {
    WaveformTrigger trigger = new WaveformTrigger(4);
    trigger.setMode(WaveformTrigger.Mode.NORMAL);
    trigger.setSlope(WaveformTrigger.Slope.RISING);
    trigger.setHoldoffFrames(0);

    // Pure falling signal, no rising crossing.
    float[] samples = new float[] {0.9f, 0.5f, 0.1f, -0.1f, -0.4f, -0.7f, -0.9f};
    AudioBlock block = new AudioBlock(MONO_44K, new float[][] {samples}, 0L, 0L);

    Optional<WaveformTrigger.TriggeredView> view = trigger.process(block, 0);
    assertTrue(view.isEmpty());
  }

  @Test
  void firesOnFallingWhenFallingSelected() {
    WaveformTrigger trigger = new WaveformTrigger(4);
    trigger.setMode(WaveformTrigger.Mode.NORMAL);
    trigger.setSlope(WaveformTrigger.Slope.FALLING);
    trigger.setHoldoffFrames(0);

    float[] samples = new float[] {0.5f, -0.5f, -0.4f, -0.3f, -0.2f, -0.1f, 0.0f};
    AudioBlock block = new AudioBlock(MONO_44K, new float[][] {samples}, 0L, 0L);

    Optional<WaveformTrigger.TriggeredView> view = trigger.process(block, 0);
    assertTrue(view.isPresent());
    assertTrue(view.get().triggered());
    assertEquals(-0.5f, view.get().samplesView()[0], 1e-6f);
  }

  @Test
  void respectsHoldoff() {
    WaveformTrigger trigger = new WaveformTrigger(2);
    trigger.setMode(WaveformTrigger.Mode.NORMAL);
    trigger.setHoldoffFrames(5);

    // Two close rising crossings; only the first should fire while holdoff is active.
    float[] samples = new float[] {-0.5f, 0.5f, -0.5f, 0.5f, -0.5f, 0.5f, -0.5f, 0.5f, -0.5f, 0.5f};
    AudioBlock block = new AudioBlock(MONO_44K, new float[][] {samples}, 0L, 0L);
    Optional<WaveformTrigger.TriggeredView> view = trigger.process(block, 0);
    assertTrue(view.isPresent());
    long firstTriggerFrame = view.get().viewFrameIndex();

    // Next block right away — within holdoff. With viewFrames=2 it's not the first call's
    // suppressed second crossing that matters, but state after publication. Push a block whose
    // first samples represent a crossing immediately after.
    float[] samples2 = new float[] {-0.5f, 0.5f, -0.5f, 0.5f};
    AudioBlock block2 = new AudioBlock(MONO_44K, new float[][] {samples2}, samples.length, 0L);
    Optional<WaveformTrigger.TriggeredView> view2 = trigger.process(block2, 0);
    // Either a trigger past holdoff, or nothing — but the frame index must be at least
    // firstTriggerFrame + holdoff.
    if (view2.isPresent()) {
      assertTrue(
          view2.get().viewFrameIndex() >= firstTriggerFrame + 5,
          "trigger fired inside holdoff window");
    }
  }

  @Test
  void autoModeFiresAfterTimeoutOnConstantSignal() {
    WaveformTrigger trigger = new WaveformTrigger(4);
    trigger.setMode(WaveformTrigger.Mode.AUTO);
    trigger.setAutoTimeoutFrames(8);
    trigger.setHoldoffFrames(0);
    trigger.setLevel(0.5f);

    // Constant 0.0 signal — never crosses 0.5 from below.
    float[] silence = new float[16];
    AudioBlock block = new AudioBlock(MONO_44K, new float[][] {silence}, 0L, 0L);
    Optional<WaveformTrigger.TriggeredView> view = trigger.process(block, 0);
    assertTrue(view.isPresent(), "AUTO mode should publish even without a trigger");
    assertFalse(view.get().triggered());
  }

  @Test
  void normalModeStaysSilentOnConstantSignal() {
    WaveformTrigger trigger = new WaveformTrigger(4);
    trigger.setMode(WaveformTrigger.Mode.NORMAL);
    trigger.setHoldoffFrames(0);
    trigger.setLevel(0.5f);

    float[] silence = new float[64];
    AudioBlock block = new AudioBlock(MONO_44K, new float[][] {silence}, 0L, 0L);
    assertTrue(trigger.process(block, 0).isEmpty());
  }

  @Test
  void crossingAcrossBlockBoundaryFires() {
    WaveformTrigger trigger = new WaveformTrigger(2);
    trigger.setMode(WaveformTrigger.Mode.NORMAL);
    trigger.setHoldoffFrames(0);

    // First block ends below level. No crossing visible yet.
    AudioBlock block1 = new AudioBlock(MONO_44K, new float[][] {{-0.5f, -0.5f}}, 0L, 0L);
    assertTrue(trigger.process(block1, 0).isEmpty());

    // Second block starts above level — crossing happens at the boundary.
    AudioBlock block2 = new AudioBlock(MONO_44K, new float[][] {{0.5f, 0.6f}}, 2L, 0L);
    Optional<WaveformTrigger.TriggeredView> view = trigger.process(block2, 0);
    assertTrue(view.isPresent());
    assertTrue(view.get().triggered());
  }

  @Test
  void resetClearsState() {
    WaveformTrigger trigger = new WaveformTrigger(2);
    trigger.setMode(WaveformTrigger.Mode.NORMAL);
    AudioBlock block = new AudioBlock(MONO_44K, new float[][] {{-0.5f, 0.5f}}, 0L, 0L);
    trigger.process(block, 0);

    trigger.reset();

    // After reset there should be no held prevSample. A block that does not itself contain a
    // crossing should produce no trigger.
    AudioBlock flat = new AudioBlock(MONO_44K, new float[][] {{0.5f, 0.6f, 0.7f}}, 2L, 0L);
    assertTrue(trigger.process(flat, 0).isEmpty());
  }

  @Test
  void rejectsInvalidLevel() {
    WaveformTrigger trigger = new WaveformTrigger();
    assertThrows(IllegalArgumentException.class, () -> trigger.setLevel(2.0f));
    assertThrows(IllegalArgumentException.class, () -> trigger.setLevel(Float.NaN));
  }

  @Test
  void rejectsInvalidViewFrames() {
    assertThrows(IllegalArgumentException.class, () -> new WaveformTrigger(0));
  }

  @Test
  void triggeredViewIsImmutable() {
    WaveformTrigger trigger = new WaveformTrigger(2);
    trigger.setMode(WaveformTrigger.Mode.NORMAL);
    trigger.setHoldoffFrames(0);
    AudioBlock block = new AudioBlock(MONO_44K, new float[][] {{-0.5f, 0.5f, 0.6f}}, 0L, 0L);
    WaveformTrigger.TriggeredView view = trigger.process(block, 0).orElseThrow();
    float[] copy = view.samples();
    copy[0] = 99f;
    assertNotEquals(99f, view.samplesView()[0]);
  }
}
