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

### 3. Luồng sử dụng và quy tắc nghiệp vụ

Một lượt sử dụng điển hình là xem sách, đăng nhập, chọn sách để mượn, theo dõi phiếu mượn và trả sách. Người dùng chưa có tài khoản cần đăng ký trước; việc xem danh sách và chi tiết sách không yêu cầu đăng nhập.

#### 3.1. Từ tài khoản đến phiếu mượn

1. **Tạo tài khoản và đăng nhập.** Người dùng đăng ký bằng `username` và mật khẩu. Username phải duy nhất; Đăng nhập thành công trả về access token JWT để gọi các API được bảo vệ. API `/api/users/me` cho phép xem thông tin tài khoản hiện tại.
2. **Tra cứu và quản lý sách.** Mỗi đầu sách có thông tin tên, tác giả, thể loại, tổng số bản (`quantity`) và số bản còn cho mượn (`availableQuantity`). Danh sách và chi tiết sách được xem công khai. Thêm hoặc xóa sách yêu cầu đăng nhập.
3. **Mượn một bản sách.** Client gửi `bookId`; backend xác định người mượn từ tài khoản đã xác thực. Hệ thống kiểm tra sách tồn tại, còn bản khả dụng và tài khoản đang có dưới **5 phiếu mượn hoạt động**. Khi hợp lệ, backend tạo phiếu `BORROWING`, ghi thời điểm mượn, hạn trả và cập nhật `availableQuantity`. Mỗi request thành công tương ứng một phiếu mượn cho một bản sách; hạn trả mặc định là **14 ngày**, cấu hình bằng `BORROWING_LOAN_DAYS`.
4. **Theo dõi việc mượn.** API `/api/borrowings/me` trả các phiếu của người đang đăng nhập, gồm ID phiếu, ID sách, trạng thái, ngày mượn và hạn trả. Danh sách gồm cả phiếu đang mượn lẫn phiếu đã trả; chỉ phiếu `BORROWING` được tính vào giới hạn 5 phiếu.
5. **Trả sách bằng ID phiếu mượn.** Client gửi ID phiếu cho API trả sách, không dùng ID sách thay thế. Backend kiểm tra phiếu tồn tại của tài khoản hiện tại và vẫn ở trạng thái `BORROWING`. Trả thành công chuyển trạng thái sang `RETURNED`, lưu thời điểm trả và cập nhật `availableQuantity`. Trả lại cùng một phiếu bị từ chối để tránh sai sót.

#### 3.2. Kiểm soát dữ liệu

- **Số lượng sách:** khi tạo sách, tổng số bản và số bản khả dụng phải không âm; `availableQuantity` không được lớn hơn `quantity`.
- **Quyền sở hữu:** người dùng chỉ xem danh sách phiếu của mình và trả phiếu thuộc tài khoản của mình.
- **Transaction:** tạo/cập nhật phiếu và thay đổi số lượng khả dụng được thực hiện trong cùng transaction. Nếu thao tác thất bại và transaction bị rollback, các thay đổi dữ liệu đi cùng cũng bị hủy.
- **Khóa bản ghi:** luồng mượn khóa tài khoản trước rồi khóa sách. Luồng trả khóa tài khoản, phiếu mượn và sách. Các request liên quan cùng tài khoản hoặc cùng sách lần lượt được kiểm tra dữ liệu hiện hành, nhằm giữ giới hạn mượn và cập nhật đúng số lượng khả dụng.
- **Phản hồi lỗi:** thiếu hoặc sai token trả `401`; trả phiếu của người khác trả `403`; sách/phiếu không tồn tại trả `404`; lỗi validation request, hết sách, vượt giới hạn hoặc trả trùng trả `400`. Client đọc thông báo lỗi để hiển thị lý do cho người dùng.

### 4. Đặc tả API

| Method | Endpoint | Quyền truy cập | Mục đích |
|---|---|---|---|
| POST | `/api/auth/register` | Công khai | Đăng ký tài khoản |
| POST | `/api/auth/login` | Công khai | Đăng nhập |
| GET | `/api/users/me` | User đã đăng nhập | Lấy thông tin tài khoản hiện tại |
| GET | `/api/books` | Công khai | Lấy danh sách sách |
| GET | `/api/books/{id}` | Công khai | Lấy chi tiết sách |
| POST | `/api/books` | User đã đăng nhập | Thêm sách |
| DELETE | `/api/books/{id}` | User đã đăng nhập | Xóa sách |
| POST | `/api/borrowings` | User đã đăng nhập | Mượn sách |
| GET | `/api/borrowings/me` | User đã đăng nhập | Xem các phiếu mượn của mình |
| POST | `/api/borrowings/{id}/return` | User đã đăng nhập | Trả sách của mình |

### 5. OpenAPI Specification

Tài liệu đặc tả API được lưu tại [`openapi.json`](./openapi.json).

Có thể xem tài liệu tương tác thông qua Swagger UI khi ứng dụng đang chạy tại:

`http://localhost:8080/swagger-ui/index.html`

### 6. Những điểm cần hoàn thiện ở Phase 2

Phase 2 nên ưu tiên các vấn đề ảnh hưởng trực tiếp đến quyền quản lý sách, tính đúng đắn của dữ liệu mượn/trả và khả năng phục vụ khi quy mô sử dụng tăng.

## Các vấn đề và hướng cải tiến ở Phase 2

| Vấn đề tại Phase 1 | Cải tiến tại Phase 2 |
|---|---|
| **1. Xóa sách đã có lịch sử mượn:** Việc xóa sách đã có phiếu mượn có thể gây lỗi hoặc ảnh hưởng đến lịch sử mượn/trả. | Bổ sung cơ chế ngừng lưu hành sách hoặc không cho xóa sách đã có lịch sử mượn để bảo toàn dữ liệu. |
| **2. Gửi yêu cầu mượn sách nhiều lần:** Khi không nhận được phản hồi, người dùng có thể gửi lại yêu cầu và vô tình tạo nhiều phiếu mượn cho cùng một thao tác. | Bổ sung cơ chế nhận diện yêu cầu trùng lặp để tránh tạo nhiều phiếu mượn và giảm số lượng sách nhiều lần. |
| **3. Hiệu năng khi dữ liệu tăng:** Khi số lượng sách, phiếu mượn và người dùng tăng, việc tải toàn bộ dữ liệu cùng lúc có thể khiến hệ thống phản hồi chậm. | Bổ sung phân trang, kiểm tra và tối ưu truy vấn cơ sở dữ liệu, đồng thời kiểm thử hiệu năng để đánh giá khả năng đáp ứng của hệ thống. |
| **4. Người dùng không đến nhận sách:** Người dùng tạo phiếu mượn nhưng không đến thư viện nhận sách có thể khiến số lượng sách khả dụng bị giảm dù việc mượn chưa thực sự diễn ra. | Bổ sung trạng thái `PENDING_PICKUP` (chờ nhận sách) và cơ chế tự động hủy phiếu khi quá thời hạn nhận. Khi hủy, hệ thống cập nhật lại số lượng sách khả dụng. |
