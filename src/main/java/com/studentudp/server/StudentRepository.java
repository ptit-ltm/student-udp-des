package com.studentudp.server;

import com.studentudp.common.DbConfigDTO;
import java.sql.SQLException;
import java.util.List;

/**
 * Persistence boundary for encrypted student records.
 */
public interface StudentRepository {

    void connect(DbConfigDTO config) throws SQLException;

    boolean isConnected();

    String getConnectionSummary();

    void insertStudent(EncryptedStudentRow row) throws SQLException;

    List<EncryptedStudentRow> findAllEncrypted() throws SQLException;

    void close();
}
