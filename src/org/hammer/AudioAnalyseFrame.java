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
import javax.swing.Action;
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
 *
 * @author chammer (refactored by copilot)
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

	/**
	 * Launch the application.
	 */
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

	/**
	 * Create the frame.
	 */
	public AudioAnalyseFrame() {
		setTitle("AudioAnalyzer");
		// we'll handle proper shutdown to stop audio thread
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		contentPane = new JPanel(new BorderLayout(0, 0));
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);

		// initialize components
		initMenu();
		initTopSettingsPanel();
		initCenterAndEast();
		initSouthSlider();

		// Create timer for periodic refresh of UI components from AudioInDataRunnable
		refreshTimer = new Timer(250, e -> updateUIFromModel());
		refreshTimer.setRepeats(true);

		// ensure we stop background thread when window closes
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				// Stop audio thread in a background thread to avoid blocking the EDT
				new Thread(() -> stopAudioThreadIfRunning()).start();
				if (refreshTimer.isRunning()) {
					refreshTimer.stop();
				}
			}
		});

		pack();
		setSize(640, 420);
		setLocationRelativeTo(null);

		// Start timer after all UI initialization is complete
		refreshTimer.start();
	}

	private void initMenu() {
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);

		mntmStart = new JCheckBoxMenuItem("Start/Stop");
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

		// datasize
		JLabel lblDatasize = new JLabel("datasize");
		textfelder.add(lblDatasize);

		textFieldDataSize = new JTextField();
		textFieldDataSize.setEnabled(false);
		textFieldDataSize.setEditable(false);
		textFieldDataSize.setColumns(10);
		textfelder.add(textFieldDataSize);

		// channel placeholder (kept as original)
		JLabel lblChannel = new JLabel("channel");
		textfelder.add(lblChannel);

		JTextField label_1 = new JTextField();
		label_1.setText("...");
		label_1.setEnabled(false);
		label_1.setEditable(false);
		textfelder.add(label_1);

		// divisor
		JLabel lblDivisor = new JLabel("divisor");
		textfelder.add(lblDivisor);

		textFieldDivisor = new JTextField();
		textFieldDivisor.setEnabled(false);
		textFieldDivisor.setEditable(false);
		textFieldDivisor.setColumns(10);
		textfelder.add(textFieldDivisor);

		// filler row (kept)
		JLabel label_2 = new JLabel("");
		textfelder.add(label_2);
		JLabel label_3 = new JLabel("");
		textfelder.add(label_3);

		// audioformat
		JLabel lblAudioformat = new JLabel("audioformat");
		textfelder.add(lblAudioformat);

		textFieldAudioFormat = new JTextField();
		textFieldAudioFormat.setHorizontalAlignment(SwingConstants.CENTER);
		textFieldAudioFormat.setEnabled(false);
		textFieldAudioFormat.setEditable(false);
		textFieldAudioFormat.setColumns(30);
		textfelder.add(textFieldAudioFormat);

		// initialize values from model (if present)
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
		Integer divisor = getModelDivisor();
		slider.setValue(divisor == null || divisor < 1 ? 1 : divisor);
		slider.setToolTipText("Adjust divisor (affects sampling / display)");
		slider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				int value = ((JSlider) e.getSource()).getValue();
				// ChangeListener is already called on EDT, so we can update directly
				if (AudioInDataRunnable.INSTANCE != null) {
					AudioInDataRunnable.INSTANCE.divisor = value;
					AudioInDataRunnable.INSTANCE.computedatasize();
					AudioInDataRunnable.INSTANCE.recomputexvalues();
				}
				textFieldDivisor.setText(String.valueOf(value));
				// repaint UI so waveform panel can reflect changes immediately
				contentPane.repaint();
			}
		});
		contentPane.add(slider, BorderLayout.SOUTH);
	}

	/**
	 * Toggle start/stop of the audio thread when menu item is clicked.
	 */
	private void toggleAudioStartStop(ActionEvent evt) {
		if (isAudioThreadRunning()) {
			// request stop
			AudioInDataRunnable.INSTANCE.stopped = true;
			mntmStart.setSelected(false);
			// attempt to interrupt the thread so it can terminate more promptly
			Thread t = audioThreadRef.getAndSet(null);
			if (t != null) {
				t.interrupt();
			}
		} else {
			// start
			if (AudioInDataRunnable.INSTANCE != null) {
				Thread t = new Thread(AudioInDataRunnable.INSTANCE, "AudioInDataRunnable");
				AudioInDataRunnable.INSTANCE.stopped = false;
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
					// restore interrupted flag
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	/**
	 * Update the UI fields from the AudioInDataRunnable model. Must be called on EDT.
	 */
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
		// reflect thread state in menu item
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
