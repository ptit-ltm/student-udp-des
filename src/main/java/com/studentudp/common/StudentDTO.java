package com.studentudp.common;

/**
 * Student input payload sent from Client to Server in ADD_STUDENT packets.
 */
public class StudentDTO {

    private String maSV;
    private String hoTen;
    private double toan;
    private double van;
    private double anh;

    public StudentDTO() {
    }

    public StudentDTO(String maSV, String hoTen, double toan, double van, double anh) {
        this.maSV = maSV;
        this.hoTen = hoTen;
        this.toan = toan;
        this.van = van;
        this.anh = anh;
    }

    /**
     * Validates student identity fields and ensures all scores lie within [0, 10].
     */
    public void validate() {
        if (maSV == null || maSV.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã sinh viên không được để trống.");
        }
        if (hoTen == null || hoTen.trim().isEmpty()) {
            throw new IllegalArgumentException("Họ tên sinh viên không được để trống.");
        }
        validateScore("Toán", toan);
        validateScore("Văn", van);
        validateScore("Anh", anh);
    }

    private static void validateScore(String subject, double score) {
        if (Double.isNaN(score) || Double.isInfinite(score) || score < 0.0 || score > 10.0) {
            throw new IllegalArgumentException("Điểm " + subject + " phải nằm trong khoảng từ 0 đến 10.");
        }
    }

    public String getMaSV() {
        return maSV;
    }

    public void setMaSV(String maSV) {
        this.maSV = maSV;
    }

    public String getHoTen() {
        return hoTen;
    }

    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
    }

    public double getToan() {
        return toan;
    }

    public void setToan(double toan) {
        this.toan = toan;
    }

    public double getVan() {
        return van;
    }

    public void setVan(double van) {
        this.van = van;
    }

    public double getAnh() {
        return anh;
    }

    public void setAnh(double anh) {
        this.anh = anh;
    }
}
