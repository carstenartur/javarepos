package org.hammer.audio.diagnosis;

/** Categorical type of a {@link DiagnosisFinding}. */
public enum DiagnosisType {
  /** A clear single dominant tone is present. */
  DOMINANT_TONE,
  /** Energy concentrated around 50 Hz (typical European mains frequency). */
  MAINS_HUM_50HZ,
  /** Energy concentrated around 60 Hz (typical North American mains frequency). */
  MAINS_HUM_60HZ,
  /** Hard clipping detected: at least one sample reached full scale. */
  CLIPPING,
  /** Broadband, near-flat spectrum without a clear tone (noise-like signal). */
  BROADBAND_NOISE,
  /** Short-lived, high-frequency energy bursts visible in the recent history. */
  INTERMITTENT_HIGH_FREQUENCY_BURST,
  /** Dominant peak drifts over time, indicating a sweep, chirp or moving source. */
  DRIFTING_PEAK,
  /** Signal too quiet or otherwise too low-confidence to draw conclusions. */
  LOW_CONFIDENCE_SILENCE
}
