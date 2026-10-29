# HỆ THỐNG QUẢN LÝ MƯỢN/TRẢ SÁCH THƯ VIỆN

## Phase 1 — Baseline

### 1. Mô tả bài toán

#### 1.1. Bối cảnh

Trong hoạt động quản lý thư viện, việc theo dõi thông tin sách, số lượng sách còn khả dụng và các phiếu mượn/trả là những nghiệp vụ cơ bản. Nếu quản lý thiếu nhất quán, thư viện có thể gặp khó khăn trong việc kiểm soát số lượng sách, xác định ai đang mượn sách và theo dõi trạng thái trả sách.

#### 1.2. Mục tiêu

- Quản lý thông tin sách và số lượng sách khả dụng.
- Hỗ trợ đăng ký, đăng nhập và xác thực người dùng.
- Hỗ trợ tạo phiếu mượn, xem các phiếu mượn của bản thân và trả sách.
- Kiểm soát các quy tắc nghiệp vụ, hạn chế thao tác không hợp lệ.
- Đảm bảo dữ liệu phiếu mượn và số lượng sách được cập nhật nhất quán.

### 2. Mô tả tác nhân và nghiệp vụ

#### 2.1. Tác nhân

| Tác nhân | Vai trò |
|---|---|
| User | Đăng ký, đăng nhập, xem sách, mượn sách, xem phiếu mượn và trả sách của mình |
| Hệ thống | Xác thực, kiểm tra quy tắc nghiệp vụ và cập nhật dữ liệu |

#### 2.2. Các nghiệp vụ chính

**Nghiệp vụ 1 — Đăng ký và đăng nhập**

Người dùng đăng ký bằng username và password. Khi đăng nhập thành công, hệ thống cấp access token JWT để truy cập các API được bảo vệ.

**Nghiệp vụ 2 — Quản lý và tra cứu sách**

Người dùng xem danh sách hoặc chi tiết sách.

**Nghiệp vụ 3 — Mượn sách**

Hệ thống kiểm tra sách tồn tại, số lượng còn khả dụng và giới hạn mượn của người dùng. Nếu hợp lệ, hệ thống tạo phiếu mượn và giảm số lượng sách khả dụng đi một đơn vị.

**Nghiệp vụ 4 — Trả sách**

Hệ thống kiểm tra phiếu mượn tồn tại, thuộc về người dùng hiện tại và chưa được trả. Sau đó cập nhật trạng thái, thời điểm trả và tăng số lượng sách khả dụng.

### 3. Đặc tả API

| Method | Endpoint | Quyền truy cập | Mục đích |
|---|---|---|---|
| POST | `/api/auth/register` | Công khai | Đăng ký tài khoản |
| POST | `/api/auth/login` | Công khai | Đăng nhập |
| GET | `/api/users/me` | User đã đăng nhập | Lấy thông tin tài khoản hiện tại |
| GET | `/api/books` | Công khai | Lấy danh sách sách |
| GET | `/api/books/{id}` | Công khai | Lấy chi tiết sách |
| POST | `/api/borrowings` | User đã đăng nhập | Mượn sách |
| GET | `/api/borrowings/me` | User đã đăng nhập | Xem các phiếu mượn của mình |
| POST | `/api/borrowings/{id}/return` | User đã đăng nhập | Trả sách của mình |

### 4. Database
<img width="502" height="635" alt="image" src="https://github.com/user-attachments/assets/d0fe7b9b-4a4c-42c4-814c-f91a22845599" />


### 5. OpenAPI Specification

Tài liệu đặc tả API được lưu tại [`openapi.json`](./openapi.json).

Có thể xem tài liệu tương tác thông qua Swagger UI khi ứng dụng đang chạy tại:

`http://localhost:8080/swagger-ui/index.html`
