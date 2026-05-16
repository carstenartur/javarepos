package org.hammer.audio.diagnosis;

import static org.junit.jupiter.api.Assertions.*;

import org.hammer.audio.analysis.SpectrumAnalyzer;
import org.hammer.audio.analysis.SpectrumSnapshot;
import org.hammer.audio.core.AudioBlock;
import org.hammer.audio.core.AudioFormatDescriptor;
import org.hammer.audio.spectrogram.SpectrogramAnalyzer;
import org.hammer.audio.spectrogram.SpectrogramHistory;
import org.junit.jupiter.api.Test;

class DiagnosisAnalyzerTest {

  private static final float SR = 16000f;
  private static final int FRAMES = 4096;

  private static AudioBlock sineBlock(double frequencyHz, double amplitude, long frameIndex) {
    AudioFormatDescriptor format = new AudioFormatDescriptor(SR, 1, 16);
    float[][] samples = new float[1][FRAMES];
    for (int i = 0; i < FRAMES; i++) {
      samples[0][i] = (float) (amplitude * Math.sin(2 * Math.PI * frequencyHz * i / SR));
    }
    return AudioBlock.wrap(format, samples, frameIndex, frameIndex * 1000L);
  }

  private static AudioBlock humBlock(double fundamentalHz, long frameIndex) {
    AudioFormatDescriptor format = new AudioFormatDescriptor(SR, 1, 16);
    float[][] samples = new float[1][FRAMES];
    for (int i = 0; i < FRAMES; i++) {
      double s =
          0.5 * Math.sin(2 * Math.PI * fundamentalHz * i / SR)
              + 0.25 * Math.sin(2 * Math.PI * 2 * fundamentalHz * i / SR)
              + 0.15 * Math.sin(2 * Math.PI * 3 * fundamentalHz * i / SR);
      samples[0][i] = (float) s;
    }
    return AudioBlock.wrap(format, samples, frameIndex, frameIndex * 1000L);
  }

  private static AudioBlock silenceBlock() {
    AudioFormatDescriptor format = new AudioFormatDescriptor(SR, 1, 16);
    return AudioBlock.wrap(format, new float[1][FRAMES], 0L, 0L);
  }

  private static AudioBlock clipBlock() {
    AudioFormatDescriptor format = new AudioFormatDescriptor(SR, 1, 16);
    float[][] samples = new float[1][FRAMES];
    for (int i = 0; i < FRAMES; i++) {
      samples[0][i] = (i % 2 == 0) ? 1.0f : -1.0f;
    }
    return AudioBlock.wrap(format, samples, 0L, 0L);
  }

  private static SpectrumSnapshot spectrumOf(AudioBlock block) {
    return new SpectrumAnalyzer(1024, 0, SR).analyze(block);
  }

  @Test
  void sine_reportsDominantTone() {
    AudioBlock block = sineBlock(440.0, 0.8, 0L);
    DiagnosisSnapshot snap = new DiagnosisAnalyzer().analyze(block, spectrumOf(block), null, null);
    assertTrue(
        snap.findings().stream().anyMatch(f -> f.type() == DiagnosisType.DOMINANT_TONE),
        snap.findings().toString());
  }

  @Test
  void hum50Hz_reportsMainsHum() {
    AudioBlock block = humBlock(50.0, 0L);
    DiagnosisSnapshot snap = new DiagnosisAnalyzer().analyze(block, spectrumOf(block), null, null);
    assertTrue(
        snap.findings().stream().anyMatch(f -> f.type() == DiagnosisType.MAINS_HUM_50HZ),
        snap.findings().toString());
  }

  @Test
  void clipping_reportsCritical() {
    AudioBlock block = clipBlock();
    DiagnosisSnapshot snap = new DiagnosisAnalyzer().analyze(block, spectrumOf(block), null, null);
    assertTrue(
        snap.findings().stream()
            .anyMatch(
                f ->
                    f.type() == DiagnosisType.CLIPPING
                        && f.severity() == DiagnosisSeverity.CRITICAL),
        snap.findings().toString());
  }

  @Test
  void silence_reportsLowConfidenceAndNoFalseFindings() {
    AudioBlock block = silenceBlock();
    DiagnosisSnapshot snap = new DiagnosisAnalyzer().analyze(block, spectrumOf(block), null, null);
    assertEquals(1, snap.findings().size(), snap.findings().toString());
    assertEquals(DiagnosisType.LOW_CONFIDENCE_SILENCE, snap.findings().get(0).type());
  }

  @Test
  void highFrequencyBurst_isDetectedFromHistory() {
    SpectrogramAnalyzer spectroAnalyzer = new SpectrogramAnalyzer(1024, 0, SR, 16);
    // Build a quiet history then a loud high-frequency burst at the end.
    for (int i = 0; i < 8; i++) {
      spectroAnalyzer.analyze(sineBlock(200.0, 0.02, i));
    }
    AudioBlock burst = sineBlock(6000.0, 0.6, 100L);
    spectroAnalyzer.analyze(burst);

    DiagnosisAnalyzer analyzer = new DiagnosisAnalyzer();
    DiagnosisSnapshot snap =
        analyzer.analyze(burst, spectrumOf(burst), spectroAnalyzer.history(), null);
    assertTrue(
        snap.findings().stream()
            .anyMatch(f -> f.type() == DiagnosisType.INTERMITTENT_HIGH_FREQUENCY_BURST),
        snap.findings().toString());
  }

  @Test
  void driftingPeak_isDetected() {
    SpectrogramAnalyzer spectroAnalyzer = new SpectrogramAnalyzer(1024, 0, SR, 16);
    double[] sweep = {500.0, 700.0, 900.0, 1200.0, 1500.0, 1800.0, 2200.0, 2600.0};
    for (int i = 0; i < sweep.length; i++) {
      spectroAnalyzer.analyze(sineBlock(sweep[i], 0.6, i));
    }
    AudioBlock current = sineBlock(sweep[sweep.length - 1], 0.6, 100L);
    SpectrogramHistory history = spectroAnalyzer.history();
    DiagnosisSnapshot snap =
        new DiagnosisAnalyzer().analyze(current, spectrumOf(current), history, null);
    assertTrue(
        snap.findings().stream().anyMatch(f -> f.type() == DiagnosisType.DRIFTING_PEAK),
        snap.findings().toString());
  }
}
