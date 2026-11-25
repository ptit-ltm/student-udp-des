package com.studentudp.common;

/**
 * Shared protocol constants, defaults, and UDP limits used by Client and Server.
 */
public final class ProtocolConstants {

    public static final String TYPE_PING = "PING";
    public static final String TYPE_PONG = "PONG";
    public static final String TYPE_DB_CONFIG = "DB_CONFIG";
    public static final String TYPE_ADD_STUDENT = "ADD_STUDENT";
    public static final String TYPE_GET_RESULTS = "GET_RESULTS";
    public static final String TYPE_ERROR = "ERROR";

    public static final String STATUS_OK = "OK";
    public static final String STATUS_ERROR = "ERROR";

    public static final int CLIENT_TIMEOUT_MS = 3000;
    public static final int MAX_UDP_PACKET_SIZE = 65507;
    public static final int UDP_BUFFER_SIZE = 65535;

    public static final int DEFAULT_SERVER_PORT = 9876;
    public static final int DEFAULT_MYSQL_PORT = 3306;
    public static final int DES_KEY_LENGTH_BYTES = 8;

    private ProtocolConstants() {
    }
}
