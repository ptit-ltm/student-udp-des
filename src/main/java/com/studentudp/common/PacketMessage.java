package com.studentudp.common;

import com.google.gson.JsonElement;

/**
 * Unified JSON packet envelope transmitted over UDP DatagramPackets.
 */
public class PacketMessage {

    private String type;
    private String status;
    private String message;
    private JsonElement payload;

    public PacketMessage() {
    }

    public PacketMessage(String type, String status, String message, JsonElement payload) {
        this.type = type;
        this.status = status;
        this.message = message;
        this.payload = payload;
    }

    public static PacketMessage request(String type, JsonElement payload) {
        return new PacketMessage(type, ProtocolConstants.STATUS_OK, "", payload);
    }

    public static PacketMessage ok(String type, String message, JsonElement payload) {
        return new PacketMessage(type, ProtocolConstants.STATUS_OK, message, payload);
    }

    public static PacketMessage error(String type, String message) {
        return new PacketMessage(type, ProtocolConstants.STATUS_ERROR, message, null);
    }

    public boolean isOk() {
        return ProtocolConstants.STATUS_OK.equalsIgnoreCase(status);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public JsonElement getPayload() {
        return payload;
    }

    public void setPayload(JsonElement payload) {
        this.payload = payload;
    }
}
