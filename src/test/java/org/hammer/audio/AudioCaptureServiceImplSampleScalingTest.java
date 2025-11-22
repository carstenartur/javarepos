package org.hammer.audio;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Tests for AudioCaptureServiceImpl sample scaling to pixel coordinates. */
class AudioCaptureServiceImplSampleScalingTest {

  private AudioCaptureServiceImpl service;

  @AfterEach
  void cleanup() {
    if (service != null && service.isRunning()) {
      service.stop();
    }
  }

  @Test
  void unsigned_8bit_samples_scale_to_panel_bounds() throws InterruptedException {
    // Create test data with unsigned 8-bit samples (0-255)
    byte[] testData = new byte[128];
    testData[0] = (byte) 0; // Min unsigned value (0)
    testData[1] = (byte) 255; // Max unsigned value (255)
    testData[2] = (byte) 128; // Mid unsigned value (128)

    TestAudioLineProvider provider = new TestAudioLineProvider(4096, testData);

    int panelHeight = 200;
    service = new AudioCaptureServiceImpl(16000.0f, 8, 1, false, false, 8, provider);
    service.recomputeLayout(640, panelHeight);

    service.start();
    Thread.sleep(150); // Let capture loop process data

    WaveformModel model = service.getLatestModel();
    int[] yPoints = model.getYPointsForChannel(0);

    // Verify all y-values are within panel bounds [0, panelHeight]
    for (int y : yPoints) {
      assertTrue(y >= 0, "Y value " + y + " should be >= 0");
      assertTrue(y <= panelHeight, "Y value " + y + " should be <= " + panelHeight);
    }
  }

  @Test
  void signed_8bit_samples_scale_centered_around_middle() throws InterruptedException {
    // Create test data with signed 8-bit samples (-128 to 127)
    byte[] testData = new byte[128];
    testData[0] = (byte) 0; // Zero sample (center line)
    testData[1] = (byte) 127; // Max positive
    testData[2] = (byte) -128; // Max negative
    testData[3] = (byte) 64; // Positive value
    testData[4] = (byte) -64; // Negative value

    TestAudioLineProvider provider = new TestAudioLineProvider(4096, testData);

    int panelHeight = 200;
    service = new AudioCaptureServiceImpl(16000.0f, 8, 1, true, false, 8, provider);
    service.recomputeLayout(640, panelHeight);

    service.start();
    Thread.sleep(150);

    WaveformModel model = service.getLatestModel();
    int[] yPoints = model.getYPointsForChannel(0);

    // Verify all y-values are within reasonable panel bounds
    // Note: Due to rounding, values can slightly exceed panelHeight
    for (int y : yPoints) {
      assertTrue(y >= 0, "Y value " + y + " should be >= 0");
      assertTrue(y <= panelHeight + 1, "Y value " + y + " should be <= " + (panelHeight + 1));
    }

    // For signed samples, zero should be near center (height/2)
    // We check the first point which corresponds to our zero sample
    if (yPoints.length > 0) {
      int centerY = panelHeight / 2;
      int tolerance = panelHeight / 4; // Allow some tolerance
      assertTrue(
          Math.abs(yPoints[0] - centerY) <= tolerance,
          "Zero sample should be near center line, got "
              + yPoints[0]
              + " expected near "
              + centerY);
    }
  }

  @Test
  void stereo_channels_produce_separate_yPoints() throws InterruptedException {
    // Create interleaved stereo data (left/right alternating)
    byte[] testData = new byte[256];
    for (int i = 0; i < testData.length; i++) {
      // Create different patterns for left (even) and right (odd) channels
      if (i % 2 == 0) {
        testData[i] = (byte) (i / 2); // Left channel
      } else {
        testData[i] = (byte) (255 - i / 2); // Right channel (inverted)
      }
    }

    TestAudioLineProvider provider = new TestAudioLineProvider(4096, testData);

    int panelHeight = 200;
    service = new AudioCaptureServiceImpl(16000.0f, 8, 2, false, false, 8, provider); // 2 channels
    service.recomputeLayout(640, panelHeight);

    service.start();
    Thread.sleep(150);

    WaveformModel model = service.getLatestModel();
    assertEquals(2, model.getChannelCount(), "Should have 2 channels");

    int[] yPointsCh0 = model.getYPointsForChannel(0);
    int[] yPointsCh1 = model.getYPointsForChannel(1);

    assertTrue(yPointsCh0.length > 0, "Channel 0 should have data");
    assertTrue(yPointsCh1.length > 0, "Channel 1 should have data");
    assertEquals(
        yPointsCh0.length, yPointsCh1.length, "Both channels should have same number of points");
  }

  @Test
  void signed_16bit_samples_scale_to_panel_bounds() throws InterruptedException {
    // Create test data with signed 16-bit samples (little-endian)
    byte[] testData = new byte[256];
    // Sample 0: zero value (0x0000)
    testData[0] = 0;
    testData[1] = 0;
    // Sample 1: max positive (0x7FFF = 32767)
    testData[2] = (byte) 0xFF;
    testData[3] = (byte) 0x7F;
    // Sample 2: max negative (0x8000 = -32768)
    testData[4] = (byte) 0x00;
    testData[5] = (byte) 0x80;

    TestAudioLineProvider provider = new TestAudioLineProvider(4096, testData);

    int panelHeight = 200;
    service =
        new AudioCaptureServiceImpl(16000.0f, 16, 1, true, false, 8, provider); // 16-bit signed
    service.recomputeLayout(640, panelHeight);

    service.start();
    Thread.sleep(150);

    WaveformModel model = service.getLatestModel();
    int[] yPoints = model.getYPointsForChannel(0);

    // Verify all y-values are within panel bounds
    for (int y : yPoints) {
      assertTrue(y >= 0, "Y value " + y + " should be >= 0");
      assertTrue(y <= panelHeight, "Y value " + y + " should be <= " + panelHeight);
    }
  }
}
