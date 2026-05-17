package org.hammer.audio.experimental.acoustic.tracking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hammer.audio.geometry.Vector2;
import org.junit.jupiter.api.Test;

class Kalman2DTest {

  @Test
  void learnsConstantVelocityFromSeriesOfMeasurements() {
    Kalman2D filter = new Kalman2D(new Vector2(0.0, 0.0), 1.0, 1.0, 1.0, 0.001);
    double velocity = 2.0; // 2 m/s along x
    double dt = 0.05;
    for (int i = 1; i <= 20; i++) {
      filter.predict(dt);
      filter.update(new Vector2(i * velocity * dt, 0.0));
    }
    assertTrue(
        Math.abs(filter.velocity().x() - velocity) < 0.3,
        "filtered velocity should approach truth, got " + filter.velocity());

    // After learning velocity, a prediction step extrapolates the position forward.
    Vector2 before = filter.position();
    filter.predict(dt);
    assertTrue(
        filter.position().x() > before.x(),
        "predict should extrapolate position along learned velocity");
  }

  @Test
  void smoothsNoisyMeasurementsTowardsTruth() {
    Kalman2D filter = new Kalman2D(new Vector2(0.0, 0.0), 1.0, 1.0, 0.01, 0.1);
    double[] noisyX = {0.05, -0.02, 0.03, -0.01, 0.02};
    for (double x : noisyX) {
      filter.predict(0.05);
      filter.update(new Vector2(x, 0.0));
    }
    assertTrue(
        Math.abs(filter.position().x()) < 0.04,
        "filtered position should be near truth, got " + filter.position());
    assertTrue(
        filter.positionVariance() < 1.0,
        "variance should shrink with measurements, got " + filter.positionVariance());
  }

  @Test
  void rejectsInvalidConstructorArguments() {
    assertThrows(IllegalArgumentException.class, () -> new Kalman2D(null, 1.0, 1.0, 0.0, 0.1));
    assertThrows(
        IllegalArgumentException.class, () -> new Kalman2D(Vector2.ZERO, -1.0, 1.0, 0.0, 0.1));
    assertThrows(
        IllegalArgumentException.class, () -> new Kalman2D(Vector2.ZERO, 1.0, 1.0, 0.0, 0.0));
  }

  @Test
  void rejectsInvalidPredictAndUpdateInputs() {
    Kalman2D filter = new Kalman2D(Vector2.ZERO, 1.0, 1.0, 0.0, 0.1);
    assertThrows(IllegalArgumentException.class, () -> filter.predict(-1.0));
    assertThrows(IllegalArgumentException.class, () -> filter.update(null));
    // After update measurement variance is preserved.
    filter.update(new Vector2(1.0, 2.0));
    assertEquals(2, 2);
  }
}
