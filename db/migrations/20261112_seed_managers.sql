-- =============================================================================
-- SQL TẠO TÀI KHOẢN QUẢN LÝ CỬA HÀNG VÀ QUẢN LÝ KHO HÀNG TRÊN SUPABASE
-- Bạn mở Supabase Dashboard -> chọn SQL Editor -> dán toàn bộ đoạn này và bấm RUN
-- =============================================================================

-- 1. Bật extension pgcrypto (nếu chưa có) để mã hóa mật khẩu
CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA extensions;

DO $$
DECLARE
    v_store_id uuid;
    v_warehouse_id uuid;
    v_store_mgr_id uuid := gen_random_uuid();
    v_kho_mgr_id uuid := gen_random_uuid();
    v_store_mgr_email text := 'quanlycuahang@ssm4.vn';
    v_kho_mgr_email text := 'quanlykho@ssm4.vn';
    v_default_password text := '123456';
BEGIN
    -- Lấy ID cửa hàng đầu tiên (hoặc tạo mới nếu bảng stores trống)
    SELECT id INTO v_store_id FROM public.stores WHERE is_active = true ORDER BY created_at ASC LIMIT 1;
    IF v_store_id IS NULL THEN
        INSERT INTO public.stores (name, address, phone, is_active)
        VALUES ('Cửa hàng SSM4 - Quận 1', '123 Lê Lợi, Phường Bến Nghé, Quận 1, TP.HCM', '0901234567', true)
        RETURNING id INTO v_store_id;
    END IF;

    -- Lấy ID kho hàng đầu tiên (hoặc tạo mới nếu bảng warehouses trống)
    SELECT id INTO v_warehouse_id FROM public.warehouses WHERE is_active = true ORDER BY created_at ASC LIMIT 1;
    IF v_warehouse_id IS NULL THEN
        INSERT INTO public.warehouses (name, address, phone, is_active)
        VALUES ('Kho Trung Tâm Thủ Đức', 'KCN Sóng Thần, Đại lộ Độc Lập, TP. Thủ Đức', '0909888777', true)
        RETURNING id INTO v_warehouse_id;
    END IF;

    -- -------------------------------------------------------------------------
    -- TÀI KHOẢN 1: QUẢN LÝ CỬA HÀNG (role: store_manager)
    -- Email: quanlycuahang@ssm4.vn | Mật khẩu: 123456
    -- -------------------------------------------------------------------------
    -- Xóa các liên kết ca làm, bảng công, người quản lý cũ nếu tài khoản đã tồn tại
    UPDATE public.stores SET manager_id = NULL WHERE manager_id IN (SELECT id FROM auth.users WHERE email = v_store_mgr_email);
    DELETE FROM public.employee_shifts WHERE employee_id IN (SELECT id FROM auth.users WHERE email = v_store_mgr_email);
    DELETE FROM public.employee_timesheets WHERE employee_id IN (SELECT id FROM auth.users WHERE email = v_store_mgr_email);
    DELETE FROM public.employees WHERE email = v_store_mgr_email;
    DELETE FROM auth.users WHERE email = v_store_mgr_email;

    -- Tạo user trong auth.users của Supabase
    INSERT INTO auth.users (
        instance_id,
        id,
        aud,
        role,
        email,
        encrypted_password,
        email_confirmed_at,
        raw_app_meta_data,
        raw_user_meta_data,
        created_at,
        updated_at,
        confirmation_token,
        recovery_token,
        email_change_token_new,
        email_change
    ) VALUES (
        '00000000-0000-0000-0000-000000000000',
        v_store_mgr_id,
        'authenticated',
        'authenticated',
        v_store_mgr_email,
        extensions.crypt(v_default_password, extensions.gen_salt('bf')),
        now(),
        '{"provider":"email","providers":["email"]}'::jsonb,
        jsonb_build_object('full_name', 'Quản Lý Cửa Hàng', 'phone', '0901234567'),
        now(),
        now(),
        '', '', '', ''
    );

    -- Tạo identity trong auth.identities
    BEGIN
        INSERT INTO auth.identities (
            id,
            user_id,
            identity_data,
            provider,
            provider_id,
            last_sign_in_at,
            created_at,
            updated_at
        ) VALUES (
            v_store_mgr_id::text,
            v_store_mgr_id,
            jsonb_build_object('sub', v_store_mgr_id::text, 'email', v_store_mgr_email),
            'email',
            v_store_mgr_email,
            now(),
            now(),
            now()
        ) ON CONFLICT DO NOTHING;
    EXCEPTION WHEN OTHERS THEN
        NULL;
    END;

    -- Upsert hồ sơ nhân viên trong public.employees
    INSERT INTO public.employees (
        id, full_name, email, phone, role, store_id, warehouse_id, is_active
    ) VALUES (
        v_store_mgr_id,
        'Quản Lý Cửa Hàng',
        v_store_mgr_email,
        '0901234567',
        'store_manager'::public.user_role,
        v_store_id,
        NULL,
        true
    ) ON CONFLICT (id) DO UPDATE SET
        full_name = EXCLUDED.full_name,
        email = EXCLUDED.email,
        phone = EXCLUDED.phone,
        role = EXCLUDED.role,
        store_id = EXCLUDED.store_id,
        warehouse_id = EXCLUDED.warehouse_id,
        is_active = EXCLUDED.is_active;

    -- Gán quản lý cho cửa hàng
    UPDATE public.stores SET manager_id = v_store_mgr_id WHERE id = v_store_id;

    -- -------------------------------------------------------------------------
    -- TÀI KHOẢN 2: QUẢN LÝ KHO HÀNG (role: kho_manager)
    -- Email: quanlykho@ssm4.vn | Mật khẩu: 123456
    -- -------------------------------------------------------------------------
    -- Xóa các liên kết ca làm, bảng công, người quản lý cũ nếu tài khoản đã tồn tại
    UPDATE public.warehouses SET manager_id = NULL WHERE manager_id IN (SELECT id FROM auth.users WHERE email = v_kho_mgr_email);
    DELETE FROM public.employee_shifts WHERE employee_id IN (SELECT id FROM auth.users WHERE email = v_kho_mgr_email);
    DELETE FROM public.employee_timesheets WHERE employee_id IN (SELECT id FROM auth.users WHERE email = v_kho_mgr_email);
    DELETE FROM public.employees WHERE email = v_kho_mgr_email;
    DELETE FROM auth.users WHERE email = v_kho_mgr_email;

    -- Tạo user trong auth.users của Supabase
    INSERT INTO auth.users (
        instance_id,
        id,
        aud,
        role,
        email,
        encrypted_password,
        email_confirmed_at,
        raw_app_meta_data,
        raw_user_meta_data,
        created_at,
        updated_at,
        confirmation_token,
        recovery_token,
        email_change_token_new,
        email_change
    ) VALUES (
        '00000000-0000-0000-0000-000000000000',
        v_kho_mgr_id,
        'authenticated',
        'authenticated',
        v_kho_mgr_email,
        extensions.crypt(v_default_password, extensions.gen_salt('bf')),
        now(),
        '{"provider":"email","providers":["email"]}'::jsonb,
        jsonb_build_object('full_name', 'Quản Lý Kho Hàng', 'phone', '0909888777'),
        now(),
        now(),
        '', '', '', ''
    );

    -- Tạo identity trong auth.identities
    BEGIN
        INSERT INTO auth.identities (
            id,
            user_id,
            identity_data,
            provider,
            provider_id,
            last_sign_in_at,
            created_at,
            updated_at
        ) VALUES (
            v_kho_mgr_id::text,
            v_kho_mgr_id,
            jsonb_build_object('sub', v_kho_mgr_id::text, 'email', v_kho_mgr_email),
            'email',
            v_kho_mgr_email,
            now(),
            now(),
            now()
        ) ON CONFLICT DO NOTHING;
    EXCEPTION WHEN OTHERS THEN
        NULL;
    END;

    -- Upsert hồ sơ nhân viên trong public.employees
    INSERT INTO public.employees (
        id, full_name, email, phone, role, store_id, warehouse_id, is_active
    ) VALUES (
        v_kho_mgr_id,
        'Quản Lý Kho Hàng',
        v_kho_mgr_email,
        '0909888777',
        'kho_manager'::public.user_role,
        NULL,
        v_warehouse_id,
        true
    ) ON CONFLICT (id) DO UPDATE SET
        full_name = EXCLUDED.full_name,
        email = EXCLUDED.email,
        phone = EXCLUDED.phone,
        role = EXCLUDED.role,
        store_id = EXCLUDED.store_id,
        warehouse_id = EXCLUDED.warehouse_id,
        is_active = EXCLUDED.is_active;

    -- Gán quản lý cho kho hàng
    UPDATE public.warehouses SET manager_id = v_kho_mgr_id WHERE id = v_warehouse_id;

    RAISE NOTICE 'Hoàn tất khởi tạo 2 tài khoản Quản lý Cửa hàng và Quản lý Kho trên Supabase!';
END $$;

