package org.hammer.tools;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import javax.imageio.ImageIO;
import org.hammer.audio.analysis.PeakHoldSpectrum;
import org.hammer.audio.analysis.SpectrumAnalyzer;
import org.hammer.audio.analysis.SpectrumAverager;
import org.hammer.audio.analysis.SpectrumSnapshot;
import org.hammer.audio.analysis.WaveformTrigger;
import org.hammer.audio.core.AudioBlock;
import org.hammer.audio.core.AudioFormatDescriptor;
import org.hammer.audio.signal.SineGenerator;
import org.hammer.audio.signal.SquareGenerator;
import org.hammer.audio.ui.theme.PlotRenderTheme;

/**
 * Headless utility that renders deterministic PNG screenshots used in the feature documentation.
 *
 * <p>Re-run with:
 *
 * <pre>
 *   ./mvnw -pl audio-app -am package -DskipTests
 *   java -cp "audio-app/target/audio-app-0.0.1-SNAPSHOT.jar:audio-app/target/lib/*" \
 *        org.hammer.tools.DocImageRenderer docs/images/features
 * </pre>
 *
 * <p>The output directory defaults to {@code docs/images/features} when no argument is given.
 */
public final class DocImageRenderer {

  private static final int W = 760;
  private static final int H = 320;
  private static final AudioFormatDescriptor MONO_44K = new AudioFormatDescriptor(44100f, 1, 16);
  private static final int FFT = 1024;

  private DocImageRenderer() {}

  /**
   * @param args optional output directory; defaults to {@code docs/images/features}
   * @throws IOException if any of the PNGs cannot be written
   */
  public static void main(String[] args) throws IOException {
    Path outDir = Path.of(args.length > 0 ? args[0] : "docs/images/features");
    Files.createDirectories(outDir);

    writePng(outDir.resolve("waveform-trigger.png"), renderTrigger());
    writePng(outDir.resolve("spectrum-peak-hold.png"), renderSpectrumPeakHold());
    writePng(outDir.resolve("recording-format.png"), renderRecordingFormat());
    writePng(outDir.resolve("ab-comparison.png"), renderAbComparison());
    System.out.println("Wrote feature screenshots to " + outDir.toAbsolutePath());
  }

  private static BufferedImage renderTrigger() {
    SineGenerator gen = new SineGenerator(MONO_44K, 220.0, 0.7f);
    AudioBlock block = gen.nextBlock(4096);
    WaveformTrigger trigger = new WaveformTrigger(1024);
    trigger.setMode(WaveformTrigger.Mode.NORMAL);
    trigger.setHoldoffFrames(64);
    WaveformTrigger.TriggeredView view = trigger.process(block, 0).orElseThrow();

    BufferedImage img = createImage();
    Graphics2D g = img.createGraphics();
    try {
      applyHints(g);
      Rectangle plot = new Rectangle(0, 0, W, H);
      PlotRenderTheme.drawPlotBackground(g, W, H, plot);
      PlotRenderTheme.drawGrid(g, plot, 10, 8);
      PlotRenderTheme.drawTitle(g, 10, 16, "Waveform (triggered)");

      float[] samples = view.samplesView();
      int n = samples.length;
      int centerY = H / 2;
      int amplitude = H / 2 - 8;
      int[] xs = new int[n];
      int[] ys = new int[n];
      for (int i = 0; i < n; i++) {
        xs[i] = (int) ((long) i * (W - 1) / Math.max(1, n - 1));
        ys[i] = centerY - (int) (Math.max(-1f, Math.min(1f, samples[i])) * amplitude);
      }
      g.setColor(PlotRenderTheme.CENTER_LINE);
      g.setStroke(PlotRenderTheme.AXIS_STROKE);
      g.drawLine(0, centerY, W - 1, centerY);

      g.setColor(PlotRenderTheme.WAVEFORM_LEFT);
      g.setStroke(PlotRenderTheme.TRACE_STROKE);
      g.drawPolyline(xs, ys, n);

      g.setColor(PlotRenderTheme.TEXT_MUTED);
      g.setFont(PlotRenderTheme.LABEL_FONT);
      g.drawString(
          String.format(
              Locale.ROOT,
              "Trig: FIRED  Slope: rising  Level: %+.2f  view=1024 samples",
              view.level()),
          10,
          32);
    } finally {
      g.dispose();
    }
    return img;
  }

  private static BufferedImage renderSpectrumPeakHold() {
    SineGenerator gen = new SineGenerator(MONO_44K, 1200.0, 0.6f);
    SpectrumAnalyzer analyzer = new SpectrumAnalyzer(FFT, 0, MONO_44K.sampleRate());
    SpectrumAverager avg = new SpectrumAverager(0.3f);
    PeakHoldSpectrum peak = new PeakHoldSpectrum(0.999f);

    for (int i = 0; i < 12; i++) {
      AudioBlock b = gen.nextBlock(FFT);
      SpectrumSnapshot s = analyzer.analyze(b);
      avg.update(s.magnitudesView());
      peak.update(s.magnitudesView());
    }
    SquareGenerator extra = new SquareGenerator(MONO_44K, 4400.0, 0.4f);
    AudioBlock burst = extra.nextBlock(FFT);
    SpectrumSnapshot burstSnap = analyzer.analyze(burst);
    peak.update(burstSnap.magnitudesView());

    BufferedImage img = createImage();
    Graphics2D g = img.createGraphics();
    try {
      applyHints(g);
      Rectangle plot = new Rectangle(0, 0, W, H);
      PlotRenderTheme.drawPlotBackground(g, W, H, plot);
      PlotRenderTheme.drawGrid(g, plot, 10, 8);
      PlotRenderTheme.drawTitle(g, 10, 16, "Spectrum (averaged + peak hold)");

      float[] live = avg.averageView();
      float[] held = peak.peaks();
      int bins = live.length;
      float maxMag = 1e-6f;
      for (float v : live) {
        if (Math.abs(v) > maxMag) {
          maxMag = Math.abs(v);
        }
      }
      for (float v : held) {
        if (Math.abs(v) > maxMag) {
          maxMag = Math.abs(v);
        }
      }
      int floor = H - 24;
      int top = 36;
      int[] xs = new int[bins];
      int[] ysLive = new int[bins];
      int[] ysPeak = new int[bins];
      for (int i = 0; i < bins; i++) {
        xs[i] = (int) ((long) i * (W - 1) / Math.max(1, bins - 1));
        ysLive[i] = floor - (int) ((Math.abs(live[i]) / maxMag) * (floor - top));
        ysPeak[i] = floor - (int) ((Math.abs(held[i]) / maxMag) * (floor - top));
      }
      g.setColor(PlotRenderTheme.SPECTRUM_LINE);
      g.setStroke(PlotRenderTheme.TRACE_STROKE);
      g.drawPolyline(xs, ysLive, bins);
      g.setColor(PlotRenderTheme.HIGHLIGHT);
      g.setStroke(PlotRenderTheme.PEAK_STROKE);
      g.drawPolyline(xs, ysPeak, bins);

      g.setColor(PlotRenderTheme.TEXT_MUTED);
      g.setFont(PlotRenderTheme.LABEL_FONT);
      g.drawString("solid: averaged live spectrum   dashed: peak hold", 10, 32);
    } finally {
      g.dispose();
    }
    return img;
  }

  private static BufferedImage renderRecordingFormat() {
    BufferedImage img = createImage();
    Graphics2D g = img.createGraphics();
    try {
      applyHints(g);
      g.setColor(PlotRenderTheme.PANEL_BACKGROUND);
      g.fillRect(0, 0, W, H);
      g.setColor(PlotRenderTheme.TEXT_PRIMARY);
      g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
      g.drawString("AudioAnalyzer .aar recording layout (big-endian)", 16, 26);

      int y = 56;
      int x = 16;
      int w = W - 32;
      int rowH = 28;

      drawBlock(g, x, y, w, rowH, "Header", PlotRenderTheme.SPECTRUM_LINE);
      y += rowH;
      drawField(g, x + 16, y, "u32 magic 'AAR1'", "u16 version", "u16 channels");
      y += rowH;
      drawField(g, x + 16, y, "f32 sampleRate", "u16 bitsPerSample", "u16 reserved=0");
      y += rowH + 12;

      drawBlock(g, x, y, w, rowH, "Frame record (repeats until EOF)", PlotRenderTheme.HIGHLIGHT);
      y += rowH;
      drawField(g, x + 16, y, "u32 frames", "i64 frameIndex", "i64 timestampNanos");
      y += rowH;
      drawField(g, x + 16, y, "f32 ch0 sample[0..frames)", "f32 ch1 sample[0..frames)", "...");
      y += rowH + 8;
      g.setColor(PlotRenderTheme.TEXT_MUTED);
      g.setFont(PlotRenderTheme.LABEL_FONT);
      g.drawString("Channels are stored non-interleaved within each frame record.", x + 4, y + 16);
      g.drawString(
          "Reader/writer live in audio-dsp; the format is stable from version 1 onward.",
          x + 4,
          y + 32);
    } finally {
      g.dispose();
    }
    return img;
  }

  private static void drawBlock(
      Graphics2D g, int x, int y, int w, int h, String label, Color color) {
    g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 60));
    g.fillRect(x, y, w, h);
    g.setColor(color);
    g.setStroke(new BasicStroke(1.2f));
    g.drawRect(x, y, w, h);
    g.setColor(PlotRenderTheme.TEXT_PRIMARY);
    g.setFont(PlotRenderTheme.TITLE_FONT);
    g.drawString(label, x + 8, y + h - 9);
  }

  private static void drawField(Graphics2D g, int x, int y, String a, String b, String c) {
    int colW = (W - 64) / 3;
    drawCell(g, x, y, colW, a);
    drawCell(g, x + colW + 4, y, colW, b);
    drawCell(g, x + 2 * (colW + 4), y, colW, c);
  }

  private static void drawCell(Graphics2D g, int x, int y, int w, String label) {
    g.setColor(PlotRenderTheme.PLOT_BACKGROUND);
    g.fillRect(x, y, w, 24);
    g.setColor(PlotRenderTheme.AXIS_COLOR);
    g.drawRect(x, y, w, 24);
    g.setColor(PlotRenderTheme.TEXT_PRIMARY);
    g.setFont(PlotRenderTheme.LABEL_FONT);
    g.drawString(label, x + 6, y + 16);
  }

  private static BufferedImage renderAbComparison() {
    SpectrumAnalyzer analyzer = new SpectrumAnalyzer(FFT, 0, MONO_44K.sampleRate());
    SineGenerator a = new SineGenerator(MONO_44K, 440.0, 0.6f);
    SineGenerator b = new SineGenerator(MONO_44K, 880.0, 0.6f);
    SpectrumSnapshot sa = null;
    SpectrumSnapshot sb = null;
    for (int i = 0; i < 6; i++) {
      sa = analyzer.analyze(a.nextBlock(FFT));
    }
    for (int i = 0; i < 6; i++) {
      sb = analyzer.analyze(b.nextBlock(FFT));
    }

    BufferedImage img = createImage();
    Graphics2D g = img.createGraphics();
    try {
      applyHints(g);
      g.setColor(PlotRenderTheme.PANEL_BACKGROUND);
      g.fillRect(0, 0, W, H);
      int halfW = W / 2 - 4;
      Rectangle leftPlot = new Rectangle(0, 0, halfW, H);
      Rectangle rightPlot = new Rectangle(halfW + 8, 0, halfW, H);

      drawSpectrumPanel(g, leftPlot, sa, "A — 440 Hz", PlotRenderTheme.SPECTRUM_LINE);
      drawSpectrumPanel(g, rightPlot, sb, "B — 880 Hz", PlotRenderTheme.WAVEFORM_RIGHT);

      g.setColor(PlotRenderTheme.HIGHLIGHT);
      g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
      g.drawString("|delta dominant freq| ~ 440 Hz", W / 2 - 96, H - 12);
    } finally {
      g.dispose();
    }
    return img;
  }

  private static void drawSpectrumPanel(
      Graphics2D g, Rectangle plot, SpectrumSnapshot snap, String title, Color color) {
    PlotRenderTheme.drawPlotBackground(g, plot.width, plot.height, plot);
    PlotRenderTheme.drawGrid(g, plot, 10, 8);
    PlotRenderTheme.drawTitle(g, plot.x + 8, plot.y + 16, title);
    float[] mag = snap.magnitudesView();
    int bins = mag.length;
    float max = 1e-6f;
    for (float v : mag) {
      if (Math.abs(v) > max) {
        max = Math.abs(v);
      }
    }
    int floor = plot.y + plot.height - 24;
    int top = plot.y + 36;
    int[] xs = new int[bins];
    int[] ys = new int[bins];
    for (int i = 0; i < bins; i++) {
      xs[i] = plot.x + (int) ((long) i * (plot.width - 1) / Math.max(1, bins - 1));
      ys[i] = floor - (int) ((Math.abs(mag[i]) / max) * (floor - top));
    }
    g.setColor(color);
    g.setStroke(PlotRenderTheme.TRACE_STROKE);
    g.drawPolyline(xs, ys, bins);
  }

  private static BufferedImage createImage() {
    return new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
  }

  private static void applyHints(Graphics2D g) {
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
  }

  private static void writePng(Path out, BufferedImage img) throws IOException {
    if (!ImageIO.write(img, "png", out.toFile())) {
      throw new IOException("PNG writer not available for " + out);
    }
  }
}
