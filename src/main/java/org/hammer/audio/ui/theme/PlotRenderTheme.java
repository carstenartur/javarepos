package org.hammer.audio.ui.theme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

/** Shared plot colors, fonts and drawing helpers for Swing renderers. */
public final class PlotRenderTheme {

  public static final Color PANEL_BACKGROUND = new Color(24, 27, 32);
  public static final Color PLOT_BACKGROUND = new Color(18, 21, 27);
  public static final Color GRID_MAJOR = new Color(88, 102, 125, 70);
  public static final Color GRID_MINOR = new Color(88, 102, 125, 35);
  public static final Color AXIS_COLOR = new Color(118, 134, 160, 120);
  public static final Color TEXT_PRIMARY = new Color(222, 226, 234);
  public static final Color TEXT_MUTED = new Color(159, 171, 191);
  public static final Color CENTER_LINE = new Color(129, 110, 189, 120);
  public static final Color WAVEFORM_LEFT = new Color(104, 188, 255, 210);
  public static final Color WAVEFORM_RIGHT = new Color(102, 235, 187, 180);
  public static final Color SPECTRUM_FILL = new Color(78, 152, 255, 95);
  public static final Color SPECTRUM_LINE = new Color(120, 188, 255, 220);
  public static final Color HIGHLIGHT = new Color(255, 164, 91, 210);
  public static final Color PHASE_TRACE = new Color(132, 198, 255, 145);

  public static final Font LABEL_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
  public static final Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 12);

  public static final BasicStroke GRID_STROKE = new BasicStroke(1f);
  public static final BasicStroke AXIS_STROKE = new BasicStroke(1.2f);
  public static final BasicStroke TRACE_STROKE = new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
  public static final BasicStroke THIN_TRACE_STROKE = new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
  public static final BasicStroke PEAK_STROKE = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {4f, 4f}, 0f);

  private static final double DB_FLOOR = -80.0d;
  private static final double MAG_EPSILON = 1.0e-7d;

  private PlotRenderTheme() {}

  /** Enables high quality 2D rendering hints. */
  public static void applyQualityRendering(Graphics2D g2) {
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
    g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
  }

  /** Draws panel and plot backgrounds. */
  public static void drawPlotBackground(Graphics2D g2, int width, int height, Rectangle plotBounds) {
    g2.setColor(PANEL_BACKGROUND);
    g2.fillRect(0, 0, width, height);
    g2.setColor(PLOT_BACKGROUND);
    g2.fillRect(plotBounds.x, plotBounds.y, plotBounds.width, plotBounds.height);
  }

  /** Draws evenly spaced major/minor grid lines and a border around plot bounds. */
  public static void drawGrid(Graphics2D g2, Rectangle plotBounds, int verticalSteps, int horizontalSteps) {
    if (plotBounds.width <= 1 || plotBounds.height <= 1) {
      return;
    }
    int safeV = Math.max(1, verticalSteps);
    int safeH = Math.max(1, horizontalSteps);

    g2.setStroke(GRID_STROKE);
    for (int i = 1; i < safeV; i++) {
      int x = plotBounds.x + (int) Math.round(i * (plotBounds.width - 1.0) / safeV);
      g2.setColor(i % 2 == 0 ? GRID_MAJOR : GRID_MINOR);
      g2.drawLine(x, plotBounds.y, x, plotBounds.y + plotBounds.height - 1);
    }
    for (int i = 1; i < safeH; i++) {
      int y = plotBounds.y + (int) Math.round(i * (plotBounds.height - 1.0) / safeH);
      g2.setColor(i % 2 == 0 ? GRID_MAJOR : GRID_MINOR);
      g2.drawLine(plotBounds.x, y, plotBounds.x + plotBounds.width - 1, y);
    }

    g2.setColor(AXIS_COLOR);
    g2.setStroke(AXIS_STROKE);
    g2.drawRect(plotBounds.x, plotBounds.y, plotBounds.width - 1, plotBounds.height - 1);
  }

  /** Draws an empty-state message centered in plot bounds. */
  public static void drawEmptyState(Graphics2D g2, Rectangle plotBounds, String message) {
    g2.setFont(LABEL_FONT);
    g2.setColor(TEXT_MUTED);
    int textWidth = g2.getFontMetrics().stringWidth(message);
    int x = plotBounds.x + Math.max(6, (plotBounds.width - textWidth) / 2);
    int y = plotBounds.y + plotBounds.height / 2;
    g2.drawString(message, x, y);
  }

  /** Draws top-left title text. */
  public static void drawTitle(Graphics2D g2, int x, int y, String title) {
    g2.setFont(TITLE_FONT);
    g2.setColor(TEXT_PRIMARY);
    g2.drawString(title, x, y);
  }

  /** Draws label text. */
  public static void drawLabel(Graphics2D g2, int x, int y, String label) {
    g2.setFont(LABEL_FONT);
    g2.setColor(TEXT_MUTED);
    g2.drawString(label, x, y);
  }

  /** Converts linear magnitude to clipped dB FS display value. */
  public static double magnitudeToDb(float magnitude) {
    double db = 20.0d * Math.log10(Math.max(MAG_EPSILON, magnitude));
    return Math.max(DB_FLOOR, db);
  }

  /** Maps dB value into a [0, 1] normalized range based on configured dB floor. */
  public static double normalizedDb(double db) {
    return Math.max(0d, Math.min(1d, (db - DB_FLOOR) / Math.abs(DB_FLOOR)));
  }
}
