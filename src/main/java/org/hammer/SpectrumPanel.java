package org.hammer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import org.hammer.audio.AudioCaptureService;
import org.hammer.audio.analysis.SpectrumAnalyzer;
import org.hammer.audio.analysis.SpectrumSnapshot;
import org.hammer.audio.core.AudioBlock;

/** Panel for displaying the current FFT magnitude spectrum. */
public final class SpectrumPanel extends javax.swing.JPanel {

  private static final long serialVersionUID = 1L;
  private static final int FFT_SIZE = 1024;
  private static final int LEFT_MARGIN = 44;
  private static final int RIGHT_MARGIN = 12;
  private static final int TOP_MARGIN = 18;
  private static final int BOTTOM_MARGIN = 28;
  private static final float SAMPLE_RATE_TOLERANCE = 0.0001f;

  private AudioCaptureService audioCaptureService;
  private transient SpectrumAnalyzer analyzer;
  private transient SpectrumSnapshot latestSpectrum;
  private transient SpectrumSnapshot frozenSpectrum;
  private long latestSpectrumFrameIndex = Long.MIN_VALUE;
  private long latestSpectrumTimestampNanos = Long.MIN_VALUE;
  private boolean frozen;

  public SpectrumPanel() {
    super(true);
    setPreferredSize(new Dimension(320, 180));
    javax.swing.Timer timer =
        new javax.swing.Timer(UiConstants.REFRESH_INTERVAL_MS, e -> repaint());
    timer.start();
  }

  /**
   * Set the audio capture service that supplies audio blocks to analyze.
   *
   * @param service the audio service
   */
  public void setAudioCaptureService(AudioCaptureService service) {
    this.audioCaptureService = service;
    this.analyzer = null;
    this.latestSpectrum = null;
    this.frozenSpectrum = null;
    this.latestSpectrumFrameIndex = Long.MIN_VALUE;
    this.latestSpectrumTimestampNanos = Long.MIN_VALUE;
    this.frozen = false;
  }

  /**
   * Freeze or unfreeze the currently displayed spectrum.
   *
   * @param frozen true to freeze the current spectrum
   */
  public void setFrozen(boolean frozen) {
    if (frozen && !this.frozen) {
      frozenSpectrum = getCurrentSpectrum();
    } else if (!frozen) {
      frozenSpectrum = null;
    }
    this.frozen = frozen;
    repaint();
  }

  /**
   * @return current spectrum snapshot, or {@code null} if no audio block is available
   */
  public SpectrumSnapshot getCurrentSpectrum() {
    if (frozen && frozenSpectrum != null) {
      return frozenSpectrum;
    }
    AudioCaptureService service = audioCaptureService;
    if (service == null) {
      return latestSpectrum;
    }
    AudioBlock block = service.getLatestBlock();
    if (block == null) {
      return latestSpectrum;
    }
    if (latestSpectrum != null
        && latestSpectrumFrameIndex == block.frameIndex()
        && latestSpectrumTimestampNanos == block.timestampNanos()) {
      return latestSpectrum;
    }
    SpectrumAnalyzer currentAnalyzer = analyzer;
    if (currentAnalyzer == null
        || currentAnalyzer.fftSize() != FFT_SIZE
        || latestSpectrum == null
        || Math.abs(latestSpectrum.sampleRate() - block.format().sampleRate())
            > SAMPLE_RATE_TOLERANCE) {
      currentAnalyzer = new SpectrumAnalyzer(FFT_SIZE, 0, block.format().sampleRate());
      analyzer = currentAnalyzer;
    }
    latestSpectrum = currentAnalyzer.analyze(block);
    latestSpectrumFrameIndex = block.frameIndex();
    latestSpectrumTimestampNanos = block.timestampNanos();
    return latestSpectrum;
  }

  /**
   * @return peak frequency in Hz, or {@code NaN} if no spectrum is available
   */
  public double getPeakFrequencyHz() {
    SpectrumSnapshot spectrum = getCurrentSpectrum();
    if (spectrum == null || spectrum.binCount() <= 1) {
      return Double.NaN;
    }
    int peakBin = -1;
    float peakMagnitude = 0f;
    for (int bin = 1; bin < spectrum.binCount(); bin++) {
      float magnitude = spectrum.magnitude(bin);
      if (magnitude > peakMagnitude) {
        peakMagnitude = magnitude;
        peakBin = bin;
      }
    }
    if (peakBin < 0) {
      return Double.NaN;
    }
    return spectrum.frequencyOfBin(peakBin);
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      paintSpectrum(g2);
    } finally {
      g2.dispose();
    }
  }

  private void paintSpectrum(Graphics2D g) {
    int width = getWidth();
    int height = getHeight();
    g.setColor(Color.BLACK);
    g.fillRect(0, 0, width, height);

    g.setColor(Color.DARK_GRAY);
    int plotX = LEFT_MARGIN;
    int plotY = TOP_MARGIN;
    int plotWidth = Math.max(1, width - LEFT_MARGIN - RIGHT_MARGIN);
    int plotHeight = Math.max(1, height - TOP_MARGIN - BOTTOM_MARGIN);
    g.drawRect(plotX, plotY, plotWidth, plotHeight);

    SpectrumSnapshot spectrum = getCurrentSpectrum();
    if (spectrum == null) {
      g.setColor(Color.LIGHT_GRAY);
      g.drawString("No spectrum data", plotX + 8, plotY + plotHeight / 2);
      return;
    }

    float[] magnitudes = spectrum.magnitudes();
    float max = 0f;
    for (int i = 1; i < magnitudes.length; i++) {
      max = Math.max(max, magnitudes[i]);
    }
    if (max <= 0f) {
      max = 1f;
    }

    g.setColor(new Color(80, 220, 120));
    int bins = Math.max(1, magnitudes.length - 1);
    for (int bin = 1; bin < magnitudes.length; bin++) {
      int x = plotX + (int) Math.round((bin - 1) * (plotWidth - 1.0) / bins);
      int nextX = plotX + (int) Math.round(bin * (plotWidth - 1.0) / bins);
      int barWidth = Math.max(1, nextX - x);
      int barHeight = Math.round((magnitudes[bin] / max) * plotHeight);
      g.fillRect(x, plotY + plotHeight - barHeight, barWidth, barHeight);
    }

    double peakHz = getPeakFrequencyHz();
    g.setColor(Color.WHITE);
    g.drawString("FFT spectrum", plotX, 14);
    g.drawString("0 Hz", plotX, height - 8);
    g.drawString(String.format("%.0f Hz", spectrum.sampleRate() / 2.0), width - 74, height - 8);
    if (!Double.isNaN(peakHz)) {
      g.setColor(Color.ORANGE);
      g.drawString(String.format("Peak: %.1f Hz", peakHz), plotX + 100, 14);
    }
  }
}
