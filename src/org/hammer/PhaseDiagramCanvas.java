package org.hammer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;

public class PhaseDiagramCanvas extends JPanel {

	public PhaseDiagramCanvas() {
		super(true);
		javax.swing.Timer t = new javax.swing.Timer(200, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				repaint();
			}
		});
		t.start();
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		g.drawRect(0, 0, this.getWidth() - 1, this.getHeight() - 1);
		g.setXORMode(Color.yellow);
		g.translate(-AudioInDataRunnable.INSTANCE.yPoints()[0][0], -AudioInDataRunnable.INSTANCE.yPoints()[1][0]);
		g.drawPolyline(AudioInDataRunnable.INSTANCE.yPoints()[0],
				AudioInDataRunnable.INSTANCE.yPoints()[1],
				AudioInDataRunnable.INSTANCE.numberOfPoints());
	}
}
