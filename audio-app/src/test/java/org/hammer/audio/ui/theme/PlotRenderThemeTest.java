package org.hammer.audio.ui.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

class PlotRenderThemeTest {

  @Test
  void magnitudeToDb_appliesFloorForVerySmallValues() {
    double db = PlotRenderTheme.magnitudeToDb(0f);
    assertEquals(-80.0d, db, 1e-9);
  }

  @Test
  void magnitudeToDb_scalesExpectedReferenceValues() {
    assertEquals(0.0d, PlotRenderTheme.magnitudeToDb(1f), 1e-6);
    assertEquals(-20.0d, PlotRenderTheme.magnitudeToDb(0.1f), 1e-6);
  }

  @Test
  void normalizedDb_clampsToZeroAndOne() {
    assertEquals(0.0d, PlotRenderTheme.normalizedDb(-120.0d), 1e-9);
    assertEquals(0.0d, PlotRenderTheme.normalizedDb(-80.0d), 1e-9);
    assertEquals(1.0d, PlotRenderTheme.normalizedDb(0.0d), 1e-9);
    assertTrue(PlotRenderTheme.normalizedDb(-40.0d) > 0.4d);
    assertTrue(PlotRenderTheme.normalizedDb(-40.0d) < 0.6d);
  }

  @Test
  void tickLabelsRenderInsideReservedMargins() {
    BufferedImage image = new BufferedImage(240, 160, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = image.createGraphics();
    Rectangle plot = new Rectangle(56, 24, 140, 90);
    try {
      PlotRenderTheme.applyQualityRendering(g);
      PlotRenderTheme.drawPlotBackground(g, image.getWidth(), image.getHeight(), plot);
      PlotRenderTheme.drawXTicks(
          g, plot, new double[] {0.0d, 0.5d, 1.0d}, new String[] {"0 Hz", "11 kHz", "22 kHz"});
      PlotRenderTheme.drawYTicks(
          g, plot, new double[] {0.0d, 0.5d, 1.0d}, new String[] {"0 dB", "-40 dB", "-80 dB"});
    } finally {
      g.dispose();
    }

    assertTrue(hasNonBackgroundPixel(image, 0, 0, plot.x, image.getHeight()));
    assertTrue(
        hasNonBackgroundPixel(
            image,
            0,
            plot.y + plot.height,
            image.getWidth(),
            image.getHeight() - plot.y - plot.height));
  }

  @Test
  void tickRendererRejectsMismatchedLabels() {
    BufferedImage image = new BufferedImage(120, 80, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = image.createGraphics();
    try {
      Rectangle plot = new Rectangle(32, 16, 72, 40);
      assertThrows(
          IllegalArgumentException.class,
          () -> PlotRenderTheme.drawXTicks(g, plot, new double[] {0.0d}, new String[] {"0", "1"}));
      assertThrows(
          IllegalArgumentException.class,
          () -> PlotRenderTheme.drawYTicks(g, plot, new double[] {0.0d}, new String[] {"0", "1"}));
    } finally {
      g.dispose();
    }
  }

  private static boolean hasNonBackgroundPixel(
      BufferedImage image, int startX, int startY, int width, int height) {
    int background = PlotRenderTheme.PANEL_BACKGROUND.getRGB() & 0x00ffffff;
    for (int y = startY; y < startY + height; y++) {
      for (int x = startX; x < startX + width; x++) {
        if ((image.getRGB(x, y) & 0x00ffffff) != background) {
          return true;
        }
      }
    }
    return false;
  }
}
