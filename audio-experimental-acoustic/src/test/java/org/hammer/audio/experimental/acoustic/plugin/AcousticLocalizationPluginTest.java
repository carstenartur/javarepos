package org.hammer.audio.experimental.acoustic.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ServiceLoader;
import javax.swing.JComponent;
import org.hammer.audio.plugin.AudioAnalyzerPlugin;
import org.hammer.audio.plugin.MenuContribution;
import org.hammer.audio.plugin.ViewContribution;
import org.junit.jupiter.api.Test;

class AcousticLocalizationPluginTest {

  @Test
  void pluginIsDiscoverableViaServiceLoader() {
    boolean found =
        ServiceLoader.load(AudioAnalyzerPlugin.class).stream()
            .map(ServiceLoader.Provider::get)
            .anyMatch(p -> p instanceof AcousticLocalizationPlugin);
    assertTrue(found, "AcousticLocalizationPlugin must be registered via ServiceLoader");
  }

  @Test
  void descriptorMatchesIssueSpecification() {
    AcousticLocalizationPlugin plugin = new AcousticLocalizationPlugin();
    assertEquals("acoustic-localization", plugin.descriptor().id());
    assertEquals("Experimental Acoustic Localization", plugin.descriptor().name());
    assertEquals("0.1.0", plugin.descriptor().version());
    assertTrue(plugin.descriptor().experimental());
    assertNotNull(plugin.descriptor().documentationPath());
  }

  @Test
  void contributionsAreNonEmpty() {
    AcousticLocalizationPlugin plugin = new AcousticLocalizationPlugin();
    assertFalse(plugin.analysisContributions().isEmpty());
    assertFalse(plugin.demoSignalContributions().isEmpty());
    assertFalse(plugin.menuContributions().isEmpty());
    assertFalse(plugin.viewContributions().isEmpty());
  }

  @Test
  void viewContributionProducesUniqueComponents() {
    AcousticLocalizationPlugin plugin = new AcousticLocalizationPlugin();
    ViewContribution view = plugin.viewContributions().get(0);
    JComponent first = view.componentFactory().get();
    JComponent second = view.componentFactory().get();
    assertNotNull(first);
    assertNotNull(second);
    assertFalse(first == second, "factory must return fresh component instances");
  }

  @Test
  void menuActionsAreRunnableWithoutThrowing() {
    AcousticLocalizationPlugin plugin = new AcousticLocalizationPlugin();
    for (MenuContribution menu : plugin.menuContributions()) {
      menu.action().run();
    }
  }
}
