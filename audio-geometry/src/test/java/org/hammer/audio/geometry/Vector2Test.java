package org.hammer.audio.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class Vector2Test {

  @Test
  void computesDistanceAndNormalization() {
    Vector2 vector = new Vector2(3.0, 4.0);

    assertEquals(5.0, vector.length(), 1.0e-9);
    assertEquals(1.0, vector.normalized().length(), 1.0e-9);
    assertEquals(5.0, Vector2.ZERO.distanceTo(vector), 1.0e-9);
  }

  @Test
  void rejectsInvalidCoordinates() {
    assertThrows(IllegalArgumentException.class, () -> new Vector2(Double.NaN, 0.0));
  }
}
