# Đồ án Bảo mật Cơ sở dữ liệu - Hệ thống Theo dõi Giao hàng (Nhóm 12)

Hệ thống quản lý giao hàng tích hợp các giải pháp bảo mật đa lớp (Defense in Depth) giữa Ứng dụng (Java) và Cơ sở dữ liệu (Oracle).

## 🚀 Công nghệ sử dụng

* **Backend:** Java 21, Spring Boot 3.2.5, Spring Security.
* **Frontend:** Thymeleaf, Bootstrap (SB Admin 2), Animate.css.
* **Database:** Oracle Database (19c/21c), H2 Database (Chế độ Setup).
* **Security:** AES-256, RSA 1024-bit, Hybrid Encryption (AES + RSA).

---

## ⚙️ Hướng dẫn Cài đặt & Chạy hệ thống

### Bước 1: Chuẩn bị Cơ sở dữ liệu Oracle
Trước khi chạy ứng dụng, bạn cần thiết lập môi trường Database:

1.  **Nạp thư viện mã hóa Java vào Oracle:**
    * Giải nén file `crypto4ora.zip`.
    * Mở CMD tại thư mục giải nén, chạy lệnh:
        ```bash
        loadjava -u sys/password@orcl -r -v -f -genmissing crypto4ora.jar
        ```
2.  **Chạy Script SQL:**
    * Mở SQL Developer, kết nối với quyền Admin.
    * Chạy file **`crypto4ora.sql`** để tạo Package `CRYPTO`.
    * Chạy file **`CSDL_NHOM12.sql`** để tạo Bảng, Trigger và Dữ liệu mẫu.

### Bước 2: Cấu hình Kết nối (Lần đầu tiên)
Hệ thống hỗ trợ kết nối động, không cần sửa code.

1.  Chạy ứng dụng: `mvn spring-boot:run`
2.  Truy cập trình duyệt: `http://localhost:8080`
3.  Hệ thống sẽ tự chuyển hướng sang trang **Cấu hình Kết nối**.
4.  Nhập thông tin Oracle của bạn (Host, Port, Service Name, User, Password).
5.  Nhấn **Lưu**. Sau đó **Khởi động lại ứng dụng** để áp dụng.

---

## 📖 Hướng dẫn Sử dụng Chức năng

### 1. Đăng ký Tài khoản (Sinh khóa RSA)
* **Lưu ý:** Phải tạo tài khoản qua giao diện Web để hệ thống tự động sinh cặp khóa RSA (Public/Private).
* **Link Đăng ký Khách hàng:** `http://localhost:8080/register`
* **Link Đăng ký Nhân viên (Admin/Shipper):** `http://localhost:8080/register-admin`

### 2. Chức năng Giao hàng (Ký số & Xác thực)
* **Khách hàng:**
    * Đăng nhập -> Chọn **"Tạo đơn hàng"**.
    * Điền thông tin và nhấn **"Xác nhận & Ký số"**.
    * *Cơ chế:* App dùng Private Key ký vào đơn hàng. DB Trigger kiểm tra chữ ký trước khi lưu.
* **Shipper:**
    * Đăng nhập -> Chọn **"Đơn hàng cần xử lý"**.
    * Bấm **"Cập nhật"** -> Chọn trạng thái (VD: Đã lấy, Đang giao).
    * *Cơ chế:* App ký vào trạng thái mới. DB Trigger xác thực toàn vẹn dữ liệu.

### 3. Chức năng Bảo mật cao (Mã hóa Lai)
Hệ thống tự động dùng cơ chế Mã hóa Lai (AES động + RSA) cho các dữ liệu lớn sau:

* **Hộp thư Mật (Quản lý & Shipper):**
    * Truy cập menu **"Hộp thư Mật"** trên thanh bên trái.
    * Gửi tin nhắn chỉ đạo/mật khẩu. Chỉ người nhận đúng mới giải mã được.
* **Báo cáo Sự cố (Shipper):**
    * Khi cập nhật đơn hàng, nếu chọn trạng thái **"Giao thất bại"** và nhập ghi chú dài (>10 ký tự).
    * Hệ thống tự động mã hóa nội dung này gửi về cho Admin.
* **Khai báo Hàng giá trị cao (Khách hàng):**
    * Khi tạo đơn, nhập thông tin vào ô **"Mô tả hàng hóa (Bảo mật)"**.
    * Nội dung này Shipper không đọc được, chỉ Admin giải mã được khi cần đối soát.

### 4. Chức năng Quản trị & Nhật ký
* Đăng nhập tài khoản **Quản lý**.
* Vào menu **"Nhật ký Vận hành"**.
* Hệ thống sẽ tự động giải mã các log hành động (vốn được DB mã hóa ngầm) để hiển thị tiếng Việt.

### 5. Tiện ích khác
* **Quên mật khẩu:** Tại màn hình đăng nhập, chọn "Quên mật khẩu". Nhập username và xem mã OTP tại **Console Log** của ứng dụng.
* **Tra cứu:** Truy cập `http://localhost:8080/tra-cuu` để xem hành trình đơn hàng công khai.

---

## 📂 Cấu trúc Thư mục chính
* `src/main/java/.../config`: Cấu hình Security và DB động.
* `src/main/java/.../controller`: Điều hướng yêu cầu.
* `src/main/java/.../service`: Xử lý nghiệp vụ và Mã hóa (`HybridEncryptionService`).
* `src/main/java/.../utils`: Các lớp tiện ích mã hóa (`RSAUtil`, `EncryptionUtil`).
* `src/main/resources/templates`: Giao diện người dùng.