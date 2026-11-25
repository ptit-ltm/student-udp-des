package com.studentudp.common;

/**
 * Decrypted student record with computed average score (ĐTB).
 */
public class ResultDTO {

    private String maSV;
    private String hoTen;
    private double toan;
    private double van;
    private double anh;
    private double diemTB;

    public ResultDTO() {
    }

    public ResultDTO(String maSV, String hoTen, double toan, double van, double anh, double diemTB) {
        this.maSV = maSV;
        this.hoTen = hoTen;
        this.toan = toan;
        this.van = van;
        this.anh = anh;
        this.diemTB = diemTB;
    }

    public static ResultDTO fromScores(String maSV, String hoTen, double toan, double van, double anh) {
        double avg = (toan + van + anh) / 3.0;
        return new ResultDTO(maSV, hoTen, toan, van, anh, avg);
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

    public double getDiemTB() {
        return diemTB;
    }

    public void setDiemTB(double diemTB) {
        this.diemTB = diemTB;
    }
}
