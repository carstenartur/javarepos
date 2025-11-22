package org.hammer;

import static org.junit.jupiter.api.Assertions.*;

import org.hammer.audio.WaveformModel;
import org.junit.jupiter.api.Test;

class WaveformModelTest {

  @Test
  void constructor_and_getters_make_defensive_copies() {
    int[] x = new int[] {0, 10, 20};
    int[][] y = new int[][] {{1, 2, 3}, {4, 5, 6}};
    WaveformModel m = new WaveformModel(x, y, 1, 1024);

    // modify originals should not affect model
    x[0] = 999;
    y[0][0] = 999;

    int[] xFromModel = m.getXPoints();
    int[][] yFromModel = m.getYPoints();

    assertEquals(3, m.getNumberOfPoints());
    assertEquals(0, xFromModel[0]);
    assertEquals(1, yFromModel[0][0]);

    // altering returned arrays must not change subsequent calls
    xFromModel[0] = -1;
    int[] xFromModel2 = m.getXPoints();
    assertEquals(0, xFromModel2[0]);
  }
}
