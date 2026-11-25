package com.studentudp.server;

import com.studentudp.common.DbConfigDTO;
import com.studentudp.common.PacketMessage;
import com.studentudp.common.ProtocolConstants;
import com.studentudp.common.ResultDTO;
import com.studentudp.common.StudentDTO;
import com.studentudp.common.UdpHelper;
import com.studentudp.common.UdpHelper.ReceivedPacket;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Background UDP server engine handling PING, DB_CONFIG, ADD_STUDENT, and GET_RESULTS.
 * Coordinates DES encryption/decryption and MySQL persistence without blocking Swing EDT.
 */
public class UdpServerEngine {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * Listener notified when server state, logs, DB status, or decrypted records change.
     */
    public interface ServerEventListener {
        void onLog(String message);

        void onServerStateChanged(boolean running, int port);

        void onDbStatusChanged(boolean connected, String summary);

        void onResultsUpdated(List<ResultDTO> results);

        void onDecryptionError(String errorMessage);
    }

    private final StudentRepository repository;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile ServerEventListener listener;
    private volatile String desKey;
    private DatagramSocket socket;
    private Thread workerThread;

    public UdpServerEngine() {
        this(new DatabaseManager());
    }

    public UdpServerEngine(StudentRepository repository) {
        this.repository = repository;
    }

    public void setListener(ServerEventListener listener) {
        this.listener = listener;
    }

    public boolean isRunning() {
        return running.get();
    }

    public synchronized int getLocalPort() {
        return socket != null ? socket.getLocalPort() : -1;
    }

    public synchronized void start(int port) throws SocketException {
        if (running.get()) {
            return;
        }
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Port UDP không hợp lệ (1 - 65535).");
        }
        DatagramSocket serverSocket = new DatagramSocket(port);
        this.socket = serverSocket;
        this.running.set(true);

        int boundPort = serverSocket.getLocalPort();
        workerThread = new Thread(() -> listenLoop(serverSocket), "udp-server-listener");
        workerThread.setDaemon(true);
        workerThread.start();

        log("Server UDP đã khởi động tại port " + boundPort + ".");
        notifyServerState(true, boundPort);
    }

    public synchronized void stop() {
        if (!running.getAndSet(false)) {
            return;
        }
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        if (workerThread != null) {
            workerThread.interrupt();
        }
        log("Server UDP đã dừng.");
        notifyServerState(false, -1);
    }

    public void shutdown() {
        stop();
        repository.close();
        notifyDbStatus(false, "Chưa kết nối");
    }

    /**
     * Validates and applies a new 8-character DES key from the Server UI, then reloads & decrypts MySQL rows.
     */
    public void applyDesKeyAndReload(String newKey) throws DesCryptoException {
        DesCryptoUtil.buildKey(newKey);
        this.desKey = newKey;
        log("Đã cập nhật khóa DES (8 bytes).");

        if (!repository.isConnected()) {
            return;
        }
        try {
            List<ResultDTO> results = loadAndDecryptAll();
            notifyResults(results);
            log("Đã giải mã lại " + results.size() + " bản ghi từ cơ sở dữ liệu.");
        } catch (DesCryptoException ex) {
            log("LỖI GIẢI MÃ DES: " + ex.getMessage());
            notifyResults(Collections.emptyList());
            notifyDecryptionError(ex.getMessage());
            throw ex;
        } catch (SQLException ex) {
            log("LỖI CSDL khi đọc lại dữ liệu: " + ex.getMessage());
            notifyDecryptionError("Lỗi đọc cơ sở dữ liệu: " + ex.getMessage());
        }
    }

    /**
     * Reloads and decrypts records using the currently active DES key.
     */
    public List<ResultDTO> reloadWithCurrentKey() throws DesCryptoException, SQLException {
        ensureDesKeySet();
        List<ResultDTO> results = loadAndDecryptAll();
        notifyResults(results);
        return results;
    }

    private void listenLoop(DatagramSocket serverSocket) {
        while (running.get() && !serverSocket.isClosed()) {
            try {
                ReceivedPacket packet = UdpHelper.receivePacket(serverSocket);
                PacketMessage response = handleRequest(packet);
                UdpHelper.sendPacket(serverSocket, packet.address(), packet.port(), response);
            } catch (IOException ex) {
                if (running.get() && !serverSocket.isClosed()) {
                    log("Lỗi nhận/gửi gói tin UDP: " + ex.getMessage());
                }
            } catch (Exception ex) {
                if (running.get()) {
                    log("Lỗi không mong đợi khi xử lý gói tin: " + ex.getMessage());
                }
            }
        }
    }

    private PacketMessage handleRequest(ReceivedPacket packet) {
        PacketMessage req = packet.message();
        String type = req.getType() == null ? "" : req.getType().trim().toUpperCase();
        String clientTag = packet.address().getHostAddress() + ":" + packet.port();

        if (ProtocolConstants.TYPE_DB_CONFIG.equals(type)) {
            log("RECV [" + type + "] từ " + clientTag + " (cấu hình MySQL)");
        } else {
            log("RECV [" + type + "] từ " + clientTag + " | " + packet.rawJson());
        }

        return switch (type) {
            case ProtocolConstants.TYPE_PING ->
                    PacketMessage.ok(ProtocolConstants.TYPE_PONG, "Server UDP sẵn sàng.", null);
            case ProtocolConstants.TYPE_DB_CONFIG -> handleDbConfig(req);
            case ProtocolConstants.TYPE_ADD_STUDENT -> handleAddStudent(req);
            case ProtocolConstants.TYPE_GET_RESULTS -> handleGetResults();
            default -> PacketMessage.error(
                    ProtocolConstants.TYPE_ERROR,
                    "Loại thông điệp không được hỗ trợ: " + type
            );
        };
    }

    private PacketMessage handleDbConfig(PacketMessage req) {
        try {
            DbConfigDTO config = UdpHelper.fromJson(req.getPayload(), DbConfigDTO.class);
            if (config == null) {
                return PacketMessage.error(ProtocolConstants.TYPE_DB_CONFIG, "Thiếu thông tin cấu hình MySQL.");
            }
            config.validate();
            repository.connect(config);
            String summary = repository.getConnectionSummary();
            log("Đã kết nối MySQL: " + summary);
            notifyDbStatus(true, summary);

            if (desKey != null && !desKey.isEmpty()) {
                try {
                    List<ResultDTO> existing = loadAndDecryptAll();
                    notifyResults(existing);
                } catch (DesCryptoException ex) {
                    log("Cảnh báo giải mã dữ liệu có sẵn bằng khóa hiện tại: " + ex.getMessage());
                    notifyDecryptionError(ex.getMessage());
                }
            }
            return PacketMessage.ok(ProtocolConstants.TYPE_DB_CONFIG, "Kết nối MySQL thành công (" + summary + ").", null);
        } catch (IllegalArgumentException | SQLException ex) {
            log("Lỗi kết nối MySQL: " + ex.getMessage());
            notifyDbStatus(false, "Lỗi kết nối");
            return PacketMessage.error(ProtocolConstants.TYPE_DB_CONFIG, "Kết nối MySQL thất bại: " + ex.getMessage());
        }
    }

    private PacketMessage handleAddStudent(PacketMessage req) {
        try {
            ensureDbConnected();
            String key = ensureDesKeySet();

            StudentDTO student = UdpHelper.fromJson(req.getPayload(), StudentDTO.class);
            if (student == null) {
                return PacketMessage.error(ProtocolConstants.TYPE_ADD_STUDENT, "Thiếu dữ liệu sinh viên.");
            }
            student.validate();

            String maSvEnc = DesCryptoUtil.encrypt(student.getMaSV().trim(), key);
            String hoTenEnc = DesCryptoUtil.encrypt(student.getHoTen().trim(), key);
            String toanEnc = DesCryptoUtil.encrypt(String.valueOf(student.getToan()), key);
            String vanEnc = DesCryptoUtil.encrypt(String.valueOf(student.getVan()), key);
            String anhEnc = DesCryptoUtil.encrypt(String.valueOf(student.getAnh()), key);

            log("DES Base64 lưu DB -> ma_sv=" + maSvEnc
                    + ", ho_ten=" + hoTenEnc
                    + ", toan=" + toanEnc
                    + ", van=" + vanEnc
                    + ", anh=" + anhEnc);

            repository.insertStudent(new EncryptedStudentRow(maSvEnc, hoTenEnc, toanEnc, vanEnc, anhEnc));
            List<ResultDTO> results = loadAndDecryptAll();
            notifyResults(results);

            return PacketMessage.ok(
                    ProtocolConstants.TYPE_ADD_STUDENT,
                    "Đã mã hóa DES, lưu MySQL và tính ĐTB thành công.",
                    UdpHelper.toJsonTree(results)
            );
        } catch (IllegalArgumentException | IllegalStateException ex) {
            log("Từ chối ADD_STUDENT: " + ex.getMessage());
            return PacketMessage.error(ProtocolConstants.TYPE_ADD_STUDENT, ex.getMessage());
        } catch (DesCryptoException ex) {
            log("LỖI DES khi xử lý ADD_STUDENT: " + ex.getMessage());
            notifyDecryptionError(ex.getMessage());
            return PacketMessage.error(ProtocolConstants.TYPE_ADD_STUDENT, ex.getMessage());
        } catch (SQLException ex) {
            log("Lỗi MySQL khi lưu sinh viên: " + ex.getMessage());
            return PacketMessage.error(ProtocolConstants.TYPE_ADD_STUDENT, "Lỗi lưu MySQL: " + ex.getMessage());
        }
    }

    private PacketMessage handleGetResults() {
        try {
            ensureDbConnected();
            ensureDesKeySet();
            List<ResultDTO> results = loadAndDecryptAll();
            notifyResults(results);
            return PacketMessage.ok(
                    ProtocolConstants.TYPE_GET_RESULTS,
                    "Tải danh sách thành công (" + results.size() + " sinh viên).",
                    UdpHelper.toJsonTree(results)
            );
        } catch (IllegalStateException ex) {
            return PacketMessage.error(ProtocolConstants.TYPE_GET_RESULTS, ex.getMessage());
        } catch (DesCryptoException ex) {
            log("LỖI GIẢI MÃ DES khi GET_RESULTS: " + ex.getMessage());
            notifyDecryptionError(ex.getMessage());
            return PacketMessage.error(ProtocolConstants.TYPE_GET_RESULTS, ex.getMessage());
        } catch (SQLException ex) {
            log("Lỗi MySQL khi GET_RESULTS: " + ex.getMessage());
            return PacketMessage.error(ProtocolConstants.TYPE_GET_RESULTS, "Lỗi truy vấn MySQL: " + ex.getMessage());
        }
    }

    private List<ResultDTO> loadAndDecryptAll() throws SQLException, DesCryptoException {
        String key = ensureDesKeySet();
        List<EncryptedStudentRow> encryptedRows = repository.findAllEncrypted();
        List<ResultDTO> results = new ArrayList<>(encryptedRows.size());

        for (EncryptedStudentRow row : encryptedRows) {
            String maSv = DesCryptoUtil.decrypt(row.maSvEnc(), key);
            String hoTen = DesCryptoUtil.decrypt(row.hoTenEnc(), key);
            String toanStr = DesCryptoUtil.decrypt(row.toanEnc(), key);
            String vanStr = DesCryptoUtil.decrypt(row.vanEnc(), key);
            String anhStr = DesCryptoUtil.decrypt(row.anhEnc(), key);
            try {
                double toan = Double.parseDouble(toanStr);
                double van = Double.parseDouble(vanStr);
                double anh = Double.parseDouble(anhStr);
                results.add(ResultDTO.fromScores(maSv, hoTen, toan, van, anh));
            } catch (NumberFormatException ex) {
                throw new DesCryptoException(
                        "Dữ liệu điểm sau giải mã không hợp lệ (có thể sai khóa DES hoặc dữ liệu bị sửa).",
                        ex
                );
            }
        }
        return results;
    }

    private void ensureDbConnected() {
        if (!repository.isConnected()) {
            throw new IllegalStateException("Server chưa kết nối tới MySQL. Vui lòng cấu hình MySQL từ Client.");
        }
    }

    private String ensureDesKeySet() throws DesCryptoException {
        String current = this.desKey;
        DesCryptoUtil.buildKey(current);
        return current;
    }

    private void log(String message) {
        ServerEventListener target = this.listener;
        if (target != null) {
            target.onLog("[" + LocalTime.now().format(TIME_FMT) + "] " + message);
        }
    }

    private void notifyServerState(boolean isRunning, int port) {
        ServerEventListener target = this.listener;
        if (target != null) {
            target.onServerStateChanged(isRunning, port);
        }
    }

    private void notifyDbStatus(boolean connected, String summary) {
        ServerEventListener target = this.listener;
        if (target != null) {
            target.onDbStatusChanged(connected, summary);
        }
    }

    private void notifyResults(List<ResultDTO> results) {
        ServerEventListener target = this.listener;
        if (target != null) {
            target.onResultsUpdated(results);
        }
    }

    private void notifyDecryptionError(String errorMessage) {
        ServerEventListener target = this.listener;
        if (target != null) {
            target.onDecryptionError(errorMessage);
        }
    }
}
