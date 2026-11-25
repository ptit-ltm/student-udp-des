package com.studentudp.server;

/**
 * Represents one row stored in the MySQL sinh_vien table (all student fields DES-Base64 encrypted).
 */
public record EncryptedStudentRow(
        int id,
        String maSvEnc,
        String hoTenEnc,
        String toanEnc,
        String vanEnc,
        String anhEnc
) {
    public EncryptedStudentRow(String maSvEnc, String hoTenEnc, String toanEnc, String vanEnc, String anhEnc) {
        this(0, maSvEnc, hoTenEnc, toanEnc, vanEnc, anhEnc);
    }
}
