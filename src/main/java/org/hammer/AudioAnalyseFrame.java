package org.hammer;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JCheckBoxMenuItem;
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
import org.hammer.audio.AudioCaptureService;
import org.hammer.audio.AudioCaptureServiceImpl;

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

  private final JPanel contentPane;
  private final WaveformPanel waveformPanel = new WaveformPanel();
  private final PhaseDiagramPanel phaseDiagramPanel = new PhaseDiagramPanel();

  private final JTextField textFieldDataSize;
  private final JTextField textFieldDivisor;
  private final JTextField textFieldAudioFormat;

  private final JCheckBoxMenuItem mntmStart;
  private final Timer refreshTimer;

  // Audio capture service
  private AudioCaptureService audioCaptureService;

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

    contentPane = new JPanel(new BorderLayout(0, 0));
    contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
    setContentPane(contentPane);

    // Initialize text fields before calling init methods
    textFieldDataSize = new JTextField();
    textFieldDivisor = new JTextField();
    textFieldAudioFormat = new JTextField();
    mntmStart = new JCheckBoxMenuItem("Start/Stop");

    // Create audio capture service
    initializeAudioService();

    initMenu();
    initTopSettingsPanel();
    initCenterAndEast();
    initSouthSlider();

    // Timer to periodically refresh UI from model at consistent interval
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
    setSize(640, 420);
    setLocationRelativeTo(null);
    LOGGER.info("AudioAnalyseFrame initialized successfully");
  }

  /** Initialize the audio capture service and inject into panels. */
  private void initializeAudioService() {
    LOGGER.info("Initializing audio capture service");
    // Create service with default audio parameters
    // 16 kHz, 8-bit, 2 channels (stereo), unsigned, little-endian, divisor 1
    audioCaptureService = new AudioCaptureServiceImpl(16000.0f, 8, 2, false, false, 1);

    // Inject service into panels
    waveformPanel.setAudioCaptureService(audioCaptureService);
    phaseDiagramPanel.setAudioCaptureService(audioCaptureService);
  }

  private void initMenu() {
    JMenuBar menuBar = new JMenuBar();
    setJMenuBar(menuBar);

    JMenu mnFile = new JMenu("File");
    menuBar.add(mnFile);

    mntmStart.setToolTipText("Start or stop audio input");
    mntmStart.addActionListener(this::toggleAudioStartStop);
    mnFile.add(mntmStart);

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
    textfelder.setLayout(new GridLayout(3, 2, 0, 0));

    JLabel lblDatasize = new JLabel("datasize");
    textfelder.add(lblDatasize);

    textFieldDataSize.setEnabled(false);
    textFieldDataSize.setEditable(false);
    textFieldDataSize.setColumns(10);
    textfelder.add(textFieldDataSize);

    JLabel lblChannel = new JLabel("channel");
    textfelder.add(lblChannel);

    JTextField label_1 = new JTextField();
    label_1.setText("...");
    label_1.setEnabled(false);
    label_1.setEditable(false);
    textfelder.add(label_1);

    JLabel lblDivisor = new JLabel("divisor");
    textfelder.add(lblDivisor);

    textFieldDivisor.setEnabled(false);
    textFieldDivisor.setEditable(false);
    textFieldDivisor.setColumns(10);
    textfelder.add(textFieldDivisor);

    JLabel label_2 = new JLabel("");
    textfelder.add(label_2);
    JLabel label_3 = new JLabel("");
    textfelder.add(label_3);

    JLabel lblAudioformat = new JLabel("audioformat");
    textfelder.add(lblAudioformat);

    textFieldAudioFormat.setHorizontalAlignment(SwingConstants.CENTER);
    textFieldAudioFormat.setEnabled(false);
    textFieldAudioFormat.setEditable(false);
    textFieldAudioFormat.setColumns(30);
    textfelder.add(textFieldAudioFormat);

    updateUIFromModel();
  }

  private void initCenterAndEast() {
    JScrollPane scrollPane = new JScrollPane();
    contentPane.add(scrollPane, BorderLayout.CENTER);
    scrollPane.setViewportView(waveformPanel);

    contentPane.add(phaseDiagramPanel, BorderLayout.EAST);
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
              : "n/a");
      mntmStart.setSelected(audioCaptureService.isRunning());
    } else {
      textFieldDataSize.setText("");
      textFieldDivisor.setText("");
      textFieldAudioFormat.setText("");
      mntmStart.setSelected(false);
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
