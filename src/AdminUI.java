import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/** Small shared helpers for the admin sub-screens (header, dark table, CSV export). */
public final class AdminUI {

    private AdminUI() {}

    /** Header bar with the AssessPro brand, a section caption and a "← Dashboard" button. */
    public static JPanel subHeader(String caption, Runnable onBack) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(DashboardUI.BG_CARD);
        header.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, DashboardUI.BORDER),
                new EmptyBorder(14, 30, 14, 30)));

        JLabel logo = new JLabel("AssessPro");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        logo.setForeground(DashboardUI.ACCENT);
        JLabel sub = new JLabel(caption);
        sub.setFont(new Font("Segoe UI", Font.BOLD, 10));
        sub.setForeground(DashboardUI.TEXT_MUT);
        JPanel branding = new JPanel();
        branding.setOpaque(false);
        branding.setLayout(new BoxLayout(branding, BoxLayout.Y_AXIS));
        branding.add(logo);
        branding.add(sub);

        DashboardUI.QuietButton back = new DashboardUI.QuietButton("← Dashboard");
        back.addActionListener(e -> onBack.run());

        header.add(branding, BorderLayout.WEST);
        header.add(back, BorderLayout.EAST);
        return header;
    }

    /** Flat dark combo box: no focus box, no bordered arrow button, themed dropdown list. */
    public static void flatCombo(JComboBox<?> combo) {
        combo.setFont(DashboardUI.UI);
        combo.setBackground(DashboardUI.BG_CARD2);
        combo.setForeground(DashboardUI.TEXT_PRI);
        combo.setFocusable(false);
        combo.setBorder(new CompoundBorder(new LineBorder(DashboardUI.BORDER, 1, true), new EmptyBorder(0, 6, 0, 2)));
        combo.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton b = new JButton("\u25BE");
                b.setForeground(DashboardUI.TEXT_MUT);
                b.setBorder(null);
                b.setContentAreaFilled(false);
                b.setFocusable(false);
                return b;
            }

            @Override
            protected javax.swing.plaf.basic.ComboPopup createPopup() {
                javax.swing.plaf.basic.BasicComboPopup popup = new javax.swing.plaf.basic.BasicComboPopup(comboBox);
                popup.setBorder(new LineBorder(DashboardUI.BORDER, 1));
                popup.getList().setBackground(DashboardUI.BG_CARD2);
                popup.getList().setForeground(DashboardUI.TEXT_PRI);
                popup.getList().setSelectionBackground(DashboardUI.ACCENT);
                popup.getList().setSelectionForeground(Color.WHITE);
                return popup;
            }
        });
    }

    /** A read-only dark table with a flat header (no white edge lines). */
    public static JTable darkTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setForeground(DashboardUI.TEXT_PRI);
        table.setBackground(DashboardUI.BG_CARD);
        table.setRowHeight(34);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setGridColor(DashboardUI.BORDER);
        table.setSelectionBackground(DashboardUI.BG_CARD2);
        table.setSelectionForeground(DashboardUI.TEXT_PRI);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JTableHeader th = table.getTableHeader();
        th.setReorderingAllowed(false);
        th.setPreferredSize(new Dimension(0, 36));
        th.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, false, false, r, c);
                l.setFont(new Font("Segoe UI", Font.BOLD, 12));
                l.setBackground(DashboardUI.BG_CARD2);
                l.setForeground(DashboardUI.TEXT_MUT);
                l.setOpaque(true);
                l.setBorder(new CompoundBorder(
                        new MatteBorder(0, 0, 1, 0, DashboardUI.BORDER), new EmptyBorder(0, 10, 0, 10)));
                l.setHorizontalAlignment(SwingConstants.LEFT);
                return l;
            }
        });
        return table;
    }

    /** Zebra-striped renderer; subclasses can recolour the text of individual cells. */
    public static class StripedRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
            if (!isSelected) {
                c.setBackground(row % 2 == 0 ? DashboardUI.BG_CARD : DashboardUI.BG_APP);
                c.setForeground(DashboardUI.TEXT_PRI);
            }
            c.setBorder(new EmptyBorder(0, 10, 0, 10));
            return c;
        }
    }

    public static JScrollPane tableScroll(JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(new LineBorder(DashboardUI.BORDER, 1, true));
        sp.getViewport().setBackground(DashboardUI.BG_CARD);
        sp.setBackground(DashboardUI.BG_CARD);
        return sp;
    }

    /** Writes the model's contents to a UTF-8 CSV (with BOM so Excel opens it correctly). */
    public static void exportCsv(JFrame owner, DefaultTableModel model, String dialogTitle, String defaultName) {
        if (model.getRowCount() == 0) {
            ToastNotification.show(owner, "Nothing to export.", ToastNotification.Type.WARNING);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(dialogTitle);
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV files (*.csv)", "csv"));
        chooser.setSelectedFile(new File(defaultName));
        if (chooser.showSaveDialog(owner) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(file.getParentFile(), file.getName() + ".csv");
        }
        try (BufferedWriter w = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            w.write('﻿');
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < model.getColumnCount(); c++) {
                if (c > 0) sb.append(',');
                sb.append(csv(model.getColumnName(c)));
            }
            w.write(sb.append("\r\n").toString());
            for (int r = 0; r < model.getRowCount(); r++) {
                sb.setLength(0);
                for (int c = 0; c < model.getColumnCount(); c++) {
                    if (c > 0) sb.append(',');
                    Object v = model.getValueAt(r, c);
                    sb.append(csv(v == null ? "" : v.toString()));
                }
                w.write(sb.append("\r\n").toString());
            }
            ToastNotification.show(owner, "Exported to " + file.getName(), ToastNotification.Type.SUCCESS);
        } catch (IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(owner, "Export failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String csv(String v) {
        if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
