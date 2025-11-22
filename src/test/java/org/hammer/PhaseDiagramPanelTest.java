package org.hammer;

import org.junit.jupiter.api.Test;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple smoke test for PhaseDiagramPanel to ensure painting
 * doesn't throw exceptions in headless environment.
 */
class PhaseDiagramPanelTest {

    @Test
    void testPaintDoesNotThrow() {
        // Set headless mode for CI
        System.setProperty("java.awt.headless", "true");
        
        PhaseDiagramPanel panel = new PhaseDiagramPanel();
        panel.setSize(200, 100);
        
        // Create a BufferedImage to get a Graphics object for headless testing
        BufferedImage image = new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB);
        Graphics graphics = image.getGraphics();
        
        // This should not throw any exceptions
        assertDoesNotThrow(() -> {
            panel.paint(graphics);
        }, "paint should not throw exceptions");
        
        graphics.dispose();
    }

    @Test
    void testPaintWithNullService() {
        // Set headless mode for CI
        System.setProperty("java.awt.headless", "true");
        
        PhaseDiagramPanel panel = new PhaseDiagramPanel();
        panel.setSize(200, 100);
        
        // Don't set any service (null by default)
        BufferedImage image = new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB);
        Graphics graphics = image.getGraphics();
        
        // Should handle null service gracefully
        assertDoesNotThrow(() -> {
            panel.paint(graphics);
        }, "paint should handle null service gracefully");
        
        graphics.dispose();
    }
}
