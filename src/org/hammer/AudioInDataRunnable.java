package org.hammer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.swing.JPanel;

public enum AudioInDataRunnable implements Runnable {
	INSTANCE;
	/**
	 * Tausendstel also jede Millisekunde ein Tick
	 */
	public static final float TICKDISTANCE_IN_MS = 1f / 1000f;
	public boolean stopped;
	public TargetDataLine line;
	public byte[] datas;
	public float sampleRate;
	public int sampleSizeInBits;
	public int channels;
	public int tickeverynsample;
	public boolean signed;
	public boolean bigEndian;
	public int[] xPoints;
	public int[][] yPoints;
	public int datasize;
	public int divisor;
	public AudioFormat format;
	JPanel panel;
	public int relation;

	@Override
	public void run() {
		// Assume that the TargetDataLine, line, has already
		// been obtained and opened.
		// ByteArrayOutputStream out = new ByteArrayOutputStream();
		int numBytesRead;
		stopped=false;
		// Begin audio capture.
		line.start();

		// Here, stopped is a global boolean set by another thread.
		while (!stopped) {
			// Read the next chunk of data from the TargetDataLine.
			try {
				numBytesRead = line.read(datas, 0, datas.length);
				// Save this chunk of data.

				for (int i = 0; i < datasize / channels; i++) {
					int j = (panel.getHeight() * (datas[i * channels] & 0xFF)) / 256;
					yPoints[0][i] = j;
					int k = (panel.getHeight() * (datas[i * channels + 1] & 0xFF)) / 256;
					yPoints[1][i] = k;
					// System.out.println(yPoints[i]);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public void process(JPanel panel, float sampleRate,
			int sampleSizeInBits, int channels, boolean signed,
			boolean bigEndian, int divisor) {
		this.sampleRate = sampleRate;
		this.sampleSizeInBits = sampleSizeInBits;
		this.channels = channels;
		this.signed = signed;
		this.bigEndian = bigEndian;
		this.divisor = divisor;
		this.panel = panel;
		tickeverynsample = (int) ((TICKDISTANCE_IN_MS) / (1f / sampleRate));
		format = new AudioFormat(sampleRate, sampleSizeInBits, channels,
				signed, bigEndian);
		line = null;
		DataLine.Info info = new DataLine.Info(TargetDataLine.class, format); // format
																				// is
																				// an
																				// AudioFormat
																				// object
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
	}

	/**
    *
    */
	public void computedatasize() {
		// int bufferSize = (int)format.getSampleRate() *
		// format.getFrameSize();
		// byte buffer[] = new byte[bufferSize];
		datasize = line.getBufferSize() / divisor;
		relation = datasize / channels;
		// if((datasize%2)>0)datasize=+1;
		datas = new byte[datasize];
		xPoints = new int[relation];
		yPoints = new int[2][relation];

	}

	/**
   *
   */
	public void recomputexvalues() {
		for (int i = 0; i < relation; i++) {
			xPoints[i] = (panel.getWidth() * i) / datasize * channels;
		}
	}
}