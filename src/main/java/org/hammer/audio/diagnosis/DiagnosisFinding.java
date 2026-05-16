package org.hammer.audio.diagnosis;

import java.util.Objects;

/**
 * Immutable individual diagnostic finding produced by {@link DiagnosisAnalyzer}.
 *
 * <p>A finding bundles the categorical {@link DiagnosisType}, a {@link DiagnosisSeverity}, a {@code
 * [0,1]} confidence score, a short human-readable message and (where applicable) a measured
 * frequency and value. {@link Double#NaN} indicates that the field is not meaningful for the
 * finding.
 */
public final class DiagnosisFinding {

  private final DiagnosisType type;
  private final DiagnosisSeverity severity;
  private final double confidence;
  private final String message;
  private final double frequencyHz;
  private final double value;

  /**
   * Create a finding.
   *
   * @param type categorical type; must not be {@code null}
   * @param severity severity; must not be {@code null}
   * @param confidence confidence in {@code [0, 1]}
   * @param message short human-readable explanation; must not be {@code null}
   * @param frequencyHz measured frequency in Hz, or {@link Double#NaN} if not applicable
   * @param value measured numeric value, or {@link Double#NaN} if not applicable
   */
  public DiagnosisFinding(
      DiagnosisType type,
      DiagnosisSeverity severity,
      double confidence,
      String message,
      double frequencyHz,
      double value) {
    this.type = Objects.requireNonNull(type, "type");
    this.severity = Objects.requireNonNull(severity, "severity");
    if (Double.isNaN(confidence) || confidence < 0.0 || confidence > 1.0) {
      throw new IllegalArgumentException("confidence must be in [0,1], was " + confidence);
    }
    this.confidence = confidence;
    this.message = Objects.requireNonNull(message, "message");
    this.frequencyHz = frequencyHz;
    this.value = value;
  }

  public DiagnosisType type() {
    return type;
  }

  public DiagnosisSeverity severity() {
    return severity;
  }

  public double confidence() {
    return confidence;
  }

  public String message() {
    return message;
  }

  /**
   * @return measured frequency in Hz, or {@link Double#NaN} if not applicable
   */
  public double frequencyHz() {
    return frequencyHz;
  }

  /**
   * @return measured numeric value, or {@link Double#NaN} if not applicable
   */
  public double value() {
    return value;
  }

  @Override
  public String toString() {
    return "DiagnosisFinding["
        + severity
        + " "
        + type
        + " conf="
        + confidence
        + " msg="
        + message
        + "]";
  }
}
