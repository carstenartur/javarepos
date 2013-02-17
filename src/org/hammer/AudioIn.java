package org.hammer;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.swing.JPanel;

/**
 *
 * @author chammer
 */
public final class AudioIn extends JPanel {
    /**
     * Tausendstel also jede Millisekunde ein Tick
     */
    public static final float TICKDISTANCE_IN_MS = 1f/1000f;

    boolean stopped = false;
    private TargetDataLine line;
    private byte[] datas;
    float sampleRate = 16000.0f;
    int sampleSizeInBits = 8;
    int channels = 2;
    int tickeverynsample=(int) ((TICKDISTANCE_IN_MS)/(1f/sampleRate)); 
    boolean signed = false;
    boolean bigEndian = false;
    private int[] xPoints;
    private int[][] yPoints;
    private int datasize;
    private int divisor = 1;
    AudioFormat format;

    /**
     *
     * @return
     */
    public int getDivisor() {
        return divisor;
    }

    /**
     *
     * @param divisor
     */
    public void setDivisor(int divisor) {
        this.divisor = divisor;
        computedatasize();
    }

    /**
     *
     * @return
     */
    public int getDatasize() {
        return datasize;
    }

    /**
     *
     */
    public AudioIn() {
        super(true);

        format = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
        line = null;
        DataLine.Info info = new DataLine.Info(TargetDataLine.class,
                format); // format is an AudioFormat object
        if (!AudioSystem.isLineSupported(info)) {
            // Handle the error ... 
        }
        // Obtain and open the line.
        try {
            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
        } catch (LineUnavailableException ex) {
            // Handle the error ... 
        }
        computedatasize();

        javax.swing.Timer t = new javax.swing.Timer(200, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                repaint();
            }
        });
        t.start();
        final Thread thread = new Thread(processing);
        thread.start();
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                recomputexvalues();
            }
        });
    }

    /**
     *
     */
    public void recomputexvalues() {
        for (int i = 0; i < datasize/channels; i++) {
            xPoints[i] = (getWidth() * i) / datasize*channels;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawRect(0, 0, this.getWidth() - 1, this.getHeight() - 1);
        g.setXORMode(Color.yellow);
        g.drawPolyline(xPoints, yPoints[0], datasize/channels);
        g.setXORMode(Color.cyan);
        g.drawPolyline(xPoints, yPoints[1], datasize/channels);
        g.setXORMode(Color.red);
   //     g.setColor(Color.red);
        g.drawLine(0, getHeight()/2, getWidth() - 1, getHeight()/2);
         for (int i = 0; i < datasize/channels; i=i+tickeverynsample) {
             g.drawLine(xPoints[i], getHeight()/2, xPoints[i], getHeight()/2+6);
        }
    }
    
    Runnable processing = new Runnable() {
        @Override
        public void run() {
            // Assume that the TargetDataLine, line, has already
            // been obtained and opened.
//		ByteArrayOutputStream out  = new ByteArrayOutputStream();
            int numBytesRead;


            // Begin audio capture.
            line.start();

            // Here, stopped is a global boolean set by another thread.
            while (!stopped) {
                // Read the next chunk of data from the TargetDataLine.
                try{
                numBytesRead = line.read(datas, 0, datas.length);
                // Save this chunk of data.

                for (int i = 0; i < datasize/channels; i++) {
                    int j = (getHeight() * (datas[i*channels] & 0xFF)) / 256;
                    yPoints[0][i] = j;
                    int k = (getHeight() * (datas[i*channels+1] & 0xFF)) / 256;
                    yPoints[1][i] = k;
//				System.out.println(yPoints[i]);
                }
                }catch(Exception ex){
                    
                }
            }
        }
    };

    /**
     *
     */
    public void computedatasize() {
        //		int bufferSize = (int)format.getSampleRate() * 
        //			    format.getFrameSize();
        //			  byte buffer[] = new byte[bufferSize];
        datasize = line.getBufferSize() / divisor;
      //  if((datasize%2)>0)datasize=+1;
        datas = new byte[datasize];
        xPoints = new int[datasize/channels];
        yPoints = new int[2][datasize/channels];
    }
}
