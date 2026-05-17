package org.hammer.audio.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import javax.swing.JLabel;
import org.junit.jupiter.api.Test;

class AudioAnalyzerPluginApiTest {

  @Test
  void pluginDescriptorRejectsBlankRequiredFields() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new PluginDescriptor("", "name", "1.0", "desc", null, false));
    assertThrows(
        IllegalArgumentException.class,
        () -> new PluginDescriptor("id", " ", "1.0", "desc", null, false));
    assertThrows(
        IllegalArgumentException.class,
        () -> new PluginDescriptor("id", "name", "", "desc", null, false));
  }

  @Test
  void pluginDescriptorPreservesValues() {
    PluginDescriptor d =
        new PluginDescriptor("p", "Plugin", "1.2.3", "does things", "docs/p.md", true);
    assertEquals("p", d.id());
    assertEquals("Plugin", d.name());
    assertEquals("1.2.3", d.version());
    assertEquals("does things", d.description());
    assertEquals("docs/p.md", d.documentationPath());
    assertTrue(d.experimental());
  }

  @Test
  void pluginDefaultsReturnEmptyContributionLists() {
    AudioAnalyzerPlugin plugin = () -> new PluginDescriptor("x", "X", "1", "desc", null, false);
    assertTrue(plugin.analysisContributions().isEmpty());
    assertTrue(plugin.viewContributions().isEmpty());
    assertTrue(plugin.menuContributions().isEmpty());
    assertTrue(plugin.demoSignalContributions().isEmpty());
    assertFalse(plugin.descriptor().experimental());
  }

  @Test
  void viewContributionInvokesFactoryOnDemand() {
    ViewContribution view =
        new ViewContribution() {
          @Override
          public String id() {
            return "v";
          }

          @Override
          public String title() {
            return "View";
          }

          @Override
          public java.util.function.Supplier<JLabel> componentFactory() {
            return () -> new JLabel("hello");
          }
        };
    assertEquals("hello", ((JLabel) view.componentFactory().get()).getText());
    // Two invocations must produce distinct instances.
    assertNotSame(view.componentFactory().get(), view.componentFactory().get());
  }

  @Test
  void menuContributionActionIsInvokable() {
    boolean[] fired = {false};
    MenuContribution menu =
        new MenuContribution() {
          @Override
          public String id() {
            return "m";
          }

          @Override
          public String label() {
            return "Menu";
          }

          @Override
          public Runnable action() {
            return () -> fired[0] = true;
          }
        };
    menu.action().run();
    assertTrue(fired[0]);
  }

  @Test
  void demoSignalAndAnalysisContributionsExposeMetadata() {
    DemoSignalContribution demo =
        new DemoSignalContribution() {
          @Override
          public String id() {
            return "demo";
          }

          @Override
          public String label() {
            return "Demo";
          }

          @Override
          public String description() {
            return "desc";
          }
        };
    assertEquals("demo", demo.id());
    AnalysisContribution analysis =
        new AnalysisContribution() {
          @Override
          public String id() {
            return "a";
          }

          @Override
          public String description() {
            return "d";
          }
        };
    assertEquals(List.of("a", "d"), List.of(analysis.id(), analysis.description()));
  }
}
