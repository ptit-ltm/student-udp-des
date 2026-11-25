package com.studentudp.client;

import com.google.gson.reflect.TypeToken;
import com.studentudp.common.DbConfigDTO;
import com.studentudp.common.PacketMessage;
import com.studentudp.common.ProtocolConstants;
import com.studentudp.common.ResultDTO;
import com.studentudp.common.StudentDTO;
import com.studentudp.common.UdpHelper;
import com.studentudp.common.UdpHelper.ReceivedPacket;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

/**
 * UDP client networking engine enforcing a 3000ms socket timeout on every request/response cycle.
 */
public class UdpClientEngine {

    private static final Type RESULT_LIST_TYPE = new TypeToken<List<ResultDTO>>() {
    }.getType();

    private InetAddress serverAddress;
    private int serverPort = -1;
    private String serverHostDisplay = "";

    public synchronized String getServerEndpointDisplay() {
        if (serverAddress == null || serverPort < 1) {
            return "Chưa kết nối";
        }
        return serverHostDisplay + ":" + serverPort;
    }

    public synchronized void ping(String host, int port) throws ClientRequestException {
        if (host == null || host.trim().isEmpty()) {
            throw new ClientRequestException("Địa chỉ IP Server không được để trống.");
        }
        if (port < 1 || port > 65535) {
            throw new ClientRequestException("Port UDP phải nằm trong khoảng 1 - 65535.");
        }

        InetAddress targetAddress;
        try {
            targetAddress = InetAddress.getByName(host.trim());
        } catch (UnknownHostException ex) {
            throw new ClientRequestException("Không phân giải được địa chỉ Server: " + host, ex);
        }

        PacketMessage response = sendAndReceive(
                targetAddress,
                port,
                PacketMessage.request(ProtocolConstants.TYPE_PING, null)
        );
        if (!response.isOk() || !ProtocolConstants.TYPE_PONG.equalsIgnoreCase(response.getType())) {
            throw new ClientRequestException(
                    response.getMessage() != null && !response.getMessage().isBlank()
                            ? response.getMessage()
                            : "Server phản hồi không hợp lệ khi kiểm tra PING."
            );
        }

        this.serverAddress = targetAddress;
        this.serverPort = port;
        this.serverHostDisplay = host.trim();
    }

    public String configureDatabase(DbConfigDTO config) throws ClientRequestException {
        ensureServerSelected();
        try {
            config.validate();
        } catch (IllegalArgumentException ex) {
            throw new ClientRequestException(ex.getMessage(), ex);
        }

        PacketMessage request = PacketMessage.request(
                ProtocolConstants.TYPE_DB_CONFIG,
                UdpHelper.toJsonTree(config)
        );
        PacketMessage response = sendAndReceive(serverAddress, serverPort, request);
        if (!response.isOk()) {
            throw new ClientRequestException(response.getMessage());
        }
        return response.getMessage();
    }

    public List<ResultDTO> addStudent(StudentDTO student) throws ClientRequestException {
        ensureServerSelected();
        try {
            student.validate();
        } catch (IllegalArgumentException ex) {
            throw new ClientRequestException(ex.getMessage(), ex);
        }

        PacketMessage request = PacketMessage.request(
                ProtocolConstants.TYPE_ADD_STUDENT,
                UdpHelper.toJsonTree(student)
        );
        PacketMessage response = sendAndReceive(serverAddress, serverPort, request);
        if (!response.isOk()) {
            throw new ClientRequestException(response.getMessage());
        }
        return parseResultList(response);
    }

    public List<ResultDTO> getResults() throws ClientRequestException {
        ensureServerSelected();
        PacketMessage request = PacketMessage.request(ProtocolConstants.TYPE_GET_RESULTS, null);
        PacketMessage response = sendAndReceive(serverAddress, serverPort, request);
        if (!response.isOk()) {
            throw new ClientRequestException(response.getMessage());
        }
        return parseResultList(response);
    }

    private synchronized void ensureServerSelected() throws ClientRequestException {
        if (serverAddress == null || serverPort < 1) {
            throw new ClientRequestException("Chưa kết nối tới Server UDP.");
        }
    }

    private PacketMessage sendAndReceive(InetAddress address, int port, PacketMessage request)
            throws ClientRequestException {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(ProtocolConstants.CLIENT_TIMEOUT_MS);
            UdpHelper.sendPacket(socket, address, port, request);
            ReceivedPacket received = UdpHelper.receivePacket(socket);
            return received.message();
        } catch (SocketTimeoutException ex) {
            throw new ClientRequestException(
                    "Hết thời gian chờ phản hồi từ Server (" + ProtocolConstants.CLIENT_TIMEOUT_MS
                            + "ms). Vui lòng kiểm tra lại IP/Port và chắc chắn Server đang chạy.",
                    ex
            );
        } catch (IOException ex) {
            throw new ClientRequestException("Lỗi kết nối UDP tới Server: " + ex.getMessage(), ex);
        }
    }

    private static List<ResultDTO> parseResultList(PacketMessage response) {
        if (response.getPayload() == null || response.getPayload().isJsonNull()) {
            return Collections.emptyList();
        }
        List<ResultDTO> list = UdpHelper.fromJson(response.getPayload(), RESULT_LIST_TYPE);
        return list != null ? list : Collections.emptyList();
    }
}
