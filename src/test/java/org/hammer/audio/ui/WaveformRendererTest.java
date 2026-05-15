package org.hammer.audio.ui;

import static org.junit.jupiter.api.Assertions.*;

import org.hammer.audio.snapshot.WaveformSnapshot;
import org.junit.jupiter.api.Test;

class WaveformRendererTest {

  @Test
  void computeXPoints_spans_panel_width() {
    int[] xs = WaveformRenderer.computeXPoints(10, 100);
    assertEquals(10, xs.length);
    assertEquals(0, xs[0]);
    assertEquals(99, xs[9]);
    for (int i = 1; i < xs.length; i++) {
      assertTrue(xs[i] >= xs[i - 1], "x-values should be monotonically non-decreasing");
    }
  }

  @Test
  void computeXPoints_handles_edge_cases() {
    assertEquals(0, WaveformRenderer.computeXPoints(0, 100).length);
    assertArrayEquals(new int[] {0}, WaveformRenderer.computeXPoints(1, 100));
  }

  @Test
  void computeYPoints_maps_minus_one_to_bottom_and_plus_one_to_top() {
    float[] samples = {1f, 0f, -1f};
    WaveformSnapshot snap = WaveformSnapshot.wrap(new float[][] {samples}, 48000f, 0L, 0L);
    int[] ys = WaveformRenderer.computeYPoints(snap, 0, 200);
    // +1 -> 0 (top), 0 -> 100 (centre), -1 -> 200 (bottom)
    assertEquals(0, ys[0]);
    assertEquals(100, ys[1]);
    assertEquals(200, ys[2]);
  }

  @Test
  void computeYPoints_clips_out_of_range_samples() {
    float[] samples = {2f, -3f}; // out-of-range values
    WaveformSnapshot snap = WaveformSnapshot.wrap(new float[][] {samples}, 48000f, 0L, 0L);
    int[] ys = WaveformRenderer.computeYPoints(snap, 0, 200);
    assertEquals(0, ys[0]);
    assertEquals(200, ys[1]);
  }

  @Test
  void computeYPointsAllChannels_maps_each_channel() {
    float[] left = {1f, -1f};
    float[] right = {-1f, 1f};
    WaveformSnapshot snap = WaveformSnapshot.wrap(new float[][] {left, right}, 48000f, 0L, 0L);
    int[][] result = WaveformRenderer.computeYPointsAllChannels(snap, 100);
    assertEquals(2, result.length);
    assertEquals(0, result[0][0]); // top
    assertEquals(100, result[0][1]); // bottom
    assertEquals(100, result[1][0]); // bottom
    assertEquals(0, result[1][1]); // top
  }

  @Test
  void computeYPoints_returns_empty_for_invalid_channel() {
    WaveformSnapshot snap = WaveformSnapshot.wrap(new float[][] {{0.5f}}, 48000f, 0L, 0L);
    assertEquals(0, WaveformRenderer.computeYPoints(snap, -1, 100).length);
    assertEquals(0, WaveformRenderer.computeYPoints(snap, 1, 100).length);
    assertEquals(0, WaveformRenderer.computeYPoints(snap, 0, 0).length);
  }
}
