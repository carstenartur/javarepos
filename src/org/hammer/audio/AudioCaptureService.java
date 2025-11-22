package org.hammer.audio;

import javax.sound.sampled.AudioFormat;

/**
 * Service interface for audio capture and waveform data management.
 * 
 * <p>This interface defines the contract for starting/stopping audio capture,
 * retrieving the latest waveform model snapshots, and adjusting capture parameters.
 * 
 * <p>Thread-safety: Implementations must ensure thread-safe access to all methods.
 * Model snapshots returned by {@link #getLatestModel()} should be immutable or
 * defensive copies to prevent concurrent modification issues.
 * 
 * @author refactoring
 */
public interface AudioCaptureService {
    
    /**
     * Start audio capture.
     * 
     * <p>Initializes audio input device and begins capturing audio data in a background thread.
     * 
     * @throws IllegalStateException if the service is already started or if the audio device
     *                               cannot be initialized
     */
    void start();
    
    /**
     * Stop audio capture.
     * 
     * <p>Gracefully stops the capture thread and releases audio device resources.
     * This method should be idempotent - calling it multiple times should be safe.
     */
    void stop();
    
    /**
     * Check if audio capture is currently running.
     * 
     * @return true if capture is active, false otherwise
     */
    boolean isRunning();
    
    /**
     * Get the latest waveform model snapshot.
     * 
     * <p>Returns an immutable snapshot of the current waveform data. This method
     * must be thread-safe and return defensive copies to prevent concurrent modification.
     * 
     * @return the latest WaveformModel snapshot, never null
     */
    WaveformModel getLatestModel();
    
    /**
     * Get the audio format being used for capture.
     * 
     * @return the AudioFormat, or null if not initialized
     */
    AudioFormat getFormat();
    
    /**
     * Set the divisor for buffer size calculation.
     * 
     * <p>The divisor affects how much data is captured and displayed. Higher values
     * mean smaller buffers and less data per frame.
     * 
     * @param divisor the divisor value (must be >= 1)
     * @throws IllegalArgumentException if divisor < 1
     */
    void setDivisor(int divisor);
    
    /**
     * Get the current divisor value.
     * 
     * @return the current divisor
     */
    int getDivisor();
    
    /**
     * Recompute layout/coordinates based on current panel dimensions.
     * 
     * <p>This should be called when the display panel is resized to adjust
     * the x-coordinates of the waveform points.
     * 
     * @param width the panel width in pixels
     * @param height the panel height in pixels
     */
    void recomputeLayout(int width, int height);
}
