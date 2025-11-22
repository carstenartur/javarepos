package org.hammer.audio;

import java.util.Arrays;

/**
 * Immutable snapshot of waveform data for rendering.
 * 
 * <p>This class represents a point-in-time snapshot of audio waveform data,
 * containing x and y coordinates for drawing, as well as metadata about
 * tick intervals.
 * 
 * <p>Thread-safety: This class is immutable and thread-safe. All arrays
 * are defensive copies, preventing external modification.
 * 
 * @author refactoring
 */
public final class WaveformModel {
    
    private final int[] xPoints;
    private final int[][] yPoints;
    private final int tickEveryNSample;
    private final int numberOfPoints;
    private final int dataSize;
    
    /**
     * Create a new WaveformModel.
     * 
     * @param xPoints x-coordinates for drawing (will be copied)
     * @param yPoints y-coordinates for each channel (will be deep copied)
     * @param tickEveryNSample interval between tick marks
     * @param dataSize the buffer data size in bytes
     */
    public WaveformModel(int[] xPoints, int[][] yPoints, int tickEveryNSample, int dataSize) {
        // Defensive copies
        this.xPoints = xPoints != null ? Arrays.copyOf(xPoints, xPoints.length) : new int[0];
        
        if (yPoints != null) {
            this.yPoints = new int[yPoints.length][];
            for (int i = 0; i < yPoints.length; i++) {
                this.yPoints[i] = yPoints[i] != null ? Arrays.copyOf(yPoints[i], yPoints[i].length) : new int[0];
            }
        } else {
            this.yPoints = new int[0][];
        }
        
        this.tickEveryNSample = tickEveryNSample;
        this.numberOfPoints = xPoints != null ? xPoints.length : 0;
        this.dataSize = dataSize;
    }
    
    /**
     * Get x-coordinates for drawing.
     * 
     * @return defensive copy of x-coordinates array
     */
    public int[] getXPoints() {
        return Arrays.copyOf(xPoints, xPoints.length);
    }
    
    /**
     * Get y-coordinates for all channels.
     * 
     * @return defensive deep copy of y-coordinates array
     */
    public int[][] getYPoints() {
        int[][] copy = new int[yPoints.length][];
        for (int i = 0; i < yPoints.length; i++) {
            copy[i] = Arrays.copyOf(yPoints[i], yPoints[i].length);
        }
        return copy;
    }
    
    /**
     * Get y-coordinates for a specific channel.
     * 
     * @param channel the channel index
     * @return defensive copy of y-coordinates for the channel, or empty array if invalid
     */
    public int[] getYPointsForChannel(int channel) {
        if (channel >= 0 && channel < yPoints.length) {
            return Arrays.copyOf(yPoints[channel], yPoints[channel].length);
        }
        return new int[0];
    }
    
    /**
     * Get the tick interval in samples.
     * 
     * @return samples between tick marks
     */
    public int getTickEveryNSample() {
        return tickEveryNSample;
    }
    
    /**
     * Get the number of points in the waveform.
     * 
     * @return number of points
     */
    public int getNumberOfPoints() {
        return numberOfPoints;
    }
    
    /**
     * Get the number of audio channels.
     * 
     * @return number of channels
     */
    public int getChannelCount() {
        return yPoints.length;
    }
    
    /**
     * Get the data buffer size in bytes.
     * 
     * @return data size
     */
    public int getDataSize() {
        return dataSize;
    }
    
    @Override
    public String toString() {
        return String.format("WaveformModel[points=%d, channels=%d, tick=%d, dataSize=%d]",
                numberOfPoints, yPoints.length, tickEveryNSample, dataSize);
    }
}
