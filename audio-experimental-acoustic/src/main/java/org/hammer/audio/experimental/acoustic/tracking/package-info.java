/**
 * Real-time multi-source tracking pipeline for the experimental acoustic-localization plugin.
 *
 * <p>This package promotes the previously isolated DSP experiments into a coherent pipeline:
 *
 * <pre>
 *   AudioBlock (one synchronized multichannel frame)
 *     -&gt; FFT + {@link org.hammer.audio.experimental.acoustic.tracking.MultiPeakDetector} (per channel)
 *     -&gt; {@link org.hammer.audio.experimental.acoustic.tracking.FrequencyClusterer}
 *     -&gt; {@link org.hammer.audio.experimental.acoustic.TdoaEstimator} (per cluster, on the array)
 *     -&gt; {@link org.hammer.audio.experimental.acoustic.DelayAndSumBeamformer} (localization)
 *     -&gt; {@link org.hammer.audio.experimental.acoustic.tracking.SourceTracker}
 *         (temporal smoothing with a 2D constant-velocity Kalman filter,
 *          identity persistence across frames)
 * </pre>
 *
 * <p>All stage outputs are immutable records ({@link
 * org.hammer.audio.experimental.acoustic.tracking.TrackingSnapshot} for the pipeline, {@link
 * org.hammer.audio.experimental.acoustic.tracking.TrackedSource} for individual tracks).
 *
 * <p>Real-time readiness is supported by {@link
 * org.hammer.audio.experimental.acoustic.tracking.FrameSchedule} and {@link
 * org.hammer.audio.experimental.acoustic.tracking.ProcessingBudget}, which describe the bounded
 * per-frame timing budget that callers must enforce.
 */
package org.hammer.audio.experimental.acoustic.tracking;
