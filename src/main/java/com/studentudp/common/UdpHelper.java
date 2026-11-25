package com.studentudp.common;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

/**
 * Utility for serializing PacketMessage payloads as UTF-8 JSON over UDP DatagramSockets.
 */
public final class UdpHelper {

    private static final Gson GSON = new Gson();

    private UdpHelper() {
    }

    /**
     * Holds a decoded UDP datagram along with sender address metadata and raw JSON.
     */
    public record ReceivedPacket(
            PacketMessage message,
            InetAddress address,
            int port,
            String rawJson
    ) {
    }

    public static Gson gson() {
        return GSON;
    }

    public static JsonElement toJsonTree(Object value) {
        return GSON.toJsonTree(value);
    }

    public static <T> T fromJson(JsonElement element, Class<T> clazz) {
        return GSON.fromJson(element, clazz);
    }

    public static <T> T fromJson(JsonElement element, Type type) {
        return GSON.fromJson(element, type);
    }

    public static void sendPacket(DatagramSocket socket, InetAddress address, int port, PacketMessage packetMessage)
            throws IOException {
        String json = GSON.toJson(packetMessage);
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        if (data.length > ProtocolConstants.MAX_UDP_PACKET_SIZE) {
            throw new IOException("Kích thước gói tin vượt quá giới hạn UDP (" + data.length + " bytes).");
        }
        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        socket.send(packet);
    }

    public static ReceivedPacket receivePacket(DatagramSocket socket) throws IOException {
        byte[] buffer = new byte[ProtocolConstants.UDP_BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        String rawJson = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
        try {
            PacketMessage message = GSON.fromJson(rawJson, PacketMessage.class);
            if (message == null || message.getType() == null || message.getType().isBlank()) {
                throw new IOException("Gói tin JSON không hợp lệ hoặc thiếu trường type.");
            }
            return new ReceivedPacket(message, packet.getAddress(), packet.getPort(), rawJson);
        } catch (JsonSyntaxException ex) {
            throw new IOException("Không thể phân tích JSON từ gói tin UDP: " + ex.getMessage(), ex);
        }
    }
}
