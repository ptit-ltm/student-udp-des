package com.studentudp.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.studentudp.client.ClientRequestException;
import com.studentudp.client.UdpClientEngine;
import com.studentudp.common.DbConfigDTO;
import com.studentudp.common.ResultDTO;
import com.studentudp.common.StudentDTO;
import com.studentudp.server.DesCryptoException;
import com.studentudp.server.DesCryptoUtil;
import com.studentudp.server.EncryptedStudentRow;
import com.studentudp.server.StudentRepository;
import com.studentudp.server.UdpServerEngine;
import java.net.DatagramSocket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UdpProtocolIntegrationTest {

    private static final String KEY_OK = "PTIT2026";
    private static final String KEY_WRONG = "WRONG999";

    private InMemoryRepository repository;
    private UdpServerEngine server;
    private UdpClientEngine client;
    private int serverPort;

    @BeforeEach
    void setUp() throws Exception {
        repository = new InMemoryRepository();
        server = new UdpServerEngine(repository);
        server.start(0);
        serverPort = server.getLocalPort();
        client = new UdpClientEngine();
    }

    @AfterEach
    void tearDown() {
        server.shutdown();
    }

    @Test
    void fullWorkflowPingDbConfigAddStudentGetResultsAndWrongKeyRecovery() throws Exception {
        // 1. PING / PONG
        client.ping("127.0.0.1", serverPort);

        // 2. DB_CONFIG
        String dbMsg = client.configureDatabase(new DbConfigDTO("localhost", 3306, "student_db", "root", "pw"));
        assertTrue(dbMsg.contains("thành công"));

        // 3. ADD_STUDENT before DES key is set returns clean error without crashing server
        ClientRequestException missingKeyEx = assertThrows(
                ClientRequestException.class,
                () -> client.addStudent(new StudentDTO("SV001", "Nguyễn Văn A", 8.0, 9.0, 7.0))
        );
        assertTrue(missingKeyEx.getMessage().contains("Khóa DES"));

        // 4. Set valid 8-char DES key on server and add student
        server.applyDesKeyAndReload(KEY_OK);
        List<ResultDTO> results = client.addStudent(new StudentDTO("B21DCCN001", "Nguyễn Văn An", 8.5, 7.5, 9.0));

        assertEquals(1, results.size());
        ResultDTO first = results.get(0);
        assertEquals("B21DCCN001", first.getMaSV());
        assertEquals("Nguyễn Văn An", first.getHoTen());
        assertEquals((8.5 + 7.5 + 9.0) / 3.0, first.getDiemTB(), 1e-6);

        // Verify stored row in repository is DES Base64 ciphertext, not plaintext
        List<EncryptedStudentRow> stored = repository.findAllEncrypted();
        assertEquals(1, stored.size());
        EncryptedStudentRow row = stored.get(0);
        assertNotEquals("B21DCCN001", row.maSvEnc());
        assertNotEquals("Nguyễn Văn An", row.hoTenEnc());
        assertEquals("Nguyễn Văn An", DesCryptoUtil.decrypt(row.hoTenEnc(), KEY_OK));

        // 5. GET_RESULTS returns expected list
        List<ResultDTO> fetched = client.getResults();
        assertEquals(1, fetched.size());
        assertEquals("B21DCCN001", fetched.get(0).getMaSV());

        // 6. Switch server to wrong DES key -> throws DesCryptoException safely; server remains alive
        assertThrows(DesCryptoException.class, () -> server.applyDesKeyAndReload(KEY_WRONG));
        ClientRequestException wrongKeyClientEx = assertThrows(ClientRequestException.class, () -> client.getResults());
        assertTrue(wrongKeyClientEx.getMessage().contains("Giải mã DES thất bại"));

        // 7. Restore correct key -> recovery succeeds immediately
        server.applyDesKeyAndReload(KEY_OK);
        List<ResultDTO> recovered = client.getResults();
        assertEquals(1, recovered.size());
        assertEquals("Nguyễn Văn An", recovered.get(0).getHoTen());
    }

    @Test
    void pingToUnreachablePortTimesOutAfterThreeSeconds() throws Exception {
        int unusedPort;
        try (DatagramSocket temp = new DatagramSocket(0)) {
            unusedPort = temp.getLocalPort();
        }
        long startMs = System.currentTimeMillis();
        ClientRequestException ex = assertThrows(
                ClientRequestException.class,
                () -> client.ping("127.0.0.1", unusedPort)
        );
        long elapsedMs = System.currentTimeMillis() - startMs;

        assertTrue(ex.getMessage().contains("3000ms"));
        assertTrue(elapsedMs >= 2500, "Expected ~3000ms timeout, got " + elapsedMs + "ms");
    }

    private static final class InMemoryRepository implements StudentRepository {
        private final List<EncryptedStudentRow> rows = new ArrayList<>();
        private boolean connected = false;
        private String summary = "Chưa kết nối";

        @Override
        public synchronized void connect(DbConfigDTO config) throws SQLException {
            config.validate();
            this.connected = true;
            this.summary = config.getHost() + ":" + config.getPort() + "/" + config.getDbName();
        }

        @Override
        public synchronized boolean isConnected() {
            return connected;
        }

        @Override
        public synchronized String getConnectionSummary() {
            return summary;
        }

        @Override
        public synchronized void insertStudent(EncryptedStudentRow row) throws SQLException {
            if (!connected) {
                throw new SQLException("Not connected");
            }
            rows.add(new EncryptedStudentRow(
                    rows.size() + 1,
                    row.maSvEnc(),
                    row.hoTenEnc(),
                    row.toanEnc(),
                    row.vanEnc(),
                    row.anhEnc()
            ));
        }

        @Override
        public synchronized List<EncryptedStudentRow> findAllEncrypted() throws SQLException {
            if (!connected) {
                throw new SQLException("Not connected");
            }
            return new ArrayList<>(rows);
        }

        @Override
        public synchronized void close() {
            connected = false;
        }
    }
}
