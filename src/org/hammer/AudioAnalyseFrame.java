package org.hammer;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.GridLayout;
import javax.swing.JCheckBoxMenuItem;

/**
 * 
 * @author chammer
 */
public class AudioAnalyseFrame extends JFrame {
	private JPanel contentPane;
	private final Action action = new SwingAction();
	private JTextField textFielddatasize;
	private JTextField textFielddivisor;
	private JTextField audioformat;
	final WaveformPanel panel = new WaveformPanel();
	private Thread thread;
	JCheckBoxMenuItem mntmStart;

	/**
	 * Launch the application.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					AudioAnalyseFrame frame = new AudioAnalyseFrame();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public AudioAnalyseFrame() {
		setTitle("AudioAnalyzer");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 512, 406);

		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		mntmStart = new JCheckBoxMenuItem("Start/Stop");
		
		mntmStart.setIcon(null);
		mntmStart.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (thread != null && thread.isAlive()) {
					AudioInDataRunnable.INSTANCE.stopped = true;
					mntmStart.setSelected(false);
				}				else {
					thread = new Thread(AudioInDataRunnable.INSTANCE);
					mntmStart.setSelected(true);
					thread.start();
					
				}
			}
		});
		mnFile.add(mntmStart);
		
		menuBar.add(Box.createGlue());

		JMenu mnHelp = new JMenu("Help");
		menuBar.add(mnHelp);

		JMenuItem mntmAbout = new JMenuItem("About");
		mntmAbout.setAction(action);
		mnHelp.add(mntmAbout);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);

		JPanel textfelder = new JPanel();
		contentPane.add(textfelder, BorderLayout.NORTH);
		textfelder.setLayout(new GridLayout(3, 2, 0, 0));

		JLabel lblDatasize = new JLabel("datasize");
		textfelder.add(lblDatasize);

		textFielddatasize = new JTextField();
		textFielddatasize.setEnabled(false);
		textFielddatasize.setEditable(false);
		textfelder.add(textFielddatasize);
		textFielddatasize.setColumns(10);

		JScrollPane scrollPane = new JScrollPane();
		contentPane.add(scrollPane, BorderLayout.CENTER);

		scrollPane.setViewportView(panel);

		textFielddatasize.setText("" + AudioInDataRunnable.INSTANCE.datasize);

		JLabel lblChannel = new JLabel("channel");
		textfelder.add(lblChannel);

		JTextField label_1 = new JTextField();
		label_1.setText("...");
		label_1.setEnabled(false);
		label_1.setEditable(false);
		textfelder.add(label_1);

		JLabel lblDivisor = new JLabel("divisor");
		textfelder.add(lblDivisor);

		textFielddivisor = new JTextField();
		textFielddivisor.setEnabled(false);
		textFielddivisor.setEditable(false);
		textfelder.add(textFielddivisor);
		textFielddivisor.setColumns(10);
		textFielddivisor.setText("" + AudioInDataRunnable.INSTANCE.divisor);

		JLabel label_2 = new JLabel("");
		textfelder.add(label_2);

		JLabel label_3 = new JLabel("");
		textfelder.add(label_3);

		JLabel lblAudioformat = new JLabel("audioformat");
		textfelder.add(lblAudioformat);

		audioformat = new JTextField();
		audioformat.setHorizontalAlignment(SwingConstants.CENTER);
		audioformat.setEnabled(false);
		audioformat.setEditable(false);
		textfelder.add(audioformat);
		audioformat.setColumns(30);
		audioformat.setText(AudioInDataRunnable.INSTANCE.format.toString());

		JSlider slider = new JSlider();
		slider.setMinimum(1);
		slider.setValue(1);
		slider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				int value = ((JSlider) e.getSource()).getValue();
				AudioInDataRunnable.INSTANCE.divisor = value;
				AudioInDataRunnable.INSTANCE.recomputexvalues();
				textFielddivisor.setText("" + value);
				contentPane.repaint();
			}
		});
		contentPane.add(slider, BorderLayout.SOUTH);

		JPanel panel_1 = new PhaseDiagramPanel();
		contentPane.add(panel_1, BorderLayout.EAST);
	}

	private class SwingAction extends AbstractAction {

		public SwingAction() {
			putValue(NAME, "About");
			putValue(SHORT_DESCRIPTION, "Some short description");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			JOptionPane.showMessageDialog(null,
					"Carsten Hammer carsten.hammer@t-online.de");
		}
	}
}
