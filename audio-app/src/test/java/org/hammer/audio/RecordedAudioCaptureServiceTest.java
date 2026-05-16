package org.hammer.audio;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.hammer.audio.core.AudioBlock;
import org.hammer.audio.core.AudioFormatDescriptor;
import org.junit.jupiter.api.Test;

class RecordedAudioCaptureServiceTest {

  private static final AudioFormatDescriptor MONO_44K = new AudioFormatDescriptor(44100f, 1, 16);

  private static List<AudioBlock> blocks() {
    List<AudioBlock> out = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      out.add(new AudioBlock(MONO_44K, new float[][] {{0.1f, 0.2f, 0.3f}}, i * 3L, 0L));
    }
    return out;
  }

  @Test
  void exposesFormatAndBlockCount() {
    RecordedAudioCaptureService svc = new RecordedAudioCaptureService(blocks(), false);
    assertEquals(MONO_44K, svc.getDescriptor());
    assertEquals(3, svc.blockCount());
    assertFalse(svc.isRunning());
  }

  @Test
  void rejectsEmptyBlocks() {
    assertThrows(
        IllegalArgumentException.class, () -> new RecordedAudioCaptureService(List.of(), false));
  }

  @Test
  void rejectsMixedFormats() {
    AudioBlock a = new AudioBlock(MONO_44K, new float[][] {{0f}}, 0L, 0L);
    AudioBlock b =
        new AudioBlock(new AudioFormatDescriptor(48000f, 1, 16), new float[][] {{0f}}, 1L, 0L);
    assertThrows(
        IllegalArgumentException.class,
        () -> new RecordedAudioCaptureService(List.of(a, b), false));
  }

  @Test
  void startStopReplay() throws InterruptedException {
    RecordedAudioCaptureService svc = new RecordedAudioCaptureService(blocks(), true);
    svc.start();
    assertTrue(svc.isRunning());
    // Poll for a published block with a bounded timeout to avoid flakiness on slow CI.
    long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
    while (svc.getLatestBlock() == null && System.nanoTime() < deadline) {
      Thread.sleep(5);
    }
    assertNotNull(svc.getLatestBlock(), "no block published within 5 s");
    svc.stop();
    assertFalse(svc.isRunning());
  }
}
