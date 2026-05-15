package org.hammer;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.hammer.audio.AudioCaptureService;
import org.hammer.audio.analysis.SpectrumSnapshot;
import org.hammer.audio.core.AudioBlock;
import org.hammer.audio.core.AudioFormatDescriptor;
import org.junit.jupiter.api.Test;

class SpectrumPanelTest {

  @Test
  void getCurrentSpectrum_reusesSnapshotForSameBlock() {
    AudioFormatDescriptor format = new AudioFormatDescriptor(16000.0f, 1, 16);
    AudioBlock firstBlock = AudioBlock.wrap(format, new float[][] {new float[1024]}, 42L, 100L);
    AudioBlock secondBlock = AudioBlock.wrap(format, new float[][] {new float[1024]}, 43L, 200L);

    AudioCaptureService service = mock(AudioCaptureService.class);
    when(service.getLatestBlock()).thenReturn(firstBlock, firstBlock, secondBlock);

    SpectrumPanel panel = new SpectrumPanel();
    panel.setAudioCaptureService(service);

    SpectrumSnapshot first = panel.getCurrentSpectrum();
    SpectrumSnapshot cached = panel.getCurrentSpectrum();
    SpectrumSnapshot changed = panel.getCurrentSpectrum();

    assertSame(first, cached);
    assertNotSame(first, changed);
  }
}
