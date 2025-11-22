package org.hammer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.logging.Logger;
import javax.swing.JPanel;
import org.hammer.audio.AudioCaptureService;
import org.hammer.audio.WaveformModel;

/**
 * Canvas for displaying phase diagram (X-Y plot of two channels).
 *
 * <p>Refactored to use AudioCaptureService and WaveformModel instead of direct singleton access.
 * Uses thread-safe model snapshots for rendering.
 *
 * @author chammer
 */
public class PhaseDiagramCanvas extends JPanel {

  private static final Logger LOGGER = Logger.getLogger(PhaseDiagramCanvas.class.getName());

  private AudioCaptureService audioCaptureService;

  public PhaseDiagramCanvas() {
    super(true);
    // Timer to periodically repaint at consistent interval for smooth display updates
    javax.swing.Timer t = new javax.swing.Timer(UiConstants.REFRESH_INTERVAL_MS, e -> repaint());
    t.start();
  }

  /**
   * Set the audio capture service to use for phase diagram data.
   *
   * @param service the AudioCaptureService
   */
  public void setAudioCaptureService(AudioCaptureService service) {
    this.audioCaptureService = service;
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    g.drawRect(0, 0, this.getWidth() - 1, this.getHeight() - 1);

    if (audioCaptureService == null) {
      LOGGER.warning("No audio service connected during paintComponent");
      g.drawString("No audio service", 10, getHeight() / 2);
      return;
    }

    // Get thread-safe snapshot of model
    WaveformModel model = audioCaptureService.getLatestModel();

    final int points = model.getNumberOfPoints();
    if (model.getChannelCount() < 2 || points == 0) {
      return;
    }

    int[][] yPoints = model.getYPoints();

    // Draw phase diagram (channel 0 vs channel 1)
    // Use a Graphics2D copy to avoid accumulating transformations across multiple paint calls.
    // The translation centers the diagram based on the first point, but we must restore
    // the original transform to prevent drift or interference with future paint operations.
    Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setColor(Color.yellow);
      if (yPoints[0].length > 0 && yPoints[1].length > 0) {
        g2.translate(-yPoints[0][0], -yPoints[1][0]);
        g2.drawPolyline(yPoints[0], yPoints[1], points);
      }
    } finally {
      g2.dispose(); // Restore original graphics state
    }
  }
}
