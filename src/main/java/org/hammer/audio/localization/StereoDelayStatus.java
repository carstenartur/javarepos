package org.hammer.audio.localization;

/** Classification of a stereo delay estimate. */
public enum StereoDelayStatus {
  VALID,
  MONO_INPUT,
  SILENCE,
  LOW_CORRELATION,
  DELAY_OUTSIDE_PHYSICAL_RANGE
}
