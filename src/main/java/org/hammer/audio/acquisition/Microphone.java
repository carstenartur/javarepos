package org.hammer.audio.acquisition;

import org.hammer.audio.geometry.Vector2;

/** Stable microphone metadata independent of a concrete audio API or research plugin. */
public record Microphone(String id, Vector2 positionMeters, int channel) {

  /** Create metadata for one microphone/channel in a synchronized multichannel stream. */
  public Microphone {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("id must not be blank");
    }
    if (positionMeters == null) {
      throw new IllegalArgumentException("positionMeters must not be null");
    }
    if (channel < 0) {
      throw new IllegalArgumentException("channel must be >= 0");
    }
  }
}
