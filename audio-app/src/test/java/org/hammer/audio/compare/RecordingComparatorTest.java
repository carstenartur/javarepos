package org.hammer.audio.compare;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.hammer.audio.core.AudioBlock;
import org.hammer.audio.core.AudioFormatDescriptor;
import org.hammer.audio.signal.SineGenerator;
import org.hammer.audio.signal.SquareGenerator;
import org.junit.jupiter.api.Test;

class RecordingComparatorTest {

  private static final AudioFormatDescriptor MONO_44K = new AudioFormatDescriptor(44100f, 1, 16);

  private static List<AudioBlock> recordSine(double freq, int blockCount) {
    SineGenerator gen = new SineGenerator(MONO_44K, freq, 0.5f);
    List<AudioBlock> blocks = new ArrayList<>();
    for (int i = 0; i < blockCount; i++) {
      blocks.add(gen.nextBlock(2048));
    }
    return blocks;
  }

  private static List<AudioBlock> recordSquare(double freq, int blockCount) {
    SquareGenerator gen = new SquareGenerator(MONO_44K, freq, 0.5f);
    List<AudioBlock> blocks = new ArrayList<>();
    for (int i = 0; i < blockCount; i++) {
      blocks.add(gen.nextBlock(2048));
    }
    return blocks;
  }

  @Test
  void detectsDifferentDominantFrequencies() {
    List<AudioBlock> a = recordSine(440.0, 8);
    List<AudioBlock> b = recordSine(880.0, 8);
    ComparisonReport report = new RecordingComparator().compareBlocks(a, b, "440Hz", "880Hz");

    assertEquals("440Hz", report.a().label());
    assertEquals("880Hz", report.b().label());
    double aFreq = report.a().measurement().dominantFrequencyHz();
    double bFreq = report.b().measurement().dominantFrequencyHz();
    assertTrue(Math.abs(aFreq - 440.0) < 50.0, "A dominant freq off: " + aFreq);
    assertTrue(Math.abs(bFreq - 880.0) < 50.0, "B dominant freq off: " + bFreq);

    String md = new MarkdownComparisonReportRenderer().render(report);
    assertTrue(md.contains("# A/B comparison report"));
    assertTrue(md.contains("440Hz"));
    assertTrue(md.contains("880Hz"));
    assertTrue(md.contains("Dominant freq"));
  }

  @Test
  void rendersTimbreDifferenceInCentroid() {
    List<AudioBlock> sine = recordSine(440.0, 8);
    List<AudioBlock> square = recordSquare(440.0, 8);
    ComparisonReport report =
        new RecordingComparator().compareBlocks(sine, square, "sine", "square");
    String md = new MarkdownComparisonReportRenderer().render(report);
    assertTrue(md.contains("Spectral centroid"));
  }

  @Test
  void rejectsEmptyInputs() {
    RecordingComparator c = new RecordingComparator();
    List<AudioBlock> ok = recordSine(440.0, 1);
    assertThrows(IllegalArgumentException.class, () -> c.compareBlocks(List.of(), ok, "a", "b"));
    assertThrows(IllegalArgumentException.class, () -> c.compareBlocks(ok, List.of(), "a", "b"));
  }

  @Test
  void rejectsBadFftSize() {
    assertThrows(IllegalArgumentException.class, () -> new RecordingComparator(0));
    assertThrows(IllegalArgumentException.class, () -> new RecordingComparator(100));
  }
}
