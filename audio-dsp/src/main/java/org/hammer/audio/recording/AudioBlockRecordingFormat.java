package org.hammer.audio.recording;

/**
 * On-disk format for {@link org.hammer.audio.core.AudioBlock} recordings written by {@link
 * AudioBlockRecordingWriter} and read by {@link AudioBlockRecordingReader}.
 *
 * <p>Layout (all integers big-endian, IEEE-754 floats):
 *
 * <pre>
 * Header
 *   u32  magic                          = {@value #MAGIC}
 *   u16  version                        = {@value #VERSION}
 *   u16  channels
 *   f32  sampleRate
 *   u16  sourceSampleSizeInBits
 *   u16  reserved                       = 0
 *
 * Frame records (repeated until EOF)
 *   u32  frames
 *   i64  frameIndex
 *   i64  timestampNanos
 *   frames * channels * f32 samples     (non-interleaved: ch0 frames, then ch1 frames, ...)
 * </pre>
 *
 * <p>The format is intentionally simple — it is not a substitute for WAV/AIFF. It exists so the
 * application can faithfully round-trip normalized {@code float} blocks with their original frame
 * index and timestamp metadata for deterministic replay.
 */
public final class AudioBlockRecordingFormat {

  /** Magic header value ({@code 'A','A','R','1'} = "AAR1"). */
  public static final int MAGIC = 0x41415231;

  /** Current format version. */
  public static final int VERSION = 1;

  /** Header size in bytes. */
  public static final int HEADER_BYTES = 4 + 2 + 2 + 4 + 2 + 2;

  private AudioBlockRecordingFormat() {}
}
