# Phân tích Use Case: Hệ thống quản lý chuỗi cửa hàng tiện lợi và kho

*Dựa trên nghiệp vụ đã mô tả và cơ sở dữ liệu Supabase (PostgreSQL) đã xây dựng*

## 1. Tổng quan

Hệ thống phục vụ một chuỗi cửa hàng tiện lợi và các kho cấp hàng cho chuỗi đó. Có ba mảng nghiệp vụ chính:

- **Kho:** nhập hàng từ nhà cung cấp, duyệt yêu cầu của cửa hàng và xuất hàng đến cửa hàng.
- **Cửa hàng:** bán hàng, theo dõi tồn trên kệ, nhận cảnh báo sắp hết hàng và gửi yêu cầu bổ sung hàng cho kho.
- **Quản trị:** quản lý nhân viên, cửa hàng, kho và danh mục sản phẩm.

Ràng buộc tổ chức: mỗi cửa hàng có đúng 1 quản lý, mỗi kho có đúng 1 quản lý, và có một tài khoản admin quản lý chung. Quản lý khách hàng chỉ ở mức ưu tiên thấp vì hệ thống tập trung vào bán hàng và kho bãi.

## 2. Tác nhân (Actor)

| Tác nhân | Vai trò trong CSDL | Mô tả | Phạm vi dữ liệu |
|---|---|---|---|
| Admin | admin | Quản trị chung toàn chuỗi | Toàn bộ hệ thống |
| Quản lý cửa hàng | store_manager | Điều hành một cửa hàng, đặt ngưỡng cảnh báo | Cửa hàng của mình |
| Nhân viên bán hàng | sales_staff | Bán hàng, tạo phiếu yêu cầu, nhận hàng | Cửa hàng của mình |
| Quản lý kho | warehouse_manager | Điều hành một kho, duyệt phiếu, xuất kho | Kho của mình |
| Nhân viên kho | warehouse_staff | Nhập kho, xuất kho theo phiếu đã duyệt | Kho của mình |
| Hệ thống | trigger và hàm CSDL | Tác nhân tự động: cập nhật tồn, sinh cảnh báo | Toàn bộ |
| Khách hàng | không có tài khoản | Chỉ xuất hiện trên hoá đơn (có thể là khách vãng lai) | Không truy cập |
| Nhà cung cấp | không đăng nhập | Đối tác bên ngoài giao hàng vào kho | Không truy cập |

Tài khoản mới đăng ký mặc định **chưa kích hoạt** và không có quyền gì cho đến khi admin gán vai trò và nơi làm việc.

## 3. Luồng nghiệp vụ tổng thể

1. Kho nhập hàng từ nhà cung cấp, tồn kho tăng.
2. Cửa hàng bán hàng, tồn trên kệ giảm theo từng hoá đơn.
3. Khi tồn một sản phẩm chạm ngưỡng tối thiểu, hệ thống tự sinh cảnh báo cho nhân viên cửa hàng.
4. Nhân viên (hoặc quản lý) cửa hàng tạo phiếu yêu cầu nhập hàng gồm sản phẩm và số lượng, gửi cho một kho.
5. Kho xem phiếu, duyệt (có thể điều chỉnh số lượng) hoặc từ chối.
6. Với phiếu đã duyệt, kho lập phiếu xuất: tồn kho giảm và hàng được giao đến cửa hàng.
7. Cửa hàng xác nhận nhận hàng: tồn trên kệ tăng, cảnh báo liên quan được đóng.

## 4. Danh sách use case

| Mã | Use case | Tác nhân chính |
|---|---|---|
| **Xác thực và quản trị** | | |
| UC01 | Đăng nhập, đăng xuất | Mọi nhân viên |
| UC02 | Quản lý nhân viên và phân quyền | Admin |
| UC03 | Quản lý cửa hàng (kèm gán quản lý) | Admin |
| UC04 | Quản lý kho (kèm gán quản lý) | Admin |
| **Danh mục** | | |
| UC05 | Quản lý danh mục và thương hiệu | Admin |
| UC06 | Quản lý sản phẩm | Admin |
| UC07 | Quản lý nhà cung cấp | Admin |
| **Kho** | | |
| UC08 | Nhập kho từ nhà cung cấp | Quản lý kho, Nhân viên kho |
| UC09 | Xem tồn kho | Mọi nhân viên |
| UC10 | Duyệt phiếu yêu cầu | Quản lý kho, Nhân viên kho |
| UC11 | Từ chối phiếu yêu cầu | Quản lý kho, Nhân viên kho |
| UC12 | Xuất kho cho cửa hàng | Quản lý kho, Nhân viên kho |
| **Cửa hàng** | | |
| UC13 | Xem tồn kệ cửa hàng | Quản lý cửa hàng, Nhân viên bán hàng |
| UC14 | Cấu hình ngưỡng cảnh báo | Quản lý cửa hàng |
| UC15 | Xem cảnh báo sắp hết hàng | Quản lý cửa hàng, Nhân viên bán hàng |
| UC16 | Tạo phiếu yêu cầu nhập hàng | Quản lý cửa hàng, Nhân viên bán hàng |
| UC17 | Theo dõi phiếu yêu cầu và phiếu xuất | Cửa hàng và kho liên quan |
| UC18 | Xác nhận nhận hàng | Quản lý cửa hàng, Nhân viên bán hàng |
| **Bán hàng** | | |
| UC19 | Lập hoá đơn bán hàng | Quản lý cửa hàng, Nhân viên bán hàng |
| UC20 | Tra cứu hoá đơn | Quản lý cửa hàng, Nhân viên bán hàng |
| UC21 | Quản lý khách hàng (ưu tiên thấp) | Quản lý cửa hàng, Nhân viên bán hàng |
| **Tự động (Hệ thống)** | | |
| UC22 | Cập nhật tồn kho khi nhập kho | Hệ thống |
| UC23 | Trừ tồn cửa hàng và tính tiền khi bán | Hệ thống |
| UC24 | Sinh cảnh báo sắp hết hàng | Hệ thống |
| UC25 | Đóng cảnh báo khi hàng về | Hệ thống |
| UC26 | Tạo hồ sơ nhân viên khi có tài khoản mới | Hệ thống |

### Quan hệ giữa các use case

- UC16 **extend** UC15: từ một cảnh báo, người dùng có thể tạo phiếu yêu cầu.
- UC08 **include** UC22; UC19 **include** UC23; UC23 **include** UC24; UC18 **include** UC25.
- UC10 và UC11 là hai nhánh thay thế của cùng bước xét duyệt phiếu.
- UC12 chỉ thực hiện được sau UC10; UC18 chỉ thực hiện được sau UC12.
- UC02 phụ thuộc UC26: hồ sơ nhân viên được tạo tự động rồi admin mới gán quyền.

## 5. Đặc tả các use case chính

### UC16 - Tạo phiếu yêu cầu nhập hàng

- **Tác nhân:** Quản lý cửa hàng, Nhân viên bán hàng (admin cũng có quyền).
- **Tiền điều kiện:** đã đăng nhập, tài khoản đã kích hoạt và thuộc đúng cửa hàng gửi phiếu.
- **Luồng chính:**
  1. Nhân viên mở danh sách cảnh báo (UC15) và chọn các sản phẩm cần bổ sung.
  2. Chọn kho nhận yêu cầu.
  3. Nhập số lượng yêu cầu cho từng sản phẩm và ghi chú nếu cần.
  4. Gửi phiếu.
  5. Hệ thống kiểm tra quyền, tạo phiếu ở trạng thái **pending** cùng các dòng chi tiết.
  6. Phiếu xuất hiện trong danh sách chờ của kho.
- **Luồng ngoại lệ:** người dùng không thuộc cửa hàng gửi phiếu thì bị từ chối; danh sách sản phẩm rỗng thì báo lỗi; một sản phẩm lặp lại hai lần trong cùng phiếu hoặc số lượng không dương thì bị từ chối.
- **Hậu điều kiện:** có một phiếu yêu cầu pending, ghi nhận người tạo và thời điểm tạo. **Chưa có tồn kho hay tồn kệ nào thay đổi.**

### UC10 - Duyệt phiếu yêu cầu

- **Tác nhân:** Quản lý kho, Nhân viên kho.
- **Tiền điều kiện:** phiếu đang pending và gửi đến kho của người duyệt.
- **Luồng chính:**
  1. Nhân viên kho mở phiếu và xem các dòng yêu cầu.
  2. Đối chiếu với tồn kho hiện có (UC09).
  3. Chấp nhận nguyên số lượng hoặc điều chỉnh số lượng duyệt cho từng dòng.
  4. Xác nhận duyệt.
  5. Hệ thống ghi số lượng duyệt (mặc định bằng số lượng yêu cầu), chuyển phiếu sang **approved**, ghi người duyệt và thời điểm duyệt.
- **Luồng ngoại lệ:** phiếu không còn ở trạng thái pending hoặc không thuộc kho của người duyệt thì bị từ chối.
- **Lưu ý:** bước duyệt **không kiểm tra tồn kho** và **không chặn** số lượng duyệt lớn hơn số lượng yêu cầu; việc kiểm tra đủ hàng được thực hiện ở bước xuất kho, còn giới hạn số lượng duyệt nên đặt ở giao diện.

### UC11 - Từ chối phiếu yêu cầu

- **Tác nhân:** Quản lý kho, Nhân viên kho.
- **Luồng chính:** mở phiếu pending, nhập lý do, xác nhận. Phiếu chuyển sang **rejected** và lưu lý do, người xử lý, thời điểm.
- **Hậu điều kiện:** phiếu kết thúc; cửa hàng cần tạo phiếu mới nếu vẫn cần hàng.

### UC12 - Xuất kho cho cửa hàng

- **Tác nhân:** Quản lý kho, Nhân viên kho.
- **Tiền điều kiện:** phiếu yêu cầu ở trạng thái approved và thuộc kho của người xuất.
- **Luồng chính:**
  1. Nhân viên kho chọn phiếu đã duyệt và chọn xuất kho.
  2. Hệ thống tạo phiếu xuất ở trạng thái **shipping** (mỗi phiếu yêu cầu chỉ có một phiếu xuất).
  3. Với từng dòng có số lượng duyệt lớn hơn 0, hệ thống trừ tồn kho và tạo dòng chi tiết phiếu xuất.
  4. Phiếu yêu cầu chuyển sang **exported**.
  5. Kho giao hàng đến cửa hàng.
- **Luồng ngoại lệ:** nếu bất kỳ dòng nào không đủ tồn kho, toàn bộ thao tác bị huỷ và không có tồn kho nào bị trừ; phiếu không có dòng nào được duyệt thì báo lỗi; phiếu đã xuất rồi thì không xuất lại được.
- **Hậu điều kiện:** tồn kho giảm đúng bằng số lượng duyệt (có thể ít hơn số lượng cửa hàng yêu cầu). Tồn kệ cửa hàng **chưa** thay đổi.

### UC18 - Xác nhận nhận hàng

- **Tác nhân:** Quản lý cửa hàng, Nhân viên bán hàng.
- **Tiền điều kiện:** phiếu xuất ở trạng thái shipping và gửi đến cửa hàng của người xác nhận.
- **Luồng chính:**
  1. Nhân viên đối chiếu hàng thực nhận với phiếu xuất.
  2. Xác nhận đã nhận hàng.
  3. Hệ thống cộng tồn trên kệ (tạo dòng tồn mới với ngưỡng mặc định 5 nếu cửa hàng chưa có sản phẩm này).
  4. Hệ thống đóng các cảnh báo đang mở của những sản phẩm vừa nhận, nếu tồn đã vượt ngưỡng.
  5. Phiếu xuất chuyển sang **delivered** (ghi người nhận, thời điểm); phiếu yêu cầu chuyển sang **received**.
- **Luồng ngoại lệ:** phiếu đã được xác nhận trước đó, hoặc người dùng không thuộc cửa hàng nhận, thì bị từ chối.
- **Hạn chế hiện tại:** chưa cho nhập số lượng thực nhận khác với phiếu xuất, nên chưa xử lý chênh lệch hàng hao hụt.

### UC19 - Lập hoá đơn bán hàng

- **Tác nhân:** Quản lý cửa hàng, Nhân viên bán hàng.
- **Tiền điều kiện:** đã đăng nhập, thuộc cửa hàng lập hoá đơn, sản phẩm có tồn trên kệ.
- **Luồng chính:**
  1. Nhân viên quét mã vạch hoặc chọn sản phẩm, nhập số lượng.
  2. (Tuỳ chọn) chọn khách hàng; để trống nếu là khách vãng lai.
  3. Chọn phương thức thanh toán (tiền mặt, thẻ, chuyển khoản, ví điện tử) và nhập giảm giá nếu có.
  4. Lưu hoá đơn.
  5. Hệ thống sinh mã hoá đơn dạng HDyymmddNNNNNN, lấy đơn giá hiện tại của sản phẩm, trừ tồn kệ, cộng dồn tạm tính; tổng tiền bằng tạm tính trừ giảm giá.
- **Luồng ngoại lệ:** không đủ hàng trên kệ thì dòng hàng bị từ chối kèm thông báo lỗi.
- **Lưu ý triển khai:** nếu ứng dụng ghi tiêu đề hoá đơn và các dòng hàng bằng hai lệnh riêng, lỗi ở dòng hàng sẽ để lại một hoá đơn rỗng. Nên gói việc lập hoá đơn vào một hàm RPC duy nhất để toàn bộ nằm trong một giao dịch.

### UC08 - Nhập kho từ nhà cung cấp

- **Tác nhân:** Quản lý kho, Nhân viên kho.
- **Luồng chính:**
  1. Chọn kho (của mình) và nhà cung cấp.
  2. Thêm các dòng: sản phẩm, số lượng, giá nhập, số lô, hạn sử dụng.
  3. Lưu phiếu nhập.
  4. Hệ thống cộng tồn kho, cập nhật giá nhập gần nhất của sản phẩm và cộng dồn tổng tiền phiếu.
- **Luồng ngoại lệ:** số lượng không dương, sản phẩm lặp trong cùng phiếu, hoặc kho không phải của người lập thì bị từ chối.

### UC02 - Quản lý nhân viên và phân quyền

- **Tác nhân:** Admin.
- **Luồng chính:**
  1. Admin tạo tài khoản đăng nhập trong hệ thống xác thực.
  2. Hệ thống tự tạo hồ sơ nhân viên ở trạng thái chưa kích hoạt (UC26).
  3. Admin gán vai trò và nơi làm việc, sau đó bật kích hoạt.
  4. Admin gán người quản lý cho cửa hàng hoặc kho.
- **Ràng buộc kiểm tra:** admin không thuộc cửa hàng hay kho nào; quản lý cửa hàng và nhân viên bán hàng bắt buộc có cửa hàng; quản lý kho và nhân viên kho bắt buộc có kho; mỗi người chỉ quản lý tối đa một nơi.

### UC24 - Sinh cảnh báo sắp hết hàng

- **Tác nhân:** Hệ thống.
- **Kích hoạt:** mỗi khi số lượng hoặc ngưỡng tối thiểu của một dòng tồn kệ thay đổi mà số lượng nhỏ hơn hoặc bằng ngưỡng.
- **Luồng chính:** nếu chưa có cảnh báo đang mở cho cặp (cửa hàng, sản phẩm) thì tạo cảnh báo **open**; nếu đã có thì chỉ cập nhật số lượng hiện tại.
- **Hậu điều kiện:** cảnh báo hiển thị cho nhân viên cửa hàng và được đẩy theo thời gian thực.

## 6. Quản lý số lượng sản phẩm: nhập kho, xuất kho và tồn trên kệ

Phần này mô tả số lượng sản phẩm được lưu ở đâu, thay đổi khi nào và theo công thức nào.

### 6.1. Nơi lưu số lượng

| Loại số lượng | Bảng và cột | Khoá chính | Ràng buộc |
|---|---|---|---|
| **Tồn kho** (hàng đang nằm trong kho) | warehouse_stock.quantity | (kho, sản phẩm) | Không âm |
| **Tồn trên kệ cửa hàng** | store_stock.quantity | (cửa hàng, sản phẩm) | Không âm |
| **Ngưỡng cảnh báo** của từng sản phẩm tại từng cửa hàng | store_stock.min_quantity | (cửa hàng, sản phẩm) | Không âm, mặc định 5 |
| Số lượng trên phiếu nhập kho | goods_receipt_items.quantity | (phiếu nhập, sản phẩm) | Lớn hơn 0 |
| Số lượng cửa hàng yêu cầu | restock_request_items.requested_quantity | (phiếu yêu cầu, sản phẩm) | Lớn hơn 0 |
| Số lượng kho duyệt | restock_request_items.approved_quantity | (phiếu yêu cầu, sản phẩm) | Không âm; để trống cho đến khi duyệt |
| Số lượng trên phiếu xuất | stock_export_items.quantity | (phiếu xuất, sản phẩm) | Lớn hơn 0 |
| Số lượng bán trên hoá đơn | invoice_items.quantity | (hoá đơn, sản phẩm) | Lớn hơn 0 |

Mỗi (kho, sản phẩm) và mỗi (cửa hàng, sản phẩm) chỉ có một dòng tồn. Cùng một sản phẩm có tồn kho riêng ở từng kho và tồn kệ riêng ở từng cửa hàng.

### 6.2. Số lượng thay đổi khi nào

| Sự kiện | Tồn kho | Tồn kệ cửa hàng | Công thức và điều kiện |
|---|---|---|---|
| **Nhập kho từ nhà cung cấp** (UC08) | Tăng | Không đổi | tồn kho mới = tồn kho cũ + số lượng nhập. Nếu kho chưa có sản phẩm này thì tạo dòng tồn mới. Không giới hạn trên. |
| **Tạo phiếu yêu cầu** (UC16) | Không đổi | Không đổi | Chỉ ghi số lượng yêu cầu. |
| **Duyệt phiếu** (UC10) | Không đổi | Không đổi | Ghi số lượng duyệt (mặc định bằng số lượng yêu cầu). Chưa kiểm tra tồn kho. |
| **Xuất kho** (UC12) | **Giảm** | Không đổi | tồn kho mới = tồn kho cũ - số lượng duyệt. Chỉ trừ khi tồn kho lớn hơn hoặc bằng số lượng duyệt; thiếu ở một dòng thì huỷ toàn bộ phiếu xuất. Dòng có số lượng duyệt bằng 0 bị bỏ qua. |
| **Nhận hàng** (UC18) | Không đổi | **Tăng** | tồn kệ mới = tồn kệ cũ + số lượng trên phiếu xuất. Nếu cửa hàng chưa có sản phẩm này thì tạo dòng tồn mới với ngưỡng 5. |
| **Bán hàng** (UC19) | Không đổi | **Giảm** | tồn kệ mới = tồn kệ cũ - số lượng bán. Chỉ trừ khi tồn kệ lớn hơn hoặc bằng số lượng bán; thiếu thì từ chối dòng hàng. |
| **Đổi ngưỡng cảnh báo** (UC14) | Không đổi | Không đổi | Chỉ đổi min_quantity, nhưng có thể làm sinh cảnh báo nếu tồn kệ đang nhỏ hơn hoặc bằng ngưỡng mới. |

### 6.3. Quy tắc cảnh báo theo số lượng

- Cảnh báo được sinh khi **tồn kệ nhỏ hơn hoặc bằng ngưỡng** (bằng ngưỡng cũng cảnh báo).
- Mỗi cặp (cửa hàng, sản phẩm) chỉ có một cảnh báo đang mở; tồn tiếp tục giảm thì cập nhật số lượng hiện tại thay vì tạo cảnh báo mới.
- Cảnh báo chỉ được đóng khi cửa hàng **xác nhận nhận hàng** và tồn kệ sau khi nhận **lớn hơn ngưỡng**. Nếu nhận về mà vẫn chưa vượt ngưỡng thì cảnh báo vẫn mở.

### 6.4. Hàng đang vận chuyển

Giữa lúc kho xuất hàng (UC12) và cửa hàng xác nhận nhận (UC18), phiếu xuất ở trạng thái **shipping**. Trong khoảng này số hàng đó **không còn trong tồn kho và cũng chưa nằm trong tồn kệ**. Số lượng hàng đang vận chuyển chính là tổng số lượng các dòng của những phiếu xuất đang shipping. Nếu cửa hàng chưa xác nhận, tồn kệ sẽ chưa tăng và cảnh báo vẫn mở.

### 6.5. Ví dụ minh hoạ bằng số

Kho Trung Tâm Quận 7 đang có 400 lon Coca-Cola và 400 lon Pepsi. Cửa hàng Lê Văn Việt đang có 6 lon Coca (ngưỡng 12) và 30 lon Pepsi (ngưỡng 12), nên đang có một cảnh báo Coca ở trạng thái open.

| Bước | Sự kiện | Kho: Coca / Pepsi | Kệ cửa hàng: Coca / Pepsi | Ghi chú |
|---|---|---|---|---|
| 0 | Trạng thái ban đầu | 400 / 400 | 6 / 30 | Coca: 6 nhỏ hơn hoặc bằng 12, cảnh báo open |
| 1 | Cửa hàng tạo phiếu: yêu cầu 60 Coca, 60 Pepsi | 400 / 400 | 6 / 30 | Phiếu pending, chưa đổi tồn |
| 2 | Kho duyệt: Coca 60, Pepsi giảm còn 48 | 400 / 400 | 6 / 30 | Phiếu approved, chưa đổi tồn |
| 3 | Kho xuất hàng | 340 / 352 | 6 / 30 | Phiếu xuất shipping; 108 lon đang vận chuyển |
| 4 | Cửa hàng xác nhận nhận hàng | 340 / 352 | 66 / 78 | Phiếu delivered; Coca 66 lớn hơn 12 nên cảnh báo chuyển resolved |
| 5 | Cửa hàng bán 2 lon Coca | 340 / 352 | 64 / 78 | Tồn kệ giảm theo hoá đơn |

Ví dụ nhập kho: phiếu nhập 300 chai TEA+ vào kho chưa từng có sản phẩm này sẽ tạo dòng tồn mới với 300; nhập thêm 100 chai nữa thì tồn là 400.

### 6.6. Truy vấn kiểm tra nhanh

```sql
-- Tồn kho kèm tên kho và sản phẩm
select * from public.warehouse_stock_overview order by warehouse, product;

-- Tồn kệ của từng cửa hàng, đánh dấu sản phẩm dưới ngưỡng
select s.name as cua_hang, p.name as san_pham, ss.quantity, ss.min_quantity,
       ss.quantity <= ss.min_quantity as sap_het
from public.store_stock ss
join public.stores s on s.id = ss.store_id
join public.products p on p.id = ss.product_id
order by s.name, p.name;

-- Hàng đang vận chuyển (đã xuất kho, cửa hàng chưa xác nhận nhận)
select s.name as cua_hang, p.name as san_pham, sum(i.quantity) as dang_van_chuyen
from public.stock_exports e
join public.stock_export_items i on i.export_id = e.id
join public.stores s on s.id = e.store_id
join public.products p on p.id = i.product_id
where e.status = 'shipping'
group by s.name, p.name;
```

### 6.7. Lưu ý và hạn chế về số lượng

- Mọi thao tác thay đổi tồn kho chạy trong một giao dịch: hoặc thành công toàn bộ, hoặc không thay đổi gì.
- Hệ thống **chưa có sổ kho** ghi từng lần biến động. Lịch sử chỉ suy ra được từ các phiếu nhập, phiếu xuất và hoá đơn.
- Chưa có chức năng điều chỉnh tồn khi kiểm kê. Chỉ admin mới sửa trực tiếp được bảng tồn và việc sửa này không để lại dấu vết.
- Chưa chặn số lượng duyệt lớn hơn số lượng yêu cầu ở phía CSDL.
- Số lượng nhận thực tế luôn được coi bằng số lượng trên phiếu xuất.

## 7. Quy tắc nghiệp vụ

| Mã | Quy tắc |
|---|---|
| BR01 | Mỗi cửa hàng và mỗi kho có đúng một quản lý; một người chỉ quản lý một nơi. |
| BR02 | Tài khoản mới chưa kích hoạt thì không có quyền nào. |
| BR03 | Nhân viên chỉ thao tác trên dữ liệu của cửa hàng hoặc kho mình; admin có toàn quyền. |
| BR04 | Tồn kho và tồn cửa hàng không bao giờ âm; bán hoặc xuất vượt tồn bị từ chối. |
| BR05 | Đơn giá trên hoá đơn là giá tại thời điểm bán; đổi giá sản phẩm sau đó không ảnh hưởng hoá đơn cũ. |
| BR06 | Giá nhập của sản phẩm là giá lần nhập kho gần nhất; lịch sử giá nằm ở từng phiếu nhập. |
| BR07 | Chỉ xuất kho được phiếu đã duyệt; mỗi phiếu yêu cầu có tối đa một phiếu xuất. |
| BR08 | Số lượng xuất bằng số lượng kho duyệt, có thể thấp hơn số lượng cửa hàng yêu cầu. |
| BR09 | Mỗi cặp (cửa hàng, sản phẩm) có tối đa một cảnh báo đang mở. |
| BR10 | Ngưỡng cảnh báo đặt theo từng cửa hàng và từng sản phẩm, mặc định là 5. |
| BR11 | Phiếu yêu cầu và phiếu xuất chỉ thay đổi qua các hàm nghiệp vụ, không sửa trực tiếp. |
| BR12 | Mỗi sản phẩm chỉ xuất hiện một lần trong cùng một phiếu hoặc hoá đơn; mã vạch sản phẩm là duy nhất. |
| BR13 | Hoá đơn có thể không gắn khách hàng (khách vãng lai). |
| BR14 | Mọi thao tác làm thay đổi tồn kho chạy trong một giao dịch: hoặc thành công toàn bộ, hoặc không thay đổi gì. |

## 8. Vòng đời trạng thái

| Đối tượng | Chuyển trạng thái | Use case thực hiện |
|---|---|---|
| Phiếu yêu cầu | pending, sang approved | UC10 |
| Phiếu yêu cầu | pending, sang rejected (kết thúc) | UC11 |
| Phiếu yêu cầu | approved, sang exported | UC12 |
| Phiếu yêu cầu | exported, sang received (kết thúc) | UC18 |
| Phiếu xuất | shipping, sang delivered | UC18 |
| Cảnh báo | open, sang resolved | UC25 |

## 9. Ma trận phân quyền

Ký hiệu: C = có quyền, - = không có quyền. Quyền của nhân viên luôn giới hạn trong cửa hàng hoặc kho của họ.

| Chức năng | Admin | QL cửa hàng | NV bán hàng | QL kho | NV kho |
|---|---|---|---|---|---|
| Quản lý nhân viên, cửa hàng, kho | C | - | - | - | - |
| Quản lý sản phẩm, danh mục, nhà cung cấp | C | - | - | - | - |
| Xem sản phẩm, nhà cung cấp | C | C | C | C | C |
| Xem tồn kho | C | C | C | C | C |
| Nhập kho từ nhà cung cấp | C | - | - | C | C |
| Duyệt, từ chối phiếu yêu cầu | C | - | - | C | C |
| Xuất kho | C | - | - | C | C |
| Xem tồn kệ và cảnh báo | C | C | C | - | - |
| Chỉnh ngưỡng cảnh báo | C | C | - | - | - |
| Tạo phiếu yêu cầu nhập hàng | C | C | C | - | - |
| Xác nhận nhận hàng | C | C | C | - | - |
| Lập và xem hoá đơn | C | C | C | - | - |
| Quản lý khách hàng | C | C | C | - | - |
| Xem phiếu yêu cầu và phiếu xuất | C | C (của mình) | C (của mình) | C (của kho) | C (của kho) |

## 10. Ánh xạ use case với cơ sở dữ liệu

| Use case | Bảng chính | Hàm, trigger hoặc cơ chế |
|---|---|---|
| UC01, UC02, UC26 | employees, stores, warehouses | Trigger tạo hồ sơ khi có user mới; ràng buộc vai trò và nơi làm việc |
| UC05, UC06, UC07 | categories, brands, products, suppliers | Chính sách RLS: chỉ admin ghi |
| UC08, UC22 | goods_receipts, goods_receipt_items, warehouse_stock | Trigger cộng tồn kho, cập nhật giá nhập và tổng tiền |
| UC09 | warehouse_stock | View warehouse_stock_overview |
| UC13, UC14 | store_stock | Chính sách RLS theo cửa hàng; quản lý cửa hàng được chỉnh ngưỡng |
| UC15, UC24, UC25 | stock_alerts, store_stock | Trigger sinh cảnh báo; đóng cảnh báo trong hàm nhận hàng; Realtime |
| UC16 | restock_requests, restock_request_items | Hàm create_restock_request |
| UC10, UC11 | restock_requests, restock_request_items | Hàm approve_restock_request, reject_restock_request |
| UC12 | stock_exports, stock_export_items, warehouse_stock | Hàm create_stock_export |
| UC18 | stock_exports, store_stock, stock_alerts | Hàm confirm_stock_receipt |
| UC17 | restock_requests, stock_exports | Chính sách RLS cho cả bên cửa hàng và bên kho; Realtime |
| UC19, UC23 | invoices, invoice_items, store_stock | Trigger lấy giá, trừ tồn, tính tạm tính; cột tổng tiền tự tính |
| UC20 | invoices, invoice_items | Chính sách RLS theo cửa hàng |
| UC21 | customers | Chính sách RLS cho nhân viên cửa hàng |

## 11. Yêu cầu phi chức năng

- **Bảo mật:** bật Row Level Security trên mọi bảng; các hàm nghiệp vụ tự kiểm tra vai trò và nơi làm việc của người gọi.
- **Toàn vẹn dữ liệu:** ràng buộc không âm, khoá chính ghép cho các dòng chi tiết, khoá ngoại đầy đủ; thao tác kho chạy trong giao dịch.
- **Truy vết:** mọi phiếu ghi người tạo, người duyệt, người nhận và thời điểm tương ứng.
- **Thời gian thực:** cảnh báo, phiếu yêu cầu và phiếu xuất được đẩy về ứng dụng qua Realtime để nhân viên thấy ngay.
- **Hiệu năng:** có chỉ mục cho các khoá ngoại thường truy vấn (cửa hàng, kho, trạng thái, thời gian).

## 12. Phạm vi chưa làm và hướng mở rộng

- Huỷ hoặc sửa phiếu yêu cầu sau khi gửi; hoàn trả và huỷ hoá đơn.
- Kiểm kê kho, chuyển hàng giữa các kho, xử lý chênh lệch khi nhận hàng.
- Sổ kho ghi từng lần biến động số lượng để truy vết lịch sử.
- Xuất kho theo lô và hạn sử dụng (FEFO); hiện số lô và hạn sử dụng mới chỉ được lưu ở phiếu nhập.
- Tích điểm khách hàng: cột điểm đã có nhưng chưa có logic cộng điểm.
- Báo cáo doanh thu và hàng bán chạy: thực hiện bằng truy vấn hoặc view bổ sung.
- Cho quản lý cửa hàng và quản lý kho tự quản lý nhân viên của mình (hiện chỉ admin).
- Kiểm tra tồn kho ngay ở bước duyệt phiếu để cảnh báo sớm cho người duyệt, và chặn số lượng duyệt lớn hơn số lượng yêu cầu.
