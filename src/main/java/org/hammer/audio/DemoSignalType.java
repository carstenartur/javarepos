package org.hammer.audio;

/** Available deterministic demo/test signals for UI playback without microphone input. */
public enum DemoSignalType {
  SINE("Sine"),
  SQUARE("Square"),
  CHIRP("Chirp"),
  MOSQUITO_BURST("Mosquito-like high-frequency burst"),
  MOVING_CHIRP("Moving chirp source"),
  HUM_HARMONICS("50 Hz hum + harmonics"),
  CLIPPING_TEST("Clipping test"),
  STEREO_DELAY_TEST("Stereo delay test");

  private final String label;

  DemoSignalType(String label) {
    this.label = label;
  }

  @Override
  public String toString() {
    return label;
  }
}
