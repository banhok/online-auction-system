1.online-auction-system

Bài tập lớn Lập trình nâng cao — Hệ thống đấu giá trực tuyến



2. Giới thiệu

Hệ thống cho phép nhiều người dùng cùng tham gia đấu giá sản phẩm theo thời gian thực — tương tự eBay. Giá bán cuối cùng được xác định qua quá trình cạnh tranh giữa các người mua trong một khoảng thời gian nhất định.

3. Tính năng chính

- *Quản lý người dùng* — Đăng ký, đăng nhập. Vai trò: Bidder, Seller, Admin
- *Quản lý sản phẩm* — Thêm/sửa/xóa sản phẩm, giá khởi điểm, thời gian đấu giá
- *Đấu giá realtime* — Đặt giá, cập nhật người dẫn đầu, tự động đóng phiên
- *Xử lý đồng thời* — Tránh lost update, race condition khi nhiều người bid cùng lúc
- *Auto-Bidding* — Tự động trả giá theo maxBid và bước giá increment
- *Anti-sniping* — Tự động gia hạn phiên nếu có bid trong X giây cuối

4. Kiến trúc

Client–Server. Client dùng JavaFX + FXML theo MVC. Server xử lý nghiệp vụ và truy cập database.

Giao tiếp qua Socket với dữ liệu JSON. Realtime update dùng Observer Pattern.

5. Design Pattern

- Singleton — quản lý kết nối
- Factory Method — tạo các loại Item (Electronics, Art, Vehicle)
- Observer — realtime bid update cho tất cả client
- Strategy / Command — xử lý các loại bid khác nhau



6 Nhóm thực hiện

| Thành viên | Vai trò |
|---|---|
| Đỗ Văn Nam | ... |
| Nguyễn Trần Đức Minh | ... |
| Phan Minh Quang | ... |
