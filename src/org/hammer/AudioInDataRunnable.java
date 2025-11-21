package org.hammer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Audio input capture runnable (singleton).
 *
 * Verbesserungen:
 * - Thread-sicherer Stopp-Flag (volatile).
 * - Korrekte Behandlung von sample size (8/16 bit), signed/unsigned und endianess.
 * - Vermeidet direkte Swing-Operationen im Audio-Thread (nur repaint() auf EDT).
 * - Benutzt tatsächlich gelesene Bytes (numBytesRead) und berechnet Frames korrekt.
 * - yPoints dimensioniert dynamisch nach Anzahl der Kanäle.
 * - Rechenfehler in recomputexvalues behoben.
 * - Robustere Fehlerbehandlung / Logging.
 */
public enum AudioInDataRunnable implements Runnable {
    INSTANCE;

    private static final Logger LOGGER = Logger.getLogger(AudioInDataRunnable.class.getName());

    /**
     * Tick distance in seconds (1 ms).
     */
    public static final float TICK_SECONDS = 1f / 1000f;

    private volatile boolean stopped;
    private TargetDataLine line;
    private byte[] datas;
    private float sampleRate;
    private int sampleSizeInBits;
    private int channels;
    private int tickEveryNSample;
    private boolean signed;
    private boolean bigEndian;
    private int[] xPoints;
    private int[][] yPoints;
    private int datasize;
    private int divisor;
    private AudioFormat format;
    private JPanel panel;
    private int numberOfPoints;

    @Override
    public void run() {
        if (line == null) {
            LOGGER.warning("TargetDataLine is null, aborting run.");
            return;
        }

        stopped = false;
        line.start();

        final int bytesPerSample = Math.max(1, sampleSizeInBits / 8);
        final int frameSize = bytesPerSample * channels;

        while (!stopped) {
            int numBytesRead;
            try {
                numBytesRead = line.read(datas, 0, datas.length);
                if (numBytesRead <= 0) {
                    continue;
                }

                final int framesRead = numBytesRead / frameSize;
                final int height = panel != null ? panel.getHeight() : 0;
                // ensure arrays large enough for framesRead (we use numberOfPoints as configured)
                final int points = Math.min(numberOfPoints, framesRead);

                // Prepare temporary arrays to avoid partial updates being visible to UI
                int[][] tmpY = new int[channels][points];

                for (int frame = 0; frame < points; frame++) {
                    int frameOffset = frame * frameSize;
                    for (int ch = 0; ch < channels; ch++) {
                        int sampleOffset = frameOffset + ch * bytesPerSample;
                        int sample = 0;
                        if (bytesPerSample == 1) {
                            int b = datas[sampleOffset] & 0xFF;
                            if (signed) {
                                sample = (byte) b; // sign extend
                            } else {
                                sample = b;
                            }
                        } else if (bytesPerSample == 2) {
                            int hi = datas[sampleOffset + (bigEndian ? 0 : 1)] & 0xFF;
                            int lo = datas[sampleOffset + (bigEndian ? 1 : 0)] & 0xFF;
                            int raw = (hi << 8) | lo;
                            if (signed) {
                                sample = (short) raw; // sign extend to int
                            } else {
                                sample = raw & 0xFFFF;
                            }
                        } else {
                            // support for other sizes can be added if needed
                            for (int b = 0; b < bytesPerSample; b++) {
                                sample = (sample << 8) | (datas[sampleOffset + b] & 0xFF);
                            }
                        }

                        // scale sample to panel height (0..height)
                        int y = 0;
                        if (height > 0) {
                            if (signed) {
                                int maxAbs = (1 << (sampleSizeInBits - 1)) - 1;
                                float norm = (float) sample / (float) maxAbs; // -1..1
                                // center vertically
                                y = Math.round((height / 2f) - norm * (height / 2f));
                            } else {
                                int max = (1 << sampleSizeInBits) - 1;
                                float norm = (float) sample / (float) max; // 0..1
                                y = Math.round(height - norm * height);
                            }
                        }
                        tmpY[ch][frame] = y;
                    }
                }

                // publish to yPoints atomically
                synchronized (this) {
                    // ensure yPoints sized for channels and points
                    if (yPoints == null || yPoints.length != channels || yPoints[0].length != numberOfPoints) {
                        // reconstruct to expected size
                        yPoints = new int[channels][numberOfPoints];
                    }
                    // copy tmpY into start of yPoints
                    for (int ch = 0; ch < channels; ch++) {
                        Arrays.fill(yPoints[ch], 0); // optional: clear previous
                        System.arraycopy(tmpY[ch], 0, yPoints[ch], 0, tmpY[ch].length);
                    }
                }

                // request repaint on EDT only
                if (panel != null) {
                    SwingUtilities.invokeLater(panel::repaint);
                }

            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Error during audio capture loop", ex);
            }
        }

        // stop and close the line on exit
        try {
            line.stop();
            line.flush();
            line.close();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error closing TargetDataLine", e);
        }
    }

    /**
     * Initialize capture parameters and open the line.
     */
    public void init(JPanel panel, float sampleRate,
                     int sampleSizeInBits, int channels, boolean signed,
                     boolean bigEndian, int divisor) {
        this.sampleRate = sampleRate;
        this.sampleSizeInBits = sampleSizeInBits;
        this.channels = Math.max(1, channels);
        this.signed = signed;
        this.bigEndian = bigEndian;
        this.divisor = Math.max(1, divisor);
        this.panel = panel;

        tickEveryNSample = (int) (TICK_SECONDS * sampleRate);

        format = new AudioFormat(sampleRate, sampleSizeInBits, this.channels, signed, bigEndian);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            throw new IllegalStateException("TargetDataLine not supported for format: " + format);
        }

        try {
            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
        } catch (LineUnavailableException ex) {
            throw new IllegalStateException("Unable to open TargetDataLine", ex);
        }

        computeDataSize();
    }

    /**
     * Compute buffer sizes and allocate arrays.
     */
    public void computeDataSize() {
        if (line == null) {
            throw new IllegalStateException("Line must be opened before computing buffer sizes.");
        }
        datasize = Math.max(256, line.getBufferSize() / Math.max(1, divisor));
        numberOfPoints = datasize / Math.max(1, (sampleSizeInBits / 8) * channels);
        if (numberOfPoints <= 0) numberOfPoints = 1;

        datas = new byte[datasize];
        xPoints = new int[numberOfPoints];
        yPoints = new int[channels][numberOfPoints];
        recomputeXValues();
    }

    /**
     * Recompute X coordinates for drawing. Uses panel width and numberOfPoints.
     */
    public void recomputeXValues() {
        int width = panel != null ? panel.getWidth() : 0;
        if (numberOfPoints <= 1) {
            if (xPoints != null && xPoints.length > 0) xPoints[0] = 0;
            return;
        }
        for (int i = 0; i < numberOfPoints; i++) {
            // distribute points evenly across the panel width
            xPoints[i] = Math.round((width - 1) * (i / (float) (numberOfPoints - 1)));
        }
    }

    /**
     * Stop capture loop (thread-safe).
     */
    public void stopCapture() {
        stopped = true;
    }

    /**
     * Whether capture is stopped.
     */
    public boolean isStopped() {
        return stopped;
    }

    /**
     * Get latest yPoints snapshot (caller should not modify returned array).
     */
    public synchronized int[][] getYPointsSnapshot() {
        if (yPoints == null) return new int[channels][0];
        int[][] copy = new int[yPoints.length][];
        for (int i = 0; i < yPoints.length; i++) {
            copy[i] = Arrays.copyOf(yPoints[i], yPoints[i].length);
        }
        return copy;
    }

    /**
     * Get latest xPoints snapshot.
     */
    public synchronized int[] getXPointsSnapshot() {
        return xPoints != null ? Arrays.copyOf(xPoints, xPoints.length) : new int[0];
    }

    // Legacy compatibility accessors - to be used by WaveformPanel, PhaseDiagramCanvas, AudioAnalyseFrame
    
    /**
     * @deprecated Use getYPointsSnapshot() for thread-safe access
     */
    @Deprecated
    public int[][] yPoints() {
        return yPoints;
    }
    
    /**
     * @deprecated Use getXPointsSnapshot() for thread-safe access
     */
    @Deprecated
    public int[] xPoints() {
        return xPoints;
    }
    
    public int numberOfPoints() {
        return numberOfPoints;
    }
    
    public int tickEveryNSample() {
        return tickEveryNSample;
    }
    
    public int datasize() {
        return datasize;
    }
    
    public int divisor() {
        return divisor;
    }
    
    public void setDivisor(int divisor) {
        this.divisor = Math.max(1, divisor);
    }
    
    public AudioFormat format() {
        return format;
    }
    
    /**
     * @deprecated Use stopCapture() instead
     */
    @Deprecated
    public void setStopped(boolean stopped) {
        this.stopped = stopped;
    }
    
    /**
     * Legacy method name for compatibility
     * @deprecated Use computeDataSize() instead
     */
    @Deprecated
    public void computedatasize() {
        computeDataSize();
    }
    
    /**
     * Legacy method name for compatibility
     * @deprecated Use recomputeXValues() instead
     */
    @Deprecated
    public void recomputexvalues() {
        recomputeXValues();
    }
}