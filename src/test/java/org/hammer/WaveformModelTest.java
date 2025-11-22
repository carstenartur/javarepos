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

  @Test
  void getYPointsForChannel_returns_defensive_copy() {
    int[] x = new int[] {0, 10, 20};
    int[][] y = new int[][] {{1, 2, 3}, {4, 5, 6}};
    WaveformModel m = new WaveformModel(x, y, 1, 1024);

    int[] channel0 = m.getYPointsForChannel(0);
    assertEquals(1, channel0[0]);

    // Modify returned array should not affect model
    channel0[0] = 999;
    int[] channel0Again = m.getYPointsForChannel(0);
    assertEquals(1, channel0Again[0]);
  }

  @Test
  void getYPointsForChannel_returns_empty_array_for_invalid_channel() {
    int[] x = new int[] {0, 10, 20};
    int[][] y = new int[][] {{1, 2, 3}, {4, 5, 6}};
    WaveformModel m = new WaveformModel(x, y, 1, 1024);

    // Invalid channels should return empty arrays
    int[] invalidNegative = m.getYPointsForChannel(-1);
    assertEquals(0, invalidNegative.length);

    int[] invalidTooHigh = m.getYPointsForChannel(2);
    assertEquals(0, invalidTooHigh.length);

    int[] invalidWayTooHigh = m.getYPointsForChannel(100);
    assertEquals(0, invalidWayTooHigh.length);
  }

  @Test
  void empty_constant_has_expected_properties() {
    WaveformModel empty = WaveformModel.EMPTY;

    assertEquals(0, empty.getChannelCount(), "EMPTY should have 0 channels");
    assertEquals(0, empty.getNumberOfPoints(), "EMPTY should have 0 points");
    assertEquals(0, empty.getDataSize(), "EMPTY should have data size of 0");
    assertEquals(0, empty.getTickEveryNSample(), "EMPTY should have tick interval of 0");
  }

  @Test
  void empty_constant_returns_empty_arrays() {
    WaveformModel empty = WaveformModel.EMPTY;

    int[] xPoints = empty.getXPoints();
    int[][] yPoints = empty.getYPoints();

    assertEquals(0, xPoints.length, "EMPTY xPoints should be empty array");
    assertEquals(0, yPoints.length, "EMPTY yPoints should be empty array");
  }
}
