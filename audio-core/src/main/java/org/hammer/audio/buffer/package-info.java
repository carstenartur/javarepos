/**
 * Bounded ring buffer suitable for realtime audio workloads.
 *
 * <p>The current implementation, {@link AudioRingBuffer}, is a lock-free SPSC (single-producer /
 * single-consumer) buffer used to hand audio blocks from the capture thread to downstream DSP and
 * analysis modules without locking the realtime path.
 */
package org.hammer.audio.buffer;
