package org.hammer;

import java.awt.BorderLayout;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.hammer.audio.AudioCaptureService;

/**
 * Panel containing the phase diagram visualization.
 *
 * <p>Refactored to inject AudioCaptureService into the canvas.
 *
 * @author chammer
 */
public class PhaseDiagramPanel extends JPanel {

  private static final Logger LOGGER = Logger.getLogger(PhaseDiagramPanel.class.getName());

  private final PhaseDiagramCanvas canvas;

  public PhaseDiagramPanel() {
    super();
    setLayout(new BorderLayout(0, 0));

    JLabel lblNewLabel = new JLabel("Phase diagram");
    add(lblNewLabel, BorderLayout.NORTH);

    canvas = new PhaseDiagramCanvas();
    add(canvas, BorderLayout.CENTER);
  }

  /**
   * Set the audio capture service for the phase diagram.
   *
   * @param service the AudioCaptureService
   */
  public void setAudioCaptureService(AudioCaptureService service) {
    LOGGER.fine("Audio capture service set for phase diagram panel");
    canvas.setAudioCaptureService(service);
  }
}
