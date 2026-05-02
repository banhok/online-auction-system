ONLINE AUCTION SYSTEM
Hệ thống đấu giá trực tuyến - Bài tập lớn môn Lập trình nâng cao

1. Công nghệ sử dụng
 Thành phần    | Công nghệ          
 --------------|------------------------
 Ngôn ngữ      | Java 17   
 Giao diện     | JavaFX 17.0.2 + FXML 
 Database      | SQLite 3.45.1 
 Serialization | Gson 2.10.1 
 Giao tiếp     | Socket (TCP) 
 Unit Test     | JUnit 5.10.2 
 Build tool    | Maven 3+ (multi-module) 
 CI/CD         | GitHub Actions 
 AI            | Gemini, ChatGPT, Claude

2. Tính năng
**Cơ bản:**
- Đăng ký / đăng nhập với 3 vai trò: Bidder, Seller, Admin
- Seller đăng sản phẩm (Electronics, Art, Vehicle) và tạo phiên đấu giá
- Bidder đặt giá, theo dõi phiên theo thời gian thực
- Nạp, rút, đặt cọc, thanh toán
- Tự động đóng phiên khi hết giờ, xác định người thắng
- Quản lý ví tiền: nạp, rút, đặt cọc, hoàn cọc
- Admin quản lý người dùng và phiên đấu giá

**Nâng cao:**
- Realtime update: bid mới được push ngay đến tất cả client đang xem, không polling
- Concurrent bidding: xử lý an toàn nhiều người đặt giá cùng lúc (ReentrantLock per-auction)
- Anti-sniping: tự động gia hạn phiên nếu có bid trong vòng X giây cuối
- Bid History Chart: biểu đồ đường giá cập nhật realtime trong màn hình đấu giá

4. Kiến trúc hệ thống
- auction-common: thư viện dùng chung: OOP hierarchy, protocol socket, exception
- auction-server: xử lý nghiệp vụ, quản lý database, broadcast event realtime
- auction-client: giao diện JavaFX, kết nối server qua socket

5. Design Patterns
- Singleton: Đảm bảo chỉ có một instance kết nối DB / socket / navigation manager. Thread-safe với double-checked locking
- Factory Method: Tạo đối tượng Item phù hợp theo ItemCategory mà không cần biết class cụ thể tại nơi gọi
- Observer: Khi có bid mới hoặc phiên kết thúc, AuctionEventManager broadcast tới tất cả ClientHandler đang subscribe — không polling
- Strategy: AuctionService chọn chiến lược kết thúc phiên: bình thường hoặc gia hạn anti-sniping
- Command: Mỗi request được đóng gói thành một CommandType; CommandRouter điều hướng đến đúng controller mà không cần if-else dài

6. Yêu cầu hệ thống
- Java 17+
- Maven 3.8+

7. Cài đặt và chạy

8. Phân công công việc
### Minh — Backend Developer
- Thiết kế schema database và toàn bộ tầng truy cập dữ liệu (DAO)
- Xây dựng business logic: đăng nhập, quản lý sản phẩm, ví tiền, lịch sử giao dịch,...
- Xử lý đấu giá đồng thời an toàn với ReentrantLock per-auction (tránh race condition, lost update)
- Triển khai các Design Pattern: Factory (tạo Item), Observer (realtime push), Strategy (chiến lược kết thúc phiên)
- Lập lịch tự động đóng phiên đấu giá khi hết giờ
- Viết toàn bộ Unit Test (Nam hỗ trợ)

### Quang — Frontend Developer
- Thiết kế và xây dựng toàn bộ giao diện người dùng cân đối, đẹp mắt, các tiện ích liên quan với JavaFX + FXML
- Xử lý luồng nghiệp vụ phía client: đăng nhập, đăng ký, duyệt phiên, đặt giá, quản lý sản phẩm,...
- Hiển thị biểu đồ giá realtime (LineChart) và đồng hồ đếm ngược trên màn hình đấu giá
- Kết nối và lắng nghe push event từ server qua socket để cập nhật UI tức thì
- Xử lý lỗi và thông báo người dùng (giá không hợp lệ, mất kết nối, phiên đã đóng,...)

### Nam — Architect / DevOps
- Thiết kế toàn bộ OOP hierarchy và định nghĩa các lớp dùng chung (model, DTO, enum, exception)
- Xây dựng protocol giao tiếp client–server qua socket (Request/Response + CommandType)
- Triển khai tầng network server: tiếp nhận kết nối, phân luồng, điều phối lệnh đến đúng controller
- Cấu hình Maven multi-module, quản lý dependency và build toàn dự án
- Quản lý dự án trên Github: luồng làm việc, pull request,...
- Thiết lập CI/CD với GitHub Actions: tự động build và chạy test khi push/PR
- Viết tài liệu: README, class diagram, Git convention, coding style guide
