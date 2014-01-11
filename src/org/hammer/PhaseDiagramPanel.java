package org.hammer;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;



public class PhaseDiagramPanel extends JPanel {
	public PhaseDiagramPanel() {
		super();
		setLayout(new BorderLayout(0, 0));
		
		JLabel lblNewLabel = new JLabel("Phase diagram");
		add(lblNewLabel, BorderLayout.NORTH);
		
		JPanel panel = new PhaseDiagramCanvas();
		add(panel, BorderLayout.CENTER);	
	}
}
