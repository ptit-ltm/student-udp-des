# AGENTS.md - Engineering Rules & Conventions

## 1. Build, Test & Run Commands
Luôn sử dụng Gradle Wrapper tại thư mục gốc (`./gradlew` trên Linux/macOS hoặc `.\gradlew.bat` trên Windows):
- Biên dịch: `./gradlew compileJava`
- Chạy kiểm thử: `./gradlew clean test`
- Build toàn bộ dự án: `./gradlew build`
- Chạy Server GUI: `./gradlew runServer`
- Chạy Client GUI: `./gradlew runClient`
- Dựng MySQL local (tùy chọn): `docker compose up -d`

## 2. Kiến trúc & Quy tắc phân tầng
- Không trộn logic mạng (`DatagramSocket`), mã hóa (`DesCryptoUtil`), JDBC (`DatabaseManager`) vào trong các class Swing (`*Frame`, `*Panel`).
- Tầng `common` chứa DTO, hằng số giao thức, helper UDP và `UiTheme`. Không tạo phụ thuộc chéo từ `server` sang `client`.

## 3. Quy tắc luồng Swing EDT (Event Dispatch Thread)
- Mọi thao tác UI (`JFrame`, `JPanel`, `JTable`, cập nhật label/log) phải chạy trên EDT (`SwingUtilities.invokeLater`).
- Mọi thao tác I/O mạng UDP và JDBC tuyệt đối không chạy trên EDT:
  - Client dùng `SwingWorker` cho các request `PING`, `DB_CONFIG`, `ADD_STUDENT`, `GET_RESULTS`.
  - Server chạy vòng lặp `DatagramSocket.receive()` trên worker thread riêng và đẩy cập nhật UI qua `SwingUtilities.invokeLater`.

## 4. Chuẩn xử lý ngoại lệ & Bảo mật
- **Không hardcode khóa DES hoặc cấu hình MySQL** trong logic nghiệp vụ.
- Kiểm tra khóa DES đúng 8 bytes UTF-8 trước khi tạo `DESKeySpec`.
- Bắt rõ `BadPaddingException`, `IllegalBlockSizeException` và lỗi Base64 khi giải mã DES (trường hợp sai khóa); trả lỗi rõ ràng lên UI và log, tuyệt đối không làm sập Server hay Client.
- Client phải đặt `DatagramSocket.setSoTimeout(3000)` cho mọi phiên gửi/nhận để tránh treo giao diện.

## 5. Chuẩn UI/UX (Anti-AI-Slop)
- Dùng `FlatLightLaf` (`com.formdev:flatlaf`).
- **Cấm tuyệt đối emoji hoặc icon màu mè** trong tiêu đề, nút bấm, nhãn trạng thái, log.
- **Không dùng gradient lòe loẹt hoặc dark-mode neon.**
- Giữ bảng màu tiết chế: nền `#F7F8FA`, card `#FFFFFF`, viền `1px` `#E2E8F0`, chữ `#1E293B`, màu nhấn chính `#2563EB`, trạng thái xanh lá/đỏ trầm (`#16A34A` / `#DC2626`).
- Chiều cao ô nhập và nút bấm đồng nhất (`32–34px`), padding `12–16px`, console log của Server dùng font monospace (`Consolas` / `Monospaced`).
