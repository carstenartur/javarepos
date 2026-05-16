package org.hammer.audio.recording;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.hammer.audio.core.AudioBlock;
import org.hammer.audio.core.AudioFormatDescriptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AudioBlockRecordingRoundTripTest {

  private static final AudioFormatDescriptor STEREO_44K = new AudioFormatDescriptor(44100f, 2, 16);

  @Test
  void roundTripsBlocksWithSamplesIndexAndTimestamp(@TempDir Path dir) throws IOException {
    Path file = dir.resolve("rec.aar");
    AudioBlock b1 =
        new AudioBlock(
            STEREO_44K,
            new float[][] {{0.1f, 0.2f, 0.3f}, {-0.1f, -0.2f, -0.3f}},
            10L,
            1_000_000_000L);
    AudioBlock b2 =
        new AudioBlock(
            STEREO_44K, new float[][] {{0.4f, 0.5f}, {-0.4f, -0.5f}}, 13L, 2_000_000_000L);

    try (AudioBlockRecordingWriter w = AudioBlockRecordingWriter.open(file)) {
      w.write(b1);
      w.write(b2);
      assertEquals(2L, w.blocksWritten());
      assertEquals(STEREO_44K, w.format());
    }

    List<AudioBlock> read = AudioBlockRecordingReader.readAll(file);
    assertEquals(2, read.size());
    assertEquals(STEREO_44K, read.get(0).format());
    assertEquals(10L, read.get(0).frameIndex());
    assertEquals(1_000_000_000L, read.get(0).timestampNanos());
    assertArrayEquals(new float[] {0.1f, 0.2f, 0.3f}, read.get(0).channelView(0), 1e-6f);
    assertArrayEquals(new float[] {-0.1f, -0.2f, -0.3f}, read.get(0).channelView(1), 1e-6f);
    assertArrayEquals(new float[] {0.4f, 0.5f}, read.get(1).channelView(0), 1e-6f);
  }

  @Test
  void readerRejectsBadMagic(@TempDir Path dir) throws IOException {
    Path file = dir.resolve("garbage.aar");
    Files.write(file, new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15});
    IOException ex = assertThrows(IOException.class, () -> AudioBlockRecordingReader.readAll(file));
    assertTrue(ex.getMessage().contains("magic"));
  }

  @Test
  void writerRejectsFormatChange(@TempDir Path dir) throws IOException {
    Path file = dir.resolve("change.aar");
    AudioBlock b1 = new AudioBlock(STEREO_44K, new float[][] {{0f}, {0f}}, 0L, 0L);
    AudioBlock b2 =
        new AudioBlock(
            new AudioFormatDescriptor(48000f, 2, 16), new float[][] {{0f}, {0f}}, 1L, 0L);
    try (AudioBlockRecordingWriter w = AudioBlockRecordingWriter.open(file)) {
      w.write(b1);
      assertThrows(IllegalStateException.class, () -> w.write(b2));
    }
  }

  @Test
  void singleBlockRecordingHasExpectedSize(@TempDir Path dir) throws IOException {
    Path file = dir.resolve("single-block.aar");
    AudioBlock b1 = new AudioBlock(STEREO_44K, new float[][] {{0f}, {0f}}, 0L, 0L);
    try (AudioBlockRecordingWriter w = AudioBlockRecordingWriter.open(file)) {
      w.write(b1);
    }
    long size = Files.size(file);
    // Header (16 bytes) + 1 record:
    //   u32 frames + i64 frameIndex + i64 timestampNanos = 20 bytes header
    //   + 2 ch * 1 frame * 4 bytes float = 8 bytes samples
    //   => 28 bytes per record.
    assertEquals(AudioBlockRecordingFormat.HEADER_BYTES + 28L, size);
  }
}
