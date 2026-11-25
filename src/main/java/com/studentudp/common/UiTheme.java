package com.studentudp.common;

import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

/**
 * Shared FlatLaf theme, color palette, typography, and component styling helpers.
 * Enforces a clean native desktop look with zero emojis and restrained colors.
 */
public final class UiTheme {

    public static final Color BG_WINDOW = new Color(0xF7, 0xF8, 0xFA);
    public static final Color BG_CARD = Color.WHITE;
    public static final Color BG_TABLE_ALT = new Color(0xF8, 0xFA, 0xFC);
    public static final Color BORDER_SUBTLE = new Color(0xE2, 0xE8, 0xF0);
    public static final Color TEXT_PRIMARY = new Color(0x1E, 0x29, 0x3B);
    public static final Color TEXT_SECONDARY = new Color(0x64, 0x74, 0x8B);
    public static final Color ACCENT_PRIMARY = new Color(0x25, 0x63, 0xEB);
    public static final Color STATUS_CONNECTED = new Color(0x16, 0xA3, 0x4A);
    public static final Color STATUS_DISCONNECTED = new Color(0xDC, 0x26, 0x26);

    public static final int CONTROL_HEIGHT = 33;
    public static final int PADDING = 14;

    private UiTheme() {
    }

    /**
     * Installs FlatLightLaf and configures global Swing component defaults.
     */
    public static void install() {
        FlatLightLaf.setup();
        Font baseFont = uiFont(Font.PLAIN, 13);
        UIManager.put("defaultFont", baseFont);
        UIManager.put("Panel.background", BG_WINDOW);
        UIManager.put("Component.arc", 6);
        UIManager.put("Button.arc", 6);
        UIManager.put("TextComponent.arc", 6);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.focusColor", new Color(0x93, 0xC5, 0xFD));
        UIManager.put("Component.borderColor", BORDER_SUBTLE);
        UIManager.put("TextField.background", Color.WHITE);
        UIManager.put("PasswordField.background", Color.WHITE);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.gridColor", BORDER_SUBTLE);
        UIManager.put("Table.selectionBackground", new Color(0xDB, 0xEA, 0xFE));
        UIManager.put("Table.selectionForeground", TEXT_PRIMARY);
    }

    public static Font uiFont(int style, int size) {
        return new Font("Segoe UI", style, size);
    }

    public static Font monoFont(int size) {
        return new Font("Consolas", Font.PLAIN, size);
    }

    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_SUBTLE, 1),
                BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING)
        );
    }

    public static JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(BG_CARD);
        panel.setBorder(cardBorder());
        return panel;
    }

    public static void styleField(JTextField field) {
        field.setFont(uiFont(Font.PLAIN, 13));
        field.setForeground(TEXT_PRIMARY);
        Dimension pref = field.getPreferredSize();
        field.setPreferredSize(new Dimension(pref.width, CONTROL_HEIGHT));
        field.setMinimumSize(new Dimension(60, CONTROL_HEIGHT));
    }

    public static void stylePrimaryButton(JButton button) {
        button.setFont(uiFont(Font.BOLD, 13));
        button.setBackground(ACCENT_PRIMARY);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        setControlHeight(button);
    }

    public static void styleSecondaryButton(JButton button) {
        button.setFont(uiFont(Font.PLAIN, 13));
        button.setBackground(Color.WHITE);
        button.setForeground(TEXT_PRIMARY);
        button.setFocusPainted(false);
        setControlHeight(button);
    }

    public static void styleDangerButton(JButton button) {
        button.setFont(uiFont(Font.BOLD, 13));
        button.setBackground(STATUS_DISCONNECTED);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        setControlHeight(button);
    }

    public static void setStatusLabel(JLabel label, boolean connected, String text) {
        label.setFont(uiFont(Font.BOLD, 12));
        label.setForeground(connected ? STATUS_CONNECTED : STATUS_DISCONNECTED);
        label.setText(text);
    }

    public static void setControlHeight(JComponent component) {
        Dimension pref = component.getPreferredSize();
        component.setPreferredSize(new Dimension(pref.width, CONTROL_HEIGHT));
        component.setMinimumSize(new Dimension(pref.width, CONTROL_HEIGHT));
    }

    /**
     * Applies consistent row height, header styling, alternating row colors, and numeric right-alignment.
     */
    public static void styleTable(JTable table, boolean[] rightAlignColumns) {
        table.setFont(uiFont(Font.PLAIN, 13));
        table.setForeground(TEXT_PRIMARY);
        table.setRowHeight(30);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(false);

        JTableHeader header = table.getTableHeader();
        header.setFont(uiFont(Font.BOLD, 12));
        header.setForeground(TEXT_SECONDARY);
        header.setBackground(BG_WINDOW);
        header.setReorderingAllowed(false);

        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int column
            ) {
                Component comp = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    comp.setBackground(row % 2 == 0 ? BG_CARD : BG_TABLE_ALT);
                    comp.setForeground(TEXT_PRIMARY);
                }
                boolean rightAlign = rightAlignColumns != null
                        && column >= 0
                        && column < rightAlignColumns.length
                        && rightAlignColumns[column];
                setHorizontalAlignment(rightAlign ? SwingConstants.RIGHT : SwingConstants.LEFT);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return comp;
            }
        };
        table.setDefaultRenderer(Object.class, cellRenderer);
    }
}
