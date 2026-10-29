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

1. **Tạo tài khoản và đăng nhập.** Người dùng đăng ký bằng `username` và mật khẩu. Username phải duy nhất; mật khẩu được băm bằng BCrypt trước khi lưu. Đăng nhập thành công trả về access token JWT để gọi các API được bảo vệ. API `/api/users/me` cho phép xem thông tin tài khoản hiện tại, chưa hỗ trợ sửa hồ sơ.
2. **Tra cứu và quản lý sách.** Mỗi đầu sách có thông tin tên, tác giả, ISBN, thể loại, tổng số bản (`quantity`) và số bản còn cho mượn (`availableQuantity`). Danh sách và chi tiết sách được xem công khai. Thêm hoặc xóa sách yêu cầu đăng nhập; phiên bản hiện tại chưa giới hạn hai thao tác này cho vai trò ADMIN.
3. **Mượn một bản sách.** Client gửi `bookId`; backend xác định người mượn từ tài khoản đã xác thực, không nhận user ID do client tự chọn. Hệ thống kiểm tra sách tồn tại, còn bản khả dụng và tài khoản đang có dưới **5 phiếu mượn hoạt động**. Khi hợp lệ, backend tạo phiếu `BORROWING`, ghi thời điểm mượn, hạn trả và giảm `availableQuantity` đi 1. Mỗi request thành công tương ứng một phiếu mượn cho một bản sách; hạn trả mặc định là **14 ngày**, cấu hình bằng `BORROWING_LOAN_DAYS`.
4. **Theo dõi việc mượn.** API `/api/borrowings/me` trả các phiếu của người đang đăng nhập, gồm ID phiếu, ID sách, trạng thái, ngày mượn và hạn trả. Danh sách gồm cả phiếu đang mượn lẫn phiếu đã trả; chỉ phiếu `BORROWING` được tính vào giới hạn 5 phiếu.
5. **Trả sách bằng ID phiếu mượn.** Client gửi ID phiếu trên đường dẫn API trả sách, không dùng ID sách thay thế. Backend kiểm tra phiếu tồn tại, thuộc tài khoản hiện tại và vẫn ở trạng thái `BORROWING`. Trả thành công chuyển trạng thái sang `RETURNED`, lưu thời điểm trả và tăng `availableQuantity` lên 1. Trả lại cùng một phiếu bị từ chối để tránh tăng sai tồn kho.

#### 3.2. Kiểm soát dữ liệu và yêu cầu đồng thời

- **Số lượng sách:** khi tạo sách, tổng số bản và số bản khả dụng phải không âm; `availableQuantity` không được lớn hơn `quantity`. ISBN có ràng buộc duy nhất trong database.
- **Quyền sở hữu:** người dùng chỉ xem danh sách phiếu của mình và trả phiếu thuộc tài khoản của mình. Biết ID phiếu của người khác không đồng nghĩa có quyền trả phiếu đó.
- **Transaction:** tạo/cập nhật phiếu và thay đổi số lượng khả dụng được thực hiện trong cùng transaction. Nếu thao tác thất bại và transaction bị rollback, các thay đổi dữ liệu đi cùng cũng bị hủy.
- **Khóa bản ghi:** luồng mượn khóa tài khoản trước rồi khóa sách. Luồng trả khóa tài khoản, phiếu mượn và sách. Khóa ghi bi quan giúp các request liên quan cùng tài khoản hoặc cùng sách lần lượt kiểm tra dữ liệu hiện hành, nhằm giữ giới hạn mượn và cập nhật đúng số lượng khả dụng.
- **Phản hồi lỗi:** thiếu hoặc sai token trả `401`; trả phiếu của người khác trả `403`; sách/phiếu không tồn tại trả `404`; lỗi validation request, hết sách, vượt giới hạn hoặc trả trùng trả `400`. Client cần đọc thông báo lỗi để hiển thị lý do cho người dùng.

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

| Vấn đề chính | Ảnh hưởng trong dự án hiện tại | Hướng xử lý |
|---|---|---|
| **Quyền quản lý sách chưa tách khỏi người mượn.** | Mọi tài khoản đã đăng nhập đều có thể thêm hoặc xóa sách. Nếu hệ thống có vai trò thủ thư/quản trị, chính sách hiện tại chưa giới hạn các thao tác thay đổi kho sách cho đúng người phụ trách. | Thống nhất vai trò nghiệp vụ với nhóm tiếp nhận; bổ sung phân quyền và kiểm thử quyền truy cập cho các thao tác quản lý sách. |
| **Chưa có quy tắc xóa sách đã phát sinh phiếu mượn.** | API xóa trực tiếp sau khi kiểm tra sách tồn tại. Sách đã được mượn có thể bị database từ chối xóa do khóa ngoại, nhưng backend chưa phản hồi bằng lỗi nghiệp vụ rõ ràng. Việc xử lý xóa phải bảo toàn các phiếu và lịch sử mượn/trả. | Chọn chính sách ngừng lưu hành/xóa mềm hoặc từ chối xóa sách có lịch sử; kiểm thử cả trường hợp sách đang được mượn và đã được trả. |
| **Gửi lại yêu cầu mượn có thể phát sinh phiếu ngoài ý muốn.** | Khi backend đã lưu phiếu nhưng client mất phản hồi, gửi lại cùng yêu cầu có thể tạo thêm phiếu và giảm tiếp số lượng khả dụng nếu vẫn còn sách và chưa vượt giới hạn. Transaction và khóa bản ghi chưa giải quyết việc nhận diện hai request là cùng một thao tác. | Bổ sung cơ chế nhận diện yêu cầu gửi lại, chẳng hạn idempotency key, để trả kết quả đã xử lý thay vì tạo phiếu mới. |
| **Khả năng đáp ứng khi dữ liệu và lượng truy cập tăng cần được đánh giá.** | API danh sách đang lấy toàn bộ sách hoặc phiếu của người dùng. Đồng thời, các yêu cầu mượn/trả cùng sách hoặc tài khoản có thể phải chờ khóa. Đây là các điểm có thể làm tăng thời gian phản hồi khi thư viện lớn hơn, chưa phải kết luận từ benchmark. | Bổ sung phân trang; đo truy vấn, thời gian chờ khóa và tải trên các đầu sách được mượn nhiều. Dựa vào kết quả đo để tối ưu, đồng thời giữ các quy tắc về tồn kho và giới hạn mượn. |

Trước khi bàn giao, cần chốt nhánh hoặc commit dùng làm phiên bản Phase 1, chạy lại bộ kiểm thử trên đúng phiên bản đó và cập nhật `openapi.json` theo API thực tế.
