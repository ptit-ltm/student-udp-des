package com.studentudp.common;

/**
 * Dynamic MySQL connection configuration sent from Client to Server via DB_CONFIG.
 */
public class DbConfigDTO {

    private String host;
    private int port;
    private String dbName;
    private String username;
    private String password;

    public DbConfigDTO() {
    }

    public DbConfigDTO(String host, int port, String dbName, String username, String password) {
        this.host = host;
        this.port = port;
        this.dbName = dbName;
        this.username = username;
        this.password = password;
    }

    /**
     * Validates required database connection parameters before JDBC connection attempt.
     */
    public void validate() {
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("Host MySQL không được để trống.");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port MySQL phải nằm trong khoảng 1 - 65535.");
        }
        if (dbName == null || dbName.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên database không được để trống.");
        }
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username MySQL không được để trống.");
        }
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
