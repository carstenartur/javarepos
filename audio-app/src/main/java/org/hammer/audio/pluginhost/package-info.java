/**
 * Host-side plugin runtime for the Audio Analyzer application.
 *
 * <p>Discovers {@link org.hammer.audio.plugin.AudioAnalyzerPlugin} implementations via {@link
 * java.util.ServiceLoader}, isolates plugin initialization failures and exposes the loaded plugins
 * to the Swing UI through {@link org.hammer.audio.pluginhost.PluginRegistry}.
 *
 * <p>The host does not import any concrete plugin classes; all interaction goes through the stable
 * {@code audio-plugin-api} module.
 */
package org.hammer.audio.pluginhost;
