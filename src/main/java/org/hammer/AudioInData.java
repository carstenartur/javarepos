package org.hammer;

import javax.swing.JPanel;

public class AudioInData {
    public static final AudioInData INSTANCE = new AudioInData();

    public JPanel panel;
    public int numberOfPoints = 0;
    private int[] xPoints = new int[0];

    public AudioInData() {
        this.panel = new JPanel();
    }

    public void recomputeXValues() {
        int n = numberOfPoints;
        if (n <= 0) {
            xPoints = new int[0];
            return;
        }
        int width = panel.getWidth();
        if (width <= 0) width = 1;
        xPoints = new int[n];
        if (n == 1) {
            xPoints[0] = 0;
            return;
        }
        for (int i = 0; i < n; i++) {
            xPoints[i] = Math.round((width - 1) * ((float) i / (n - 1)));
        }
    }

    public int[] getXPointsSnapshot() {
        return xPoints.clone();
    }
}
