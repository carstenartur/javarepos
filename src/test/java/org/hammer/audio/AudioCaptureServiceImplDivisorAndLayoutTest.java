package org.hammer.audio;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Tests for AudioCaptureServiceImpl divisor and layout computation. */
class AudioCaptureServiceImplDivisorAndLayoutTest {

  private AudioCaptureServiceImpl service;

  @AfterEach
  void cleanup() {
    if (service != null && service.isRunning()) {
      service.stop();
    }
  }

  @Test
  void setDivisor_updates_divisor_value() {
    service = new AudioCaptureServiceImpl(16000.0f, 8, 1, true, false, 1);

    assertEquals(1, service.getDivisor());

    service.setDivisor(4);
    assertEquals(4, service.getDivisor());

    service.setDivisor(16);
    assertEquals(16, service.getDivisor());
  }

  @Test
  void setDivisor_throws_for_invalid_values() {
    service = new AudioCaptureServiceImpl(16000.0f, 8, 1, true, false, 1);

    assertThrows(IllegalArgumentException.class, () -> service.setDivisor(0));
    assertThrows(IllegalArgumentException.class, () -> service.setDivisor(-1));
    assertThrows(IllegalArgumentException.class, () -> service.setDivisor(-100));
  }

  @Test
  void setDivisor_affects_model_numberOfPoints_after_start() throws InterruptedException {
    byte[] testData = new byte[256];
    TestAudioLineProvider provider = new TestAudioLineProvider(4096, testData);

    service = new AudioCaptureServiceImpl(16000.0f, 8, 1, false, false, 1, provider);

    service.start();
    Thread.sleep(100); // Let capture loop initialize

    WaveformModel model1 = service.getLatestModel();
    int points1 = model1.getNumberOfPoints();

    // Increase divisor should reduce numberOfPoints (smaller buffer)
    service.setDivisor(4);
    Thread.sleep(100); // Let new computation take effect

    WaveformModel model2 = service.getLatestModel();
    int points2 = model2.getNumberOfPoints();

    assertTrue(
        points2 < points1,
        "Increasing divisor should reduce numberOfPoints (got " + points2 + " vs " + points1 + ")");
  }

  @Test
  void recomputeLayout_updates_xPoints_to_span_width() throws InterruptedException {
    byte[] testData = new byte[256];
    for (int i = 0; i < testData.length; i++) {
      testData[i] = (byte) i;
    }
    TestAudioLineProvider provider = new TestAudioLineProvider(4096, testData);

    service = new AudioCaptureServiceImpl(16000.0f, 8, 1, false, false, 8, provider);

    service.start();
    Thread.sleep(100); // Let capture loop initialize

    // Recompute layout for different width
    int targetWidth = 800;
    int targetHeight = 300;
    service.recomputeLayout(targetWidth, targetHeight);
    Thread.sleep(100); // Let recomputation take effect

    WaveformModel model = service.getLatestModel();
    int[] xPoints = model.getXPoints();

    if (xPoints.length > 0) {
      assertEquals(0, xPoints[0], "First x-point should be at 0");
      if (xPoints.length > 1) {
        assertEquals(
            targetWidth - 1, xPoints[xPoints.length - 1], "Last x-point should be at width-1");
      }
    }
  }

  @Test
  void recomputeLayout_xPoints_are_monotonically_increasing() throws InterruptedException {
    byte[] testData = new byte[256];
    TestAudioLineProvider provider = new TestAudioLineProvider(4096, testData);

    service = new AudioCaptureServiceImpl(16000.0f, 8, 1, false, false, 8, provider);

    service.start();
    Thread.sleep(100);

    service.recomputeLayout(640, 200);
    Thread.sleep(100);

    WaveformModel model = service.getLatestModel();
    int[] xPoints = model.getXPoints();

    // Verify monotonically increasing
    for (int i = 1; i < xPoints.length; i++) {
      assertTrue(xPoints[i] >= xPoints[i - 1], "xPoints should be monotonically increasing");
    }
  }
}
