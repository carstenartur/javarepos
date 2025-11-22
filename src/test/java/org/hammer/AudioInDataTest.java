package org.hammer;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import javax.swing.JPanel;

public class AudioInDataTest {

    @Test
    void recomputeXValues_distributesEvenly() {
        AudioInData instance = new AudioInData();
        instance.numberOfPoints = 5;
        JPanel panel = new JPanel();
        panel.setSize(200, 100);
        instance.panel = panel;

        instance.recomputeXValues();
        int[] x = instance.getXPointsSnapshot();

        assertEquals(5, x.length);
        int width = 200;
        for (int i = 0; i < 5; i++) {
            int expected = Math.round((width - 1) * ((float) i / (5 - 1)));
            assertEquals(expected, x[i], "x[" + i + "] mismatch");
        }
    }

    @Test
    void getXPointsSnapshot_returnsDefensiveCopy() {
        AudioInData instance = new AudioInData();
        instance.numberOfPoints = 3;
        JPanel panel = new JPanel();
        panel.setSize(100, 50);
        instance.panel = panel;

        instance.recomputeXValues();
        int[] a = instance.getXPointsSnapshot();
        int[] b = instance.getXPointsSnapshot();

        a[0] = a[0] + 9999;
        assertNotEquals(a[0], b[0], "Expected defensive copy: mutation of returned array should not affect subsequent snapshot");
    }
}
