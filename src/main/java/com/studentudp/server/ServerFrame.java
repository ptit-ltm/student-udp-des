package com.studentudp.server;

import com.studentudp.common.ProtocolConstants;
import com.studentudp.common.ResultDTO;
import com.studentudp.common.UiTheme;
import com.studentudp.server.UdpServerEngine.ServerEventListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.SocketException;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

/**
 * Server Control Panel GUI displaying UDP port control, 8-character DES key input,
 * MySQL connection status, decrypted student table, and raw Base64 console log.
 */
public class ServerFrame extends JFrame implements ServerEventListener {

    private final UdpServerEngine serverEngine;

    private final JTextField portField = new JTextField(String.valueOf(ProtocolConstants.DEFAULT_SERVER_PORT), 6);
    private final JButton startStopButton = new JButton("Khởi động");
    private final JTextField desKeyField = new JTextField(12);
    private final JButton applyKeyButton = new JButton("Áp dụng khóa & Đọc giải mã");

    private final JLabel serverStatusLabel = new JLabel("Server: Đang dừng");
    private final JLabel mysqlStatusLabel = new JLabel("MySQL: Chưa kết nối");
    private final JLabel cryptoErrorLabel = new JLabel(" ");

    private final DefaultTableModel tableModel;
    private final JTable studentTable;
    private final JTextArea logArea = new JTextArea();

    public ServerFrame() {
        super("UDP Student Management - Server Control Panel");
        this.serverEngine = new UdpServerEngine();
        this.serverEngine.setListener(this);

        String[] columns = {"Mã SV", "Họ tên", "Toán", "Văn", "Anh", "Điểm TB"};
        this.tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.studentTable = new JTable(tableModel);

        initUi();
        wireEvents();
    }

    private void initUi() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1040, 640));
        setPreferredSize(new Dimension(1160, 700));
        setLayout(new BorderLayout(0, UiTheme.PADDING));
        getContentPane().setBackground(UiTheme.BG_WINDOW);

        JPanel rootPadding = new JPanel(new BorderLayout(0, UiTheme.PADDING));
        rootPadding.setBackground(UiTheme.BG_WINDOW);
        rootPadding.setBorder(BorderFactory.createEmptyBorder(
                UiTheme.PADDING, UiTheme.PADDING, UiTheme.PADDING, UiTheme.PADDING
        ));

        rootPadding.add(buildTopToolbarCard(), BorderLayout.NORTH);
        rootPadding.add(buildMainSplitPane(), BorderLayout.CENTER);
        setContentPane(rootPadding);

        pack();
        setLocationRelativeTo(null);
    }

    private JPanel buildTopToolbarCard() {
        JPanel card = UiTheme.createCardPanel();
        card.setLayout(new BorderLayout(0, 8));

        JPanel controlsRow = new JPanel(new GridBagLayout());
        controlsRow.setBackground(UiTheme.BG_CARD);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;

        UiTheme.styleField(portField);
        UiTheme.stylePrimaryButton(startStopButton);
        UiTheme.styleField(desKeyField);
        desKeyField.putClientProperty("JTextField.placeholderText", "Nhập đúng 8 ký tự");
        UiTheme.styleSecondaryButton(applyKeyButton);

        gbc.gridx = 0;
        controlsRow.add(createFieldLabel("Port UDP:"), gbc);

        gbc.gridx = 1;
        controlsRow.add(portField, gbc);

        gbc.gridx = 2;
        controlsRow.add(startStopButton, gbc);

        gbc.gridx = 3;
        gbc.insets = new Insets(0, 14, 0, 10);
        controlsRow.add(createFieldLabel("Khóa DES (8 ký tự):"), gbc);

        gbc.gridx = 4;
        gbc.insets = new Insets(0, 0, 0, 10);
        controlsRow.add(desKeyField, gbc);

        gbc.gridx = 5;
        controlsRow.add(applyKeyButton, gbc);

        gbc.gridx = 6;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 12, 0, 0);
        JPanel statusBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        statusBox.setBackground(UiTheme.BG_CARD);
        UiTheme.setStatusLabel(serverStatusLabel, false, "Server: Đang dừng");
        UiTheme.setStatusLabel(mysqlStatusLabel, false, "MySQL: Chưa kết nối");
        statusBox.add(serverStatusLabel);
        statusBox.add(mysqlStatusLabel);
        controlsRow.add(statusBox, gbc);

        cryptoErrorLabel.setFont(UiTheme.uiFont(Font.PLAIN, 12));
        cryptoErrorLabel.setForeground(UiTheme.STATUS_DISCONNECTED);

        card.add(controlsRow, BorderLayout.CENTER);
        card.add(cryptoErrorLabel, BorderLayout.SOUTH);
        return card;
    }

    private JSplitPane buildMainSplitPane() {
        JPanel tableCard = UiTheme.createCardPanel();
        tableCard.setLayout(new BorderLayout(0, 10));

        JLabel tableTitle = new JLabel("Dữ liệu Sinh viên sau giải mã");
        tableTitle.setFont(UiTheme.uiFont(Font.BOLD, 14));
        tableTitle.setForeground(UiTheme.TEXT_PRIMARY);
        tableCard.add(tableTitle, BorderLayout.NORTH);

        UiTheme.styleTable(studentTable, new boolean[]{false, false, true, true, true, true});
        JScrollPane tableScroll = new JScrollPane(studentTable);
        tableScroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER_SUBTLE, 1));
        tableCard.add(tableScroll, BorderLayout.CENTER);

        JPanel logCard = UiTheme.createCardPanel();
        logCard.setLayout(new BorderLayout(0, 10));

        JPanel logHeader = new JPanel(new BorderLayout());
        logHeader.setBackground(UiTheme.BG_CARD);
        JLabel logTitle = new JLabel("Console Log (Gói tin UDP & Chuỗi DES Base64)");
        logTitle.setFont(UiTheme.uiFont(Font.BOLD, 14));
        logTitle.setForeground(UiTheme.TEXT_PRIMARY);

        JButton clearLogButton = new JButton("Xóa log");
        UiTheme.styleSecondaryButton(clearLogButton);
        clearLogButton.addActionListener(e -> logArea.setText(""));
        logHeader.add(logTitle, BorderLayout.WEST);
        logHeader.add(clearLogButton, BorderLayout.EAST);
        logCard.add(logHeader, BorderLayout.NORTH);

        logArea.setEditable(false);
        logArea.setFont(UiTheme.monoFont(12));
        logArea.setForeground(UiTheme.TEXT_PRIMARY);
        logArea.setBackground(UiTheme.BG_WINDOW);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setMargin(new Insets(8, 10, 8, 10));

        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER_SUBTLE, 1));
        logCard.add(logScroll, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableCard, logCard);
        splitPane.setResizeWeight(0.54);
        splitPane.setDividerSize(8);
        splitPane.setBorder(null);
        splitPane.setBackground(UiTheme.BG_WINDOW);
        return splitPane;
    }

    private JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UiTheme.uiFont(Font.BOLD, 12));
        label.setForeground(UiTheme.TEXT_SECONDARY);
        return label;
    }

    private void wireEvents() {
        startStopButton.addActionListener(e -> toggleServer());
        applyKeyButton.addActionListener(e -> applyDesKey());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                serverEngine.shutdown();
            }
        });
    }

    private void toggleServer() {
        if (serverEngine.isRunning()) {
            serverEngine.stop();
            return;
        }
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
            if (port < 1 || port > 65535) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Port UDP phải là số nguyên từ 1 đến 65535.",
                    "Port không hợp lệ",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        try {
            serverEngine.start(port);
        } catch (SocketException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Không thể mở port UDP " + port + ": " + ex.getMessage(),
                    "Lỗi khởi động Server",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void applyDesKey() {
        String key = desKeyField.getText();
        cryptoErrorLabel.setText(" ");
        applyKeyButton.setEnabled(false);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                serverEngine.applyDesKeyAndReload(key);
                return null;
            }

            @Override
            protected void done() {
                applyKeyButton.setEnabled(true);
                try {
                    get();
                    cryptoErrorLabel.setForeground(UiTheme.STATUS_CONNECTED);
                    cryptoErrorLabel.setText("Đã áp dụng khóa DES hợp lệ.");
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    cryptoErrorLabel.setForeground(UiTheme.STATUS_DISCONNECTED);
                    cryptoErrorLabel.setText(cause.getMessage());
                    JOptionPane.showMessageDialog(
                            ServerFrame.this,
                            cause.getMessage(),
                            "Lỗi khóa / giải mã DES",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        }.execute();
    }

    @Override
    public void onLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    @Override
    public void onServerStateChanged(boolean running, int port) {
        SwingUtilities.invokeLater(() -> {
            portField.setEnabled(!running);
            if (running) {
                startStopButton.setText("Dừng Server");
                UiTheme.styleDangerButton(startStopButton);
                UiTheme.setStatusLabel(serverStatusLabel, true, "Server: Đang chạy (Port " + port + ")");
            } else {
                startStopButton.setText("Khởi động");
                UiTheme.stylePrimaryButton(startStopButton);
                UiTheme.setStatusLabel(serverStatusLabel, false, "Server: Đang dừng");
            }
        });
    }

    @Override
    public void onDbStatusChanged(boolean connected, String summary) {
        SwingUtilities.invokeLater(() -> {
            String text = connected ? "MySQL: Đã kết nối (" + summary + ")" : "MySQL: " + summary;
            UiTheme.setStatusLabel(mysqlStatusLabel, connected, text);
        });
    }

    @Override
    public void onResultsUpdated(List<ResultDTO> results) {
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);
            for (ResultDTO item : results) {
                tableModel.addRow(new Object[]{
                        item.getMaSV(),
                        item.getHoTen(),
                        String.format(Locale.US, "%.2f", item.getToan()),
                        String.format(Locale.US, "%.2f", item.getVan()),
                        String.format(Locale.US, "%.2f", item.getAnh()),
                        String.format(Locale.US, "%.2f", item.getDiemTB())
                });
            }
            if (!results.isEmpty()) {
                cryptoErrorLabel.setForeground(UiTheme.STATUS_CONNECTED);
                cryptoErrorLabel.setText("Hiển thị " + results.size() + " sinh viên đã giải mã.");
            }
        });
    }

    @Override
    public void onDecryptionError(String errorMessage) {
        SwingUtilities.invokeLater(() -> {
            cryptoErrorLabel.setForeground(UiTheme.STATUS_DISCONNECTED);
            cryptoErrorLabel.setText(errorMessage);
        });
    }

    public static void main(String[] args) {
        UiTheme.install();
        SwingUtilities.invokeLater(() -> {
            ServerFrame frame = new ServerFrame();
            frame.setVisible(true);
        });
    }
}
