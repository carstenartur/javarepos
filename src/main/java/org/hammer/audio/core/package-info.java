/**
 * Audio-domain core: immutable models that flow through the platform.
 *
 * <p>This package intentionally contains <strong>no</strong> UI, JavaSound, pixel or rendering
 * concerns. Everything here is normalized floating-point audio data with self-describing format
 * metadata, suitable for use by capture, ring buffer, DSP, analysis and any downstream consumer
 * (Swing UI, JavaFX UI, REST API, file export, ...).
 *
 * @see AudioBlock
 * @see AudioFormatDescriptor
 */
package org.hammer.audio.core;
