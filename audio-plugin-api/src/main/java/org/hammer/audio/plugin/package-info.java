/**
 * Stable plugin API for the Audio Analyzer host application.
 *
 * <p>This package defines the contracts that plugins implement to extend the host application with
 * additional analyses, demo signals, views and menu entries. The API intentionally does not depend
 * on JavaSound or any concrete audio-analysis module ({@code audio-core}, {@code audio-dsp}, {@code
 * audio-acquisition}, {@code audio-geometry}). {@link org.hammer.audio.plugin.ViewContribution}
 * references {@link javax.swing.JComponent} from the JDK so the host can render plugin views
 * generically; no third-party UI framework is bundled.
 *
 * <p>Plugins register their {@link org.hammer.audio.plugin.AudioAnalyzerPlugin} implementations via
 * the Java {@link java.util.ServiceLoader} mechanism, i.e. by adding a {@code
 * META-INF/services/org.hammer.audio.plugin.AudioAnalyzerPlugin} file to the plugin JAR.
 */
package org.hammer.audio.plugin;
