package org.hammer;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import javax.swing.JLabel;



public class PhaseDiagramPanel extends JPanel {
	public PhaseDiagramPanel() {
		super(true);
		setLayout(new BorderLayout(0, 0));
		
		JLabel lblNewLabel = new JLabel("Phase diagram");
		add(lblNewLabel, BorderLayout.NORTH);
		
		JPanel panel = new JPanel();
		add(panel, BorderLayout.CENTER);
	}
}
