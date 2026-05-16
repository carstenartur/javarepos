package org.hammer.audio.ui.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
