# CONTEXT.md - Student Management System (UDP + DES + MySQL)

## 1. Tổng quan bài toán
Ứng dụng desktop Quản lý Sinh viên theo mô hình **Client–Server** sử dụng:
- **Ngôn ngữ & Nền tảng:** Java 17+, Gradle, Java Swing + FlatLaf (`FlatLightLaf`).
- **Giao thức mạng:** UDP (`DatagramSocket` / `DatagramPacket`), payload JSON (`Gson`).
- **Bảo mật dữ liệu:** Mã hóa đối xứng **DES** (`DES/ECB/PKCS5Padding`) kết hợp **Base64** trước khi lưu xuống cơ sở dữ liệu.
- **Lưu trữ:** MySQL 8.0 qua JDBC với chuỗi kết nối động do Client cấu hình.

## 2. Kiến trúc phân tầng & Package
- `com.studentudp.common`:
  - `ProtocolConstants`: Hằng số loại thông điệp, trạng thái, timeout (`3000ms`), kích thước buffer UDP.
  - `PacketMessage`: Bao gói thông điệp JSON thống nhất trên UDP.
  - `StudentDTO`, `DbConfigDTO`, `ResultDTO`: Các đối tượng truyền dữ liệu (DTO) kèm kiểm tra hợp lệ đầu vào.
  - `UdpHelper`: Đóng gói/giải gói `DatagramPacket` (UTF-8 JSON) và chuyển đổi `Gson`.
  - `UiTheme`: Cấu hình giao diện `FlatLightLaf`, bảng màu, font chữ và component helpers dùng chung cho cả Server và Client (giữ tầng `server` độc lập với tầng `client`).
- `com.studentudp.server`:
  - `DesCryptoUtil` & `DesCryptoException`: Mã hóa/giải mã `DES/ECB/PKCS5Padding` + `Base64`, không hardcode khóa.
  - `StudentRepository` & `EncryptedStudentRow`: Abstraction lưu trữ dữ liệu đã mã hóa, hỗ trợ test tích hợp UDP độc lập.
  - `DatabaseManager`: Cài đặt `StudentRepository` bằng JDBC MySQL, tự động tạo database và bảng `sinh_vien`.
  - `UdpServerEngine`: Luồng nền lắng nghe UDP, xử lý giao thức, mã hóa/giải mã và phát sự kiện về UI.
  - `ServerFrame`: Giao diện điều khiển Server (Port, Start/Stop, Khóa DES 8 ký tự, trạng thái MySQL, bảng dữ liệu giải mã, console log).
- `com.studentudp.client`:
  - `ClientRequestException`, `UdpClientEngine`: Gửi/nhận gói tin UDP với `setSoTimeout(3000)`.
  - `ConnectServerPanel`, `DbConfigPanel`, `StudentManagerPanel`, `ClientFrame`: Giao diện Client `CardLayout` 3 bước.

## 3. Luồng dữ liệu Client – Server
1. **Bước 1 (`PING` / `PONG`):** Client gửi `PING` tới IP/Port của Server (timeout 3000ms). Thành công mới chuyển sang bước 2.
2. **Bước 2 (`DB_CONFIG`):** Client gửi `{host, port, dbName, username, password}`. Server tạo kết nối JDBC động tới MySQL và tự chạy `CREATE TABLE IF NOT EXISTS sinh_vien`. Thành công mới chuyển sang bước 3.
3. **Bước 3 (`ADD_STUDENT` / `GET_RESULTS`):**
   - Client gửi `ADD_STUDENT` với `{maSV, hoTen, toan, van, anh}`.
   - Server kiểm tra khóa DES đã nhập trên UI Server -> mã hóa 5 trường bằng DES + Base64 -> `INSERT` vào bảng `sinh_vien` -> đọc toàn bộ bản ghi từ MySQL -> giải mã -> tính `diemTB = (toan + van + anh) / 3.0` -> trả danh sách `ResultDTO` về Client.
   - Server ghi log gói tin đến và chuỗi Base64 thực tế lưu xuống MySQL lên Console Log.
   - Khi người vận hành đổi khóa DES trên Server và bấm **"Áp dụng khóa & Đọc giải mã"**, Server đọc lại MySQL và giải mã bằng khóa mới; nếu sai khóa, `BadPaddingException` / `IllegalBlockSizeException` được bắt an toàn và báo rõ trên UI.

## 4. Đặc tả giao thức JSON trên UDP
Mọi gói tin tuân theo cấu trúc:
```json
{
  "type": "PING | PONG | DB_CONFIG | ADD_STUDENT | GET_RESULTS | ERROR",
  "status": "OK | ERROR",
  "message": "Mô tả trạng thái hoặc lỗi",
  "payload": {}
}
```

| `type` | Chiều | `payload` | Mô tả |
|---|---|---|---|
| `PING` | Client -> Server | `null` | Kiểm tra kết nối Server (`soTimeout = 3000ms`) |
| `PONG` | Server -> Client | `null` | Phản hồi Server sẵn sàng |
| `DB_CONFIG` | Client -> Server | `{host, port, dbName, username, password}` | Server kết nối JDBC tới MySQL và tạo bảng |
| `ADD_STUDENT` | Client -> Server | `{maSV, hoTen, toan, van, anh}` | Lưu sinh viên mã hóa DES và trả danh sách kết quả |
| `GET_RESULTS` | Client -> Server | `null` | Lấy danh sách sinh viên đã giải mã và tính ĐTB |

## 5. Quy ước mã hóa DES
- Thuật toán: `DES/ECB/PKCS5Padding` (`javax.crypto.Cipher` + `SecretKeyFactory` / `DESKeySpec`).
- Khóa DES: Đúng **8 ký tự / 8 bytes UTF-8**, nhập trực tiếp trên `ServerFrame`, tuyệt đối không hardcode trong mã nguồn nghiệp vụ.
- Mã hóa đầu ra: `Base64` (`java.util.Base64`) để lưu an toàn trong cột `VARCHAR(255)` trên MySQL.
- Trường được mã hóa: `ma_sv`, `ho_ten`, `toan`, `van`, `anh`.

## 6. Schema MySQL
Connection string động:
```text
jdbc:mysql://<host>:<port>/<dbName>?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8
```

Bảng `sinh_vien`:
```sql
CREATE TABLE IF NOT EXISTS sinh_vien (
    id INT AUTO_INCREMENT PRIMARY KEY,
    ma_sv VARCHAR(255) NOT NULL,
    ho_ten VARCHAR(255) NOT NULL,
    toan VARCHAR(255) NOT NULL,
    van VARCHAR(255) NOT NULL,
    anh VARCHAR(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```
