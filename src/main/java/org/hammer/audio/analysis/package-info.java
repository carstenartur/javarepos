
/**
 * Audio analysis layer.
 *
 * <p>Defines:
 *
 * <ul>
 *   <li>{@link AnalysisModule} — extension point for plug-in analyzers
 *   <li>{@link AnalysisSnapshot} — immutable analysis result base type
 *   <li>{@link RmsPeakAnalyzer} / {@link RmsPeakSnapshot} — level metering
 *   <li>{@link SpectrumAnalyzer} / {@link SpectrumSnapshot} — windowed FFT spectrum
 *   <li>{@link Fft} — pure-Java radix-2 FFT used by the spectrum analyzer; also reusable directly
 * </ul>
 *
 * <p>Future additions (spectrogram, phase scope, loudness, ...) plug in via {@link
 * AnalysisModule}.
 */
package org.hammer.audio.analysis;
