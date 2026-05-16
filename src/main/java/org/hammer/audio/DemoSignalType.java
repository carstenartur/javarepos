package org.hammer.audio;

/** Available deterministic demo/test signals for UI playback without microphone input. */
public enum DemoSignalType {
  SINE("Sine"),
  SQUARE("Square"),
  CHIRP("Chirp");

  private final String label;

  DemoSignalType(String label) {
    this.label = label;
  }

  @Override
  public String toString() {
    return label;
  }
}
