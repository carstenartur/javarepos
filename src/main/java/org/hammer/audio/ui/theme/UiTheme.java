package org.hammer.audio.ui.theme;

import com.formdev.flatlaf.FlatDarkLaf;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/** Shared Swing look-and-feel and component styling helpers. */
public final class UiTheme {

  private static final Font BASE_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 13);

  private UiTheme() {}

  /** Installs dark FlatLaf and applies global defaults. */
  public static void installDarkTheme() {
    FlatDarkLaf.setup();
    UIManager.put("defaultFont", BASE_FONT);
    UIManager.put("Component.arc", 8);
    UIManager.put("TextComponent.arc", 8);
    UIManager.put("Button.arc", 10);
    UIManager.put("ScrollBar.thumbArc", 999);
    UIManager.put("TabbedPane.showTabSeparators", true);
  }

  /** Applies compact, consistent padding to controls. */
  public static void applyCompactPadding(
      JComponent component, int top, int left, int bottom, int right) {
    Border current = component.getBorder();
    EmptyBorder padding = new EmptyBorder(top, left, bottom, right);
    if (current == null) {
      component.setBorder(padding);
      return;
    }
    component.setBorder(new CompoundBorder(current, padding));
  }

  /** Creates a subtle panel border suitable for dashboard cards. */
  public static Border createPanelBorder() {
    return BorderFactory.createCompoundBorder(
        new LineBorder(PlotRenderTheme.AXIS_COLOR, 1, true), new EmptyBorder(6, 8, 6, 8));
  }
}
