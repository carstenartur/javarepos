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

/**
 *
 * @author chammer
 */
public class AudioWindow extends JFrame {

    private JPanel contentPane;
    private final Action action = new SwingAction();
    private JTextField textFielddatasize;
    private JTextField textFielddivisor;
    private JTextField audioformat;

    /**
     * Launch the application.
     * @param args 
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    AudioWindow frame = new AudioWindow();
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
    public AudioWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 512, 406);

        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        JMenu mnFile = new JMenu("File");
        menuBar.add(mnFile);

        JMenuItem mntmStart = new JMenuItem("Start");
        mntmStart.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
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
        final AudioIn panel = new AudioIn();
        scrollPane.setViewportView(panel);


        textFielddatasize.setText("" + panel.getDatasize());
        
        JLabel label = new JLabel("");
        textfelder.add(label);
        
        JLabel label_1 = new JLabel("");
        textfelder.add(label_1);
                
                        JLabel lblDivisor = new JLabel("divisor");
                        textfelder.add(lblDivisor);
        
                textFielddivisor = new JTextField();
                textFielddivisor.setEnabled(false);
                textFielddivisor.setEditable(false);
                textfelder.add(textFielddivisor);
                textFielddivisor.setColumns(10);
                textFielddivisor.setText("" + panel.getDivisor());
        
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
        audioformat.setText(panel.format.toString());
        
        JSlider slider = new JSlider();
        slider.setMinimum(1);
        slider.setValue(1);
        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int value = ((JSlider) e.getSource()).getValue();
                panel.setDivisor(value);
                panel.recomputexvalues();
                textFielddivisor.setText("" + value);
                contentPane.repaint();
            }
        });
        contentPane.add(slider, BorderLayout.SOUTH);
    }

    private class SwingAction extends AbstractAction {

        public SwingAction() {
            putValue(NAME, "About");
            putValue(SHORT_DESCRIPTION, "Some short description");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JOptionPane.showMessageDialog(null, "Carsten Hammer carsten.hammer@t-online.de");
        }
    }
}
