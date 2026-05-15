package org.hammer.audio.dsp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.hammer.audio.core.AudioBlock;

/**
 * Composable chain of {@link DSPProcessor} stages.
 *
 * <p>A pipeline applies its processors in declared order, threading the output of each stage into
 * the next:
 *
 * <pre>{@code
 * DSPPipeline pipeline = DSPPipeline.of(
 *     new GainProcessor(0.5f),
 *     new DCBlockProcessor(),
 *     new HighPassProcessor(80.0f));
 * AudioBlock processed = pipeline.process(rawBlock);
 * }</pre>
 *
 * <p>The pipeline itself is immutable: stages are captured at construction time. To change the
 * topology at runtime, build a new pipeline.
 *
 * @author refactoring
 */
public final class DSPPipeline implements DSPProcessor {

  private final DSPProcessor[] stages;

  private DSPPipeline(DSPProcessor[] stages) {
    this.stages = stages;
  }

  /**
   * Create a new pipeline from an explicit list of stages.
   *
   * @param stages ordered list of processors; must not be {@code null} and must not contain {@code
   *     null} entries
   * @return a new pipeline
   */
  public static DSPPipeline of(DSPProcessor... stages) {
    Objects.requireNonNull(stages, "stages");
    DSPProcessor[] copy = new DSPProcessor[stages.length];
    for (int i = 0; i < stages.length; i++) {
      copy[i] = Objects.requireNonNull(stages[i], "stages[" + i + "]");
    }
    return new DSPPipeline(copy);
  }

  /**
   * Create a new pipeline from a list of stages.
   *
   * @param stages ordered list of processors; must not be {@code null} and must not contain {@code
   *     null} entries
   * @return a new pipeline
   */
  public static DSPPipeline of(List<? extends DSPProcessor> stages) {
    Objects.requireNonNull(stages, "stages");
    return of(stages.toArray(new DSPProcessor[0]));
  }

  /**
   * @return an empty pipeline that returns its input unchanged
   */
  public static DSPPipeline empty() {
    return new DSPPipeline(new DSPProcessor[0]);
  }

  @Override
  public AudioBlock process(AudioBlock block) {
    AudioBlock current = Objects.requireNonNull(block, "block");
    for (DSPProcessor stage : stages) {
      current = Objects.requireNonNull(stage.process(current), "stage produced null block");
    }
    return current;
  }

  /**
   * @return number of stages in this pipeline
   */
  public int size() {
    return stages.length;
  }

  /**
   * @return an unmodifiable view of the stages in declaration order
   */
  public List<DSPProcessor> stages() {
    return Collections.unmodifiableList(new ArrayList<>(Arrays.asList(stages)));
  }
}
