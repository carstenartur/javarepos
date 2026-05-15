/**
 * Audio capture and decoding utilities.
 *
 * <p>{@link SampleDecoder} converts raw interleaved PCM bytes into normalized {@code
 * float[channels][frames]} sample arrays — the canonical platform representation. The legacy {@link
 * org.hammer.audio.AudioCaptureServiceImpl} uses this decoder to produce {@link
 * org.hammer.audio.core.AudioBlock} instances and publish them to a downstream {@link
 * org.hammer.audio.buffer.AudioRingBuffer}.
 */
package org.hammer.audio.capture;
