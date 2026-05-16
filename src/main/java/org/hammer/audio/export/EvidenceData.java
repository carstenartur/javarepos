package org.hammer.audio.export;

import java.awt.image.BufferedImage;
import java.time.Instant;
import org.hammer.audio.analysis.SpectrumSnapshot;
import org.hammer.audio.core.AudioBlock;
import org.hammer.audio.diagnosis.DiagnosisSnapshot;
import org.hammer.audio.localization.StereoDelaySnapshot;
import org.hammer.audio.spectrogram.SpectrogramHistory;

/**
 * Immutable evidence payload submitted to {@link EvidenceBundleExporter}.
 *
 * <p>All fields are nullable; any artifact whose backing data is {@code null} is omitted from the
 * exported bundle.
 */
public final class EvidenceData {

  private final Instant timestamp;
  private final BufferedImage screenshot;
  private final AudioBlock block;
  private final SpectrumSnapshot spectrum;
  private final SpectrogramHistory spectrogram;
  private final StereoDelaySnapshot stereoDelay;
  private final DiagnosisSnapshot diagnosis;
  private final String notes;

  private EvidenceData(Builder b) {
    this.timestamp = b.timestamp;
    this.screenshot = b.screenshot;
    this.block = b.block;
    this.spectrum = b.spectrum;
    this.spectrogram = b.spectrogram;
    this.stereoDelay = b.stereoDelay;
    this.diagnosis = b.diagnosis;
    this.notes = b.notes;
  }

  public static Builder builder() {
    return new Builder();
  }

  public Instant timestamp() {
    return timestamp;
  }

  public BufferedImage screenshot() {
    return screenshot;
  }

  public AudioBlock block() {
    return block;
  }

  public SpectrumSnapshot spectrum() {
    return spectrum;
  }

  public SpectrogramHistory spectrogram() {
    return spectrogram;
  }

  public StereoDelaySnapshot stereoDelay() {
    return stereoDelay;
  }

  public DiagnosisSnapshot diagnosis() {
    return diagnosis;
  }

  public String notes() {
    return notes;
  }

  /**
   * @return true if any artifact payload is present
   */
  public boolean hasAnything() {
    return screenshot != null
        || block != null
        || spectrum != null
        || (spectrogram != null && !spectrogram.isEmpty())
        || stereoDelay != null
        || diagnosis != null;
  }

  /** Builder for {@link EvidenceData}. */
  public static final class Builder {
    private Instant timestamp;
    private BufferedImage screenshot;
    private AudioBlock block;
    private SpectrumSnapshot spectrum;
    private SpectrogramHistory spectrogram;
    private StereoDelaySnapshot stereoDelay;
    private DiagnosisSnapshot diagnosis;
    private String notes;

    public Builder timestamp(Instant value) {
      this.timestamp = value;
      return this;
    }

    public Builder screenshot(BufferedImage value) {
      this.screenshot = value;
      return this;
    }

    public Builder block(AudioBlock value) {
      this.block = value;
      return this;
    }

    public Builder spectrum(SpectrumSnapshot value) {
      this.spectrum = value;
      return this;
    }

    public Builder spectrogram(SpectrogramHistory value) {
      this.spectrogram = value;
      return this;
    }

    public Builder stereoDelay(StereoDelaySnapshot value) {
      this.stereoDelay = value;
      return this;
    }

    public Builder diagnosis(DiagnosisSnapshot value) {
      this.diagnosis = value;
      return this;
    }

    public Builder notes(String value) {
      this.notes = value;
      return this;
    }

    public EvidenceData build() {
      return new EvidenceData(this);
    }
  }
}
