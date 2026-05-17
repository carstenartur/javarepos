package org.hammer.audio.pluginhost;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import org.hammer.audio.plugin.AudioAnalyzerPlugin;
import org.hammer.audio.plugin.MenuContribution;
import org.hammer.audio.plugin.PluginDescriptor;
import org.hammer.audio.plugin.ViewContribution;

/**
 * Builds a Swing <i>Plugins</i> {@link JMenu} from a {@link PluginRegistry}.
 *
 * <p>The host application uses this helper to surface plugin contributions in its menu bar without
 * referencing concrete plugin classes. Each plugin gets its own submenu containing its view
 * contributions (rendered as plain dialogs), its menu contributions (rendered as menu items) and a
 * descriptor/info entry. Failed plugins are listed under a disabled "Failed plugins" submenu.
 */
public final class PluginMenuBuilder {

  private static final Logger LOGGER = Logger.getLogger(PluginMenuBuilder.class.getName());

  private final PluginRegistry registry;

  /** Wrap the given registry for menu construction. */
  public PluginMenuBuilder(PluginRegistry registry) {
    this.registry = Objects.requireNonNull(registry, "registry");
  }

  /**
   * Build a <i>Plugins</i> menu. The menu always exists (even if empty) so the UI consistently
   * exposes a plugin integration surface; an empty placeholder item is shown when no plugins are
   * available.
   *
   * @param parent parent frame for plugin view dialogs (may be {@code null})
   */
  public JMenu buildMenu(Frame parent) {
    JMenu menu = new JMenu("Plugins");
    List<AudioAnalyzerPlugin> plugins = registry.plugins();
    if (plugins.isEmpty() && registry.failures().isEmpty()) {
      JMenuItem none = new JMenuItem("(no plugins installed)");
      none.setEnabled(false);
      menu.add(none);
      return menu;
    }
    for (AudioAnalyzerPlugin plugin : plugins) {
      menu.add(safeBuildPluginSubmenu(parent, plugin));
    }
    if (!registry.failures().isEmpty()) {
      menu.addSeparator();
      JMenu failed = new JMenu("Failed plugins");
      for (PluginLoadResult result : registry.failures()) {
        JMenuItem item = new JMenuItem(result.descriptor().name());
        item.setEnabled(false);
        item.setToolTipText(result.failure().map(Throwable::toString).orElse("unknown error"));
        failed.add(item);
      }
      menu.add(failed);
    }
    return menu;
  }

  private JMenu safeBuildPluginSubmenu(Frame parent, AudioAnalyzerPlugin plugin) {
    try {
      return buildPluginSubmenu(parent, plugin);
    } catch (RuntimeException ex) {
      String label = safePluginLabel(plugin);
      LOGGER.log(Level.WARNING, "Plugin submenu for " + label + " failed to build", ex);
      JMenu fallback = new JMenu(label + " (unavailable)");
      JMenuItem error = new JMenuItem("Plugin failed to initialize");
      error.setEnabled(false);
      error.setToolTipText(ex.toString());
      fallback.add(error);
      return fallback;
    }
  }

  private static String safePluginLabel(AudioAnalyzerPlugin plugin) {
    try {
      PluginDescriptor descriptor = plugin.descriptor();
      if (descriptor != null) {
        return descriptor.name();
      }
    } catch (RuntimeException ignored) {
      // fall through to class-name based label
    }
    return plugin.getClass().getSimpleName();
  }

  private JMenu buildPluginSubmenu(Frame parent, AudioAnalyzerPlugin plugin) {
    PluginDescriptor d = plugin.descriptor();
    String title = d.experimental() ? d.name() + " (experimental)" : d.name();
    JMenu submenu = new JMenu(title);
    for (ViewContribution view : plugin.viewContributions()) {
      submenu.add(buildViewItem(parent, d, view));
    }
    for (MenuContribution menu : plugin.menuContributions()) {
      submenu.add(buildMenuItem(menu));
    }
    JMenuItem info = new JMenuItem("About \u2026");
    info.addActionListener(e -> showDescriptorDialog(parent, d));
    submenu.add(info);
    return submenu;
  }

  private JMenuItem buildViewItem(Frame parent, PluginDescriptor d, ViewContribution view) {
    JMenuItem item = new JMenuItem("Open: " + view.title());
    item.addActionListener(
        e -> {
          try {
            JComponent component = view.componentFactory().get();
            if (component == null) {
              throw new IllegalStateException(
                  "View contribution " + view.id() + " returned null component");
            }
            JDialog dialog = new JDialog(parent, d.name() + " - " + view.title(), false);
            dialog.getContentPane().add(component, BorderLayout.CENTER);
            dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            dialog.setSize(new Dimension(640, 420));
            dialog.setLocationRelativeTo(parent);
            dialog.setVisible(true);
          } catch (RuntimeException ex) {
            LOGGER.log(Level.WARNING, "Plugin view " + d.id() + "/" + view.id() + " failed", ex);
            JOptionPane.showMessageDialog(
                parent,
                "Plugin view failed: " + ex.getMessage(),
                d.name(),
                JOptionPane.ERROR_MESSAGE);
          }
        });
    return item;
  }

  private JMenuItem buildMenuItem(MenuContribution contribution) {
    JMenuItem item = new JMenuItem(contribution.label());
    item.addActionListener(
        e ->
            SwingUtilities.invokeLater(
                () -> {
                  try {
                    contribution.action().run();
                  } catch (RuntimeException ex) {
                    LOGGER.log(
                        Level.WARNING, "Plugin menu action " + contribution.id() + " failed", ex);
                  }
                }));
    return item;
  }

  private void showDescriptorDialog(Frame parent, PluginDescriptor d) {
    JTextArea text = new JTextArea();
    text.setEditable(false);
    text.setLineWrap(true);
    text.setWrapStyleWord(true);
    StringBuilder sb = new StringBuilder();
    sb.append("Id: ").append(d.id()).append('\n');
    sb.append("Name: ").append(d.name()).append('\n');
    sb.append("Version: ").append(d.version()).append('\n');
    sb.append("Experimental: ").append(d.experimental()).append('\n');
    if (d.documentationPath() != null) {
      sb.append("Documentation: ").append(d.documentationPath()).append('\n');
    }
    sb.append('\n').append(d.description());
    text.setText(sb.toString());
    JScrollPane scroll = new JScrollPane(text);
    scroll.setPreferredSize(new Dimension(480, 240));
    JDialog dialog = new JDialog(parent, d.name(), false);
    dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    dialog.getContentPane().add(new JLabel("Plugin information"), BorderLayout.NORTH);
    dialog.getContentPane().add(scroll, BorderLayout.CENTER);
    dialog.pack();
    dialog.setLocationRelativeTo(parent);
    dialog.setVisible(true);
  }
}
