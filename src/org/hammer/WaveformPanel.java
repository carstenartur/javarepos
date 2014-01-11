package org.hammer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JPanel;

/**
 *
 * @author chammer
 */
public final class WaveformPanel extends JPanel {

    /**
     *
     * @return
     */
    public int getDatasize() {
        return AudioInDataRunnable.INSTANCE.datasize;
    }

    /**
     *
     */
    public WaveformPanel() {
        super(true);

        AudioInDataRunnable.INSTANCE.process(this, 16000.0f, 8, 2, false, false, 1);
        AudioInDataRunnable.INSTANCE.computedatasize();

        javax.swing.Timer t = new javax.swing.Timer(200, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                repaint();
            }
        });
        t.start();
       
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
            	AudioInDataRunnable.INSTANCE.recomputexvalues();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawRect(0, 0, this.getWidth() - 1, this.getHeight() - 1);
        g.setXORMode(Color.yellow);
        
		g.drawPolyline(AudioInDataRunnable.INSTANCE.xPoints, AudioInDataRunnable.INSTANCE.yPoints[0], AudioInDataRunnable.INSTANCE.relation);
        g.setXORMode(Color.cyan);
        g.drawPolyline(AudioInDataRunnable.INSTANCE.xPoints, AudioInDataRunnable.INSTANCE.yPoints[1], AudioInDataRunnable.INSTANCE.relation);
        g.setXORMode(Color.red);
        g.drawLine(0, getHeight()/2, getWidth() - 1, getHeight()/2);
        
         try {
			for (int i = 0; i < AudioInDataRunnable.INSTANCE.relation; i+=AudioInDataRunnable.INSTANCE.tickeverynsample) {
             g.drawLine(AudioInDataRunnable.INSTANCE.xPoints[i], getHeight()/2, AudioInDataRunnable.INSTANCE.xPoints[i], getHeight()/2+6);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
}
