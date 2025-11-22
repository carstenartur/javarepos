package org.hammer;

import static org.mockito.Mockito.*;

import java.awt.event.ComponentEvent;
import javax.swing.SwingUtilities;
import org.hammer.audio.AudioCaptureService;
import org.hammer.audio.WaveformModel;
import org.junit.jupiter.api.Test;

class WaveformPanelTest {

  @Test
  void setAudioCaptureService_triggersInitialRecompute() {
    WaveformPanel panel = new WaveformPanel();
    panel.setSize(120, 80);

    AudioCaptureService svc = mock(AudioCaptureService.class);
    when(svc.getLatestModel()).thenReturn(new WaveformModel(new int[0], new int[0][], 0, 0));

    panel.setAudioCaptureService(svc);

    verify(svc, atLeastOnce()).recomputeLayout(120, 80);
  }

  @Test
  void resizing_triggersRecompute() throws Exception {
    WaveformPanel panel = new WaveformPanel();
    panel.setSize(200, 100);

    AudioCaptureService svc = mock(AudioCaptureService.class);
    when(svc.getLatestModel()).thenReturn(new WaveformModel(new int[0], new int[0][], 0, 0));

    panel.setAudioCaptureService(svc);

    // Clear any previous invocations
    clearInvocations(svc);

    // Resize the panel on EDT and manually dispatch resize event
    SwingUtilities.invokeAndWait(
        () -> {
          panel.setSize(300, 150);
          // Manually dispatch ComponentEvent.COMPONENT_RESIZED to trigger componentResized
          panel.dispatchEvent(new ComponentEvent(panel, ComponentEvent.COMPONENT_RESIZED));
        });

    // Verify that recomputeLayout was called with new dimensions
    verify(svc, atLeastOnce()).recomputeLayout(300, 150);
  }
}
