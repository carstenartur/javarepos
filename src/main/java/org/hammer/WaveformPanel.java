package org.hammer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.logging.Logger;
import javax.swing.JPanel;
import org.hammer.audio.AudioCaptureService;
import org.hammer.audio.WaveformModel;

/**
 * Panel for displaying audio waveform visualization.
 *
 * <p>Refactored to use AudioCaptureService and WaveformModel instead of direct singleton access.
 * The panel now receives the service via setter and uses thread-safe model snapshots for rendering.
 *
 * @author chammer
 */
public final class WaveformPanel extends JPanel {

  private static final Logger LOGGER = Logger.getLogger(WaveformPanel.class.getName());

  private AudioCaptureService audioCaptureService;

  /**
   * Create a new WaveformPanel.
   *
   * <p>Note: The audio capture service must be set via {@link #setAudioCaptureService} before the
   * panel can display waveforms.
   */
  public WaveformPanel() {
    super(true);
    LOGGER.info("WaveformPanel created");

    // Timer to periodically repaint at consistent interval for smooth display updates
    javax.swing.Timer t = new javax.swing.Timer(UiConstants.REFRESH_INTERVAL_MS, e -> repaint());
    t.start();

    // Notify service when panel is resized
    addComponentListener(
        new ComponentAdapter() {
          @Override
          public void componentResized(ComponentEvent e) {
            if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
              LOGGER.fine(String.format("WaveformPanel resized to %dx%d", getWidth(), getHeight()));
            }
            if (audioCaptureService != null) {
              LOGGER.fine(
                  String.format(
                      "Panel resized to %dx%d, recomputing layout", getWidth(), getHeight()));
              audioCaptureService.recomputeLayout(getWidth(), getHeight());
            }
          }
        });
  }

  /**
   * Set the audio capture service to use for waveform data.
   *
   * @param service the AudioCaptureService
   */
  public void setAudioCaptureService(AudioCaptureService service) {
    this.audioCaptureService = service;
    LOGGER.info("AudioCaptureService set: " + (service != null));
    if (service != null) {
      // Initial layout computation
      service.recomputeLayout(getWidth(), getHeight());
    }
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
      LOGGER.fine("paintComponent called");
    }
    g.drawRect(0, 0, this.getWidth() - 1, this.getHeight() - 1);

    if (audioCaptureService == null) {
      LOGGER.warning("paintComponent: audioCaptureService is null");
      // Draw placeholder message
      g.drawString("No audio service connected", 10, getHeight() / 2);
      return;
    }

    // Get thread-safe snapshot of model
    WaveformModel model = audioCaptureService.getLatestModel();

    final int points = model.getNumberOfPoints();
    if (points == 0) {
      return;
    }

    int[] xPoints = model.getXPoints();
    int[][] yPoints = model.getYPoints();

    // Draw channel 0 (yellow)
    if (yPoints.length > 0) {
      g.setColor(Color.yellow);
      g.drawPolyline(xPoints, yPoints[0], points);
    }

    // Draw channel 1 (cyan)
    if (yPoints.length > 1) {
      g.setColor(Color.cyan);
      g.drawPolyline(xPoints, yPoints[1], points);
    }

    // Draw center line and tick marks
    g.setColor(Color.red);
    g.drawLine(0, getHeight() / 2, getWidth() - 1, getHeight() / 2);

    int tickEveryNSample = model.getTickEveryNSample();
    if (tickEveryNSample > 0) {
      for (int i = 0; i < points; i += tickEveryNSample) {
        g.drawLine(xPoints[i], getHeight() / 2, xPoints[i], getHeight() / 2 + 6);
      }
    }
  }
}
