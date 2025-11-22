package org.hammer.audio;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Tests for AudioCaptureServiceImpl lifecycle operations (start/stop). */
class AudioCaptureServiceImplLifecycleTest {

  private AudioCaptureServiceImpl service;

  @AfterEach
  void cleanup() {
    if (service != null && service.isRunning()) {
      service.stop();
    }
  }

  @Test
  void start_sets_running_to_true() throws InterruptedException {
    // Create test data with predictable pattern
    byte[] testData = new byte[256];
    for (int i = 0; i < testData.length; i++) {
      testData[i] = (byte) ((i % 128) - 64); // Signed pattern
    }

    TestAudioLineProvider provider = new TestAudioLineProvider(4096, testData);

    // Create service with mock line provider
    service = new AudioCaptureServiceImpl(16000.0f, 8, 1, true, false, 8, provider);

    assertFalse(service.isRunning(), "Service should not be running initially");

    service.start();

    assertTrue(service.isRunning(), "Service should be running after start()");
  }

  @Test
  void start_initializes_format() throws InterruptedException {
    byte[] testData = new byte[256];
    TestAudioLineProvider provider = new TestAudioLineProvider(4096, testData);

    service = new AudioCaptureServiceImpl(16000.0f, 8, 2, false, false, 8, provider);

    assertNull(service.getFormat(), "Format should be null before start()");

    service.start();

    assertNotNull(service.getFormat(), "Format should be initialized after start()");
    assertEquals(16000.0f, service.getFormat().getSampleRate());
    assertEquals(8, service.getFormat().getSampleSizeInBits());
    assertEquals(2, service.getFormat().getChannels());
  }

  @Test
  void start_updates_model_after_first_read() throws InterruptedException {
    // Create test data
    byte[] testData = new byte[256];
    for (int i = 0; i < testData.length; i++) {
      testData[i] = (byte) i;
    }

    TestAudioLineProvider provider = new TestAudioLineProvider(4096, testData);

    service = new AudioCaptureServiceImpl(16000.0f, 8, 1, false, false, 8, provider);

    WaveformModel initialModel = service.getLatestModel();
    assertSame(WaveformModel.EMPTY, initialModel, "Model should be EMPTY before start");

    service.start();

    // Give the capture loop time to read and update the model
    Thread.sleep(100);

    WaveformModel updatedModel = service.getLatestModel();
    assertNotSame(WaveformModel.EMPTY, updatedModel, "Model should be updated after start");
    assertTrue(updatedModel.getNumberOfPoints() > 0, "Model should have points after data capture");
  }

  @Test
  void stop_sets_running_to_false() throws InterruptedException {
    byte[] testData = new byte[256];
    TestAudioLineProvider provider = new TestAudioLineProvider(4096, testData);

    service = new AudioCaptureServiceImpl(16000.0f, 8, 1, true, false, 8, provider);

    service.start();
    assertTrue(service.isRunning());

    service.stop();

    assertFalse(service.isRunning(), "Service should not be running after stop()");
  }

  @Test
  void stop_is_idempotent() {
    byte[] testData = new byte[256];
    TestAudioLineProvider provider = new TestAudioLineProvider(4096, testData);

    service = new AudioCaptureServiceImpl(16000.0f, 8, 1, true, false, 8, provider);

    // Stop without starting should not throw
    assertDoesNotThrow(() -> service.stop());

    // Stop multiple times should not throw
    service.start();
    service.stop();
    assertDoesNotThrow(() -> service.stop());
    assertDoesNotThrow(() -> service.stop());
  }
}
