package org.hammer;

import org.junit.jupiter.api.Test;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

class PhaseDiagramPanelTest {

    @Test
    void paintComponent_noData_doesNotThrow() {
        PhaseDiagramPanel panel = new PhaseDiagramPanel();
        panel.setSize(100, 80);

        BufferedImage img = new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics g = img.getGraphics();

        panel.paint(g); // should not throw

        g.dispose();
    }
}
