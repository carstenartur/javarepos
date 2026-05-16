package org.hammer.audio.diagnosis;

/** Severity classification for a {@link DiagnosisFinding}. */
public enum DiagnosisSeverity {
  /** Informational; describes a normal characteristic of the signal. */
  INFO,
  /** Warning; suggests the signal contains an undesirable feature worth investigating. */
  WARNING,
  /** Critical; indicates a measurement-invalidating condition such as clipping. */
  CRITICAL
}
