package org.hammer;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.AbstractAction;
import javax.swing.Box;
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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.border.TitledBorder;
import javax.swing.border.EtchedBorder;

/**
 * Refactored AudioAnalyseFrame
 *
 * Improvements:
 * - clearer field names
 * - split constructor into init* methods for readability
 * - safer thread start/stop with synchronization and interruption
 * - periodic UI refresh using Swing Timer (updates fields from model on EDT)
 * - tidy up UI creation and use pack()/setLocationRelativeTo
 * - window close handling to ensure background thread is stopped
 *
 * Note: This class still relies on AudioInDataRunnable.INSTANCE API (stopped,
 * divisor, datasize, format, computedatasize(), recomputexvalues()) and assumes
 * those members are thread-safe or designed to be used from other threads.
 */
public class AudioAnalyseFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    private final JPanel contentPane;
    private final WaveformPanel waveformPanel = new WaveformPanel();

    private final JTextField textFieldDataSize;
    private final JTextField textFieldDivisor;
    private final JTextField textFieldAudioFormat;

    private final AtomicReference<Thread> audioThreadRef = new AtomicReference<>(null);
    private final JCheckBoxMenuItem mntmStart;

    // timer to periodically refresh UI values from the model on the EDT
    private final Timer refreshTimer;

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                AudioAnalyseFrame frame = new AudioAnalyseFrame();
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public AudioAnalyseFrame() {
        setTitle("AudioAnalyzer");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        contentPane = new JPanel(new BorderLayout(0, 0));
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);

        // Initialize text fields before calling init methods
        textFieldDataSize = new JTextField();
        textFieldDivisor = new JTextField();
        textFieldAudioFormat = new JTextField();
        mntmStart = new JCheckBoxMenuItem("Start/Stop");

        initMenu();
        initTopSettingsPanel();
        initCenterAndEast();
        initSouthSlider();

        refreshTimer = new Timer(250, e -> updateUIFromModel());
        refreshTimer.setRepeats(true);
        refreshTimer.start();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopAudioThreadIfRunning();
                if (refreshTimer != null && refreshTimer.isRunning()) {
                    refreshTimer.stop();
                }
            }
        });

        pack();
        setSize(640, 420);
        setLocationRelativeTo(null);
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
        textfelder.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
                "Settings", TitledBorder.LEADING, TitledBorder.TOP, null, null));
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

        JPanel panelEast = new PhaseDiagramPanel();
        contentPane.add(panelEast, BorderLayout.EAST);
    }

    private void initSouthSlider() {
        JSlider slider = new JSlider();
        slider.setMinimum(1);
        slider.setValue(Objects.requireNonNullElse(getModelDivisor(), 1));
        slider.setToolTipText("Adjust divisor (affects sampling / display)");
        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int value = ((JSlider) e.getSource()).getValue();
                if (AudioInDataRunnable.INSTANCE != null) {
                    AudioInDataRunnable.INSTANCE.divisor = value;
                    AudioInDataRunnable.INSTANCE.computedatasize();
                    AudioInDataRunnable.INSTANCE.recomputexvalues();
                }
                textFieldDivisor.setText(String.valueOf(value));
                contentPane.repaint();
            }
        });
        contentPane.add(slider, BorderLayout.SOUTH);
    }

    private void toggleAudioStartStop(ActionEvent evt) {
        if (isAudioThreadRunning()) {
            if (AudioInDataRunnable.INSTANCE != null) {
                AudioInDataRunnable.INSTANCE.stopped = true;
            }
            mntmStart.setSelected(false);
            Thread t = audioThreadRef.getAndSet(null);
            if (t != null) {
                t.interrupt();
            }
        } else {
            if (AudioInDataRunnable.INSTANCE != null) {
                AudioInDataRunnable.INSTANCE.stopped = false;
                Thread t = new Thread(AudioInDataRunnable.INSTANCE, "AudioInDataRunnable");
                if (audioThreadRef.compareAndSet(null, t)) {
                    mntmStart.setSelected(true);
                    t.setDaemon(true);
                    t.start();
                }
            } else {
                JOptionPane.showMessageDialog(this, "AudioInDataRunnable.INSTANCE is not available.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private boolean isAudioThreadRunning() {
        Thread t = audioThreadRef.get();
        return t != null && t.isAlive();
    }

    private void stopAudioThreadIfRunning() {
        if (isAudioThreadRunning()) {
            if (AudioInDataRunnable.INSTANCE != null) {
                AudioInDataRunnable.INSTANCE.stopped = true;
            }
            Thread t = audioThreadRef.getAndSet(null);
            if (t != null) {
                t.interrupt();
                try {
                    t.join(500);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void updateUIFromModel() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::updateUIFromModel);
            return;
        }
        if (AudioInDataRunnable.INSTANCE != null) {
            textFieldDataSize.setText(String.valueOf(AudioInDataRunnable.INSTANCE.datasize));
            textFieldDivisor.setText(String.valueOf(AudioInDataRunnable.INSTANCE.divisor));
            textFieldAudioFormat.setText(AudioInDataRunnable.INSTANCE.format != null
                    ? AudioInDataRunnable.INSTANCE.format.toString() : "n/a");
        } else {
            textFieldDataSize.setText("");
            textFieldDivisor.setText("");
            textFieldAudioFormat.setText("");
        }
        mntmStart.setSelected(isAudioThreadRunning());
    }

    private Integer getModelDivisor() {
        if (AudioInDataRunnable.INSTANCE != null) {
            return AudioInDataRunnable.INSTANCE.divisor;
        }
        return null;
    }

    private class SwingAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        public SwingAction() {
            putValue(NAME, "About");
            putValue(SHORT_DESCRIPTION, "Some short description");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JOptionPane.showMessageDialog(AudioAnalyseFrame.this,
                    "Carsten Hammer carsten.hammer@t-online.de");
        }
    }
}
