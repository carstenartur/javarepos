package org.hammer;

import org.junit.jupiter.api.Test;
import org.hammer.audio.WaveformModel;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for WaveformModel to ensure defensive copies are returned
 * and that getNumberOfPoints() is correct.
 */
class WaveformModelTest {

    @Test
    void testGetXPointsReturnsDefensiveCopy() {
        int[] xPoints = {10, 20, 30, 40, 50};
        int[][] yPoints = {{15, 25, 35, 45, 55}};
        
        WaveformModel model = new WaveformModel(xPoints, yPoints, 10, 1024);
        
        // Get x points twice
        int[] firstCopy = model.getXPoints();
        int[] secondCopy = model.getXPoints();
        
        // Modify first copy
        firstCopy[0] = 9999;
        
        // Second copy should not be affected
        assertNotEquals(9999, secondCopy[0], "Expected defensive copy: mutation of returned array should not affect subsequent calls");
        assertEquals(10, secondCopy[0], "Second copy should have original value");
    }

    @Test
    void testGetYPointsReturnsDefensiveCopy() {
        int[] xPoints = {10, 20, 30};
        int[][] yPoints = {{15, 25, 35}, {18, 28, 38}};
        
        WaveformModel model = new WaveformModel(xPoints, yPoints, 5, 512);
        
        // Get y points twice
        int[][] firstCopy = model.getYPoints();
        int[][] secondCopy = model.getYPoints();
        
        // Modify first copy
        firstCopy[0][0] = 8888;
        
        // Second copy should not be affected
        assertNotEquals(8888, secondCopy[0][0], "Expected defensive copy: mutation of returned array should not affect subsequent calls");
        assertEquals(15, secondCopy[0][0], "Second copy should have original value");
    }

    @Test
    void testGetNumberOfPointsReturnsCorrectValue() {
        int[] xPoints = {10, 20, 30, 40, 50};
        int[][] yPoints = {{15, 25, 35, 45, 55}};
        
        WaveformModel model = new WaveformModel(xPoints, yPoints, 10, 1024);
        
        assertEquals(5, model.getNumberOfPoints(), "Number of points should match xPoints length");
    }

    @Test
    void testGetNumberOfPointsWithEmptyArray() {
        int[] xPoints = {};
        int[][] yPoints = {{}};
        
        WaveformModel model = new WaveformModel(xPoints, yPoints, 10, 1024);
        
        assertEquals(0, model.getNumberOfPoints(), "Number of points should be 0 for empty array");
    }

    @Test
    void testGetNumberOfPointsWithNullArray() {
        WaveformModel model = new WaveformModel(null, null, 10, 1024);
        
        assertEquals(0, model.getNumberOfPoints(), "Number of points should be 0 for null array");
    }
}
