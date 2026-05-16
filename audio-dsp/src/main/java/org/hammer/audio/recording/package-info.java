/**
 * Recording and replay of {@link org.hammer.audio.core.AudioBlock} streams.
 *
 * <p>The on-disk format is documented on {@link
 * org.hammer.audio.recording.AudioBlockRecordingFormat}. Writers and readers in this package are
 * deliberately small and dependency-free so they can be reused from headless tests and other
 * tooling.
 */
package org.hammer.audio.recording;
