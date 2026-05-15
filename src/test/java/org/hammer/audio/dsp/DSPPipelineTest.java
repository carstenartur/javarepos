package org.hammer.audio.dsp;

import static org.junit.jupiter.api.Assertions.*;

import org.hammer.audio.core.AudioBlock;
import org.hammer.audio.core.AudioFormatDescriptor;
import org.junit.jupiter.api.Test;

class DSPPipelineTest {

  private static final AudioFormatDescriptor MONO = new AudioFormatDescriptor(48000f, 1, 16);

  private static AudioBlock blockOf(float... samples) {
    return AudioBlock.wrap(MONO, new float[][] {samples}, 0L, 0L);
  }

  @Test
  void empty_pipeline_passes_through() {
    DSPPipeline pipeline = DSPPipeline.empty();
    AudioBlock input = blockOf(1f, 2f, 3f);
    assertSame(input, pipeline.process(input));
  }

  @Test
  void identity_pipeline_passes_through() {
    DSPPipeline pipeline = DSPPipeline.of(DSPProcessor.identity());
    AudioBlock input = blockOf(1f, 2f, 3f);
    assertSame(input, pipeline.process(input));
  }

  @Test
  void stages_apply_in_order() {
    DSPProcessor doubleIt =
        block -> {
          float[] s = block.channelView(0);
          float[] out = new float[s.length];
          for (int i = 0; i < s.length; i++) {
            out[i] = s[i] * 2f;
          }
          return AudioBlock.wrap(
              block.format(), new float[][] {out}, block.frameIndex(), block.timestampNanos());
        };
    DSPProcessor addOne =
        block -> {
          float[] s = block.channelView(0);
          float[] out = new float[s.length];
          for (int i = 0; i < s.length; i++) {
            out[i] = s[i] + 1f;
          }
          return AudioBlock.wrap(
              block.format(), new float[][] {out}, block.frameIndex(), block.timestampNanos());
        };

    DSPPipeline pipeline = DSPPipeline.of(doubleIt, addOne);
    AudioBlock result = pipeline.process(blockOf(1f, 2f, 3f));

    // Expected: ((1*2)+1, (2*2)+1, (3*2)+1) = (3, 5, 7)
    assertArrayEquals(new float[] {3f, 5f, 7f}, result.channelView(0));
  }

  @Test
  void rejects_null_stage() {
    assertThrows(NullPointerException.class, () -> DSPPipeline.of(new DSPProcessor[] {null}));
  }

  @Test
  void rejects_processor_returning_null() {
    DSPPipeline pipeline = DSPPipeline.of(block -> null);
    assertThrows(NullPointerException.class, () -> pipeline.process(blockOf(1f)));
  }

  @Test
  void size_and_stages_reflect_construction() {
    DSPProcessor a = DSPProcessor.identity();
    DSPProcessor b = DSPProcessor.identity();
    DSPPipeline pipeline = DSPPipeline.of(a, b);
    assertEquals(2, pipeline.size());
    assertEquals(2, pipeline.stages().size());
  }
}
