package org.hammer.audio.experimental.acoustic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.hammer.audio.acquisition.Microphone;
import org.hammer.audio.acquisition.MicrophoneArray;
import org.hammer.audio.core.AudioBlock;
import org.hammer.audio.core.AudioFormatDescriptor;
import org.hammer.audio.experimental.acoustic.MosquitoLocalizationPipeline.TdoaPairingMode;
import org.hammer.audio.geometry.Vector2;
import org.junit.jupiter.api.Test;

class MosquitoLocalizationPipelineTest {

  @Test
  void defaultPipelineUsesAllUsefulMicrophonePairs() {
    RecordingEstimator estimator = new RecordingEstimator();
    MosquitoLocalizationPipeline pipeline =
        new MosquitoLocalizationPipeline(
            new WingbeatFrequencyTracker(128, new FrequencyBand(300.0, 900.0)),
            estimator,
            new DelayAndSumBeamformer(343.0),
            List.of(Vector2.ZERO, new Vector2(0.1, 0.1)));

    AcousticLocalizationSnapshot snapshot = pipeline.analyze(testBlock(3), testArray(3));

    assertEquals(List.of("0:1", "0:2", "1:2"), estimator.pairs);
    assertEquals(3, snapshot.tdoaEstimates().size());
    assertEquals(3, snapshot.constraints().size());
  }

  @Test
  void referenceChannelPairingIsExplicitlyConfigurable() {
    RecordingEstimator estimator = new RecordingEstimator();
    MosquitoLocalizationPipeline pipeline =
        new MosquitoLocalizationPipeline(
            new WingbeatFrequencyTracker(128, new FrequencyBand(300.0, 900.0)),
            estimator,
            new DelayAndSumBeamformer(343.0),
            List.of(Vector2.ZERO, new Vector2(0.1, 0.1)),
            1,
            false,
            TdoaPairingMode.REFERENCE_CHANNEL);

    pipeline.analyze(testBlock(4), testArray(4));

    assertEquals(List.of("1:0", "1:2", "1:3"), estimator.pairs);
  }

  @Test
  void canAggregateFrequencyTrackingAcrossChannels() {
    MosquitoLocalizationPipeline referenceOnly =
        new MosquitoLocalizationPipeline(
            new WingbeatFrequencyTracker(128, new FrequencyBand(300.0, 900.0)),
            new RecordingEstimator(),
            new DelayAndSumBeamformer(343.0),
            List.of(Vector2.ZERO),
            0,
            false,
            TdoaPairingMode.ALL_PAIRS);
    MosquitoLocalizationPipeline aggregate =
        new MosquitoLocalizationPipeline(
            new WingbeatFrequencyTracker(128, new FrequencyBand(300.0, 900.0)),
            new RecordingEstimator(),
            new DelayAndSumBeamformer(343.0),
            List.of(Vector2.ZERO),
            0,
            true,
            TdoaPairingMode.ALL_PAIRS);
    AudioBlock block = blockWithToneOnlyOnChannelOne();
    MicrophoneArray array = testArray(2);

    assertEquals(0.0, referenceOnly.analyze(block, array).trackedFrequency().magnitude());
    assertTrue(aggregate.analyze(block, array).trackedFrequency().magnitude() > 0.0);
  }

  @Test
  void validatesConstructorAndAnalysisInputs() {
    WingbeatFrequencyTracker tracker =
        new WingbeatFrequencyTracker(128, new FrequencyBand(300.0, 900.0));
    DelayAndSumBeamformer beamformer = new DelayAndSumBeamformer(343.0);
    assertThrows(
        NullPointerException.class,
        () ->
            new MosquitoLocalizationPipeline(
                null, new RecordingEstimator(), beamformer, List.of(Vector2.ZERO)));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new MosquitoLocalizationPipeline(
                tracker, new RecordingEstimator(), beamformer, List.of()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new MosquitoLocalizationPipeline(
                tracker,
                new RecordingEstimator(),
                beamformer,
                List.of(Vector2.ZERO),
                -1,
                false,
                TdoaPairingMode.ALL_PAIRS));

    MosquitoLocalizationPipeline pipeline =
        new MosquitoLocalizationPipeline(
            tracker,
            new RecordingEstimator(),
            beamformer,
            List.of(Vector2.ZERO),
            2,
            false,
            TdoaPairingMode.ALL_PAIRS);
    assertThrows(NullPointerException.class, () -> pipeline.analyze(null, testArray(2)));
    assertThrows(NullPointerException.class, () -> pipeline.analyze(testBlock(2), null));
    assertThrows(
        IllegalArgumentException.class, () -> pipeline.analyze(testBlock(2), testArray(2)));
  }

  private static AudioBlock testBlock(int channels) {
    int frames = 128;
    float[][] samples = new float[channels][frames];
    for (int channel = 0; channel < channels; channel++) {
      for (int frame = 0; frame < frames; frame++) {
        samples[channel][frame] = (float) Math.sin(2.0 * Math.PI * 440.0 * frame / 8_000.0);
      }
    }
    return new AudioBlock(new AudioFormatDescriptor(8_000.0f, channels, 32), samples, 0, 0);
  }

  private static AudioBlock blockWithToneOnlyOnChannelOne() {
    int frames = 128;
    float[][] samples = new float[2][frames];
    for (int frame = 0; frame < frames; frame++) {
      samples[1][frame] = (float) Math.sin(2.0 * Math.PI * 440.0 * frame / 8_000.0);
    }
    return new AudioBlock(new AudioFormatDescriptor(8_000.0f, 2, 32), samples, 0, 0);
  }

  private static MicrophoneArray testArray(int channels) {
    List<Microphone> microphones = new ArrayList<>();
    for (int channel = 0; channel < channels; channel++) {
      microphones.add(new Microphone("m" + channel, new Vector2(channel * 0.05, 0.0), channel));
    }
    return new MicrophoneArray(microphones);
  }

  private static final class RecordingEstimator implements TdoaEstimator {

    private final List<String> pairs = new ArrayList<>();

    @Override
    public TdoaEstimate estimate(
        AudioBlock block, MicrophoneArray array, int firstChannel, int secondChannel) {
      pairs.add(firstChannel + ":" + secondChannel);
      return new TdoaEstimate(
          array.microphone(firstChannel).id(),
          array.microphone(secondChannel).id(),
          0,
          0.0,
          0.0,
          1.0);
    }
  }
}
