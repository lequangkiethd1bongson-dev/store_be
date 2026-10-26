-- =====================================================================
-- HỆ THỐNG QUẢN LÝ CHUỖI CỬA HÀNG TIỆN LỢI + KHO
-- Supabase (PostgreSQL) - chạy trong SQL Editor
-- =====================================================================

-- ---------------------------------------------------------------------
-- 0. ENUM
-- ---------------------------------------------------------------------
create type public.user_role as enum (
  'admin',              -- quản trị chung
  'store_manager',      -- quản lý cửa hàng
  'kho_manager',  -- quản lý kho
  'sales',        -- nhân viên bán hàng
  'kho'     -- nhân viên kho
);

create type public.request_status  as enum ('pending', 'approved', 'rejected', 'exported', 'received');
create type public.export_status   as enum ('shipping', 'delivered');
create type public.alert_status    as enum ('open', 'resolved');
create type public.payment_method  as enum ('cash', 'card', 'transfer', 'e_wallet');

-- ---------------------------------------------------------------------
-- 1. TỔ CHỨC: CỬA HÀNG, KHO, NHÂN VIÊN
-- ---------------------------------------------------------------------
create table public.stores (
  id          uuid primary key default gen_random_uuid(),
  name        text not null,
  address     text,
  phone       text,
  manager_id  uuid unique,              -- mỗi cửa hàng 1 quản lý (FK thêm sau)
  is_active   boolean not null default true,
  created_at  timestamptz not null default now()
);

create table public.warehouses (
  id          uuid primary key default gen_random_uuid(),
  name        text not null,
  address     text,
  phone       text,
  manager_id  uuid unique,              -- mỗi kho 1 quản lý (FK thêm sau)
  is_active   boolean not null default true,
  created_at  timestamptz not null default now()
);

-- Hồ sơ nhân viên, gắn 1-1 với auth.users của Supabase
create table public.employees (
  id            uuid primary key references auth.users(id) on delete cascade,
  full_name     text not null,
  phone         text,
  email         text,
  role          public.user_role not null default 'sales',
  store_id      uuid references public.stores(id)     on delete set null,
  warehouse_id  uuid references public.warehouses(id) on delete set null,
  is_active     boolean not null default false,   -- admin kích hoạt sau khi gán vai trò
  created_at    timestamptz not null default now(),
  constraint employees_scope_chk check (
    not is_active or (
      (role = 'admin'                                 and store_id is null     and warehouse_id is null) or
      (role in ('store_manager','sales')        and store_id is not null and warehouse_id is null) or
      (role in ('kho_manager','kho') and warehouse_id is not null and store_id is null)
    )
  )
);

alter table public.stores
  add constraint stores_manager_fk
  foreign key (manager_id) references public.employees(id) on delete set null;

alter table public.warehouses
  add constraint warehouses_manager_fk
  foreign key (manager_id) references public.employees(id) on delete set null;

-- ---------------------------------------------------------------------
-- 2. DANH MỤC SẢN PHẨM
-- ---------------------------------------------------------------------
create table public.categories (
  id    uuid primary key default gen_random_uuid(),
  name  text not null unique
);

create table public.brands (
  id        uuid primary key default gen_random_uuid(),
  name      text not null unique,
  is_active boolean not null default true
);

alter table public.brands
  add column if not exists is_active boolean not null default true;

create table public.products (
  id            uuid primary key default gen_random_uuid(),
  barcode       text unique,
  name          text not null,
  description   text,
  image_url     text,
  price         numeric(12,0) not null check (price >= 0),      -- giá bán
  old_price     numeric(12,0) check (old_price >= 0),           -- giá cũ
  import_price  numeric(12,0) check (import_price >= 0),        -- giá nhập gần nhất
  package_size  text,                                           -- Chai, Lon, Gói...
  rating        numeric(2,1) check (rating between 0 and 5),
  category_id   uuid references public.categories(id) on delete set null,
  brand_id      uuid references public.brands(id)     on delete set null,
  is_active     boolean not null default true,
  created_at    timestamptz not null default now()
);

-- ---------------------------------------------------------------------
-- 3. TỒN KHO (KHO + CỬA HÀNG) VÀ CẢNH BÁO
-- ---------------------------------------------------------------------
create table public.warehouse_stock (
  warehouse_id  uuid not null references public.warehouses(id) on delete cascade,
  product_id    uuid not null references public.products(id)   on delete cascade,
  quantity      integer not null default 0 check (quantity >= 0),
  updated_at    timestamptz not null default now(),
  primary key (warehouse_id, product_id)
);

create table public.store_stock (
  store_id      uuid not null references public.stores(id)   on delete cascade,
  product_id    uuid not null references public.products(id) on delete cascade,
  quantity      integer not null default 0 check (quantity >= 0),
  min_quantity  integer not null default 5 check (min_quantity >= 0),  -- ngưỡng cảnh báo
  updated_at    timestamptz not null default now(),
  primary key (store_id, product_id)
);

create table public.stock_alerts (
  id            uuid primary key default gen_random_uuid(),
  store_id      uuid not null references public.stores(id)   on delete cascade,
  product_id    uuid not null references public.products(id) on delete cascade,
  current_qty   integer not null,
  status        public.alert_status not null default 'open',
  created_at    timestamptz not null default now(),
  resolved_at   timestamptz
);
-- Mỗi (cửa hàng, sản phẩm) chỉ có tối đa 1 cảnh báo đang mở
create unique index stock_alerts_one_open
  on public.stock_alerts (store_id, product_id) where status = 'open';

-- ---------------------------------------------------------------------
-- 4. NHẬP HÀNG VÀO KHO TỪ NHÀ CUNG CẤP
-- ---------------------------------------------------------------------
create table public.suppliers (
  id        uuid primary key default gen_random_uuid(),
  name      text not null,
  address   text,
  phone     text,
  email     text,
  is_active boolean not null default true
);

alter table public.suppliers
  add column if not exists is_active boolean not null default true;

create table public.goods_receipts (
  id            uuid primary key default gen_random_uuid(),
  warehouse_id  uuid not null references public.warehouses(id),
  supplier_id   uuid references public.suppliers(id),
  created_by    uuid not null default auth.uid() references public.employees(id),
  total_amount  numeric(14,0) not null default 0,
  note          text,
  created_at    timestamptz not null default now()
);

create table public.goods_receipt_items (
  receipt_id   uuid not null references public.goods_receipts(id) on delete cascade,
  product_id   uuid not null references public.products(id),
  quantity     integer not null check (quantity > 0),
  import_price numeric(12,0) not null check (import_price >= 0),
  lot_no       text,
  expiry_date  date,
  primary key (receipt_id, product_id)
);

-- ---------------------------------------------------------------------
-- 5. QUY TRÌNH: CỬA HÀNG YÊU CẦU -> KHO DUYỆT -> KHO XUẤT -> CỬA HÀNG NHẬN
-- ---------------------------------------------------------------------
create table public.restock_requests (
  id            uuid primary key default gen_random_uuid(),
  store_id      uuid not null references public.stores(id),
  warehouse_id  uuid not null references public.warehouses(id),
  status        public.request_status not null default 'pending',
  created_by    uuid not null references public.employees(id),
  approved_by   uuid references public.employees(id),
  note          text,
  reject_reason text,
  created_at    timestamptz not null default now(),
  approved_at   timestamptz
);

create table public.restock_request_items (
  request_id          uuid not null references public.restock_requests(id) on delete cascade,
  product_id          uuid not null references public.products(id),
  requested_quantity  integer not null check (requested_quantity > 0),
  approved_quantity   integer check (approved_quantity >= 0),
  primary key (request_id, product_id)
);

create table public.stock_exports (
  id            uuid primary key default gen_random_uuid(),
  request_id    uuid not null unique references public.restock_requests(id),  -- 1 yêu cầu = 1 phiếu xuất
  warehouse_id  uuid not null references public.warehouses(id),
  store_id      uuid not null references public.stores(id),
  status        public.export_status not null default 'shipping',
  created_by    uuid not null references public.employees(id),
  received_by   uuid references public.employees(id),
  created_at    timestamptz not null default now(),
  received_at   timestamptz
);

create table public.stock_export_items (
  export_id   uuid not null references public.stock_exports(id) on delete cascade,
  product_id  uuid not null references public.products(id),
  quantity    integer not null check (quantity > 0),
  primary key (export_id, product_id)
);

-- ---------------------------------------------------------------------
-- 6. KHÁCH HÀNG VÀ BÁN HÀNG
-- ---------------------------------------------------------------------
create table public.customers (
  id          uuid primary key default gen_random_uuid(),
  full_name   text not null,
  phone       text unique,
  points      integer not null default 0,
  created_at  timestamptz not null default now()
);

create sequence public.invoice_seq;

create table public.invoices (
  id              uuid primary key default gen_random_uuid(),
  code            text not null unique
                  default ('HD' || to_char(now(), 'YYMMDD') || lpad(nextval('public.invoice_seq')::text, 6, '0')),
  store_id        uuid not null references public.stores(id),
  cashier_id      uuid not null default auth.uid() references public.employees(id),
  customer_id     uuid references public.customers(id),   -- null = khách vãng lai
  payment_method  public.payment_method not null default 'cash',
  subtotal        numeric(14,0) not null default 0,
  discount        numeric(14,0) not null default 0 check (discount >= 0),
  total_amount    numeric(14,0) generated always as (subtotal - discount) stored,
  created_at      timestamptz not null default now()
);

create table public.invoice_items (
  invoice_id  uuid not null references public.invoices(id) on delete cascade,
  product_id  uuid not null references public.products(id),
  quantity    integer not null check (quantity > 0),
  unit_price  numeric(12,0) check (unit_price >= 0),   -- để trống sẽ tự lấy products.price
  primary key (invoice_id, product_id)
);

-- ---------------------------------------------------------------------
-- 7. INDEX CHO KHOÁ NGOẠI
-- ---------------------------------------------------------------------
create index on public.employees (store_id);
create index on public.employees (warehouse_id);
create index on public.products (category_id);
create index on public.products (brand_id);
create index on public.store_stock (product_id);
create index on public.warehouse_stock (product_id);
create index on public.stock_alerts (store_id, status);
create index on public.goods_receipts (warehouse_id, created_at desc);
create index on public.restock_requests (store_id, status);
create index on public.restock_requests (warehouse_id, status);
create index on public.stock_exports (store_id, status);
create index on public.invoices (store_id, created_at desc);
create index on public.invoice_items (product_id);

-- ---------------------------------------------------------------------
-- 8. HÀM HỖ TRỢ PHÂN QUYỀN
-- ---------------------------------------------------------------------
create or replace function public.app_role()
returns public.user_role
language sql stable security definer set search_path = public as $$
  select role from public.employees where id = auth.uid() and is_active
$$;

create or replace function public.my_store_id()
returns uuid
language sql stable security definer set search_path = public as $$
  select store_id from public.employees where id = auth.uid() and is_active
$$;

create or replace function public.my_warehouse_id()
returns uuid
language sql stable security definer set search_path = public as $$
  select warehouse_id from public.employees where id = auth.uid() and is_active
$$;

create or replace function public.is_admin()
returns boolean
language sql stable security definer set search_path = public as $$
  select coalesce(public.app_role() = 'admin', false)
$$;

-- ---------------------------------------------------------------------
-- 9. TỰ TẠO HỒ SƠ NHÂN VIÊN KHI CÓ USER MỚI
--    (mặc định chưa kích hoạt; admin gán vai trò, cửa hàng/kho rồi bật is_active)
-- ---------------------------------------------------------------------
create or replace function public.handle_new_user()
returns trigger
language plpgsql security definer set search_path = public as $$
begin
  insert into public.employees (id, full_name, email, phone)
  values (
    new.id,
    coalesce(new.raw_user_meta_data->>'full_name', new.email, 'Chưa đặt tên'),
    new.email,
    new.raw_user_meta_data->>'phone'
  );
  return new;
end $$;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- ---------------------------------------------------------------------
-- 10. TRIGGER: NHẬP KHO TỪ NHÀ CUNG CẤP
-- ---------------------------------------------------------------------
create or replace function public.trg_goods_receipt_item_after_insert()
returns trigger
language plpgsql security definer set search_path = public as $$
declare v_wh uuid;
begin
  select warehouse_id into v_wh from public.goods_receipts where id = new.receipt_id;

  insert into public.warehouse_stock (warehouse_id, product_id, quantity)
  values (v_wh, new.product_id, new.quantity)
  on conflict (warehouse_id, product_id)
  do update set quantity = public.warehouse_stock.quantity + excluded.quantity,
                updated_at = now();

  update public.products set import_price = new.import_price where id = new.product_id;

  update public.goods_receipts
     set total_amount = total_amount + new.quantity * new.import_price
   where id = new.receipt_id;

  return new;
end $$;

create trigger goods_receipt_item_after_insert
  after insert on public.goods_receipt_items
  for each row execute function public.trg_goods_receipt_item_after_insert();

-- ---------------------------------------------------------------------
-- 11. TRIGGER: BÁN HÀNG (trừ tồn cửa hàng, tính tổng tiền)
-- ---------------------------------------------------------------------
create or replace function public.trg_invoice_item_before_insert()
returns trigger
language plpgsql as $$
begin
  if new.unit_price is null then
    select price into new.unit_price from public.products where id = new.product_id;
  end if;
  return new;
end $$;

create trigger invoice_item_before_insert
  before insert on public.invoice_items
  for each row execute function public.trg_invoice_item_before_insert();

create or replace function public.trg_invoice_item_after_insert()
returns trigger
language plpgsql security definer set search_path = public as $$
declare v_store uuid;
begin
  select store_id into v_store from public.invoices where id = new.invoice_id;

  update public.store_stock
     set quantity = quantity - new.quantity, updated_at = now()
   where store_id = v_store
     and product_id = new.product_id
     and quantity >= new.quantity;

  if not found then
    raise exception 'Không đủ hàng tại cửa hàng cho sản phẩm %', new.product_id;
  end if;

  update public.invoices
     set subtotal = subtotal + new.quantity * new.unit_price
   where id = new.invoice_id;

  return new;
end $$;

create trigger invoice_item_after_insert
  after insert on public.invoice_items
  for each row execute function public.trg_invoice_item_after_insert();

-- ---------------------------------------------------------------------
-- 12. TRIGGER: CẢNH BÁO SẮP HẾT HÀNG TRÊN KỆ
-- ---------------------------------------------------------------------
create or replace function public.trg_store_stock_low_alert()
returns trigger
language plpgsql security definer set search_path = public as $$
begin
  if new.quantity <= new.min_quantity then
    insert into public.stock_alerts (store_id, product_id, current_qty)
    values (new.store_id, new.product_id, new.quantity)
    on conflict (store_id, product_id) where status = 'open'
    do update set current_qty = excluded.current_qty;
  end if;
  return new;
end $$;

create trigger store_stock_low_alert
  after insert or update of quantity, min_quantity on public.store_stock
  for each row execute function public.trg_store_stock_low_alert();

-- ---------------------------------------------------------------------
-- 13. HÀM NGHIỆP VỤ (gọi bằng supabase.rpc)
-- ---------------------------------------------------------------------

-- 13.1 Cửa hàng tạo phiếu yêu cầu nhập hàng
-- p_items ví dụ: [{"product_id":"...","quantity":20}, ...]
create or replace function public.create_restock_request(
  p_store_id uuid, p_warehouse_id uuid, p_items jsonb, p_note text default null
) returns uuid
language plpgsql security definer set search_path = public as $$
declare v_id uuid;
begin
  if not (public.is_admin()
          or (public.app_role() in ('store_manager','sales') and public.my_store_id() = p_store_id)) then
    raise exception 'Bạn không có quyền tạo phiếu cho cửa hàng này';
  end if;

  if p_items is null or jsonb_typeof(p_items) <> 'array' or jsonb_array_length(p_items) = 0 then
    raise exception 'Phiếu yêu cầu phải có ít nhất 1 sản phẩm';
  end if;

  insert into public.restock_requests (store_id, warehouse_id, created_by, note)
  values (p_store_id, p_warehouse_id, auth.uid(), p_note)
  returning id into v_id;

  insert into public.restock_request_items (request_id, product_id, requested_quantity)
  select v_id, (i->>'product_id')::uuid, (i->>'quantity')::int
  from jsonb_array_elements(p_items) i;

  return v_id;
end $$;

-- 13.2 Kho duyệt phiếu (có thể truyền số lượng duyệt khác số yêu cầu)
create or replace function public.approve_restock_request(
  p_request_id uuid, p_items jsonb default null
) returns void
language plpgsql security definer set search_path = public as $$
declare v_req public.restock_requests;
begin
  select * into v_req from public.restock_requests where id = p_request_id for update;
  if not found then raise exception 'Không tìm thấy phiếu yêu cầu'; end if;

  if not (public.is_admin()
          or (public.app_role() in ('kho_manager','kho')
              and public.my_warehouse_id() = v_req.warehouse_id)) then
    raise exception 'Bạn không có quyền duyệt phiếu này';
  end if;

  if v_req.status <> 'pending' then
    raise exception 'Phiếu không ở trạng thái chờ duyệt';
  end if;

  update public.restock_request_items
     set approved_quantity = requested_quantity
   where request_id = p_request_id;

  if p_items is not null then
    update public.restock_request_items i
       set approved_quantity = (x->>'quantity')::int
      from jsonb_array_elements(p_items) x
     where i.request_id = p_request_id
       and i.product_id = (x->>'product_id')::uuid;
  end if;

  update public.restock_requests
     set status = 'approved', approved_by = auth.uid(), approved_at = now()
   where id = p_request_id;
end $$;

-- 13.3 Kho từ chối phiếu
create or replace function public.reject_restock_request(
  p_request_id uuid, p_reason text default null
) returns void
language plpgsql security definer set search_path = public as $$
declare v_req public.restock_requests;
begin
  select * into v_req from public.restock_requests where id = p_request_id for update;
  if not found then raise exception 'Không tìm thấy phiếu yêu cầu'; end if;

  if not (public.is_admin()
          or (public.app_role() in ('kho_manager','kho')
              and public.my_warehouse_id() = v_req.warehouse_id)) then
    raise exception 'Bạn không có quyền từ chối phiếu này';
  end if;

  if v_req.status <> 'pending' then
    raise exception 'Phiếu không ở trạng thái chờ duyệt';
  end if;

  update public.restock_requests
     set status = 'rejected', reject_reason = p_reason,
         approved_by = auth.uid(), approved_at = now()
   where id = p_request_id;
end $$;

-- 13.4 Kho xuất hàng cho phiếu đã duyệt (trừ tồn kho)
create or replace function public.create_stock_export(p_request_id uuid)
returns uuid
language plpgsql security definer set search_path = public as $$
declare
  v_req    public.restock_requests;
  v_export uuid;
  r        record;
begin
  select * into v_req from public.restock_requests where id = p_request_id for update;
  if not found then raise exception 'Không tìm thấy phiếu yêu cầu'; end if;

  if not (public.is_admin()
          or (public.app_role() in ('kho_manager','kho')
              and public.my_warehouse_id() = v_req.warehouse_id)) then
    raise exception 'Bạn không có quyền xuất kho cho phiếu này';
  end if;

  if v_req.status <> 'approved' then
    raise exception 'Chỉ xuất kho được cho phiếu đã duyệt';
  end if;

  if not exists (select 1 from public.restock_request_items
                  where request_id = p_request_id and coalesce(approved_quantity, 0) > 0) then
    raise exception 'Phiếu không có sản phẩm nào được duyệt';
  end if;

  insert into public.stock_exports (request_id, warehouse_id, store_id, created_by)
  values (p_request_id, v_req.warehouse_id, v_req.store_id, auth.uid())
  returning id into v_export;

  for r in
    select product_id, approved_quantity
      from public.restock_request_items
     where request_id = p_request_id and coalesce(approved_quantity, 0) > 0
  loop
    update public.warehouse_stock
       set quantity = quantity - r.approved_quantity, updated_at = now()
     where warehouse_id = v_req.warehouse_id
       and product_id = r.product_id
       and quantity >= r.approved_quantity;

    if not found then
      raise exception 'Kho không đủ hàng cho sản phẩm %', r.product_id;
    end if;

    insert into public.stock_export_items (export_id, product_id, quantity)
    values (v_export, r.product_id, r.approved_quantity);
  end loop;

  update public.restock_requests set status = 'exported' where id = p_request_id;

  return v_export;
end $$;

-- 13.5 Cửa hàng xác nhận đã nhận hàng (cộng tồn cửa hàng, đóng cảnh báo)
create or replace function public.confirm_stock_receipt(p_export_id uuid)
returns void
language plpgsql security definer set search_path = public as $$
declare v_exp public.stock_exports;
begin
  select * into v_exp from public.stock_exports where id = p_export_id for update;
  if not found then raise exception 'Không tìm thấy phiếu xuất'; end if;

  if not (public.is_admin()
          or (public.app_role() in ('store_manager','sales')
              and public.my_store_id() = v_exp.store_id)) then
    raise exception 'Bạn không có quyền xác nhận phiếu này';
  end if;

  if v_exp.status <> 'shipping' then
    raise exception 'Phiếu xuất đã được xác nhận trước đó';
  end if;

  insert into public.store_stock (store_id, product_id, quantity)
  select v_exp.store_id, product_id, quantity
    from public.stock_export_items
   where export_id = p_export_id
  on conflict (store_id, product_id)
  do update set quantity = public.store_stock.quantity + excluded.quantity,
                updated_at = now();

  update public.stock_alerts a
     set status = 'resolved', resolved_at = now()
   where a.store_id = v_exp.store_id
     and a.status = 'open'
     and a.product_id in (select product_id from public.stock_export_items where export_id = p_export_id)
     and exists (select 1 from public.store_stock s
                  where s.store_id = a.store_id and s.product_id = a.product_id
                    and s.quantity > s.min_quantity);

  update public.stock_exports
     set status = 'delivered', received_by = auth.uid(), received_at = now()
   where id = p_export_id;

  update public.restock_requests set status = 'received' where id = v_exp.request_id;
end $$;

-- Chỉ user đã đăng nhập mới gọi được các hàm nghiệp vụ
revoke all on function public.create_restock_request(uuid, uuid, jsonb, text) from public, anon;
revoke all on function public.approve_restock_request(uuid, jsonb)            from public, anon;
revoke all on function public.reject_restock_request(uuid, text)              from public, anon;
revoke all on function public.create_stock_export(uuid)                       from public, anon;
revoke all on function public.confirm_stock_receipt(uuid)                     from public, anon;
grant execute on function public.create_restock_request(uuid, uuid, jsonb, text) to authenticated;
grant execute on function public.approve_restock_request(uuid, jsonb)            to authenticated;
grant execute on function public.reject_restock_request(uuid, text)              to authenticated;
grant execute on function public.create_stock_export(uuid)                       to authenticated;
grant execute on function public.confirm_stock_receipt(uuid)                     to authenticated;

-- ---------------------------------------------------------------------
-- 14. ROW LEVEL SECURITY
-- ---------------------------------------------------------------------
-- Bật RLS + admin toàn quyền trên mọi bảng
do $$
declare t text;
begin
  for t in select tablename from pg_tables where schemaname = 'public' loop
    execute format('alter table public.%I enable row level security', t);
    execute format(
      'create policy "admin full access" on public.%I for all to authenticated
         using (public.is_admin()) with check (public.is_admin())', t);
  end loop;
end $$;

-- Dữ liệu tham chiếu: ai đăng nhập cũng xem được (admin ghi)
create policy "read categories" on public.categories for select to authenticated using (true);
create policy "read brands"     on public.brands     for select to authenticated using (true);
create policy "read products"   on public.products   for select to authenticated using (true);
create policy "read suppliers"  on public.suppliers  for select to authenticated using (true);
create policy "read stores"     on public.stores     for select to authenticated using (true);
create policy "read warehouses" on public.warehouses for select to authenticated using (true);

-- Nhân viên: xem hồ sơ mình và đồng nghiệp cùng cửa hàng / kho
create policy "read employees" on public.employees for select to authenticated
  using (id = auth.uid()
         or (store_id is not null     and store_id     = public.my_store_id())
         or (warehouse_id is not null and warehouse_id = public.my_warehouse_id()));

-- Tồn kho: kho xem tồn kho mình; mọi người xem được tồn kho để tạo yêu cầu
create policy "read warehouse stock" on public.warehouse_stock for select to authenticated using (true);

-- Tồn cửa hàng: xem tồn của cửa hàng mình; quản lý cửa hàng chỉnh ngưỡng cảnh báo
create policy "read store stock" on public.store_stock for select to authenticated
  using (store_id = public.my_store_id());
create policy "manager insert store stock" on public.store_stock for insert to authenticated
  with check (store_id = public.my_store_id() and public.app_role() = 'store_manager');
create policy "manager update store stock" on public.store_stock for update to authenticated
  using (store_id = public.my_store_id() and public.app_role() = 'store_manager')
  with check (store_id = public.my_store_id());

-- Cảnh báo: xem cảnh báo của cửa hàng mình
create policy "read alerts" on public.stock_alerts for select to authenticated
  using (store_id = public.my_store_id());

-- Nhập kho từ NCC: nhân viên kho của kho đó
create policy "warehouse read receipts" on public.goods_receipts for select to authenticated
  using (warehouse_id = public.my_warehouse_id());
create policy "warehouse insert receipts" on public.goods_receipts for insert to authenticated
  with check (warehouse_id = public.my_warehouse_id()
              and public.app_role() in ('kho_manager','kho'));

create policy "warehouse read receipt items" on public.goods_receipt_items for select to authenticated
  using (exists (select 1 from public.goods_receipts r
                  where r.id = receipt_id and r.warehouse_id = public.my_warehouse_id()));
create policy "warehouse insert receipt items" on public.goods_receipt_items for insert to authenticated
  with check (exists (select 1 from public.goods_receipts r
                       where r.id = receipt_id and r.warehouse_id = public.my_warehouse_id())
              and public.app_role() in ('kho_manager','kho'));

-- Phiếu yêu cầu / phiếu xuất: bên tạo và bên nhận đều xem được (ghi qua hàm RPC)
create policy "read restock requests" on public.restock_requests for select to authenticated
  using (store_id = public.my_store_id() or warehouse_id = public.my_warehouse_id());
create policy "read restock items" on public.restock_request_items for select to authenticated
  using (exists (select 1 from public.restock_requests r
                  where r.id = request_id
                    and (r.store_id = public.my_store_id() or r.warehouse_id = public.my_warehouse_id())));

create policy "read stock exports" on public.stock_exports for select to authenticated
  using (store_id = public.my_store_id() or warehouse_id = public.my_warehouse_id());
create policy "read export items" on public.stock_export_items for select to authenticated
  using (exists (select 1 from public.stock_exports e
                  where e.id = export_id
                    and (e.store_id = public.my_store_id() or e.warehouse_id = public.my_warehouse_id())));

-- Bán hàng: nhân viên cửa hàng lập và xem hoá đơn của cửa hàng mình
create policy "store read invoices" on public.invoices for select to authenticated
  using (store_id = public.my_store_id());
create policy "store insert invoices" on public.invoices for insert to authenticated
  with check (store_id = public.my_store_id()
              and cashier_id = auth.uid()
              and public.app_role() in ('store_manager','sales'));

create policy "store read invoice items" on public.invoice_items for select to authenticated
  using (exists (select 1 from public.invoices i
                  where i.id = invoice_id and i.store_id = public.my_store_id()));
create policy "store insert invoice items" on public.invoice_items for insert to authenticated
  with check (exists (select 1 from public.invoices i
                       where i.id = invoice_id and i.store_id = public.my_store_id()));

-- Khách hàng: nhân viên cửa hàng được xem / thêm / sửa
create policy "store read customers"   on public.customers for select to authenticated
  using (public.app_role() in ('store_manager','sales'));
create policy "store insert customers" on public.customers for insert to authenticated
  with check (public.app_role() in ('store_manager','sales'));
create policy "store update customers" on public.customers for update to authenticated
  using (public.app_role() in ('store_manager','sales'));

-- ---------------------------------------------------------------------
-- 15. REALTIME (đẩy cảnh báo / phiếu mới về app)
-- ---------------------------------------------------------------------
alter publication supabase_realtime add table
  public.stock_alerts, public.restock_requests, public.stock_exports;

-- ---------------------------------------------------------------------
-- 16. DỮ LIỆU MẪU (tuỳ chọn) - lấy từ sản phẩm trong ảnh của bạn
-- ---------------------------------------------------------------------
insert into public.categories (name) values ('Nước giải khát') on conflict do nothing;
insert into public.brands (name)     values ('TEA+')           on conflict do nothing;

insert into public.products
  (barcode, name, description, image_url, price, old_price, import_price, package_size, rating, category_id, brand_id)
select '8934588763557', 'Banh Bong Lan CUon', 'Nt dien', 'assets/images/logosieut',
       15000, 17250, 10000, 'Chai', 5,
       (select id from public.categories where name = 'Nước giải khát'),
       (select id from public.brands where name = 'TEA+')
on conflict (barcode) do nothing;

-- =====================================================================
-- SAU KHI CHẠY: TẠO TÀI KHOẢN ADMIN ĐẦU TIÊN
-- 1) Authentication > Users > Add user (tạo user trong dashboard)
-- 2) Chạy lệnh dưới (thay email của bạn):
--
--   update public.employees
--      set role = 'admin', is_active = true
--    where email = 'admin@example.com';
-- =====================================================================