# Library Management System — Backend

Phần Người 1: **Auth/User + Spring Security + JWT + integration test** theo tài liệu phân công.

## Phạm vi hiện tại

- Đăng ký tài khoản USER, đăng nhập bằng username/password, lấy thông tin tài khoản hiện tại.
- Hash mật khẩu BCrypt (cost 12); không trả password/hash trong JSON.
- JWT HS256 có `sub` (user ID), `iss`, `aud`, `iat`, `exp`, `jti`, `username`, `role`.
- Xác thực tập trung bằng `BearerTokenAuthenticationFilter` có sẵn của Spring Security.
- Kiểm tra chữ ký, thời hạn, issuer, audience và subject; đọc role hiện tại từ database sau khi xác thực token.
- JSON lỗi thống nhất cho validation, đăng nhập, 401, 403 và lỗi MVC.
- Swagger/OpenAPI, Docker Compose (backend + PostgreSQL), test Auth/User và quy tắc phân quyền.

Book/Borrowing là module của Người 2/3 và chưa được triển khai ở bản này. Các endpoint Book/Borrowing trong test chỉ là controller giả để kiểm tra Security, không nằm trong backend khi chạy thật. Docker được bổ sung để nhóm có thể chạy chung backend.

## Công nghệ và kiến trúc

Java 24, Spring Boot 4.1.1, Spring Data JPA/Hibernate, PostgreSQL, Spring Security, Spring OAuth2 Resource Server/Nimbus JWT, Springdoc 3.1.0, Maven. H2 chỉ dùng khi test.

```text
Controller -> Service -> UserRepository (interface)
                              |
                    UserRepositoryAdapter -> JpaUserRepository -> PostgreSQL
```

Service không import Controller/Web, JPA hoặc Spring Data. Controller chỉ nhận DTO, gọi Service và trả DTO. Giữ package gốc `com.library_management` của project hiện có để tránh đổi entry point khi ghép code.

```text
src/main/java/com/library_management/
  auth/                 Register/login DTO, controller, service, AccessTokenIssuer
  user/                 User, Role, UserRepository, DTO, controller, service
    persistence/        Spring Data repository và adapter
  security/             JWT encoder/decoder, SecurityFilterChain, lỗi 401/403
  common/exception/     Exception dùng chung và GlobalExceptionHandler
  common/response/      ApiError
  config/               Swagger/OpenAPI
```

## Database

`users`: `id BIGINT` tự tăng, `username VARCHAR(50)` unique và phân biệt hoa/thường, `password VARCHAR(100)` chứa BCrypt hash, `role VARCHAR(20)` (`USER`/`ADMIN`), `created_at` theo UTC.

Username dài 3–50 ký tự: chữ ASCII, số, `_`, `.`, `-`; không tự trim/chuyển chữ thường. Password từ 6–72 ký tự và tối đa 72 byte UTF-8 theo giới hạn BCrypt. Không tự trim password.

Đăng ký trùng trả `400`; unique constraint database bảo vệ cả trường hợp nhiều request đồng thời. Đăng ký luôn tạo `USER`, không nhận role từ client. Không tạo sẵn tài khoản ADMIN hoặc mật khẩu mặc định.

## Chạy bằng Docker

Cần Docker Engine/Docker Desktop đang chạy với Linux containers. Không cần cài Java/Maven trên máy khi dùng Docker.

Trong thư mục bàn giao đã có `.env` cục bộ với DB password và JWT secret ngẫu nhiên, nên có thể chạy ngay `docker compose up --build`. Các bước 1–2 dưới đây dành cho người clone project mới hoặc muốn tự tạo lại cấu hình; không ghi đè `.env` đang dùng nếu muốn giữ credential hiện tại.

1. Copy cấu hình mẫu:

   ```powershell
   Copy-Item .env.example .env
   ```

2. Điền `DB_PASSWORD` trong `.env`. Tạo secret ngẫu nhiên rồi điền vào `JWT_SECRET`:

   ```powershell
   $keyBytes = New-Object byte[] 32
   $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
   $rng.GetBytes($keyBytes)
   $rng.Dispose()
   [Convert]::ToBase64String($keyBytes)
   ```

   `.env` chỉ dùng cục bộ và đã được gitignore. Docker Compose đọc file này; Spring Boot chạy trực tiếp cần biến môi trường trong shell.

3. Build và chạy:

   ```powershell
   docker compose up --build
   ```

   Lần build đầu tải dependency và chạy toàn bộ test. Dockerfile dùng nhiều giai đoạn build, runtime chạy bằng user không phải root. PostgreSQL có healthcheck; backend chờ database sẵn sàng.

4. Truy cập Swagger: <http://localhost:8080/swagger-ui/index.html>. OpenAPI JSON: <http://localhost:8080/v3/api-docs>.

5. Dừng bằng `docker compose down`; dữ liệu vẫn nằm trong volume `postgres_data`. Chỉ dùng `docker compose down -v` khi chủ động muốn xóa database của môi trường này.

PostgreSQL được publish ở `127.0.0.1:5432` để có thể phát triển backend bên ngoài Docker. Đổi `DB_PORT` nếu máy đã có PostgreSQL; đổi `SERVER_PORT` nếu cổng 8080 đang dùng.

## Chạy backend trực tiếp

Cần JDK 24 và PostgreSQL. Có thể chạy riêng database bằng `docker compose up -d postgres` sau khi tạo `.env`.

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-24'
$env:DB_URL = 'jdbc:postgresql://localhost:5432/library_management'
$env:DB_USERNAME = 'library_user'
$env:DB_PASSWORD = '<password configured for PostgreSQL>'
$env:JWT_SECRET = '<random Base64 key generated above>'
.\mvnw.cmd spring-boot:run
```

Linux/macOS: đặt các biến môi trường tương tự rồi chạy `./mvnw spring-boot:run`.

| Biến | Mặc định / yêu cầu |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/library_management` khi chạy trực tiếp |
| `DB_USERNAME`, `DB_PASSWORD` | Bắt buộc |
| `JWT_SECRET` | Bắt buộc; Base64 của ít nhất 32 byte ngẫu nhiên |
| `JWT_ISSUER` | `library-management` |
| `JWT_ACCESS_TOKEN_TTL` | `PT1H` (ISO-8601 duration, phải dương) |
| `DB_DDL_AUTO` | `update` cho bài tập/phát triển |

JWT dùng audience `library-api`; bộ kiểm tra thời gian của Spring cho phép lệch đồng hồ mặc định 60 giây. Ứng dụng từ chối khởi động nếu key yếu/sai Base64 hoặc TTL không dương. `ddl-auto=update` phục vụ bài tập; nhóm nên dùng migration được quản lý và `validate` nếu triển khai lâu dài.

## API Auth/User

| Method | Endpoint | Quyền | Thành công |
|---|---|---|---|
| POST | `/api/auth/register` | Public | 201, thông tin user |
| POST | `/api/auth/login` | Public | 200, access token |
| GET | `/api/users/me` | Bearer JWT | 200, thông tin user hiện tại |

Register/login request:

```json
{"username":"quanquan","password":"secret123"}
```

Register response (201) và `/me` response (200):

```json
{"id":1,"username":"quanquan","role":"USER","createdAt":"2026-10-18T00:00:00Z"}
```

Login response:

```json
{"accessToken":"<JWT>","tokenType":"Bearer"}
```

Gọi API cần xác thực với `Authorization: Bearer <JWT>`. Trong Swagger, login trước, copy `accessToken`, bấm **Authorize** rồi dán token (không thêm chữ `Bearer`).

Lỗi ví dụ:

```json
{"status":401,"message":"Invalid username or password","timestamp":"2026-10-18T00:00:00Z"}
```

`400`: dữ liệu không hợp lệ hoặc trùng username. `401`: sai thông tin đăng nhập, thiếu/sai/hết hạn token hoặc tài khoản đã bị xóa. `403`: đã xác thực nhưng thiếu quyền. `404`: tài nguyên không tồn tại. Thông báo đăng nhập không phân biệt username không tồn tại và sai password.

## Phân quyền và phối hợp nhóm

| Endpoint | Quyền đã cấu hình |
|---|---|
| GET `/api/books`, `/api/books/{id}` | Public |
| POST `/api/books`, DELETE `/api/books/{id}` | ADMIN |
| `/api/users/me`, các API `/api/borrowings` | Đã đăng nhập |
| Các endpoint khác | Mặc định cần đăng nhập |

Tài liệu chưa chốt rõ quyền thêm/xóa sách; bản này chọn ADMIN. Nếu nhóm muốn mọi user được phép, chỉ cần sửa quy tắc trong `SecurityConfig` và test tương ứng. Tài liệu có đề cập package `com.library`; bản này giữ `com.library_management` theo source gốc.

- Người 2 đặt module `book` dưới package gốc; dùng chung `ApiError`/`GlobalExceptionHandler`. Không cần tự kiểm tra token trong Controller.
- Người 3 dùng `@AuthenticationPrincipal Jwt jwt`, lấy `Long.valueOf(jwt.getSubject())` làm user ID đã được Security kiểm tra. Việc xác nhận đúng chủ sở hữu borrowing vẫn thuộc BorrowingService.
- `UserRepository` là interface ứng dụng. `JpaUserRepository` nằm trong `user.persistence`; có thể dùng `User` để mapping quan hệ Borrowing.
- Khi cần một ADMIN để phát triển: đăng ký tài khoản trước, rồi người quản trị database chủ động chạy `UPDATE users SET role = 'ADMIN' WHERE username = 'your-admin-username';`. API public không có chức năng tự nâng quyền.
- Role được đọc lại từ database ở mỗi request, nên thay đổi quyền/xóa user có hiệu lực với token đang tồn tại.
- Hiện chỉ có access token; chưa có refresh token, logout/revocation theo từng token. Client xóa token khi logout; token tự hết hạn theo TTL.

## Kiểm thử

```powershell
.\mvnw.cmd verify
```

Test dùng profile `test`, H2 chế độ PostgreSQL và secret chỉ dùng cho test; không cần PostgreSQL/Docker hoặc cấu hình credential thật. Test đi qua MockMvc + filter Security thật + Service + JPA, phát hành và kiểm tra JWT thật.

Các trường hợp: register/login/me, hash password và không lộ password, không tự đăng ký ADMIN, validation và UTF-8/BCrypt, trùng username đồng thời, login thất bại, thiếu/sai/hết hạn token, sai issuer/audience/subject, token chưa có hiệu lực, thiếu expiration, quyền USER/ADMIN, hạ quyền/xóa tài khoản, endpoint public, Swagger và cấu hình JWT không hợp lệ.

H2 không thay thế kiểm thử PostgreSQL. Khi ghép Book/Borrowing, nhóm cần chạy integration test toàn bộ flow trên PostgreSQL và bổ sung kiểm thử transaction. Load test trên Kaggle CPU thuộc Người 3; bản này chưa có kết quả load test.

### Kết quả kiểm tra ngày 18/10/2026

- `mvn verify` trên Windows/JDK 24.0.2: **22 test đạt, 0 lỗi**; tạo executable JAR thành công.
- Docker build bằng Maven Wrapper/JDK 24 trên Linux: **22 test đạt, 0 lỗi**.
- Chạy backend với PostgreSQL 17 trong Docker Compose: database healthy, backend khởi động thành công.
- Kiểm tra HTTP thật: Swagger `200`, register `201`, trùng username `400`, login/me `200`, thiếu hoặc sai JWT `401`, USER thêm sách `403`.
- Truy vấn PostgreSQL xác nhận tài khoản được lưu với role USER và password hash BCrypt.

Cụm Docker dùng để kiểm thử có tên riêng `library-auth-verify` và được dọn sau kiểm thử; chạy Compose trong thư mục project để tạo môi trường của bạn.

## Tài liệu tham khảo

- [Phân chia công việc của nhóm](https://docs.google.com/document/d/1DgAvFM45hKJUmA2agaz4tnK0YcJYpqC34rdh_GescjI/edit)
- [Spring Security JWT Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [Springdoc OpenAPI](https://springdoc.org/)
