package org.hammer.audio.diagnosis;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.hammer.audio.analysis.SpectrumSnapshot;
import org.hammer.audio.core.AudioBlock;
import org.hammer.audio.localization.StereoDelaySnapshot;
import org.hammer.audio.spectrogram.SpectrogramFrame;
import org.hammer.audio.spectrogram.SpectrogramHistory;

/**
 * Rule-based acoustic diagnosis: consumes one {@link AudioBlock}, one optional {@link
 * SpectrumSnapshot}, optional {@link SpectrogramHistory} and optional {@link StereoDelaySnapshot}
 * and emits an immutable {@link DiagnosisSnapshot} of findings.
 *
 * <p>Initial rule set:
 *
 * <ul>
 *   <li>Hard clipping (any sample magnitude {@code >= clippingThreshold}).
 *   <li>Low confidence / silence (RMS below {@code silenceRmsThreshold}).
 *   <li>Mains hum at 50 Hz / 60 Hz, including 100 / 150 Hz or 120 / 180 Hz harmonics.
 *   <li>Dominant tone (single prominent peak well above the local median spectrum).
 *   <li>Broadband noise (near-flat spectrum with no prominent peak).
 *   <li>Intermittent high-frequency burst (recent high-band frame energy spikes vs. its mean).
 *   <li>Drifting peak (dominant frequency moves across the recent history).
 * </ul>
 *
 * <p>This analyzer is stateless aside from its configuration constants; instances are thread-safe
 * provided callers do not share the supplied snapshots or histories across threads.
 */
@SuppressWarnings({"PMD.GodClass", "PMD.CyclomaticComplexity", "PMD.TooManyMethods"})
public final class DiagnosisAnalyzer {

  /** Default sample-magnitude threshold treated as clipping. */
  public static final double DEFAULT_CLIPPING_THRESHOLD = 0.999;

  /** Default RMS threshold below which the signal is considered effectively silent. */
  public static final double DEFAULT_SILENCE_RMS_THRESHOLD = 1.0e-3;

  /** Default dominance ratio (peak / median) above which a single tone is reported. */
  public static final double DEFAULT_DOMINANCE_RATIO = 8.0;

  /** Default flatness threshold above which a spectrum is treated as broadband noise. */
  public static final double DEFAULT_FLATNESS_THRESHOLD = 0.45;

  /** Default high-frequency cutoff for the intermittent-burst rule (Hz). */
  public static final double DEFAULT_HIGH_FREQUENCY_CUTOFF_HZ = 4000.0;

  /** Default factor by which the last frame's high-band energy must exceed history mean. */
  public static final double DEFAULT_BURST_RATIO = 3.0;

  /** Default minimum drift (Hz) of the dominant peak across history to flag a drifting peak. */
  public static final double DEFAULT_DRIFT_HZ = 80.0;

  private static final double HUM_TOLERANCE_HZ = 4.0;
  private static final double HUM_RATIO_THRESHOLD = 5.0;
  private static final double HUM_RELATIVE_THRESHOLD = 0.25;
  private static final double HARMONIC_RATIO_THRESHOLD = 3.0;

  private final double clippingThreshold;
  private final double silenceRmsThreshold;
  private final double dominanceRatio;
  private final double flatnessThreshold;
  private final double highFrequencyCutoffHz;
  private final double burstRatio;
  private final double driftHz;

  /** Create a diagnosis analyzer with default thresholds. */
  public DiagnosisAnalyzer() {
    this(
        DEFAULT_CLIPPING_THRESHOLD,
        DEFAULT_SILENCE_RMS_THRESHOLD,
        DEFAULT_DOMINANCE_RATIO,
        DEFAULT_FLATNESS_THRESHOLD,
        DEFAULT_HIGH_FREQUENCY_CUTOFF_HZ,
        DEFAULT_BURST_RATIO,
        DEFAULT_DRIFT_HZ);
  }

  /**
   * Create a diagnosis analyzer with explicit thresholds.
   *
   * @param clippingThreshold sample magnitude threshold for clipping
   * @param silenceRmsThreshold RMS threshold below which the signal is considered silent
   * @param dominanceRatio peak / median ratio required to report a dominant tone
   * @param flatnessThreshold spectral flatness threshold for broadband noise
   * @param highFrequencyCutoffHz low edge of the high-band used for burst detection
   * @param burstRatio current / mean high-band energy ratio for burst detection
   * @param driftHz minimum drift in Hz of the dominant peak required for a drift finding
   */
  public DiagnosisAnalyzer(
      double clippingThreshold,
      double silenceRmsThreshold,
      double dominanceRatio,
      double flatnessThreshold,
      double highFrequencyCutoffHz,
      double burstRatio,
      double driftHz) {
    this.clippingThreshold = clippingThreshold;
    this.silenceRmsThreshold = silenceRmsThreshold;
    this.dominanceRatio = dominanceRatio;
    this.flatnessThreshold = flatnessThreshold;
    this.highFrequencyCutoffHz = highFrequencyCutoffHz;
    this.burstRatio = burstRatio;
    this.driftHz = driftHz;
  }

  /**
   * Run the diagnostic rules and produce a snapshot.
   *
   * @param block latest audio block (may be {@code null})
   * @param spectrum latest spectrum snapshot (may be {@code null})
   * @param history rolling spectrogram history (may be {@code null} or empty)
   * @param stereoDelay latest stereo delay snapshot (may be {@code null})
   * @return immutable diagnosis snapshot; never {@code null}
   */
  public DiagnosisSnapshot analyze(
      AudioBlock block,
      SpectrumSnapshot spectrum,
      SpectrogramHistory history,
      StereoDelaySnapshot stereoDelay) {
    long frameIndex = 0L;
    long timestamp = 0L;
    if (block != null) {
      frameIndex = block.frameIndex();
      timestamp = block.timestampNanos();
    } else if (spectrum != null) {
      frameIndex = spectrum.sourceFrameIndex();
      timestamp = spectrum.sourceTimestampNanos();
    } else if (stereoDelay != null) {
      frameIndex = stereoDelay.sourceFrameIndex();
      timestamp = stereoDelay.sourceTimestampNanos();
    }

    List<DiagnosisFinding> findings = new ArrayList<>();

    LevelStats stats = computeLevelStats(block);
    boolean clipping = stats.maxAbs >= clippingThreshold;
    if (clipping) {
      findings.add(
          new DiagnosisFinding(
              DiagnosisType.CLIPPING,
              DiagnosisSeverity.CRITICAL,
              1.0,
              String.format(
                  Locale.ROOT,
                  "Clipping detected: peak level reached %.3f.",
                  Math.min(1.0, stats.maxAbs)),
              Double.NaN,
              stats.maxAbs));
    }

    boolean silent = stats.frames > 0 && stats.rms < silenceRmsThreshold;
    if (silent) {
      findings.add(
          new DiagnosisFinding(
              DiagnosisType.LOW_CONFIDENCE_SILENCE,
              DiagnosisSeverity.INFO,
              clamp01(1.0 - stats.rms / Math.max(silenceRmsThreshold, 1e-9)),
              String.format(
                  Locale.ROOT, "Low confidence: signal level too low (RMS=%.4f).", stats.rms),
              Double.NaN,
              stats.rms));
      // Without enough signal energy we should not produce spectral findings.
      return new DiagnosisSnapshot(frameIndex, timestamp, sortFindings(findings));
    }

    if (spectrum != null && spectrum.binCount() > 2) {
      addSpectralFindings(spectrum, findings);
    }
    if (history != null && history.size() >= 4) {
      addHistoryFindings(history, findings);
    }

    return new DiagnosisSnapshot(frameIndex, timestamp, sortFindings(findings));
  }

  private void addSpectralFindings(SpectrumSnapshot spectrum, List<DiagnosisFinding> findings) {
    float[] magnitudes = spectrum.magnitudes();
    int peakBin = findPeakBin(magnitudes);
    if (peakBin <= 0) {
      return;
    }
    double peakHz = spectrum.frequencyOfBin(peakBin);
    double peakMag = magnitudes[peakBin];
    double median = robustMedian(magnitudes);
    double ratio = peakMag / Math.max(median, 1e-9);

    HumResult hum50 = humEnergyRatio(spectrum, magnitudes, 50.0, median, peakMag);
    HumResult hum60 = humEnergyRatio(spectrum, magnitudes, 60.0, median, peakMag);
    boolean has50 = hum50.ratio >= HUM_RATIO_THRESHOLD && hum50.relative >= HUM_RELATIVE_THRESHOLD;
    boolean has60 = hum60.ratio >= HUM_RATIO_THRESHOLD && hum60.relative >= HUM_RELATIVE_THRESHOLD;
    if (has50 && hum50.ratio >= hum60.ratio) {
      findings.add(buildHumFinding(spectrum, magnitudes, median, 50.0, hum50));
    } else if (has60) {
      findings.add(buildHumFinding(spectrum, magnitudes, median, 60.0, hum60));
    }

    double flatness = spectralFlatness(magnitudes);
    boolean broadband = flatness >= flatnessThreshold && ratio < dominanceRatio;
    if (broadband) {
      findings.add(
          new DiagnosisFinding(
              DiagnosisType.BROADBAND_NOISE,
              DiagnosisSeverity.INFO,
              clamp01((flatness - flatnessThreshold) / Math.max(1.0 - flatnessThreshold, 1e-6)),
              String.format(
                  Locale.ROOT, "Broadband noise: flat spectrum (flatness=%.2f).", flatness),
              Double.NaN,
              flatness));
    }

    // Dominant tone: avoid re-reporting if we already flagged this as hum.
    boolean alreadyHum =
        findings.stream()
            .anyMatch(
                f ->
                    f.type() == DiagnosisType.MAINS_HUM_50HZ
                        || f.type() == DiagnosisType.MAINS_HUM_60HZ);
    if (ratio >= dominanceRatio && !alreadyHum && !broadband) {
      findings.add(
          new DiagnosisFinding(
              DiagnosisType.DOMINANT_TONE,
              DiagnosisSeverity.INFO,
              clamp01(Math.log10(ratio) / 2.0),
              String.format(Locale.ROOT, "Dominant tone: %.1f Hz.", peakHz),
              peakHz,
              ratio));
    }
  }

  private DiagnosisFinding buildHumFinding(
      SpectrumSnapshot spectrum,
      float[] magnitudes,
      double median,
      double fundamentalHz,
      HumResult hum) {
    double h2 = harmonicRatio(spectrum, magnitudes, fundamentalHz * 2.0, median);
    double h3 = harmonicRatio(spectrum, magnitudes, fundamentalHz * 3.0, median);
    List<String> harmonics = new ArrayList<>();
    if (h2 >= HARMONIC_RATIO_THRESHOLD) {
      harmonics.add(String.format(Locale.ROOT, "%.0f Hz", fundamentalHz * 2.0));
    }
    if (h3 >= HARMONIC_RATIO_THRESHOLD) {
      harmonics.add(String.format(Locale.ROOT, "%.0f Hz", fundamentalHz * 3.0));
    }
    String harmonicText;
    if (harmonics.isEmpty()) {
      harmonicText = "no clear harmonics";
    } else if (harmonics.size() == 1) {
      harmonicText = "harmonic at " + harmonics.get(0);
    } else {
      harmonicText =
          "harmonics at " + harmonics.get(0) + " and " + harmonics.get(harmonics.size() - 1);
    }
    DiagnosisType type =
        fundamentalHz < 55.0 ? DiagnosisType.MAINS_HUM_50HZ : DiagnosisType.MAINS_HUM_60HZ;
    DiagnosisSeverity severity =
        harmonics.isEmpty() ? DiagnosisSeverity.INFO : DiagnosisSeverity.WARNING;
    double confidence = clamp01(Math.log10(Math.max(1.0, hum.ratio)) / 1.5);
    String message =
        String.format(
            Locale.ROOT, "Likely mains hum: %.0f Hz with %s.", fundamentalHz, harmonicText);
    return new DiagnosisFinding(type, severity, confidence, message, fundamentalHz, hum.ratio);
  }

  private void addHistoryFindings(SpectrogramHistory history, List<DiagnosisFinding> findings) {
    BurstResult burst = analyzeBurst(history);
    if (burst != null && burst.ratio >= burstRatio) {
      findings.add(
          new DiagnosisFinding(
              DiagnosisType.INTERMITTENT_HIGH_FREQUENCY_BURST,
              DiagnosisSeverity.WARNING,
              clamp01((burst.ratio - burstRatio) / (burstRatio * 2.0)),
              String.format(
                  Locale.ROOT,
                  "Intermittent high-frequency bursts detected (>%.0f Hz, ratio=%.1fx).",
                  highFrequencyCutoffHz,
                  burst.ratio),
              burst.dominantHz,
              burst.ratio));
    }

    DriftResult drift = analyzeDrift(history);
    if (drift != null && drift.spanHz >= driftHz) {
      findings.add(
          new DiagnosisFinding(
              DiagnosisType.DRIFTING_PEAK,
              DiagnosisSeverity.INFO,
              clamp01(drift.spanHz / Math.max(driftHz * 4.0, 1.0)),
              String.format(
                  Locale.ROOT,
                  "Drifting peak: dominant frequency moved %.0f Hz (%.0f → %.0f Hz).",
                  drift.spanHz,
                  drift.startHz,
                  drift.endHz),
              drift.endHz,
              drift.spanHz));
    }
  }

  /* ---------------- helpers ---------------- */

  private static LevelStats computeLevelStats(AudioBlock block) {
    if (block == null || block.channels() == 0 || block.frames() == 0) {
      return new LevelStats(0.0, 0.0, 0);
    }
    double sumSquares = 0.0;
    double maxAbs = 0.0;
    long count = 0L;
    for (int c = 0; c < block.channels(); c++) {
      float[] samples = block.channelView(c);
      for (int i = 0; i < block.frames(); i++) {
        float s = samples[i];
        double abs = Math.abs(s);
        sumSquares += (double) s * s;
        if (abs > maxAbs) {
          maxAbs = abs;
        }
        count++;
      }
    }
    double rms = count == 0L ? 0.0 : Math.sqrt(sumSquares / count);
    return new LevelStats(rms, maxAbs, block.frames());
  }

  private static int findPeakBin(float[] magnitudes) {
    int peakBin = -1;
    float peakMag = 0f;
    for (int i = 1; i < magnitudes.length; i++) {
      if (magnitudes[i] > peakMag) {
        peakMag = magnitudes[i];
        peakBin = i;
      }
    }
    return peakBin;
  }

  private static double robustMedian(float[] magnitudes) {
    if (magnitudes.length <= 2) {
      return 0.0;
    }
    float[] copy = new float[magnitudes.length - 1];
    System.arraycopy(magnitudes, 1, copy, 0, copy.length);
    java.util.Arrays.sort(copy);
    int mid = copy.length / 2;
    return copy[mid];
  }

  private HumResult humEnergyRatio(
      SpectrumSnapshot spectrum,
      float[] magnitudes,
      double targetHz,
      double median,
      double globalPeak) {
    int peakBin =
        findPeakBinInRange(spectrum, targetHz - HUM_TOLERANCE_HZ, targetHz + HUM_TOLERANCE_HZ);
    if (peakBin < 0) {
      return new HumResult(0.0, 0.0, 0);
    }
    double mag = magnitudes[peakBin];
    double ratio = mag / Math.max(median, 1e-9);
    double relative = globalPeak > 0.0 ? mag / globalPeak : 0.0;
    return new HumResult(ratio, relative, peakBin);
  }

  private double harmonicRatio(
      SpectrumSnapshot spectrum, float[] magnitudes, double targetHz, double median) {
    if (targetHz > spectrum.sampleRate() / 2.0) {
      return 0.0;
    }
    int peakBin =
        findPeakBinInRange(spectrum, targetHz - HUM_TOLERANCE_HZ, targetHz + HUM_TOLERANCE_HZ);
    if (peakBin < 0) {
      return 0.0;
    }
    return magnitudes[peakBin] / Math.max(median, 1e-9);
  }

  private static int findPeakBinInRange(SpectrumSnapshot spectrum, double minHz, double maxHz) {
    float binWidth = spectrum.binWidthHz();
    if (binWidth <= 0f) {
      return -1;
    }
    int minBin = Math.max(1, (int) Math.floor(minHz / binWidth));
    int maxBin = Math.min(spectrum.binCount() - 1, (int) Math.ceil(maxHz / binWidth));
    int peakBin = -1;
    float peakMag = 0f;
    for (int i = minBin; i <= maxBin; i++) {
      float m = spectrum.magnitude(i);
      if (m > peakMag) {
        peakMag = m;
        peakBin = i;
      }
    }
    return peakBin;
  }

  private static double spectralFlatness(float[] magnitudes) {
    // Geometric mean / arithmetic mean on bins above DC.
    if (magnitudes.length <= 2) {
      return 0.0;
    }
    double sumLog = 0.0;
    double sum = 0.0;
    int n = 0;
    for (int i = 1; i < magnitudes.length; i++) {
      double m = Math.max(magnitudes[i], 1e-9);
      sumLog += Math.log(m);
      sum += m;
      n++;
    }
    if (n == 0 || sum <= 0.0) {
      return 0.0;
    }
    double geo = Math.exp(sumLog / n);
    double arith = sum / n;
    return geo / arith;
  }

  private BurstResult analyzeBurst(SpectrogramHistory history) {
    SpectrogramFrame latest = history.latest();
    if (latest == null) {
      return null;
    }
    float binWidth = latest.binWidthHz();
    int startBin = Math.max(1, (int) Math.floor(highFrequencyCutoffHz / Math.max(binWidth, 1e-6)));
    if (startBin >= latest.binCount()) {
      return null;
    }
    double currentEnergy = highBandEnergy(latest, startBin);
    int historyFrames = history.size();
    if (historyFrames < 4) {
      return null;
    }
    double sum = 0.0;
    int count = 0;
    double peakBaseline = 0.0;
    for (int i = 0; i < historyFrames - 1; i++) {
      double energy = highBandEnergy(history.frameAt(i), startBin);
      sum += energy;
      if (energy > peakBaseline) {
        peakBaseline = energy;
      }
      count++;
    }
    double mean = sum / Math.max(count, 1);
    double denominator = Math.max(mean, 1e-9);
    double ratio = currentEnergy / denominator;
    // Require both elevated vs. mean and elevated vs. recent maximum to avoid sustained-noise
    // false positives.
    double peakRatio = currentEnergy / Math.max(peakBaseline, 1e-9);
    if (peakRatio < 1.5) {
      return null;
    }
    int dominantHfBin = findDominantHighBandBin(latest, startBin);
    double dominantHz = latest.frequencyOfBin(dominantHfBin);
    return new BurstResult(ratio, dominantHz);
  }

  private static double highBandEnergy(SpectrogramFrame frame, int startBin) {
    float[] view = frame.magnitudesView();
    double sum = 0.0;
    for (int i = startBin; i < view.length; i++) {
      sum += (double) view[i] * view[i];
    }
    return sum;
  }

  private static int findDominantHighBandBin(SpectrogramFrame frame, int startBin) {
    float[] view = frame.magnitudesView();
    int peakBin = startBin;
    float peakMag = 0f;
    for (int i = startBin; i < view.length; i++) {
      if (view[i] > peakMag) {
        peakMag = view[i];
        peakBin = i;
      }
    }
    return peakBin;
  }

  private DriftResult analyzeDrift(SpectrogramHistory history) {
    int frames = history.size();
    if (frames < 4) {
      return null;
    }
    double startHz = dominantHzOf(history.frameAt(0));
    double endHz = dominantHzOf(history.frameAt(frames - 1));
    double min = Math.min(startHz, endHz);
    double max = Math.max(startHz, endHz);
    int sampleCount = Math.min(frames, 8);
    for (int i = 0; i < sampleCount; i++) {
      int idx = (int) Math.round((double) i * (frames - 1) / Math.max(sampleCount - 1, 1));
      double hz = dominantHzOf(history.frameAt(idx));
      min = Math.min(min, hz);
      max = Math.max(max, hz);
    }
    double span = max - min;
    return new DriftResult(startHz, endHz, span);
  }

  private static double dominantHzOf(SpectrogramFrame frame) {
    float[] view = frame.magnitudesView();
    int peakBin = -1;
    float peakMag = 0f;
    for (int i = 1; i < view.length; i++) {
      if (view[i] > peakMag) {
        peakMag = view[i];
        peakBin = i;
      }
    }
    return peakBin <= 0 ? 0.0 : frame.frequencyOfBin(peakBin);
  }

  private static List<DiagnosisFinding> sortFindings(List<DiagnosisFinding> findings) {
    findings.sort(
        Comparator.<DiagnosisFinding>comparingInt(f -> severityRank(f.severity()))
            .reversed()
            .thenComparing(Comparator.comparingDouble(DiagnosisFinding::confidence).reversed()));
    return findings;
  }

  private static int severityRank(DiagnosisSeverity severity) {
    return switch (severity) {
      case CRITICAL -> 3;
      case WARNING -> 2;
      case INFO -> 1;
    };
  }

  private static double clamp01(double v) {
    if (Double.isNaN(v)) {
      return 0.0;
    }
    if (v < 0.0) {
      return 0.0;
    }
    if (v > 1.0) {
      return 1.0;
    }
    return v;
  }

  private record LevelStats(double rms, double maxAbs, int frames) {}

  private record HumResult(double ratio, double relative, int peakBin) {}

  private record BurstResult(double ratio, double dominantHz) {}

  private record DriftResult(double startHz, double endHz, double spanHz) {}
}
