package com.studentudp.client;

import com.studentudp.common.DbConfigDTO;
import com.studentudp.common.ResultDTO;
import com.studentudp.common.StudentDTO;
import com.studentudp.common.UiTheme;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

/**
 * Step 3 Panel: Two-zone Student Management workspace (validated input form + JTable results).
 */
public class StudentManagerPanel extends JPanel {

    public interface NavigationListener {
        void onChangeDb();

        void onDisconnect();
    }

    private final UdpClientEngine clientEngine;
    private final NavigationListener navigationListener;

    private final JLabel connectionInfoLabel = new JLabel("Server: - | MySQL: -");
    private final JButton changeDbButton = new JButton("Đổi CSDL");
    private final JButton disconnectButton = new JButton("Ngắt kết nối");

    private final JTextField maSvField = new JTextField(18);
    private final JTextField hoTenField = new JTextField(18);
    private final JTextField toanField = new JTextField(18);
    private final JTextField vanField = new JTextField(18);
    private final JTextField anhField = new JTextField(18);

    private final JButton addButton = new JButton("Thêm sinh viên");
    private final JButton refreshButton = new JButton("Làm mới danh sách");
    private final JLabel formStatusLabel = new JLabel(" ");
    private final JLabel countLabel = new JLabel("Tổng số: 0 sinh viên");

    private final DefaultTableModel tableModel;
    private final JTable resultsTable;

    public StudentManagerPanel(UdpClientEngine clientEngine, NavigationListener navigationListener) {
        this.clientEngine = clientEngine;
        this.navigationListener = navigationListener;

        String[] columns = {"Mã SV", "Họ tên", "Điểm TB"};
        this.tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.resultsTable = new JTable(tableModel);

        initUi();
    }

    public void onEnterStep(DbConfigDTO dbConfig) {
        String dbSummary = dbConfig.getHost() + ":" + dbConfig.getPort() + "/" + dbConfig.getDbName();
        connectionInfoLabel.setText(
                "Server: " + clientEngine.getServerEndpointDisplay() + "  |  MySQL: " + dbSummary
        );
        formStatusLabel.setText(" ");
        loadResultsAsync();
    }

    private void initUi() {
        setLayout(new BorderLayout(UiTheme.PADDING, UiTheme.PADDING));
        setBackground(UiTheme.BG_WINDOW);
        setBorder(BorderFactory.createEmptyBorder(
                UiTheme.PADDING, UiTheme.PADDING, UiTheme.PADDING, UiTheme.PADDING
        ));

        add(buildHeaderCard(), BorderLayout.NORTH);
        add(buildFormCard(), BorderLayout.WEST);
        add(buildTableCard(), BorderLayout.CENTER);
    }

    private JPanel buildHeaderCard() {
        JPanel card = UiTheme.createCardPanel();
        card.setLayout(new BorderLayout());

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setBackground(UiTheme.BG_CARD);
        JLabel stepTitle = new JLabel("Quản lý Sinh viên");
        stepTitle.setFont(UiTheme.uiFont(Font.BOLD, 16));
        stepTitle.setForeground(UiTheme.TEXT_PRIMARY);
        connectionInfoLabel.setFont(UiTheme.uiFont(Font.BOLD, 12));
        connectionInfoLabel.setForeground(UiTheme.STATUS_CONNECTED);
        left.add(stepTitle);
        left.add(connectionInfoLabel);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setBackground(UiTheme.BG_CARD);
        UiTheme.styleSecondaryButton(changeDbButton);
        UiTheme.styleSecondaryButton(disconnectButton);
        right.add(changeDbButton);
        right.add(disconnectButton);

        card.add(left, BorderLayout.WEST);
        card.add(right, BorderLayout.EAST);

        changeDbButton.addActionListener(e -> navigationListener.onChangeDb());
        disconnectButton.addActionListener(e -> navigationListener.onDisconnect());
        return card;
    }

    private JPanel buildFormCard() {
        JPanel card = UiTheme.createCardPanel();
        card.setLayout(new GridBagLayout());
        card.setPreferredSize(new Dimension(340, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JLabel formTitle = new JLabel("Nhập thông tin sinh viên");
        formTitle.setFont(UiTheme.uiFont(Font.BOLD, 15));
        formTitle.setForeground(UiTheme.TEXT_PRIMARY);
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 12, 0);
        card.add(formTitle, gbc);

        addFormField(card, gbc, 1, "Mã sinh viên", maSvField);
        addFormField(card, gbc, 3, "Họ và tên", hoTenField);
        addFormField(card, gbc, 5, "Điểm Toán (0 - 10)", toanField);
        addFormField(card, gbc, 7, "Điểm Văn (0 - 10)", vanField);
        addFormField(card, gbc, 9, "Điểm Anh (0 - 10)", anhField);

        UiTheme.stylePrimaryButton(addButton);
        gbc.gridy = 11;
        gbc.insets = new Insets(4, 0, 8, 0);
        card.add(addButton, gbc);

        UiTheme.styleSecondaryButton(refreshButton);
        gbc.gridy = 12;
        gbc.insets = new Insets(0, 0, 10, 0);
        card.add(refreshButton, gbc);

        formStatusLabel.setFont(UiTheme.uiFont(Font.PLAIN, 12));
        formStatusLabel.setForeground(UiTheme.TEXT_SECONDARY);
        gbc.gridy = 13;
        gbc.insets = new Insets(0, 0, 0, 0);
        card.add(formStatusLabel, gbc);

        gbc.gridy = 14;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        JPanel spacer = new JPanel();
        spacer.setBackground(UiTheme.BG_CARD);
        card.add(spacer, gbc);

        addButton.addActionListener(e -> submitStudent());
        refreshButton.addActionListener(e -> loadResultsAsync());
        anhField.addActionListener(e -> submitStudent());

        return card;
    }

    private JPanel buildTableCard() {
        JPanel card = UiTheme.createCardPanel();
        card.setLayout(new BorderLayout(0, 10));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setBackground(UiTheme.BG_CARD);
        JLabel tableTitle = new JLabel("Danh sách kết quả (Đã giải mã DES)");
        tableTitle.setFont(UiTheme.uiFont(Font.BOLD, 15));
        tableTitle.setForeground(UiTheme.TEXT_PRIMARY);
        countLabel.setFont(UiTheme.uiFont(Font.PLAIN, 12));
        countLabel.setForeground(UiTheme.TEXT_SECONDARY);
        topRow.add(tableTitle, BorderLayout.WEST);
        topRow.add(countLabel, BorderLayout.EAST);
        card.add(topRow, BorderLayout.NORTH);

        UiTheme.styleTable(resultsTable, new boolean[]{false, false, true});
        resultsTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        resultsTable.getColumnModel().getColumn(1).setPreferredWidth(260);
        resultsTable.getColumnModel().getColumn(2).setPreferredWidth(100);

        JScrollPane scrollPane = new JScrollPane(resultsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER_SUBTLE, 1));
        card.add(scrollPane, BorderLayout.CENTER);
        return card;
    }

    private void addFormField(JPanel card, GridBagConstraints gbc, int rowIdx, String labelText, JTextField field) {
        JLabel label = new JLabel(labelText);
        label.setFont(UiTheme.uiFont(Font.BOLD, 12));
        label.setForeground(UiTheme.TEXT_PRIMARY);
        gbc.gridy = rowIdx;
        gbc.insets = new Insets(0, 0, 4, 0);
        card.add(label, gbc);

        UiTheme.styleField(field);
        gbc.gridy = rowIdx + 1;
        gbc.insets = new Insets(0, 0, 10, 0);
        card.add(field, gbc);
    }

    private void submitStudent() {
        String maSv = maSvField.getText().trim();
        String hoTen = hoTenField.getText().trim();
        if (maSv.isEmpty()) {
            showStatus(false, "Mã sinh viên không được để trống.");
            return;
        }
        if (hoTen.isEmpty()) {
            showStatus(false, "Họ và tên sinh viên không được để trống.");
            return;
        }

        double toan;
        double van;
        double anh;
        try {
            toan = parseScore("Toán", toanField.getText());
            van = parseScore("Văn", vanField.getText());
            anh = parseScore("Anh", anhField.getText());
        } catch (IllegalArgumentException ex) {
            showStatus(false, ex.getMessage());
            return;
        }

        StudentDTO student = new StudentDTO(maSv, hoTen, toan, van, anh);
        setBusy(true, "Đang gửi sinh viên và giải mã kết quả...");

        new SwingWorker<List<ResultDTO>, Void>() {
            @Override
            protected List<ResultDTO> doInBackground() throws Exception {
                return clientEngine.addStudent(student);
            }

            @Override
            protected void done() {
                setBusy(false, null);
                try {
                    List<ResultDTO> results = get();
                    updateTable(results);
                    clearInputs();
                    showStatus(true, "Đã thêm sinh viên và cập nhật bảng điểm.");
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    showStatus(false, cause.getMessage());
                }
            }
        }.execute();
    }

    private void loadResultsAsync() {
        setBusy(true, "Đang tải danh sách sinh viên từ Server...");

        new SwingWorker<List<ResultDTO>, Void>() {
            @Override
            protected List<ResultDTO> doInBackground() throws Exception {
                return clientEngine.getResults();
            }

            @Override
            protected void done() {
                setBusy(false, null);
                try {
                    List<ResultDTO> results = get();
                    updateTable(results);
                    showStatus(true, "Đã tải " + results.size() + " sinh viên.");
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    showStatus(false, cause.getMessage());
                }
            }
        }.execute();
    }

    private static double parseScore(String subject, String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập điểm " + subject + ".");
        }
        double value;
        try {
            value = Double.parseDouble(raw.trim().replace(',', '.'));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Điểm " + subject + " phải là số thực hợp lệ.");
        }
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0.0 || value > 10.0) {
            throw new IllegalArgumentException("Điểm " + subject + " phải nằm trong khoảng 0 đến 10.");
        }
        return value;
    }

    private void updateTable(List<ResultDTO> results) {
        tableModel.setRowCount(0);
        for (ResultDTO item : results) {
            tableModel.addRow(new Object[]{
                    item.getMaSV(),
                    item.getHoTen(),
                    String.format(Locale.US, "%.2f", item.getDiemTB())
            });
        }
        countLabel.setText("Tổng số: " + results.size() + " sinh viên");
    }

    private void clearInputs() {
        maSvField.setText("");
        hoTenField.setText("");
        toanField.setText("");
        vanField.setText("");
        anhField.setText("");
        maSvField.requestFocusInWindow();
    }

    private void setBusy(boolean busy, String message) {
        maSvField.setEnabled(!busy);
        hoTenField.setEnabled(!busy);
        toanField.setEnabled(!busy);
        vanField.setEnabled(!busy);
        anhField.setEnabled(!busy);
        addButton.setEnabled(!busy);
        refreshButton.setEnabled(!busy);
        if (message != null) {
            formStatusLabel.setForeground(UiTheme.TEXT_SECONDARY);
            formStatusLabel.setText(message);
        }
    }

    private void showStatus(boolean success, String message) {
        formStatusLabel.setForeground(success ? UiTheme.STATUS_CONNECTED : UiTheme.STATUS_DISCONNECTED);
        String safe = message == null ? "" : message.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        formStatusLabel.setText("<html><body style='width:295px'>" + safe + "</body></html>");
    }
}
