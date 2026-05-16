package org.hammer.audio.experimental.acoustic.simulation;

import org.hammer.audio.geometry.Vector2;

/** Rectangular 2D virtual room for deterministic localization experiments. */
public record Room2D(double widthMeters, double heightMeters, double reflectionGain, double noiseAmplitude) {

  /** Create room parameters. */
  public Room2D {
    if (!(widthMeters > 0.0) || !(heightMeters > 0.0) || !Double.isFinite(widthMeters) || !Double.isFinite(heightMeters)) {
      throw new IllegalArgumentException("room dimensions must be finite and > 0");
    }
    if (reflectionGain < 0.0 || reflectionGain > 1.0 || !Double.isFinite(reflectionGain)) {
      throw new IllegalArgumentException("reflectionGain must be finite and in [0,1]");
    }
    if (noiseAmplitude < 0.0 || noiseAmplitude > 1.0 || !Double.isFinite(noiseAmplitude)) {
      throw new IllegalArgumentException("noiseAmplitude must be finite and in [0,1]");
    }
  }

  /** Return whether a point is inside the room. */
  public boolean contains(Vector2 position) {
    return position.x() >= 0.0 && position.x() <= widthMeters && position.y() >= 0.0 && position.y() <= heightMeters;
  }
}
