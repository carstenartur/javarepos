package org.hammer.audio.plugin;

import java.util.List;

/**
 * Central plugin contract for the Audio Analyzer host application.
 *
 * <p>Plugins implement this interface and register their implementation via {@link
 * java.util.ServiceLoader} (i.e. by adding a {@code
 * META-INF/services/org.hammer.audio.plugin.AudioAnalyzerPlugin} resource to their JAR). The host
 * application discovers all such implementations at runtime through a plugin manager and offers
 * their contributions to the user.
 *
 * <p>Each contribution accessor returns an immutable list. Plugins that do not supply a particular
 * contribution type return an empty list. Plugins must not import any internal host application
 * classes; all interaction with the host goes through this API package.
 */
public interface AudioAnalyzerPlugin {

  /** Returns the plugin descriptor (metadata) for this plugin. Must not be {@code null}. */
  PluginDescriptor descriptor();

  /**
   * Returns analysis contributions provided by this plugin (e.g. additional analyzers, derived
   * snapshots, post-processing). Default: empty.
   */
  default List<AnalysisContribution> analysisContributions() {
    return List.of();
  }

  /**
   * Returns view contributions (panels, frames) provided by this plugin. The host decides where to
   * surface them (menu, sidebar, plugin tab). Default: empty.
   */
  default List<ViewContribution> viewContributions() {
    return List.of();
  }

  /**
   * Returns menu contributions provided by this plugin (named actions to be exposed in a Plugins
   * menu). Default: empty.
   */
  default List<MenuContribution> menuContributions() {
    return List.of();
  }

  /** Returns demo-signal contributions provided by this plugin. Default: empty. */
  default List<DemoSignalContribution> demoSignalContributions() {
    return List.of();
  }
}
