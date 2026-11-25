package com.studentudp.client;

import com.studentudp.common.DbConfigDTO;
import com.studentudp.common.ProtocolConstants;
import com.studentudp.common.UiTheme;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

/**
 * Step 2 Panel: Configure MySQL connection parameters and send DB_CONFIG to Server.
 */
public class DbConfigPanel extends JPanel {

    public interface DbConfigListener {
        void onBackToConnect();

        void onDbConnected(DbConfigDTO config);
    }

    private final UdpClientEngine clientEngine;
    private final DbConfigListener listener;

    private final JLabel serverEndpointLabel = new JLabel("Server: -");
    private final JTextField hostField = new JTextField("localhost", 20);
    private final JTextField portField = new JTextField(String.valueOf(ProtocolConstants.DEFAULT_MYSQL_PORT), 20);
    private final JTextField dbNameField = new JTextField("student_db", 20);
    private final JTextField usernameField = new JTextField("root", 20);
    private final JPasswordField passwordField = new JPasswordField(20);

    private final JButton backButton = new JButton("Quay lại");
    private final JButton connectDbButton = new JButton("Kết nối CSDL");
    private final JLabel statusLabel = new JLabel(" ");

    public DbConfigPanel(UdpClientEngine clientEngine, DbConfigListener listener) {
        this.clientEngine = clientEngine;
        this.listener = listener;
        initUi();
    }

    public void refreshServerLabel() {
        serverEndpointLabel.setText("Server UDP: " + clientEngine.getServerEndpointDisplay());
        statusLabel.setText(" ");
    }

    private void initUi() {
        setLayout(new GridBagLayout());
        setBackground(UiTheme.BG_WINDOW);

        JPanel card = UiTheme.createCardPanel();
        card.setLayout(new GridBagLayout());
        card.setPreferredSize(new Dimension(470, 475));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JLabel stepLabel = new JLabel("Bước 2 / 3");
        stepLabel.setFont(UiTheme.uiFont(Font.BOLD, 12));
        stepLabel.setForeground(UiTheme.ACCENT_PRIMARY);
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 4, 0);
        card.add(stepLabel, gbc);

        JLabel titleLabel = new JLabel("Cấu hình kết nối MySQL");
        titleLabel.setFont(UiTheme.uiFont(Font.BOLD, 18));
        titleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 4, 0);
        card.add(titleLabel, gbc);

        serverEndpointLabel.setFont(UiTheme.uiFont(Font.PLAIN, 12));
        serverEndpointLabel.setForeground(UiTheme.STATUS_CONNECTED);
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 14, 0);
        card.add(serverEndpointLabel, gbc);

        addFormRow(card, gbc, 3, "Host MySQL", hostField);
        addFormRow(card, gbc, 5, "Port MySQL", portField);
        addFormRow(card, gbc, 7, "Tên Database", dbNameField);
        addFormRow(card, gbc, 9, "Username", usernameField);

        gbc.gridy = 11;
        gbc.insets = new Insets(0, 0, 4, 0);
        card.add(createLabel("Password"), gbc);

        UiTheme.styleField(passwordField);
        passwordField.putClientProperty("JTextField.placeholderText", "Nhập mật khẩu MySQL");
        gbc.gridy = 12;
        gbc.insets = new Insets(0, 0, 14, 0);
        card.add(passwordField, gbc);

        JPanel buttonRow = new JPanel(new GridLayout(1, 2, 10, 0));
        buttonRow.setBackground(UiTheme.BG_CARD);
        UiTheme.styleSecondaryButton(backButton);
        UiTheme.stylePrimaryButton(connectDbButton);
        buttonRow.add(backButton);
        buttonRow.add(connectDbButton);

        gbc.gridy = 13;
        gbc.insets = new Insets(0, 0, 10, 0);
        card.add(buttonRow, gbc);

        statusLabel.setFont(UiTheme.uiFont(Font.PLAIN, 12));
        statusLabel.setForeground(UiTheme.STATUS_DISCONNECTED);
        gbc.gridy = 14;
        gbc.insets = new Insets(0, 0, 0, 0);
        card.add(statusLabel, gbc);

        add(card);

        backButton.addActionListener(e -> listener.onBackToConnect());
        connectDbButton.addActionListener(e -> attemptDbConnect());
        passwordField.addActionListener(e -> attemptDbConnect());
    }

    private void addFormRow(JPanel card, GridBagConstraints gbc, int rowIdx, String labelText, JTextField field) {
        gbc.gridy = rowIdx;
        gbc.insets = new Insets(0, 0, 4, 0);
        card.add(createLabel(labelText), gbc);

        UiTheme.styleField(field);
        gbc.gridy = rowIdx + 1;
        gbc.insets = new Insets(0, 0, 10, 0);
        card.add(field, gbc);
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UiTheme.uiFont(Font.BOLD, 12));
        label.setForeground(UiTheme.TEXT_PRIMARY);
        return label;
    }

    private void attemptDbConnect() {
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ex) {
            showError("Port MySQL phải là số nguyên hợp lệ.");
            return;
        }

        DbConfigDTO config = new DbConfigDTO(
                hostField.getText().trim(),
                port,
                dbNameField.getText().trim(),
                usernameField.getText().trim(),
                new String(passwordField.getPassword())
        );

        try {
            config.validate();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
            return;
        }

        setLoading(true, "Đang yêu cầu Server kết nối tới MySQL...");

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return clientEngine.configureDatabase(config);
            }

            @Override
            protected void done() {
                setLoading(false, null);
                try {
                    get();
                    statusLabel.setText(" ");
                    listener.onDbConnected(config);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    showError(cause.getMessage());
                }
            }
        }.execute();
    }

    private void setLoading(boolean loading, String message) {
        hostField.setEnabled(!loading);
        portField.setEnabled(!loading);
        dbNameField.setEnabled(!loading);
        usernameField.setEnabled(!loading);
        passwordField.setEnabled(!loading);
        backButton.setEnabled(!loading);
        connectDbButton.setEnabled(!loading);
        connectDbButton.setText(loading ? "Đang kết nối..." : "Kết nối CSDL");
        if (message != null) {
            statusLabel.setForeground(UiTheme.TEXT_SECONDARY);
            statusLabel.setText(message);
        }
    }

    private void showError(String message) {
        statusLabel.setForeground(UiTheme.STATUS_DISCONNECTED);
        statusLabel.setText("<html><body style='width:420px'>" + escapeHtml(message) + "</body></html>");
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
