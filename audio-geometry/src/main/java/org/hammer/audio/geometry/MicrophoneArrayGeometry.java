package org.hammer.audio.geometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Calibrated 2D positions of a set of acoustic sensors, decoupled from the audio acquisition layer.
 *
 * <p>{@code MicrophoneArrayGeometry} provides reusable geometry primitives for localization
 * pipelines: pairwise distances, centroid, maximum spacing and lookup by sensor id. It is
 * intentionally independent of the {@code audio-acquisition} {@code MicrophoneArray} type so that
 * geometry utilities can be shared with non-audio sensor arrays (e.g. radar, sonar) and with
 * offline tools that do not depend on the audio stack.
 *
 * <p>Instances are immutable, all stored positions are validated to be finite, and sensor ids must
 * be unique.
 */
public final class MicrophoneArrayGeometry {

  private final List<NamedPosition> positions;
  private final Map<String, NamedPosition> byId;
  private final double maxSpacingMeters;

  /** Create a geometry from sensor ids and 2D positions in meters. */
  public MicrophoneArrayGeometry(List<NamedPosition> positions) {
    Objects.requireNonNull(positions, "positions");
    if (positions.isEmpty()) {
      throw new IllegalArgumentException("positions must not be empty");
    }
    List<NamedPosition> copy = new ArrayList<>(positions.size());
    Map<String, NamedPosition> idMap = new LinkedHashMap<>();
    for (NamedPosition position : positions) {
      Objects.requireNonNull(position, "positions must not contain null entries");
      if (idMap.put(position.id(), position) != null) {
        throw new IllegalArgumentException("duplicate sensor id: " + position.id());
      }
      copy.add(position);
    }
    this.positions = List.copyOf(copy);
    this.byId = Collections.unmodifiableMap(idMap);
    this.maxSpacingMeters = computeMaxSpacing(copy);
  }

  /** Number of sensors in this geometry. */
  public int size() {
    return positions.size();
  }

  /** Named positions in declaration order. */
  public List<NamedPosition> positions() {
    return positions;
  }

  /** Look up a position by sensor id; throws when the id is unknown. */
  public Vector2 position(String id) {
    NamedPosition position = byId.get(Objects.requireNonNull(id, "id"));
    if (position == null) {
      throw new IllegalArgumentException("unknown sensor id: " + id);
    }
    return position.positionMeters();
  }

  /** Distance between two sensors identified by their ids. */
  public double distanceBetween(String firstId, String secondId) {
    return position(firstId).distanceTo(position(secondId));
  }

  /** Geometric centroid of all sensor positions. */
  public Vector2 centroid() {
    double sumX = 0.0;
    double sumY = 0.0;
    for (NamedPosition position : positions) {
      sumX += position.positionMeters().x();
      sumY += position.positionMeters().y();
    }
    int count = positions.size();
    return new Vector2(sumX / count, sumY / count);
  }

  /** Largest pairwise distance between sensors, useful for bounding TDOA search ranges. */
  public double maxSpacingMeters() {
    return maxSpacingMeters;
  }

  /**
   * Maximum physical inter-sensor delay for a given propagation speed.
   *
   * @param speedOfSoundMetersPerSecond positive, finite speed of sound in m/s
   */
  public double maxInterSensorDelaySeconds(double speedOfSoundMetersPerSecond) {
    if (!(speedOfSoundMetersPerSecond > 0.0) || !Double.isFinite(speedOfSoundMetersPerSecond)) {
      throw new IllegalArgumentException("speedOfSoundMetersPerSecond must be finite and > 0");
    }
    return maxSpacingMeters / speedOfSoundMetersPerSecond;
  }

  private static double computeMaxSpacing(List<NamedPosition> positions) {
    double max = 0.0;
    for (int i = 0; i < positions.size(); i++) {
      Vector2 a = positions.get(i).positionMeters();
      for (int j = i + 1; j < positions.size(); j++) {
        double distance = a.distanceTo(positions.get(j).positionMeters());
        if (distance > max) {
          max = distance;
        }
      }
    }
    return max;
  }

  /** Sensor identifier and 2D position. */
  public record NamedPosition(String id, Vector2 positionMeters) {

    /** Validate identifier and position. */
    public NamedPosition {
      if (id == null || id.isBlank()) {
        throw new IllegalArgumentException("id must not be blank");
      }
      Objects.requireNonNull(positionMeters, "positionMeters");
    }
  }
}
