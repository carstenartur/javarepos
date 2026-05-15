package org.hammer;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.hammer.audio.AudioCaptureService;
import org.hammer.audio.AudioCaptureServiceImpl;
import org.hammer.audio.analysis.SpectrumSnapshot;
import org.hammer.audio.core.AudioBlock;

/**
 * Main application frame for audio analysis and visualization.
 *
 * <p>Refactored to use dependency injection with AudioCaptureService instead of direct singleton
 * access. The frame creates the service and injects it into UI panels. Audio capture is
 * started/stopped via the menu.
 *
 * @author chammer
 */
public class AudioAnalyseFrame extends JFrame {
  private static final long serialVersionUID = 1L;
  private static final Logger LOGGER = Logger.getLogger(AudioAnalyseFrame.class.getName());

  private static final float DEFAULT_SAMPLE_RATE = 16000.0f;
  private static final int DEFAULT_SAMPLE_BITS = 8;
  private static final int DEFAULT_CHANNELS = 2;
  private static final boolean DEFAULT_SIGNED = false;
  private static final boolean DEFAULT_BIG_ENDIAN = false;

  private final JPanel contentPane;
  private final JPanel visualizationPanel = new JPanel(new BorderLayout(4, 4));
  private final WaveformPanel waveformPanel = new WaveformPanel();
  private final PhaseDiagramPanel phaseDiagramPanel = new PhaseDiagramPanel();
  private final SpectrumPanel spectrumPanel = new SpectrumPanel();

  private final JTextField textFieldDataSize;
  private final JTextField textFieldDivisor;
  private final JTextField textFieldAudioFormat;
  private final JTextField textFieldPeakFrequency;
  private final JComboBox<AudioDeviceItem> comboBoxAudioDevice;

  private final JCheckBoxMenuItem mntmStart;
  private final JCheckBoxMenuItem mntmFreeze;
  private final Timer refreshTimer;

  // Audio capture service
  private AudioCaptureService audioCaptureService;
  private AudioBlock frozenBlock;

  public static void main(String[] args) {
    EventQueue.invokeLater(
        () -> {
          try {
            LOGGER.info("Starting AudioAnalyseFrame application");
            AudioAnalyseFrame frame = new AudioAnalyseFrame();
            frame.setVisible(true);
          } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start application", e);
            e.printStackTrace();
          }
        });
  }

  public AudioAnalyseFrame() {
    LOGGER.info("AudioAnalyseFrame constructor started");
    setTitle("AudioAnalyzer");
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);

    contentPane = new JPanel(new BorderLayout(5, 5));
    contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
    setContentPane(contentPane);

    // Initialize fields before calling init methods
    textFieldDataSize = new JTextField();
    textFieldDivisor = new JTextField();
    textFieldAudioFormat = new JTextField();
    textFieldPeakFrequency = new JTextField();
    comboBoxAudioDevice = new JComboBox<>();
    mntmStart = new JCheckBoxMenuItem("Start/Stop");
    mntmFreeze = new JCheckBoxMenuItem("Pause/Freeze");

    initializeAudioService(null);

    initMenu();
    initTopSettingsPanel();
    initCenterAndEast();
    initSouthSlider();

    refreshTimer = new Timer(UiConstants.REFRESH_INTERVAL_MS, e -> updateUIFromModel());
    refreshTimer.setRepeats(true);
    refreshTimer.start();

    addWindowListener(
        new WindowAdapter() {
          @Override
          public void windowClosing(WindowEvent e) {
            stopAudioIfRunning();
            if (refreshTimer != null && refreshTimer.isRunning()) {
              refreshTimer.stop();
            }
          }
        });

    pack();
    setSize(900, 680);
    setLocationRelativeTo(null);
    LOGGER.info("AudioAnalyseFrame initialized successfully");
  }

  /** Initialize the audio capture service and inject into panels. */
  private void initializeAudioService(Mixer.Info mixerInfo) {
    LOGGER.info("Initializing audio capture service");
    int divisor = audioCaptureService != null ? audioCaptureService.getDivisor() : 1;
    audioCaptureService =
        new AudioCaptureServiceImpl(
            DEFAULT_SAMPLE_RATE,
            DEFAULT_SAMPLE_BITS,
            DEFAULT_CHANNELS,
            DEFAULT_SIGNED,
            DEFAULT_BIG_ENDIAN,
            divisor,
            mixerInfo);

    waveformPanel.setAudioCaptureService(audioCaptureService);
    phaseDiagramPanel.setAudioCaptureService(audioCaptureService);
    spectrumPanel.setAudioCaptureService(audioCaptureService);
    setFrozen(false);
  }

  private void initMenu() {
    JMenuBar menuBar = new JMenuBar();
    setJMenuBar(menuBar);

    JMenu mnFile = new JMenu("File");
    menuBar.add(mnFile);

    mntmStart.setToolTipText("Start or stop audio input");
    mntmStart.addActionListener(this::toggleAudioStartStop);
    mnFile.add(mntmStart);

    mntmFreeze.setToolTipText("Freeze waveform and spectrum snapshots for inspection/export");
    mntmFreeze.addActionListener(e -> setFrozen(mntmFreeze.isSelected()));
    mnFile.add(mntmFreeze);

    mnFile.addSeparator();

    JMenuItem exportCsv = new JMenuItem("Export measurement CSV...");
    exportCsv.addActionListener(e -> exportMeasurementCsv());
    mnFile.add(exportCsv);

    JMenuItem exportPng = new JMenuItem("Export measurement PNG...");
    exportPng.addActionListener(e -> exportMeasurementPng());
    mnFile.add(exportPng);

    menuBar.add(Box.createGlue());

    JMenu mnHelp = new JMenu("Help");
    menuBar.add(mnHelp);

    JMenuItem mntmAbout = new JMenuItem();
    mntmAbout.setAction(new SwingAction());
    mnHelp.add(mntmAbout);
  }

  private void initTopSettingsPanel() {
    JPanel textfelder = new JPanel();
    textfelder.setBorder(
        new TitledBorder(
            new EtchedBorder(EtchedBorder.LOWERED, null, null),
            "Settings",
            TitledBorder.LEADING,
            TitledBorder.TOP,
            null,
            null));
    contentPane.add(textfelder, BorderLayout.NORTH);
    textfelder.setLayout(new GridLayout(5, 2, 4, 2));

    JLabel lblAudioDevice = new JLabel("audio device");
    textfelder.add(lblAudioDevice);

    populateAudioDeviceChoices();
    comboBoxAudioDevice.addItemListener(this::audioDeviceSelectionChanged);
    textfelder.add(comboBoxAudioDevice);

    JLabel lblDatasize = new JLabel("datasize");
    textfelder.add(lblDatasize);

    textFieldDataSize.setEnabled(false);
    textFieldDataSize.setEditable(false);
    textFieldDataSize.setColumns(10);
    textfelder.add(textFieldDataSize);

    JLabel lblDivisor = new JLabel("divisor");
    textfelder.add(lblDivisor);

    textFieldDivisor.setEnabled(false);
    textFieldDivisor.setEditable(false);
    textFieldDivisor.setColumns(10);
    textfelder.add(textFieldDivisor);

    JLabel lblAudioformat = new JLabel("audioformat");
    textfelder.add(lblAudioformat);

    textFieldAudioFormat.setHorizontalAlignment(SwingConstants.CENTER);
    textFieldAudioFormat.setEnabled(false);
    textFieldAudioFormat.setEditable(false);
    textFieldAudioFormat.setColumns(30);
    textfelder.add(textFieldAudioFormat);

    JLabel lblPeakFrequency = new JLabel("peak frequency");
    textfelder.add(lblPeakFrequency);

    textFieldPeakFrequency.setHorizontalAlignment(SwingConstants.CENTER);
    textFieldPeakFrequency.setEnabled(false);
    textFieldPeakFrequency.setEditable(false);
    textFieldPeakFrequency.setColumns(12);
    textfelder.add(textFieldPeakFrequency);

    updateUIFromModel();
  }

  private void initCenterAndEast() {
    JScrollPane scrollPane = new JScrollPane();
    scrollPane.setViewportView(waveformPanel);
    visualizationPanel.add(scrollPane, BorderLayout.CENTER);
    visualizationPanel.add(phaseDiagramPanel, BorderLayout.EAST);
    visualizationPanel.add(spectrumPanel, BorderLayout.SOUTH);
    contentPane.add(visualizationPanel, BorderLayout.CENTER);
  }

  private void initSouthSlider() {
    JSlider slider = new JSlider();
    slider.setMinimum(1);
    slider.setValue(audioCaptureService != null ? audioCaptureService.getDivisor() : 1);
    slider.setToolTipText("Adjust divisor (affects sampling / display)");
    slider.addChangeListener(
        new ChangeListener() {
          @Override
          public void stateChanged(ChangeEvent e) {
            int value = ((JSlider) e.getSource()).getValue();
            if (audioCaptureService != null) {
              audioCaptureService.setDivisor(value);
            }
            textFieldDivisor.setText(String.valueOf(value));
            contentPane.repaint();
          }
        });
    contentPane.add(slider, BorderLayout.SOUTH);
  }

  private void populateAudioDeviceChoices() {
    comboBoxAudioDevice.addItem(new AudioDeviceItem(null));
    AudioFormat format = defaultAudioFormat();
    DataLine.Info targetLineInfo = new DataLine.Info(TargetDataLine.class, format);
    for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
      Mixer mixer = AudioSystem.getMixer(mixerInfo);
      if (mixer.isLineSupported(targetLineInfo)) {
        comboBoxAudioDevice.addItem(new AudioDeviceItem(mixerInfo));
      }
    }
  }

  private void audioDeviceSelectionChanged(ItemEvent event) {
    if (event.getStateChange() != ItemEvent.SELECTED) {
      return;
    }
    AudioDeviceItem item = (AudioDeviceItem) event.getItem();
    boolean wasRunning = audioCaptureService != null && audioCaptureService.isRunning();
    if (wasRunning) {
      stopAudioIfRunning();
      mntmStart.setSelected(false);
    }
    initializeAudioService(item.mixerInfo());
    if (wasRunning) {
      try {
        audioCaptureService.start();
        mntmStart.setSelected(true);
      } catch (Exception ex) {
        LOGGER.log(Level.SEVERE, "Failed to restart audio capture", ex);
        JOptionPane.showMessageDialog(
            this,
            "Failed to start selected audio device: " + ex.getMessage(),
            "Error",
            JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  private void toggleAudioStartStop(ActionEvent evt) {
    if (audioCaptureService == null) {
      LOGGER.warning("toggleAudioStartStop: audioCaptureService is null");
      JOptionPane.showMessageDialog(
          this, "AudioCaptureService is not available.", "Error", JOptionPane.ERROR_MESSAGE);
      return;
    }

    if (audioCaptureService.isRunning()) {
      LOGGER.info("Stopping audio capture");
      audioCaptureService.stop();
      mntmStart.setSelected(false);
    } else {
      LOGGER.info("Starting audio capture");
      try {
        audioCaptureService.start();
        mntmStart.setSelected(true);
      } catch (Exception ex) {
        LOGGER.log(Level.SEVERE, "Failed to start audio capture", ex);
        JOptionPane.showMessageDialog(
            this,
            "Failed to start audio capture: " + ex.getMessage(),
            "Error",
            JOptionPane.ERROR_MESSAGE);
        mntmStart.setSelected(false);
      }
    }
  }

  private void stopAudioIfRunning() {
    if (audioCaptureService != null && audioCaptureService.isRunning()) {
      audioCaptureService.stop();
    }
  }

  private void setFrozen(boolean frozen) {
    frozenBlock = frozen && audioCaptureService != null ? audioCaptureService.getLatestBlock() : null;
    waveformPanel.setFrozen(frozen);
    spectrumPanel.setFrozen(frozen);
    mntmFreeze.setSelected(frozen);
  }

  private void updateUIFromModel() {
    if (!SwingUtilities.isEventDispatchThread()) {
      SwingUtilities.invokeLater(this::updateUIFromModel);
      return;
    }

    if (audioCaptureService != null) {
      textFieldDataSize.setText(String.valueOf(audioCaptureService.getLatestModel().getDataSize()));
      textFieldDivisor.setText(String.valueOf(audioCaptureService.getDivisor()));
      textFieldAudioFormat.setText(
          audioCaptureService.getFormat() != null
              ? audioCaptureService.getFormat().toString()
              : defaultAudioFormat().toString());
      double peakHz = spectrumPanel.getPeakFrequencyHz();
      textFieldPeakFrequency.setText(Double.isNaN(peakHz) ? "n/a" : String.format("%.1f Hz", peakHz));
      mntmStart.setSelected(audioCaptureService.isRunning());
    } else {
      textFieldDataSize.setText("");
      textFieldDivisor.setText("");
      textFieldAudioFormat.setText("");
      textFieldPeakFrequency.setText("n/a");
      mntmStart.setSelected(false);
    }
  }

  private void exportMeasurementCsv() {
    AudioBlock block = currentMeasurementBlock();
    SpectrumSnapshot spectrum = spectrumPanel.getCurrentSpectrum();
    if (block == null && spectrum == null) {
      JOptionPane.showMessageDialog(
          this, "No measurement data available to export.", "Export CSV", JOptionPane.INFORMATION_MESSAGE);
      return;
    }

    JFileChooser chooser = new JFileChooser();
    chooser.setFileFilter(new FileNameExtensionFilter("CSV files", "csv"));
    chooser.setSelectedFile(new java.io.File("measurement.csv"));
    if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
      return;
    }

    java.io.File file = ensureExtension(chooser.getSelectedFile(), ".csv");
    try (PrintWriter writer =
        new PrintWriter(Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8))) {
      writeMeasurementCsv(writer, block, spectrum);
      JOptionPane.showMessageDialog(this, "Measurement exported to " + file, "Export CSV", JOptionPane.INFORMATION_MESSAGE);
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, "Failed to export CSV", ex);
      JOptionPane.showMessageDialog(this, "Failed to export CSV: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  private void exportMeasurementPng() {
    if (visualizationPanel.getWidth() <= 0 || visualizationPanel.getHeight() <= 0) {
      JOptionPane.showMessageDialog(
          this, "Visualization is not ready to export.", "Export PNG", JOptionPane.INFORMATION_MESSAGE);
      return;
    }

    JFileChooser chooser = new JFileChooser();
    chooser.setFileFilter(new FileNameExtensionFilter("PNG images", "png"));
    chooser.setSelectedFile(new java.io.File("measurement.png"));
    if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
      return;
    }

    java.io.File file = ensureExtension(chooser.getSelectedFile(), ".png");
    BufferedImage image =
        new BufferedImage(
            visualizationPanel.getWidth(), visualizationPanel.getHeight(), BufferedImage.TYPE_INT_ARGB);
    java.awt.Graphics2D graphics = image.createGraphics();
    try {
      visualizationPanel.paintAll(graphics);
    } finally {
      graphics.dispose();
    }
    try {
      ImageIO.write(image, "png", file);
      JOptionPane.showMessageDialog(this, "Measurement exported to " + file, "Export PNG", JOptionPane.INFORMATION_MESSAGE);
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, "Failed to export PNG", ex);
      JOptionPane.showMessageDialog(this, "Failed to export PNG: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  private AudioBlock currentMeasurementBlock() {
    if (frozenBlock != null) {
      return frozenBlock;
    }
    return audioCaptureService != null ? audioCaptureService.getLatestBlock() : null;
  }

  private void writeMeasurementCsv(PrintWriter writer, AudioBlock block, SpectrumSnapshot spectrum) {
    writer.println("section,key,value");
    if (block != null) {
      writer.printf(Locale.ROOT, "metadata,sampleRate,%.3f%n", block.format().sampleRate());
      writer.printf(Locale.ROOT, "metadata,channels,%d%n", block.channels());
      writer.printf(Locale.ROOT, "metadata,frames,%d%n", block.frames());
      writer.printf(Locale.ROOT, "metadata,frameIndex,%d%n", block.frameIndex());
      writer.println();
      writer.print("sampleIndex");
      for (int channel = 0; channel < block.channels(); channel++) {
        writer.print(",channel");
        writer.print(channel);
      }
      writer.println();
      float[][] samples = block.samples();
      for (int frame = 0; frame < block.frames(); frame++) {
        writer.print(frame);
        for (int channel = 0; channel < block.channels(); channel++) {
          writer.printf(Locale.ROOT, ",%.9f", samples[channel][frame]);
        }
        writer.println();
      }
    }
    if (spectrum != null) {
      writer.println();
      writer.println("bin,frequencyHz,magnitude");
      for (int bin = 0; bin < spectrum.binCount(); bin++) {
        writer.printf(
            Locale.ROOT, "%d,%.6f,%.9f%n", bin, spectrum.frequencyOfBin(bin), spectrum.magnitude(bin));
      }
    }
  }

  private static AudioFormat defaultAudioFormat() {
    return new AudioFormat(
        DEFAULT_SAMPLE_RATE, DEFAULT_SAMPLE_BITS, DEFAULT_CHANNELS, DEFAULT_SIGNED, DEFAULT_BIG_ENDIAN);
  }

  private static java.io.File ensureExtension(java.io.File file, String extension) {
    String name = file.getName().toLowerCase(Locale.ROOT);
    if (name.endsWith(extension)) {
      return file;
    }
    return new java.io.File(file.getParentFile(), file.getName() + extension);
  }

  private record AudioDeviceItem(Mixer.Info mixerInfo) {
    @Override
    public String toString() {
      if (mixerInfo == null) {
        return "System default input";
      }
      return mixerInfo.getName() + " — " + mixerInfo.getDescription();
    }
  }

  private class SwingAction extends AbstractAction {
    private static final long serialVersionUID = 1L;

    public SwingAction() {
      putValue(NAME, "About");
      putValue(SHORT_DESCRIPTION, "Some short description");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      JOptionPane.showMessageDialog(
          AudioAnalyseFrame.this, "Carsten Hammer carsten.hammer@t-online.de");
    }
  }
}
