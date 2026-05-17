/**
 * Stable plugin API for the Audio Analyzer host application.
 *
 * <p>This package defines the contracts that plugins implement to extend the host application with
 * additional analyses, demo signals, views and menu entries. The API intentionally does not depend
 * on Swing, JavaSound, or any concrete audio-analysis module. UI integration is performed by the
 * host through {@link org.hammer.audio.plugin.ViewContribution} which only requires a {@link
 * javax.swing.JComponent} factory; the API itself does not pull Swing into stable non-UI modules.
 *
 * <p>Plugins register their {@link org.hammer.audio.plugin.AudioAnalyzerPlugin} implementations via
 * the Java {@link java.util.ServiceLoader} mechanism, i.e. by adding a {@code
 * META-INF/services/org.hammer.audio.plugin.AudioAnalyzerPlugin} file to the plugin JAR.
 */
package org.hammer.audio.plugin;
