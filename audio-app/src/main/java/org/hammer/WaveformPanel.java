package org.hammer;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.logging.Logger;
import javax.swing.JPanel;
import org.hammer.audio.AudioCaptureService;
import org.hammer.audio.WaveformModel;
import org.hammer.audio.analysis.WaveformTrigger;
import org.hammer.audio.core.AudioBlock;
import org.hammer.audio.ui.theme.PlotRenderTheme;

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
  private transient WaveformModel frozenModel;
  private transient AudioBlock frozenBlock;
  private boolean frozen;

  private final transient WaveformTrigger trigger = new WaveformTrigger();
  private transient WaveformTrigger.TriggeredView lastTriggeredView;
  private transient long lastTriggeredBlockFrameIndex = Long.MIN_VALUE;
  private transient int[] triggerXs = new int[0];
  private transient int[] triggerYs = new int[0];
  private boolean triggerEnabled;

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
    this.frozenModel = null;
    this.frozenBlock = null;
    this.frozen = false;
    LOGGER.info("AudioCaptureService set: " + (service != null));
    if (service != null) {
      // Initial layout computation
      service.recomputeLayout(getWidth(), getHeight());
    }
  }

  /**
   * Enable or disable oscilloscope-style triggering. When enabled, the panel uses the configured
   * {@link WaveformTrigger} to align each refresh to a stable trigger event (e.g. rising
   * zero-crossing).
   *
   * @param enabled true to enable triggering, false to display raw blocks
   */
  public void setTriggerEnabled(boolean enabled) {
    this.triggerEnabled = enabled;
    if (!enabled) {
      lastTriggeredView = null;
      trigger.reset();
    }
    repaint();
  }

  /**
   * @return true if oscilloscope-style triggering is currently enabled
   */
  public boolean isTriggerEnabled() {
    return triggerEnabled;
  }

  /**
   * @return the underlying {@link WaveformTrigger}; safe to reconfigure from the EDT
   */
  @SuppressWarnings("PMD.AvoidProtectedFieldInFinalClass")
  public WaveformTrigger getTrigger() {
    return trigger;
  }

  /**
   * Freeze or unfreeze the displayed waveform.
   *
   * @param frozen true to hold the current waveform snapshot
   */
  public void setFrozen(boolean frozen) {
    if (frozen && !this.frozen) {
      frozenModel = getCurrentModel();
      frozenBlock = getCurrentBlock();
    } else if (!frozen) {
      frozenModel = null;
      frozenBlock = null;
    }
    this.frozen = frozen;
    repaint();
  }

  /**
   * @return current waveform model, respecting frozen display state
   */
  public WaveformModel getCurrentModel() {
    if (frozen && frozenModel != null) {
      return frozenModel;
    }
    if (audioCaptureService == null) {
      return WaveformModel.EMPTY;
    }
    return audioCaptureService.getLatestModel();
  }

  private AudioBlock getCurrentBlock() {
    if (frozen && frozenBlock != null) {
      return frozenBlock;
    }
    if (audioCaptureService == null) {
      return null;
    }
    return audioCaptureService.getLatestBlock();
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g.create();
    try {
      PlotRenderTheme.applyQualityRendering(g2);
      paintWaveform(g2);
    } finally {
      g2.dispose();
    }
  }

  private void paintWaveform(Graphics2D g2) {
    if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
      LOGGER.fine("paintComponent called");
    }
    Rectangle plotBounds = new Rectangle(0, 0, Math.max(1, getWidth()), Math.max(1, getHeight()));
    PlotRenderTheme.drawPlotBackground(g2, getWidth(), getHeight(), plotBounds);
    PlotRenderTheme.drawGrid(g2, plotBounds, 10, 8);
    PlotRenderTheme.drawTitle(g2, 10, 16, triggerEnabled ? "Waveform (triggered)" : "Waveform");

    if (audioCaptureService == null) {
      LOGGER.warning("paintComponent: audioCaptureService is null");
      PlotRenderTheme.drawEmptyState(g2, plotBounds, "No audio service connected");
      return;
    }

    if (triggerEnabled) {
      if (paintTriggeredWaveform(g2, plotBounds)) {
        drawLevelOverlay(g2, plotBounds);
        return;
      }
      // Fall through to free-running mode if trigger cannot produce a view yet.
    }

    WaveformModel model = getCurrentModel();
    final int points = model.getNumberOfPoints();
    if (points == 0) {
      PlotRenderTheme.drawEmptyState(g2, plotBounds, "Waiting for waveform data...");
      return;
    }

    int[] xPoints = model.getXPoints();
    int[][] yPoints = model.getYPoints();
    int channel0Points = yPoints.length > 0 ? Math.min(points, yPoints[0].length) : 0;
    int channel1Points = yPoints.length > 1 ? Math.min(points, yPoints[1].length) : 0;
    int xCount = xPoints.length;
    int leftRenderPoints = Math.min(xCount, channel0Points);
    int rightRenderPoints = Math.min(xCount, channel1Points);

    if (channel0Points > 1 && xCount > 1) {
      g2.setColor(PlotRenderTheme.WAVEFORM_LEFT);
      g2.setStroke(PlotRenderTheme.TRACE_STROKE);
      g2.drawPolyline(xPoints, yPoints[0], leftRenderPoints);
    }

    if (channel1Points > 1 && xCount > 1) {
      g2.setColor(PlotRenderTheme.WAVEFORM_RIGHT);
      g2.setStroke(PlotRenderTheme.THIN_TRACE_STROKE);
      g2.drawPolyline(xPoints, yPoints[1], rightRenderPoints);
    }

    int centerY = getHeight() / 2;
    g2.setColor(PlotRenderTheme.CENTER_LINE);
    g2.setStroke(PlotRenderTheme.AXIS_STROKE);
    g2.drawLine(0, centerY, getWidth() - 1, centerY);

    int tickEveryNSample = model.getTickEveryNSample();
    if (tickEveryNSample > 0) {
      for (int i = 0; i < points; i += tickEveryNSample) {
        if (i < xCount) {
          g2.drawLine(xPoints[i], centerY - 3, xPoints[i], centerY + 3);
        }
      }
    }

    drawLevelOverlay(g2, plotBounds);
  }

  /**
   * Render the latest triggered view. Returns {@code true} if a view was drawn (so the caller can
   * skip the free-running model path).
   */
  private boolean paintTriggeredWaveform(Graphics2D g2, Rectangle plotBounds) {
    AudioBlock block = getCurrentBlock();
    if (block != null
        && block.channels() > 0
        && block.frames() > 0
        && block.frameIndex() != lastTriggeredBlockFrameIndex) {
      trigger.process(block, 0).ifPresent(v -> lastTriggeredView = v);
      lastTriggeredBlockFrameIndex = block.frameIndex();
    }
    WaveformTrigger.TriggeredView view = lastTriggeredView;
    if (view == null || view.samplesView().length == 0) {
      PlotRenderTheme.drawEmptyState(g2, plotBounds, "Waiting for trigger...");
      return true;
    }
    float[] samples = view.samplesView();
    int n = samples.length;
    int width = Math.max(1, plotBounds.width);
    int height = Math.max(1, plotBounds.height);
    int centerY = plotBounds.y + height / 2;
    int amplitude = Math.max(1, height / 2 - 4);

    if (triggerXs.length != n) {
      triggerXs = new int[n];
      triggerYs = new int[n];
    }
    int[] xs = triggerXs;
    int[] ys = triggerYs;
    for (int i = 0; i < n; i++) {
      xs[i] = plotBounds.x + (int) ((long) i * (width - 1) / Math.max(1, n - 1));
      float clamped = Math.max(-1f, Math.min(1f, samples[i]));
      ys[i] = centerY - (int) (clamped * amplitude);
    }

    // Trigger level indicator.
    int levelY = centerY - (int) (Math.max(-1f, Math.min(1f, view.level())) * amplitude);
    g2.setColor(PlotRenderTheme.CENTER_LINE);
    g2.setStroke(PlotRenderTheme.AXIS_STROKE);
    g2.drawLine(plotBounds.x, levelY, plotBounds.x + width - 1, levelY);

    // Center line.
    g2.drawLine(plotBounds.x, centerY, plotBounds.x + width - 1, centerY);

    // Trace.
    g2.setColor(PlotRenderTheme.WAVEFORM_LEFT);
    g2.setStroke(PlotRenderTheme.TRACE_STROKE);
    g2.drawPolyline(xs, ys, n);

    // Status text.
    String status =
        String.format(
            "Trig: %s  Slope: %s  Level: %+.2f  %s",
            view.triggered() ? "FIRED" : "AUTO",
            view.slope() == WaveformTrigger.Slope.RISING ? "↑" : "↓",
            view.level(),
            view.triggered() ? "" : "(timeout)");
    g2.setFont(PlotRenderTheme.LABEL_FONT);
    g2.setColor(PlotRenderTheme.TEXT_MUTED);
    g2.drawString(status, plotBounds.x + 10, plotBounds.y + 32);
    return true;
  }

  private void drawLevelOverlay(Graphics2D g2, Rectangle plotBounds) {
    AudioBlock block = getCurrentBlock();
    if (block == null || block.frames() == 0) {
      return;
    }
    double sumSquares = 0.0d;
    double peak = 0.0d;
    long sampleCount = 0L;
    for (int channel = 0; channel < block.channels(); channel++) {
      float[] samples = block.channelView(channel);
      for (float sample : samples) {
        double abs = Math.abs(sample);
        peak = Math.max(peak, abs);
        sumSquares += sample * sample;
        sampleCount++;
      }
    }
    if (sampleCount == 0L) {
      return;
    }
    double rms = Math.sqrt(sumSquares / sampleCount);
    String overlay = String.format("RMS %.3f  Peak %.3f", rms, peak);
    int textWidth = g2.getFontMetrics(PlotRenderTheme.LABEL_FONT).stringWidth(overlay);
    int x = plotBounds.x + Math.max(8, plotBounds.width - textWidth - 12);
    int y = plotBounds.y + 18;
    g2.setFont(PlotRenderTheme.LABEL_FONT);
    g2.setColor(PlotRenderTheme.TEXT_MUTED);
    g2.drawString(overlay, x, y);
  }
}
