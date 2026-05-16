package org.hammer.audio.experimental.acoustic;

/** Frequency range in Hz used by experimental insect/source-separation stages. */
public record FrequencyBand(double lowHz, double highHz) {

  /** Create a closed frequency band. */
  public FrequencyBand {
    if (!(lowHz >= 0.0)
        || !(highHz > lowHz)
        || !Double.isFinite(lowHz)
        || !Double.isFinite(highHz)) {
      throw new IllegalArgumentException(
          "frequency band must be finite and satisfy 0 <= low < high");
    }
  }

  /** Return whether {@code frequencyHz} is inside this band. */
  public boolean contains(double frequencyHz) {
    return frequencyHz >= lowHz && frequencyHz <= highHz;
  }
}
