package org.hammer.audio.experimental.acoustic.doppler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hammer.audio.acquisition.Microphone;
import org.hammer.audio.acquisition.MicrophoneArray;
import org.hammer.audio.experimental.acoustic.tracking.DetectedPeak;
import org.hammer.audio.geometry.Vector2;
import org.junit.jupiter.api.Test;

class DopplerEstimatorTest {

  @Test
  void estimatesSmallVelocityDopplerShift() {
    SimpleDopplerEstimator estimator = new SimpleDopplerEstimator(343.0);

    double velocity = estimator.estimateRadialVelocity(605.0, 600.0);

    assertEquals(2.858, velocity, 1.0e-3);
  }

  @Test
  void frequencyTrackSmoothsAndReportsVariance() {
    FrequencyTrack track = new FrequencyTrack(3, 0.5);

    track.update(100.0);
    track.update(110.0);
    double stable = track.update(90.0);

    assertEquals(97.5, stable, 1.0e-9);
    assertEquals(3, track.history().size());
    assertTrue(track.variance() > 0.0);
  }

  @Test
  void multiSensorEstimatorFiltersLargeOutlier() {
    SimpleMultiSensorDopplerEstimator estimator =
        new SimpleMultiSensorDopplerEstimator(new SimpleDopplerEstimator(343.0), 4.0);
    SourceObservation observation =
        new SourceObservation(
            602.0,
            600.0,
            new Vector2(1.0, 1.0),
            List.of(
                new DetectedPeak(0, 602.0, 10.0, 5.0),
                new DetectedPeak(1, 601.8, 10.0, 5.0),
                new DetectedPeak(2, 640.0, 10.0, 5.0)));

    List<RadialVelocityEstimate> estimates = estimator.estimateRadialVelocities(observation);

    assertEquals(2, estimates.size());
  }

  @Test
  void reconstructsVelocityFromArrayGeometry() {
    MicrophoneArray array =
        new MicrophoneArray(
            List.of(
                new Microphone("x", new Vector2(0.0, 1.0), 0),
                new Microphone("y", new Vector2(1.0, 0.0), 1)));
    VelocityReconstructor reconstructor = new VelocityReconstructor();

    Vector3 velocity =
        reconstructor.reconstruct(
            List.of(
                new RadialVelocityEstimate(0, 602.0, 600.0, -1.0, 1.0),
                new RadialVelocityEstimate(1, 602.0, 600.0, -2.0, 1.0)),
            array,
            new Vector2(0.0, 0.0));

    assertEquals(2.0, velocity.x(), 1.0e-9);
    assertEquals(1.0, velocity.y(), 1.0e-9);
  }

  @Test
  void rejectsInvalidInputs() {
    assertThrows(IllegalArgumentException.class, () -> new SimpleDopplerEstimator(0.0));
    assertThrows(
        IllegalArgumentException.class,
        () -> new SimpleDopplerEstimator().estimateRadialVelocity(100.0, 0.0));
  }
}
