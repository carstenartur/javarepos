package org.hammer.audio.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.hammer.audio.geometry.MicrophoneArrayGeometry.NamedPosition;
import org.junit.jupiter.api.Test;

class MicrophoneArrayGeometryTest {

  @Test
  void exposesPositionsCentroidAndMaxSpacing() {
    MicrophoneArrayGeometry geometry =
        new MicrophoneArrayGeometry(
            List.of(
                new NamedPosition("a", new Vector2(0.0, 0.0)),
                new NamedPosition("b", new Vector2(1.0, 0.0)),
                new NamedPosition("c", new Vector2(0.5, 1.0))));

    assertEquals(3, geometry.size());
    assertEquals(new Vector2(0.5, 1.0 / 3.0), geometry.centroid());
    assertEquals(Math.hypot(0.5, 1.0), geometry.maxSpacingMeters(), 1.0e-12);
    assertEquals(1.0, geometry.distanceBetween("a", "b"), 1.0e-12);
    assertEquals(new Vector2(0.5, 1.0), geometry.position("c"));
  }

  @Test
  void computesMaxInterSensorDelayFromSpeedOfSound() {
    MicrophoneArrayGeometry geometry =
        new MicrophoneArrayGeometry(
            List.of(
                new NamedPosition("a", new Vector2(0.0, 0.0)),
                new NamedPosition("b", new Vector2(0.343, 0.0))));

    assertEquals(0.001, geometry.maxInterSensorDelaySeconds(343.0), 1.0e-9);
    assertThrows(IllegalArgumentException.class, () -> geometry.maxInterSensorDelaySeconds(0.0));
    assertThrows(IllegalArgumentException.class, () -> geometry.maxInterSensorDelaySeconds(-1.0));
  }

  @Test
  void rejectsEmptyOrDuplicateIds() {
    assertThrows(IllegalArgumentException.class, () -> new MicrophoneArrayGeometry(List.of()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new MicrophoneArrayGeometry(
                List.of(
                    new NamedPosition("a", Vector2.ZERO),
                    new NamedPosition("a", new Vector2(1.0, 0.0)))));
  }

  @Test
  void unknownIdLookupFailsExplicitly() {
    MicrophoneArrayGeometry geometry =
        new MicrophoneArrayGeometry(List.of(new NamedPosition("a", Vector2.ZERO)));
    assertThrows(IllegalArgumentException.class, () -> geometry.position("missing"));
  }
}
