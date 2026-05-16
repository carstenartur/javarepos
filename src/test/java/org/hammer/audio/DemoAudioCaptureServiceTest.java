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
      service = new DemoAudioCaptureService(16_000f, 2, 16, 1, signalType);
      service.start();
      Thread.sleep(120);
      AudioBlock block = service.getLatestBlock();
      assertNotNull(block, "demo mode should generate blocks for " + signalType);
      assertEquals(2, block.channels());
      assertTrue(block.frames() > 0);
      assertNotNull(service.getLatestModel());
      service.stop();
    }
  }
}
