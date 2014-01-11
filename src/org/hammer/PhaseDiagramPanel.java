package org.hammer;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;



public class PhaseDiagramPanel extends JPanel {
	public PhaseDiagramPanel() {
		super(true);
		setLayout(new BorderLayout(0, 0));
		
		JLabel lblNewLabel = new JLabel("Phase diagram");
		add(lblNewLabel, BorderLayout.NORTH);
		
		JPanel panel = new JPanel();
		add(panel, BorderLayout.CENTER);
		
		 javax.swing.Timer t = new javax.swing.Timer(200, new ActionListener() {
	            @Override
	            public void actionPerformed(ActionEvent e) {
	                repaint();
	            }
	        });
	        t.start();
	}
}
