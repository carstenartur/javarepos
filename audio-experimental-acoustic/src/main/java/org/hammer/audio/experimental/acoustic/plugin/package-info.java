/**
 * Plugin entry points wiring the experimental acoustic-localization research code into the host
 * application through the stable {@link org.hammer.audio.plugin.AudioAnalyzerPlugin} contract.
 *
 * <p>The host application discovers the plugin via {@link java.util.ServiceLoader}; nothing in
 * {@code audio-app} imports classes from this package directly.
 */
package org.hammer.audio.experimental.acoustic.plugin;
