package com.studentudp.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.studentudp.common.DbConfigDTO;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

class DatabaseManagerTest {

    @Test
    void buildJdbcUrlMatchesExpectedDynamicFormat() {
        DbConfigDTO config = new DbConfigDTO("127.0.0.1", 3306, "student_db", "root", "secret");
        String url = DatabaseManager.buildJdbcUrl(config);

        assertEquals(
                "jdbc:mysql://127.0.0.1:3306/student_db?createDatabaseIfNotExist=true"
                        + "&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8",
                url
        );
    }

    @Test
    void operationsFailCleanlyWhenNotConnected() {
        DatabaseManager manager = new DatabaseManager();
        assertFalse(manager.isConnected());
        assertThrows(SQLException.class, manager::findAllEncrypted);
        assertThrows(
                SQLException.class,
                () -> manager.insertStudent(new EncryptedStudentRow("a", "b", "c", "d", "e"))
        );
    }

    @Test
    void invalidConfigThrowsIllegalArgumentOnUrlBuild() {
        DbConfigDTO invalidPort = new DbConfigDTO("localhost", 0, "student_db", "root", "");
        assertThrows(IllegalArgumentException.class, () -> DatabaseManager.buildJdbcUrl(invalidPort));
    }
}
