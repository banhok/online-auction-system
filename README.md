<div align="center">

# 🏷️ Online Auction System

**Hệ thống đấu giá trực tuyến — Bài tập lớn Lập trình nâng cao**

[![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/17/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-blue?logo=java&logoColor=white)](https://openjfx.io/)
[![Maven](https://img.shields.io/badge/Maven-3.8+-red?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![SQLite](https://img.shields.io/badge/SQLite-WAL-003B57?logo=sqlite&logoColor=white)](https://www.sqlite.org/)
[![Tests](https://img.shields.io/badge/Tests-64%20passed-brightgreen)](#-test--cicd)
[![Coverage](https://img.shields.io/badge/Coverage-71%25-brightgreen)](#-test--cicd)
[![CI](https://img.shields.io/badge/CI-GitHub%20Actions-2088FF?logo=githubactions&logoColor=white)](.github/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](#-license)

</div>

---

## 📚 Mục lục

- [🎯 Giới thiệu](#-giới-thiệu)
- [🛠️ Công nghệ & Yêu cầu](#️-công-nghệ--yêu-cầu-hệ-thống)
- [⚡ Quick Start](#-quick-start)
- [📦 Cài đặt & Chạy chi tiết](#-cài-đặt--chạy-chi-tiết)
- [🏗️ Kiến trúc hệ thống](#️-kiến-trúc-hệ-thống)
- [📂 Cấu trúc thư mục](#-cấu-trúc-thư-mục)
- [✨ Chức năng đã hoàn thành](#-chức-năng-đã-hoàn-thành)
- [🎨 Design Pattern áp dụng](#-design-pattern-áp-dụng)
- [🧪 Test & CI/CD](#-test--cicd)
- [📄 Tài liệu kèm theo](#-tài-liệu-kèm-theo)
- [👥 Phân công nhóm](#-phân-công-nhóm)
- [📝 License](#-license)
- [🙏 Acknowledgments](#-acknowledgments)

---

## 🎯 Giới thiệu

### Mô tả bài toán

Ứng dụng **desktop Java** cho phép nhiều người dùng tham gia đấu giá sản phẩm theo **thời gian thực** trên cùng một mạng cục bộ. Hệ thống mô phỏng nền tảng đấu giá thực tế (tương tự eBay) với 3 vai trò chính:

- **Bidder** — tham gia đặt giá, nạp/rút tiền ví
- **Seller** — đăng sản phẩm, tạo và quản lý phiên đấu giá
- **Admin** — quản lý người dùng toàn hệ thống

### Phạm vi hệ thống

- ✅ Kiến trúc **Client–Server** phân tầng rõ ràng (giao tiếp Socket + JSON)
- ✅ Xử lý **đấu giá đồng thời** an toàn (tránh lost-update bằng `ReentrantLock`)
- ✅ Cập nhật **realtime** qua Observer pattern (server push, không polling)
- ✅ **Anti-sniping**: bid trong 60s cuối → tự gia hạn phiên thêm 60s
- ✅ Quản lý vòng đời phiên tự động: `OPEN → RUNNING → FINISHED → PAID/CANCELED`
- ✅ Hệ thống ví + audit trail giao dịch
- ✅ Biểu đồ giá realtime (`LineChart` JavaFX) cập nhật theo từng bid

### Tài khoản demo

| Username | Password | Role |
|---|---|---|
| `admin` | `admin123` | ADMIN |

Đăng ký Bidder/Seller mới qua màn hình **Đăng ký** ở client.

---

## 🛠️ Công nghệ & Yêu cầu hệ thống

### Tech Stack

| Lớp | Công nghệ | Phiên bản |
|---|---|---|
| **Ngôn ngữ** | Java | 17 (LTS) |
| **Giao diện** | JavaFX + FXML + CSS | 21 |
| **Build tool** | Apache Maven (multi-module) | 3.8+ |
| **Cơ sở dữ liệu** | SQLite (WAL mode) | 3.45 |
| **Giao tiếp mạng** | TCP Socket + JSON (Gson) | Gson 2.10 |
| **Logging** | SLF4J + Simple binding | 2.0 |
| **Test framework** | JUnit Jupiter | 5.10 |
| **Coverage** | JaCoCo | 0.8 |
| **CI/CD** | GitHub Actions | — |
| **JAR packaging** | maven-shade-plugin | 3.5 |

### Yêu cầu môi trường

| Yêu cầu | Phiên bản |
|---|---|
| **JDK** | Java 17+ ([Eclipse Temurin](https://adoptium.net/) khuyến nghị) |
| **Maven** | 3.8+ (chỉ cần khi build từ source — chạy fat JAR không cần) |
| **Hệ điều hành** | Windows/Mac/Linux |

---

## ⚡ Quick Start

Cách nhanh nhất để chạy thử (đã có Java 17 + Maven):

```bash
git clone https://github.com/banhok/online-auction-system.git
cd online-auction-system
mvn clean package -DskipTests
java -jar auction-server/target/auction-server.jar      # Terminal 1
java -jar auction-client/target/auction-client.jar      # Terminal 2 (mỗi client một terminal)
```

Đăng nhập với `admin / admin123` để vào màn Admin, hoặc đăng ký tài khoản Bidder/Seller mới.

---

## 📦 Cài đặt & Chạy chi tiết

### Option A — Chạy bằng Maven (cho developer)

Build toàn bộ project:

```bash
mvn clean install -DskipTests
```

**Terminal 1 — Khởi động server**:
```bash
cd auction-server
mvn exec:java -Dexec.mainClass="com.auction.server.ServerApp"
```
Log đợi: `=== Auction Server started on port 8888 ===`

**Terminal 2+ — Mở client** (mỗi terminal là một client riêng):
```bash
cd auction-client
mvn javafx:run
```

### Option B — Chạy từ Fat JAR (cho chấm bài)

Build một lần để sinh 2 file fat JAR chạy được bằng `java -jar`:

```bash
mvn clean package -DskipTests
```

**Terminal 1 — Server**:
```bash
java -jar auction-server/target/auction-server.jar
```

**Terminal 2+ — Client** (mỗi client một terminal):
```bash
java -jar auction-client/target/auction-client.jar
```

### Vị trí file `.jar` sau khi build

| File | Kích thước | Mô tả |
|---|---|---|
| `auction-server/target/auction-server.jar` | ~14 MB | Fat JAR server, đã bundle SQLite + Gson + SLF4J |
| `auction-client/target/auction-client.jar` | ~11 MB | Fat JAR client, **hỗ trợ các OS khác nhau** |

### Chạy test

```bash
mvn test                                  # chạy 64 unit tests
mvn verify -pl auction-server -am         # test + JaCoCo coverage check (yêu cầu ≥60%)
```

### Reset database

Xóa cả 3 file (WAL mode tạo thêm 2 file phụ):

```bash
# Windows PowerShell
Remove-Item auction-server/auction.db*

# Linux/Mac
rm -f auction-server/auction.db*
```

Schema sẽ tự tạo lại lần chạy server kế tiếp.

---

## 🏗️ Kiến trúc hệ thống

### Tổng quan

Hệ thống gồm **3 module Maven** giao tiếp qua Socket + JSON, single source of truth ở SQLite phía server

### Luồng request/response

```
Client → Request{command, payload, token}
       → Socket (TCP 8888)
       → ClientHandler (1 thread / 1 client)
       → CommandRouter (dispatch theo CommandType)
       → Controller (User / Auction / Item / Wallet)
       → Service (business logic, có ReentrantLock per-auction)
       → DAO (JDBC, có DB_LOCK synchronized)
       → SQLite (WAL mode)
       ⤴ Response{success, data, message}
       ⤴ Socket
       ⤴ Client
```

### Trạng thái phiên đấu giá

```
OPEN ──(đến start_time)──> RUNNING ──(hết end_time)──> FINISHED ──> PAID
                                                              └──> CANCELED
```

`AuctionScheduler` chạy nền 2 giây/lần tự chuyển trạng thái — single source of truth cho lifecycle.

### Luồng đặt cọc

1. **Bidder** nạp tiền vào ví (`TOP_UP`)
2. **Tham gia phiên** (`JOIN_AUCTION`) → trừ 10% giá khởi điểm vào `deposits` (status `HELD`)
3. Đấu giá diễn ra — cọc bị tạm giữ, không dùng cho phiên khác
4. **Kết thúc phiên**:
   - **Thua** → cọc `REFUNDED` + hoàn về ví
   - **Thắng** → cọc `DEDUCTED`, chờ winner bấm "Thanh toán"
5. **Winner thanh toán** → trừ phần còn lại từ ví, cộng tổng cho seller, auction → `PAID`

---

## 📂 Cấu trúc thư mục

```
online-auction-system/
├── pom.xml                           # Maven parent (aggregator 3 module)
├── README.md                         # File này
├── .github/workflows/ci.yml          # GitHub Actions CI/CD
│
├── auction-common/                   # 📦 Module shared
│   └── src/main/java/com/auction/common/
│       ├── model/                    # Entity → User/Item abstract + 6 concrete + Auction, Bid...
│       ├── dto/                      # Request, Response
│       ├── command/                  # CommandType enum
│       ├── exception/                # 5 custom exception class
│       └── util/                     # JsonUtil (Gson), DateTimeUtil
│
├── auction-server/                   # 🖥️ Module server
│   └── src/
│       ├── main/java/com/auction/server/
│       │   ├── ServerApp.java        # ★ Entry point server
│       │   ├── network/              # SocketServer + ClientHandler
│       │   ├── controller/           # CommandRouter + 4 Controller
│       │   ├── service/              # 5 Service (business logic) + Scheduler
│       │   ├── dao/                  # DatabaseManager + 6 DAO
│       │   ├── observer/             # AuctionEventManager (realtime push)
│       │   ├── strategy/             # EndStrategy (Normal / AntiSnipe)
│       │   └── factory/              # ItemFactory
│       ├── main/resources/
│       │   └── schema.sql            # Single source of truth cho DB schema
│       └── test/java/                # 6 test class, 64 test case, JaCoCo 71%
│
└── auction-client/                   # 🖼️ Module client (JavaFX)
    └── src/main/
        ├── java/com/auction/client/
        │   ├── ClientApp.java        # ★ Entry point client (Application)
        │   ├── Launcher.java         # ★ Entry point cho fat JAR (java -jar)
        │   ├── controller/           # 7 Controller (Login, Dashboard, AuctionDetail, ...)
        │   ├── network/              # ServerConnection (singleton socket)
        │   └── util/                 # SceneManager, ToastUtil, ModalDialog, ImageLoader,...
        └── resources/
            ├── fxml/                 # 7 FXML scene
            ├── css/style.css         # Theme Dark Cactus
            └── images/placeholder.png
```

---

## ✨ Chức năng đã hoàn thành

### Chức năng chính (bắt buộc theo đề bài)

- 👤 **Quản lý người dùng**: đăng ký, đăng nhập, đổi mật khẩu, quản lý profile (3 vai trò Bidder/Seller/Admin)
- 📦 **Quản lý sản phẩm (CRUD)**: 3 danh mục `Electronics`/`Art`/`Vehicle` với polymorphism (`Item.getCategoryDetails()` override runtime)
- 🔨 **Phiên đấu giá**: tạo, sửa, hủy phiên (Seller) — vòng đời 5 trạng thái tự động
- 💰 **Đặt giá realtime**: validate giá hợp lệ, broadcast `BID_UPDATE` qua Observer
- 🔒 **Xử lý concurrent bidding an toàn**: per-auction `ReentrantLock` + global `DB_LOCK` 2 tầng
- ⏰ **Tự động đóng phiên**: `AuctionScheduler` chạy nền 2s/lần
- 💵 **Hệ thống ví**: nạp/rút tiền, đặt cọc 10%, thanh toán winner, hoàn cọc loser
- ❌ **Xử lý lỗi & ngoại lệ**: 5 custom exception class (`InvalidBidException`, `InsufficientBalanceException`, `AuthenticationException`, `NotFoundException`, `AuctionException`)

### Chức năng nâng cao (bonus)

- 🛡️ **Anti-sniping**: bid trong 60 giây cuối → tự gia hạn 60 giây (Strategy Pattern)
- 📈 **Biểu đồ giá realtime**: `LineChart` JavaFX, X-axis thời gian thực, cập nhật theo từng bid
- 👨‍💼 **Admin panel**: xem toàn bộ user, xóa user vi phạm

### Chức năng bổ sung (UX)

- ⏱️ Đồng hồ đếm ngược (đỏ khi < 1 phút, vàng khi gia hạn anti-snipe)
- 🔍 Tìm kiếm + filter sản phẩm theo danh mục (Electronics/Art/Vehicle)
- 🍞 Toast notification thay JavaFX Alert mặc định
- 🪟 Custom Modal Dialog (theme đồng nhất, không bị "đảo Windows")
- 📊 Quick bid +10% / +20% / +50% (nút tiện lợi)
- 🖼️ ImageLoader cache + fallback placeholder cho ảnh sản phẩm
- 👨‍🎨 Avatar tròn hash màu theo username
- 🌙 Theme **Dark Cactus** (forest gradient + mustard accent)

---

## 🎨 Design Pattern áp dụng

| Pattern | Vị trí | Mục đích |
|---|---|---|
| **Singleton** | `DatabaseManager`, `ServerConnection`, `SceneManager` | Quản lý connection / navigation duy nhất |
| **Factory Method** | `ItemFactory` | Tạo `Electronics`/`Art`/`Vehicle` từ `ItemCategory` enum |
| **Observer** | `AuctionEventManager` + `AuctionEventListener` | Push realtime event cho client subscribed (không polling) |
| **Strategy** | `EndStrategy` — `NormalEndStrategy` / `AntiSnipeEndStrategy` | Hành vi kết thúc phiên swap được |
| **Command** | `CommandType` enum + `CommandRouter` | Dispatch request không if-else dài |

---

## 🧪 Test & CI/CD

### Unit Test (JUnit 5)

**64 tests** qua 6 test class:

| Test class | Phạm vi |
|---|---|
| `DatabaseManagerTest` | Singleton, connection, schema |
| `UserServiceTest` | Đăng ký, đăng nhập, profile, đổi mật khẩu |
| `WalletServiceTest` | Nạp/rút tiền, đặt cọc, hoàn cọc |
| `AuctionServiceTest` | Tạo phiên, đặt giá, **concurrent bid với 10 thread + CountDownLatch**, kết thúc phiên |
| `ItemServiceTest` | CRUD sản phẩm + ownership checks |
| `ItemFactoryTest` | Factory Method + polymorphism `getCategoryDetails()` |

### Coverage (JaCoCo)

- **71%** line coverage (yêu cầu đề bài ≥ 60%)
- Exclude POJO model, DTO, Controller passthrough (không tăng cover value)
- Report HTML tại `auction-server/target/site/jacoco/index.html`

Chạy local:

```bash
mvn verify -pl auction-server -am
```

### CI/CD (GitHub Actions)

File workflow: [.github/workflows/ci.yml](.github/workflows/ci.yml)

Mỗi push lên `main`/`develop` hoặc PR vào `main`, GitHub Actions tự động:

1. ✅ Setup JDK 17 (Eclipse Temurin)
2. ✅ Cache Maven dependencies
3. ✅ Compile toàn bộ 3 module
4. ✅ Chạy tests + JaCoCo coverage check (fail nếu < 60%)
5. ✅ Package fat JAR

---

## 📄 Tài liệu kèm theo

| Tài liệu | Mô tả | Vị trí |
|---|---|---|
| 📑 **Báo cáo PDF** | Báo cáo Bài tập lớn | https://drive.google.com/file/d/1DCB2sM8YqZHSwF2-ELECdC7DHfxLD3L3/view?usp=drive_link |
| 🎥 **Video demo** | Demo 3 phút: login, realtime bid, concurrent race, anti-snipe, payment, admin | [Upcomming] |
| 📐 **UML class diagram** | Biểu đồ lớp | https://drive.google.com/file/d/1-IwDXq9C_3PR4fK8EekuGWHpqJreWZBe/view?usp=drive_link |

---

## 👥 Phân công nhóm

| Vai trò | Phụ trách | 
|---|---|
| **Nguyễn Trần Đức Minh — Backend** | DAO (6), Service (5), Strategy, Factory, Observer, Scheduler, 64 unit tests | 
| **Phan Minh Quang — Frontend** | 7 Controller, 7 FXML, CSS Dark Cactus theme, Util client (Toast, Modal, Avatar...), Realtime listener | 
| **Đỗ Văn Nam — Architect/Common** | `auction-common` (Model/DTO/Enum/Exception/Util), Socket+JSON protocol, CommandRouter, Build & CI, Docs |

---

## 📝 License

Dự án sử dụng **MIT License** — phát triển cho mục đích học tập, không có hạn chế thương mại.

---

## 🙏 Acknowledgments

- **Khoa Công nghệ Thông tin**, Trường Đại học Công nghệ - ĐHQGHN
- Tham khảo eBay Auctions cho thiết kế nghiệp vụ
- Các thư viện open-source: [JavaFX](https://openjfx.io/), [SQLite JDBC](https://github.com/xerial/sqlite-jdbc), [Gson](https://github.com/google/gson), [SLF4J](https://www.slf4j.org/), [JUnit 5](https://junit.org/junit5/), [JaCoCo](https://www.jacoco.org/)

---

<div align="center">

**Made with ☕ by Nhóm 4 — UET.CS2043 (2025-2026)**


</div>
