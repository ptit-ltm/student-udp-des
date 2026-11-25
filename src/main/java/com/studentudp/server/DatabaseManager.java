package com.studentudp.server;

import com.studentudp.common.DbConfigDTO;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic MySQL JDBC repository handling schema creation and encrypted student CRUD.
 */
public class DatabaseManager implements StudentRepository {

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS sinh_vien (
                id INT AUTO_INCREMENT PRIMARY KEY,
                ma_sv VARCHAR(255) NOT NULL,
                ho_ten VARCHAR(255) NOT NULL,
                toan VARCHAR(255) NOT NULL,
                van VARCHAR(255) NOT NULL,
                anh VARCHAR(255) NOT NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;

    private static final String INSERT_SQL =
            "INSERT INTO sinh_vien (ma_sv, ho_ten, toan, van, anh) VALUES (?, ?, ?, ?, ?)";

    private static final String SELECT_ALL_SQL =
            "SELECT id, ma_sv, ho_ten, toan, van, anh FROM sinh_vien ORDER BY id ASC";

    private Connection connection;
    private DbConfigDTO activeConfig;

    /**
     * Builds the dynamic MySQL JDBC connection string required by the specification.
     */
    public static String buildJdbcUrl(DbConfigDTO config) {
        if (config == null) {
            throw new IllegalArgumentException("Cấu hình MySQL không được null.");
        }
        config.validate();
        return String.format(
                "jdbc:mysql://%s:%d/%s?createDatabaseIfNotExist=true&useSSL=false"
                        + "&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8",
                config.getHost().trim(),
                config.getPort(),
                config.getDbName().trim()
        );
    }

    @Override
    public synchronized void connect(DbConfigDTO config) throws SQLException {
        if (config == null) {
            throw new SQLException("Cấu hình kết nối MySQL không hợp lệ.");
        }
        try {
            config.validate();
        } catch (IllegalArgumentException ex) {
            throw new SQLException(ex.getMessage(), ex);
        }

        closeQuietly();
        String url = buildJdbcUrl(config);
        String password = config.getPassword() == null ? "" : config.getPassword();

        DriverManager.setLoginTimeout(5);
        Connection newConn = DriverManager.getConnection(url, config.getUsername().trim(), password);
        try (Statement stmt = newConn.createStatement()) {
            stmt.execute(CREATE_TABLE_SQL);
        } catch (SQLException ex) {
            closeConnection(newConn);
            throw ex;
        }
        this.connection = newConn;
        this.activeConfig = config;
    }

    @Override
    public synchronized boolean isConnected() {
        if (connection == null) {
            return false;
        }
        try {
            return !connection.isClosed() && connection.isValid(2);
        } catch (SQLException ex) {
            return false;
        }
    }

    @Override
    public synchronized String getConnectionSummary() {
        if (activeConfig == null || !isConnected()) {
            return "Chưa kết nối";
        }
        return activeConfig.getHost() + ":" + activeConfig.getPort() + "/" + activeConfig.getDbName();
    }

    @Override
    public synchronized void insertStudent(EncryptedStudentRow row) throws SQLException {
        ensureConnected();
        try (PreparedStatement ps = connection.prepareStatement(INSERT_SQL)) {
            ps.setString(1, row.maSvEnc());
            ps.setString(2, row.hoTenEnc());
            ps.setString(3, row.toanEnc());
            ps.setString(4, row.vanEnc());
            ps.setString(5, row.anhEnc());
            ps.executeUpdate();
        }
    }

    @Override
    public synchronized List<EncryptedStudentRow> findAllEncrypted() throws SQLException {
        ensureConnected();
        List<EncryptedStudentRow> rows = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(SELECT_ALL_SQL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new EncryptedStudentRow(
                        rs.getInt("id"),
                        rs.getString("ma_sv"),
                        rs.getString("ho_ten"),
                        rs.getString("toan"),
                        rs.getString("van"),
                        rs.getString("anh")
                ));
            }
        }
        return rows;
    }

    @Override
    public synchronized void close() {
        closeQuietly();
        activeConfig = null;
    }

    private void ensureConnected() throws SQLException {
        if (connection == null) {
            throw new SQLException("Server chưa kết nối tới MySQL. Hãy cấu hình DB_CONFIG từ Client trước.");
        }
        if (connection.isClosed() || !connection.isValid(2)) {
            if (activeConfig != null) {
                connect(activeConfig);
            } else {
                throw new SQLException("Kết nối MySQL đã bị đóng.");
            }
        }
    }

    private void closeQuietly() {
        if (connection != null) {
            closeConnection(connection);
            connection = null;
        }
    }

    private static void closeConnection(Connection conn) {
        try {
            if (!conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException ignored) {
        }
    }
}
