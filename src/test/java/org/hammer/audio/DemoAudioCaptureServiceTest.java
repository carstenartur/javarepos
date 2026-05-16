package org.hammer.audio;

import static org.junit.jupiter.api.Assertions.*;

import org.hammer.audio.core.AudioBlock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DemoAudioCaptureServiceTest {

  private DemoAudioCaptureService service;

  @AfterEach
  void cleanup() {
    if (service != null && service.isRunning()) {
      service.stop();
    }
  }

  @Test
  void demo_service_generates_blocks_for_all_signals() throws InterruptedException {
    for (DemoSignalType signalType : DemoSignalType.values()) {
      service = new DemoAudioCaptureService(16_000f, 16, 2, 1, signalType);
      service.start();
      AudioBlock block = waitForLatestBlock(service, 2_000);
      assertNotNull(block, "demo mode should generate blocks for " + signalType);
      assertEquals(2, block.channels());
      assertTrue(block.frames() > 0);
      assertNotNull(service.getLatestModel());
      service.stop();
    }
  }

  @Test
  void constructor_rejects_invalid_divisor() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new DemoAudioCaptureService(16_000f, 16, 2, 0, DemoSignalType.SINE));
  }

  private static AudioBlock waitForLatestBlock(DemoAudioCaptureService service, long timeoutMillis)
      throws InterruptedException {
    long deadline = System.nanoTime() + timeoutMillis * 1_000_000L;
    AudioBlock block;
    do {
      block = service.getLatestBlock();
      if (block != null) {
        return block;
      }
      Thread.sleep(10);
    } while (System.nanoTime() < deadline);
    return null;
  }
}
