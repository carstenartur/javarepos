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
    when(svc.getLatestModel()).thenReturn(WaveformModel.EMPTY);

    panel.setAudioCaptureService(svc);

    verify(svc, atLeastOnce()).recomputeLayout(120, 80);
  }

  @Test
  void resizing_triggersRecompute() throws Exception {
    WaveformPanel panel = new WaveformPanel();
    panel.setSize(200, 100);

    AudioCaptureService svc = mock(AudioCaptureService.class);
    when(svc.getLatestModel()).thenReturn(WaveformModel.EMPTY);

    panel.setAudioCaptureService(svc);

    // Clear any previous invocations from initial layout computation
    clearInvocations(svc);

    // Perform resize on the EDT and dispatch an artificial ComponentEvent to simulate a real
    // resize.
    SwingUtilities.invokeAndWait(
        () -> {
          panel.setSize(300, 150);
          panel.dispatchEvent(new ComponentEvent(panel, ComponentEvent.COMPONENT_RESIZED));
        });

    // Verify recomputeLayout invoked with new dimensions
    verify(svc, atLeastOnce()).recomputeLayout(300, 150);
  }
}