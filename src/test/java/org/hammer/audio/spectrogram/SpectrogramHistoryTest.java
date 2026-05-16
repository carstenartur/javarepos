package org.hammer.audio.spectrogram;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SpectrogramHistoryTest {

  private static SpectrogramFrame frame(long index, float sampleRate, int fftSize, float v) {
    float[] m = new float[fftSize / 2 + 1];
    java.util.Arrays.fill(m, v);
    return new SpectrogramFrame(index, index * 100L, sampleRate, fftSize, m);
  }

  @Test
  void capacity_evictsOldest() {
    SpectrogramHistory h = new SpectrogramHistory(3);
    h.append(frame(1, 16000f, 8, 0.1f));
    h.append(frame(2, 16000f, 8, 0.2f));
    h.append(frame(3, 16000f, 8, 0.3f));
    h.append(frame(4, 16000f, 8, 0.4f));

    assertEquals(3, h.size());
    assertEquals(2L, h.frameAt(0).sourceFrameIndex());
    assertEquals(4L, h.frameAt(2).sourceFrameIndex());
    assertEquals(4L, h.latest().sourceFrameIndex());
  }

  @Test
  void changingFftSize_resetsHistory() {
    SpectrogramHistory h = new SpectrogramHistory(4);
    h.append(frame(1, 16000f, 8, 0.1f));
    h.append(frame(2, 16000f, 8, 0.1f));
    h.append(frame(3, 22050f, 8, 0.2f));

    assertEquals(1, h.size());
    assertEquals(3L, h.latest().sourceFrameIndex());
  }

  @Test
  void clear_emptiesHistory() {
    SpectrogramHistory h = new SpectrogramHistory(2);
    h.append(frame(1, 16000f, 8, 0.1f));
    h.clear();
    assertTrue(h.isEmpty());
    assertNull(h.latest());
    assertEquals(-1, h.binCount());
  }

  @Test
  void snapshot_returnsImmutableOrderedCopy() {
    SpectrogramHistory h = new SpectrogramHistory(4);
    h.append(frame(1, 16000f, 8, 0.1f));
    h.append(frame(2, 16000f, 8, 0.2f));

    var snap = h.snapshot();
    assertEquals(2, snap.size());
    assertEquals(1L, snap.get(0).sourceFrameIndex());
    assertEquals(2L, snap.get(1).sourceFrameIndex());
    assertThrows(UnsupportedOperationException.class, () -> snap.add(snap.get(0)));
  }
}
