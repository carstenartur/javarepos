package org.hammer;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.GraphicsEnvironment;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

/**
 * Lightweight smoke test for AudioAnalyseFrame GUI construction.
 *
 * <p>This test verifies that the frame can be instantiated without exceptions in headless
 * environments. The test is automatically skipped in truly headless environments.
 *
 * @author refactoring
 */
class GuiSmokeTest {

  @Test
  void audioAnalyseFrame_constructs_without_exceptions() throws Exception {
    // Skip test if running in headless environment
    if (GraphicsEnvironment.isHeadless()) {
      System.out.println("Skipping GUI smoke test in headless environment");
      return;
    }

    final Exception[] exceptions = new Exception[1];
    final AudioAnalyseFrame[] frames = new AudioAnalyseFrame[1];

    // Run on EDT
    SwingUtilities.invokeAndWait(
        () -> {
          try {
            frames[0] = new AudioAnalyseFrame();
            frames[0].pack();
          } catch (Exception e) {
            exceptions[0] = e;
          }
        });

    // Cleanup
    if (frames[0] != null) {
      SwingUtilities.invokeAndWait(
          () -> {
            frames[0].dispose();
          });
    }

    // Assert no exceptions occurred
    if (exceptions[0] != null) {
      throw exceptions[0];
    }

    assertNotNull(frames[0], "Frame should have been created");
  }
}
