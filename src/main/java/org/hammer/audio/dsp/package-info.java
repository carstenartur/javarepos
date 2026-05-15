
/**
 * Modular DSP processing pipeline.
 *
 * <p>This package defines the {@link DSPProcessor} extension point and the {@link DSPPipeline}
 * helper for chaining processors. Processors transform {@link org.hammer.audio.core.AudioBlock}s in
 * a stateless or self-synchronized manner; pipelines are immutable and cheap to share.
 *
 * <p>Concrete processors (gain, DC blocker, filters, ...) are intentionally <em>not</em> bundled
 * here yet — the architecture is the deliverable and the extension surface is stable enough to
 * add them as needed.
 */
package org.hammer.audio.dsp;
