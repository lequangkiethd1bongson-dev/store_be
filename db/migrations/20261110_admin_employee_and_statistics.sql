-- =====================================================================
-- MIGRATION: QUẢN LÝ CHẤM CÔNG NHÂN VIÊN, BẢNG CÔNG & THỐNG KÊ CƠ SỞ
-- Dự án: LTDD_04 (Supabase PostgreSQL)
-- =====================================================================

-- 1. Bảng phân ca & chấm công theo ngày của từng nhân viên tại cơ sở
create table if not exists public.employee_shifts (
  id            uuid primary key default gen_random_uuid(),
  employee_id   uuid not null references public.employees(id) on delete cascade,
  work_date     date not null,
  shift_code    text not null default 'SHIFT_1', -- 'SHIFT_1' | 'SHIFT_2' | 'OFF'
  store_id      uuid references public.stores(id) on delete set null,
  warehouse_id  uuid references public.warehouses(id) on delete set null,
  note          text,
  updated_at    timestamptz not null default now(),
  constraint employee_shifts_unique_emp_date unique (employee_id, work_date)
);

create index if not exists idx_employee_shifts_work_date
  on public.employee_shifts (work_date);
create index if not exists idx_employee_shifts_employee_id
  on public.employee_shifts (employee_id);
create index if not exists idx_employee_shifts_store_id
  on public.employee_shifts (store_id);
create index if not exists idx_employee_shifts_warehouse_id
  on public.employee_shifts (warehouse_id);

-- 2. Bảng tổng hợp ngày công & lương theo tháng của nhân viên
create table if not exists public.employee_timesheets (
  id              uuid primary key default gen_random_uuid(),
  employee_id     uuid not null references public.employees(id) on delete cascade,
  month_year      text not null, -- 'YYYY-MM'
  work_days       integer not null default 22 check (work_days >= 0),
  standard_days   integer not null default 26 check (standard_days > 0),
  total_hours     numeric(8,2) not null default 176 check (total_hours >= 0),
  overtime_hours  numeric(8,2) not null default 0 check (overtime_hours >= 0),
  hourly_rate     integer not null default 35000 check (hourly_rate >= 0),
  allowance       integer not null default 500000 check (allowance >= 0),
  deduction       integer not null default 0 check (deduction >= 0),
  total_salary    integer not null default 6660000 check (total_salary >= 0),
  status          text not null default 'PENDING', -- 'PENDING' | 'APPROVED' | 'REJECTED'
  approved_by     uuid references public.employees(id) on delete set null,
  approved_at     timestamptz,
  note            text,
  updated_at      timestamptz not null default now(),
  constraint employee_timesheets_unique_emp_month unique (employee_id, month_year)
);

create index if not exists idx_employee_timesheets_month_year
  on public.employee_timesheets (month_year);
create index if not exists idx_employee_timesheets_employee_id
  on public.employee_timesheets (employee_id);

-- 3. Bật RLS & chính sách phân quyền trên Supabase
alter table public.employee_shifts enable row level security;
alter table public.employee_timesheets enable row level security;

drop policy if exists "admin full access on employee_shifts" on public.employee_shifts;
create policy "admin full access on employee_shifts"
  on public.employee_shifts for all to authenticated
  using (public.is_admin())
  with check (public.is_admin());

drop policy if exists "read employee_shifts" on public.employee_shifts;
create policy "read employee_shifts"
  on public.employee_shifts for select to authenticated
  using (employee_id = (select auth.uid()) or public.is_admin());

drop policy if exists "admin full access on employee_timesheets" on public.employee_timesheets;
create policy "admin full access on employee_timesheets"
  on public.employee_timesheets for all to authenticated
  using (public.is_admin())
  with check (public.is_admin());

drop policy if exists "read employee_timesheets" on public.employee_timesheets;
create policy "read employee_timesheets"
  on public.employee_timesheets for select to authenticated
  using (employee_id = (select auth.uid()) or public.is_admin());

