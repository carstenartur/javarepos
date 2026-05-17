package org.hammer.audio.experimental.acoustic.tracking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hammer.audio.experimental.acoustic.tracking.SourceTracker.Observation;
import org.hammer.audio.geometry.Vector2;
import org.hammer.audio.geometry.Vector3;
import org.junit.jupiter.api.Test;

class SourceTrackerTest {

  @Test
  void assignsStableIdsAcrossFrames() {
    SourceTracker tracker = SourceTracker.withDefaults();

    List<TrackedSource> frame0 =
        tracker.update(0, 0.0, List.of(new Observation(600.0, new Vector2(1.0, 1.0))));
    List<TrackedSource> frame1 =
        tracker.update(1, 0.05, List.of(new Observation(602.0, new Vector2(1.05, 1.0))));
    List<TrackedSource> frame2 =
        tracker.update(2, 0.10, List.of(new Observation(604.0, new Vector2(1.10, 1.0))));

    assertEquals(1, frame0.size());
    assertEquals(1, frame1.size());
    assertEquals(1, frame2.size());
    int id = frame0.get(0).id();
    assertEquals(id, frame1.get(0).id());
    assertEquals(id, frame2.get(0).id());
    assertTrue(frame2.get(0).observationCount() == 3);
  }

  @Test
  void separatesTwoCloseFrequenciesIntoDistinctTracks() {
    SourceTracker tracker = new SourceTracker(20.0, 3, 0.5, 0.04, 1.0, 1.0, 0.8, 0.4);

    tracker.update(
        0,
        0.0,
        List.of(
            new Observation(600.0, new Vector2(1.0, 1.0)),
            new Observation(640.0, new Vector2(2.0, 1.0))));
    List<TrackedSource> tracks =
        tracker.update(
            1,
            0.05,
            List.of(
                new Observation(601.0, new Vector2(1.02, 1.0)),
                new Observation(641.0, new Vector2(1.98, 1.0))));

    assertEquals(2, tracks.size());
    assertNotEquals(tracks.get(0).id(), tracks.get(1).id());
  }

  @Test
  void dropsTracksAfterConfiguredMissingFrames() {
    SourceTracker tracker = new SourceTracker(20.0, 1, 0.5, 0.04, 1.0, 1.0, 0.5, 0.5);

    tracker.update(0, 0.0, List.of(new Observation(500.0, new Vector2(1.0, 1.0))));
    tracker.update(1, 0.05, List.of());
    List<TrackedSource> tracks = tracker.update(2, 0.10, List.of());

    assertTrue(tracks.isEmpty(), "stale track should be dropped");
  }

  @Test
  void rejectsDecreasingFrameIndex() {
    SourceTracker tracker = SourceTracker.withDefaults();
    tracker.update(5, 0.0, List.of());
    assertThrows(IllegalArgumentException.class, () -> tracker.update(4, 0.05, List.of()));
  }

  @Test
  void resetClearsAllStateAndIds() {
    SourceTracker tracker = SourceTracker.withDefaults();
    tracker.update(0, 0.0, List.of(new Observation(500.0, new Vector2(1.0, 1.0))));
    tracker.reset();
    List<TrackedSource> tracks =
        tracker.update(0, 0.0, List.of(new Observation(800.0, new Vector2(0.0, 0.0))));
    assertEquals(1, tracks.size());
    assertEquals(0, tracks.get(0).id(), "ids restart from 0 after reset");
  }

  @Test
  void storesDopplerVelocityAndObservedFrequency() {
    SourceTracker tracker = new SourceTracker(20.0, 3, 0.5, 0.04, 1.0, 1.0, 0.8, 0.4, 1.0);

    List<TrackedSource> tracks =
        tracker.update(
            0,
            0.0,
            List.of(
                new Observation(
                    600.0, 604.0, new Vector2(1.0, 1.0), new Vector3(0.5, -0.25, 0.0), 2.0, 0.0)));

    assertEquals(604.0, tracks.get(0).observedFrequencyHz());
    assertEquals(2.0, tracks.get(0).radialVelocityMetersPerSecond());
    assertEquals(0.5, tracks.get(0).velocityMetersPerSecond3d().x());
    assertEquals(-0.25, tracks.get(0).velocityMetersPerSecond().y());
    assertEquals(1.0, tracks.get(0).dopplerVelocityWeight());
  }

  @Test
  void highFrequencyVarianceReducesDopplerInfluenceAndConfidence() {
    SourceTracker lowVarianceTracker =
        new SourceTracker(20.0, 3, 0.5, 0.04, 1.0, 1.0, 0.8, 0.4, 1.0);
    SourceTracker highVarianceTracker =
        new SourceTracker(20.0, 3, 0.5, 0.04, 1.0, 1.0, 0.8, 0.4, 1.0);

    lowVarianceTracker.update(
        0,
        0.0,
        List.of(
            new Observation(
                600.0, 600.0, new Vector2(0.0, 0.0), new Vector3(10.0, 0.0, 0.0), 1.0, 0.0)));
    highVarianceTracker.update(
        0,
        0.0,
        List.of(
            new Observation(
                600.0, 600.0, new Vector2(0.0, 0.0), new Vector3(10.0, 0.0, 0.0), 1.0, 100.0)));

    TrackedSource lowVariance = lowVarianceTracker.snapshot().get(0);
    TrackedSource highVariance = highVarianceTracker.snapshot().get(0);

    assertTrue(highVariance.dopplerVelocityWeight() < lowVariance.dopplerVelocityWeight());
    assertTrue(
        highVariance.velocityMetersPerSecond3d().x() < lowVariance.velocityMetersPerSecond3d().x());
    assertTrue(highVariance.confidence() < lowVariance.confidence());
  }

  @Test
  void validatesConstructorArguments() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new SourceTracker(0.0, 3, 0.0, 0.1, 1.0, 1.0, 0.5, 0.5));
    assertThrows(
        IllegalArgumentException.class,
        () -> new SourceTracker(1.0, -1, 0.0, 0.1, 1.0, 1.0, 0.5, 0.5));
    assertThrows(
        IllegalArgumentException.class,
        () -> new SourceTracker(1.0, 1, 0.0, 0.1, 1.0, 1.0, 1.5, 0.5));
    assertThrows(
        IllegalArgumentException.class,
        () -> new SourceTracker(1.0, 1, 0.0, 0.1, 1.0, 1.0, 0.5, -0.1));
  }
}
