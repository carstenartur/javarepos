/**
 * Rolling spectrogram / waterfall data structures and analyzer.
 *
 * <p>Builds on the FFT spectrum infrastructure by retaining a rolling history of magnitude frames
 * suitable for waterfall visualization, drift detection and CSV export.
 */
package org.hammer.audio.spectrogram;
