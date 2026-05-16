package org.hammer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import org.hammer.audio.diagnosis.DiagnosisFinding;
import org.hammer.audio.diagnosis.DiagnosisSeverity;
import org.hammer.audio.diagnosis.DiagnosisSnapshot;

/**
 * Compact dashboard panel that renders the current top {@link DiagnosisFinding}s with severity
 * styling.
 *
 * <p>Caps the number of displayed findings to keep the UI compact; remaining findings can be
 * inspected in the exported evidence bundle.
 */
public final class DiagnosisPanel extends JPanel {

  private static final long serialVersionUID = 1L;
  private static final int MAX_DISPLAYED_FINDINGS = 4;

  private static final Color INFO_FOREGROUND = new Color(180, 210, 255);
  private static final Color WARNING_FOREGROUND = new Color(255, 198, 102);
  private static final Color CRITICAL_FOREGROUND = new Color(255, 120, 120);
  private static final Color FROZEN_BADGE = new Color(170, 140, 60);

  private final JPanel findingsList;
  private final JLabel statusLabel;
  private boolean frozen;
  private transient DiagnosisSnapshot currentSnapshot = DiagnosisSnapshot.empty();

  /** Create an empty diagnosis panel. */
  public DiagnosisPanel() {
    super(new BorderLayout(4, 4));
    setBorder(new EmptyBorder(4, 4, 4, 4));
    setPreferredSize(new Dimension(320, 110));

    JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    JLabel title = new JLabel("Diagnosis");
    title.setFont(title.getFont().deriveFont(Font.BOLD));
    header.add(title);
    statusLabel = new JLabel("");
    statusLabel.setForeground(FROZEN_BADGE);
    header.add(statusLabel);
    add(header, BorderLayout.NORTH);

    findingsList = new JPanel();
    findingsList.setLayout(new BoxLayout(findingsList, BoxLayout.Y_AXIS));
    add(findingsList, BorderLayout.CENTER);
    setFindings(DiagnosisSnapshot.empty());
  }

  /**
   * Update the displayed findings. Must be called on the EDT.
   *
   * @param snapshot latest diagnosis snapshot, never {@code null}
   */
  public void setFindings(DiagnosisSnapshot snapshot) {
    DiagnosisSnapshot effective = snapshot == null ? DiagnosisSnapshot.empty() : snapshot;
    this.currentSnapshot = effective;
    rebuild();
  }

  /**
   * @return latest diagnosis snapshot displayed
   */
  public DiagnosisSnapshot currentSnapshot() {
    return currentSnapshot;
  }

  /**
   * Update the frozen state shown in the header.
   *
   * @param frozen true if the diagnosis is frozen alongside the rest of the dashboard
   */
  public void setFrozen(boolean frozen) {
    this.frozen = frozen;
    rebuild();
  }

  private void rebuild() {
    findingsList.removeAll();
    statusLabel.setText(frozen ? "(frozen)" : "");

    List<DiagnosisFinding> findings = currentSnapshot.findings();
    if (findings.isEmpty()) {
      JLabel empty = new JLabel("No findings.");
      empty.setForeground(INFO_FOREGROUND);
      findingsList.add(empty);
    } else {
      int shown = 0;
      for (DiagnosisFinding finding : findings) {
        if (shown >= MAX_DISPLAYED_FINDINGS) {
          break;
        }
        findingsList.add(createFindingRow(finding));
        shown++;
      }
      if (findings.size() > MAX_DISPLAYED_FINDINGS) {
        JLabel more = new JLabel("+ " + (findings.size() - MAX_DISPLAYED_FINDINGS) + " more");
        more.setForeground(INFO_FOREGROUND);
        findingsList.add(more);
      }
    }
    findingsList.add(Box.createVerticalGlue());
    findingsList.revalidate();
    findingsList.repaint();
  }

  private static Component createFindingRow(DiagnosisFinding finding) {
    JPanel row = new JPanel(new GridLayout(1, 2, 6, 0));
    JLabel severityLabel = new JLabel(badgeText(finding.severity()), SwingConstants.LEFT);
    severityLabel.setForeground(severityForeground(finding.severity()));
    severityLabel.setFont(severityLabel.getFont().deriveFont(Font.BOLD));
    severityLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));
    row.add(severityLabel);

    JLabel message =
        new JLabel(String.format("%s (conf %.2f)", finding.message(), finding.confidence()));
    message.setForeground(severityForeground(finding.severity()));
    row.add(message);
    return row;
  }

  private static String badgeText(DiagnosisSeverity severity) {
    return switch (severity) {
      case CRITICAL -> "CRITICAL";
      case WARNING -> "WARN";
      case INFO -> "INFO";
    };
  }

  private static Color severityForeground(DiagnosisSeverity severity) {
    return switch (severity) {
      case CRITICAL -> CRITICAL_FOREGROUND;
      case WARNING -> WARNING_FOREGROUND;
      case INFO -> INFO_FOREGROUND;
    };
  }
}
