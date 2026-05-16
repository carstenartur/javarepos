package org.hammer.audio.analysis;

/** Immutable aggregate measurements shown in the main UI. */
public record MeasurementSnapshot(
    double rms,
    double peakLevel,
    double dominantFrequencyHz,
    double stereoCorrelation,
    boolean stereoCorrelationAvailable,
    boolean clipping) {}
