# Ứng dụng Quản lý Sinh viên (UDP + DES + MySQL)

Đồ án môn **Lập trình mạng**: hệ thống Quản lý Sinh viên theo mô hình **Client–Server** qua giao thức **UDP** (`DatagramSocket` / `DatagramPacket`), payload JSON (`Gson`), mã hóa đối xứng **DES** (`DES/ECB/PKCS5Padding` + `Base64`), lưu trữ **MySQL 8.0** qua JDBC và giao diện desktop **Java Swing + FlatLaf**.

## 1. Yêu cầu môi trường
- **JDK 17** trở lên
- **Docker** (tùy chọn để dựng nhanh MySQL 8.0 bằng `docker-compose.yml`) hoặc MySQL 8.0 local
- Gradle Wrapper đã tích hợp sẵn (`./gradlew` trên Linux/macOS hoặc `.\gradlew.bat` trên Windows)

## 2. Cấu trúc thư mục chính
- `src/main/java/com/studentudp/common/`: `ProtocolConstants`, `PacketMessage`, `StudentDTO`, `DbConfigDTO`, `ResultDTO`, `UdpHelper`, `UiTheme`
- `src/main/java/com/studentudp/server/`: `DesCryptoUtil`, `DesCryptoException`, `EncryptedStudentRow`, `StudentRepository`, `DatabaseManager`, `UdpServerEngine`, `ServerFrame`
- `src/main/java/com/studentudp/client/`: `ClientRequestException`, `UdpClientEngine`, `ConnectServerPanel`, `DbConfigPanel`, `StudentManagerPanel`, `ClientFrame`
- `src/test/java/com/studentudp/`: Unit test DES, DatabaseManager, DTO validation và Integration test UDP end-to-end

## 3. Khởi tạo MySQL local bằng Docker (tùy chọn)
```bash
docker compose up -d
```
Thông số mặc định trong `docker-compose.yml` và `init.sql`:
- Host: `localhost`
- Port: `3306`
- Database: `student_db`
- Username: `root`
- Password: `rootpassword`

## 4. Lệnh build, test và chạy ứng dụng
```bash
# Biên dịch và chạy toàn bộ kiểm thử tự động
./gradlew clean test

# Build toàn bộ dự án
./gradlew build

# Terminal 1: Khởi chạy Server Control Panel
./gradlew runServer

# Terminal 2: Khởi chạy Client 3 bước
./gradlew runClient
```
*(Trên Windows PowerShell dùng `.\gradlew.bat` thay cho `./gradlew`.)*

## 5. Kịch bản Demo bảo vệ đồ án
1. **Khởi động hạ tầng:** Chạy `docker compose up -d` (hoặc đảm bảo MySQL đang chạy).
2. **Mở Server (`./gradlew runServer`):**
   - Nhập Port UDP (`9876`) và bấm **Khởi động**.
   - Nhập **Khóa DES (8 ký tự)** bất kỳ (ví dụ `PTIT2026`) và bấm **Áp dụng khóa & Đọc giải mã**.
3. **Mở Client (`./gradlew runClient`) và kiểm chứng timeout 3 giây:**
   - Ở Bước 1 (*Kết nối Server*), nhập thử sai port (ví dụ `9999`) và bấm **Kết nối**. Sau đúng `3000ms`, Client báo lỗi timeout rõ ràng và giữ nguyên màn hình để nhập lại.
   - Nhập lại `127.0.0.1` và port `9876` -> bấm **Kết nối** -> gói tin `PING`/`PONG` thành công, chuyển sang Bước 2.
4. **Cấu hình MySQL động (Bước 2):**
   - Nhập thông số MySQL (`localhost`, `3306`, `student_db`, `root`, `rootpassword`) -> bấm **Kết nối CSDL**.
   - Thử nhập sai mật khẩu để kiểm chứng thông báo lỗi rõ ràng; sau đó nhập đúng để Server tạo kết nối JDBC động, tự chạy `CREATE TABLE IF NOT EXISTS sinh_vien` và chuyển Client sang Bước 3.
5. **Nhập sinh viên, kiểm chứng mã hóa DES trong MySQL & tính ĐTB (Bước 3):**
   - Nhập thử điểm ngoài `[0, 10]` (ví dụ `11`) để kiểm chứng validation phía Client.
   - Thêm sinh viên hợp lệ (ví dụ `B21DCCN001`, `Nguyễn Văn An`, Toán `8.5`, Văn `7.5`, Anh `9.0`).
   - Quan sát **Console Log trên Server** hiển thị gói tin `ADD_STUDENT` và các chuỗi **Base64 DES** thực tế được lưu xuống MySQL.
   - Kiểm tra trực tiếp trong MySQL (`SELECT * FROM sinh_vien;`) để thấy toàn bộ `ma_sv`, `ho_ten`, `toan`, `van`, `anh` đều là chuỗi mã hóa Base64.
   - Quan sát bảng trên Client và Server hiển thị đúng `Điểm TB = 8.33`.
6. **Kiểm chứng sai khóa DES không crash:**
   - Trên `ServerFrame`, đổi khóa DES sang một chuỗi 8 ký tự khác (ví dụ `WRONGKEY`) và bấm **Áp dụng khóa & Đọc giải mã**.
   - Hệ thống bắt `BadPaddingException` / `IllegalBlockSizeException`, báo lỗi giải mã rõ ràng trên UI & Console Log, không sập Server/Client.
   - Nhập lại đúng khóa `PTIT2026` và bấm **Áp dụng khóa & Đọc giải mã** để khôi phục dữ liệu bình thường.
