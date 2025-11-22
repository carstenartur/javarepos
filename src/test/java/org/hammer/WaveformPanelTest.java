package org.hammer;

import static org.mockito.Mockito.*;

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
  void resizing_triggersRecompute() {
    WaveformPanel panel = new WaveformPanel();
    panel.setSize(200, 100);

    AudioCaptureService svc = mock(AudioCaptureService.class);
    when(svc.getLatestModel()).thenReturn(new WaveformModel(new int[0], new int[0][], 0, 0));

    panel.setAudioCaptureService(svc);

    // Clear any previous invocations
    clearInvocations(svc);

    // Resize the panel
    panel.setSize(300, 150);

    // Verify that recomputeLayout was called with new dimensions
    verify(svc, atLeastOnce()).recomputeLayout(300, 150);
  }
}
