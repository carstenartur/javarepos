/**
 * Immutable visualization snapshots.
 *
 * <p>Snapshots in this package are produced from {@link org.hammer.audio.core.AudioBlock} or {@link
 * org.hammer.audio.analysis.AnalysisSnapshot} data and are designed for cheap consumption by any UI
 * toolkit or external API. They never carry pixel coordinates — pixel mapping is the exclusive
 * responsibility of the {@link org.hammer.audio.ui} layer.
 */
package org.hammer.audio.snapshot;
