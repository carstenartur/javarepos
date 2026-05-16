package org.hammer.audio.export;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.hammer.audio.analysis.SpectrumAnalyzer;
import org.hammer.audio.analysis.SpectrumSnapshot;
import org.hammer.audio.core.AudioBlock;
import org.hammer.audio.core.AudioFormatDescriptor;
import org.hammer.audio.diagnosis.DiagnosisFinding;
import org.hammer.audio.diagnosis.DiagnosisSeverity;
import org.hammer.audio.diagnosis.DiagnosisSnapshot;
import org.hammer.audio.diagnosis.DiagnosisType;
import org.hammer.audio.spectrogram.SpectrogramAnalyzer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EvidenceBundleExporterTest {

  @Test
  void export_writesAllArtifacts(@TempDir Path tmp) throws Exception {
    AudioFormatDescriptor format = new AudioFormatDescriptor(16000f, 1, 16);
    float[][] samples = new float[1][1024];
    for (int i = 0; i < samples[0].length; i++) {
      samples[0][i] = (float) Math.sin(2 * Math.PI * 440.0 * i / 16000.0);
    }
    AudioBlock block = AudioBlock.wrap(format, samples, 1L, 1_000_000L);
    SpectrumSnapshot spectrum = new SpectrumAnalyzer(1024, 0, 16000f).analyze(block);

    SpectrogramAnalyzer spectroAnalyzer = new SpectrogramAnalyzer(1024, 0, 16000f, 4);
    spectroAnalyzer.analyze(block);
    spectroAnalyzer.analyze(block);

    DiagnosisSnapshot diagnosis =
        new DiagnosisSnapshot(
            1L,
            1L,
            List.of(
                new DiagnosisFinding(
                    DiagnosisType.DOMINANT_TONE,
                    DiagnosisSeverity.INFO,
                    0.9,
                    "Dominant tone: 440 Hz.",
                    440.0,
                    1.0)));

    BufferedImage shot = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);

    EvidenceData data =
        EvidenceData.builder()
            .screenshot(shot)
            .block(block)
            .spectrum(spectrum)
            .spectrogram(spectroAnalyzer.history())
            .diagnosis(diagnosis)
            .notes("unit-test")
            .build();
    Path bundle = new EvidenceBundleExporter().export(tmp, data);

    assertTrue(Files.isDirectory(bundle));
    assertTrue(bundle.getFileName().toString().startsWith("measurement-"));
    assertTrue(Files.exists(bundle.resolve("screenshot.png")));
    assertTrue(Files.exists(bundle.resolve("samples.csv")));
    assertTrue(Files.exists(bundle.resolve("spectrum.csv")));
    assertTrue(Files.exists(bundle.resolve("spectrogram.csv")));
    assertTrue(Files.exists(bundle.resolve("diagnosis.txt")));
    assertTrue(Files.exists(bundle.resolve("metadata.json")));

    String diagnosisText = Files.readString(bundle.resolve("diagnosis.txt"));
    assertTrue(diagnosisText.contains("Dominant tone"), diagnosisText);

    String metadata = Files.readString(bundle.resolve("metadata.json"));
    assertTrue(metadata.contains("\"bundleName\""), metadata);
    assertTrue(metadata.contains("\"diagnosisFindings\": 1"), metadata);
  }

  @Test
  void export_rejectsEmptyPayload(@TempDir Path tmp) {
    EvidenceData data = EvidenceData.builder().build();
    assertThrows(
        IllegalArgumentException.class, () -> new EvidenceBundleExporter().export(tmp, data));
  }
}
