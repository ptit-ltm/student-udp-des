package com.studentudp.client;

import com.studentudp.common.ProtocolConstants;
import com.studentudp.common.UiTheme;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

/**
 * Step 1 Panel: Connect to the UDP Server via IP and Port with 3000ms PING/PONG verification.
 */
public class ConnectServerPanel extends JPanel {

    public interface ConnectListener {
        void onServerConnected(String host, int port);
    }

    private final UdpClientEngine clientEngine;
    private final ConnectListener listener;

    private final JTextField hostField = new JTextField("127.0.0.1", 20);
    private final JTextField portField = new JTextField(String.valueOf(ProtocolConstants.DEFAULT_SERVER_PORT), 20);
    private final JButton connectButton = new JButton("Kết nối");
    private final JLabel statusLabel = new JLabel(" ");

    public ConnectServerPanel(UdpClientEngine clientEngine, ConnectListener listener) {
        this.clientEngine = clientEngine;
        this.listener = listener;
        initUi();
    }

    private void initUi() {
        setLayout(new GridBagLayout());
        setBackground(UiTheme.BG_WINDOW);

        JPanel card = UiTheme.createCardPanel();
        card.setLayout(new GridBagLayout());
        card.setPreferredSize(new Dimension(430, 290));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JLabel stepLabel = new JLabel("Bước 1 / 3");
        stepLabel.setFont(UiTheme.uiFont(Font.BOLD, 12));
        stepLabel.setForeground(UiTheme.ACCENT_PRIMARY);
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 4, 0);
        card.add(stepLabel, gbc);

        JLabel titleLabel = new JLabel("Kết nối Server UDP");
        titleLabel.setFont(UiTheme.uiFont(Font.BOLD, 18));
        titleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 4, 0);
        card.add(titleLabel, gbc);

        JLabel subtitleLabel = new JLabel("Nhập địa chỉ IP và Port của Server đang lắng nghe.");
        subtitleLabel.setFont(UiTheme.uiFont(Font.PLAIN, 12));
        subtitleLabel.setForeground(UiTheme.TEXT_SECONDARY);
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 14, 0);
        card.add(subtitleLabel, gbc);

        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 4, 0);
        card.add(createLabel("Địa chỉ IP Server"), gbc);

        UiTheme.styleField(hostField);
        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 10, 0);
        card.add(hostField, gbc);

        gbc.gridy = 5;
        gbc.insets = new Insets(0, 0, 4, 0);
        card.add(createLabel("Port UDP"), gbc);

        UiTheme.styleField(portField);
        gbc.gridy = 6;
        gbc.insets = new Insets(0, 0, 14, 0);
        card.add(portField, gbc);

        UiTheme.stylePrimaryButton(connectButton);
        gbc.gridy = 7;
        gbc.insets = new Insets(0, 0, 10, 0);
        card.add(connectButton, gbc);

        statusLabel.setFont(UiTheme.uiFont(Font.PLAIN, 12));
        statusLabel.setForeground(UiTheme.STATUS_DISCONNECTED);
        gbc.gridy = 8;
        gbc.insets = new Insets(0, 0, 0, 0);
        card.add(statusLabel, gbc);

        add(card);

        connectButton.addActionListener(e -> attemptConnect());
        portField.addActionListener(e -> attemptConnect());
        hostField.addActionListener(e -> attemptConnect());
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UiTheme.uiFont(Font.BOLD, 12));
        label.setForeground(UiTheme.TEXT_PRIMARY);
        return label;
    }

    private void attemptConnect() {
        String host = hostField.getText().trim();
        String portText = portField.getText().trim();

        if (host.isEmpty()) {
            showError("Vui lòng nhập địa chỉ IP Server.");
            return;
        }
        int port;
        try {
            port = Integer.parseInt(portText);
            if (port < 1 || port > 65535) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            showError("Port UDP phải là số nguyên từ 1 đến 65535.");
            return;
        }

        setLoading(true, "Đang gửi PING tới " + host + ":" + port + "...");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                clientEngine.ping(host, port);
                return null;
            }

            @Override
            protected void done() {
                setLoading(false, null);
                try {
                    get();
                    statusLabel.setText(" ");
                    listener.onServerConnected(host, port);
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
        connectButton.setEnabled(!loading);
        connectButton.setText(loading ? "Đang kết nối..." : "Kết nối");
        if (message != null) {
            statusLabel.setForeground(UiTheme.TEXT_SECONDARY);
            statusLabel.setText(message);
        }
    }

    private void showError(String message) {
        statusLabel.setForeground(UiTheme.STATUS_DISCONNECTED);
        statusLabel.setText("<html><body style='width:380px'>" + escapeHtml(message) + "</body></html>");
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
