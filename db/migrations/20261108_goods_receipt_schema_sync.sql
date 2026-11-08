-- Align the live PostgreSQL goods-receipt schema with the JPA mappings.
-- Existing receipts are deliberately left with NULL status when the old
-- table had no status column; classify those rows only after reconciling
-- inventory history. New inserts use the draft default.

BEGIN;

DO $$
BEGIN
    IF to_regtype('public.goods_receipt_status') IS NULL THEN
        CREATE TYPE public.goods_receipt_status AS ENUM ('draft', 'completed', 'cancelled');
    END IF;
END
$$;

DO $$
DECLARE
    type_kind "char";
    missing_labels text[];
BEGIN
    SELECT t.typtype
      INTO type_kind
      FROM pg_type t
      JOIN pg_namespace n ON n.oid = t.typnamespace
     WHERE n.nspname = 'public'
       AND t.typname = 'goods_receipt_status';

    IF type_kind IS DISTINCT FROM 'e' THEN
        RAISE EXCEPTION 'public.goods_receipt_status must be a PostgreSQL enum';
    END IF;

    SELECT array_agg(required.label ORDER BY required.label)
      INTO missing_labels
      FROM unnest(ARRAY['draft', 'completed', 'cancelled']) AS required(label)
     WHERE NOT EXISTS (
         SELECT 1
           FROM pg_enum e
           JOIN pg_type t ON t.oid = e.enumtypid
           JOIN pg_namespace n ON n.oid = t.typnamespace
          WHERE n.nspname = 'public'
            AND t.typname = 'goods_receipt_status'
            AND e.enumlabel = required.label
     );

    IF missing_labels IS NOT NULL THEN
        RAISE EXCEPTION
            'public.goods_receipt_status is missing required values: %',
            missing_labels;
    END IF;
END
$$;

ALTER TABLE public.goods_receipts
    ADD COLUMN IF NOT EXISTS status public.goods_receipt_status;

ALTER TABLE public.goods_receipts
    ADD COLUMN IF NOT EXISTS completed_at timestamptz;

ALTER TABLE public.goods_receipts
    ADD COLUMN IF NOT EXISTS cancelled_at timestamptz;

ALTER TABLE public.goods_receipt_items
    ADD COLUMN IF NOT EXISTS lot_no text;

ALTER TABLE public.goods_receipt_items
    ADD COLUMN IF NOT EXISTS expiry_date date;

-- Preserve status values while normalizing an older text/enum representation.
-- Stop rather than guessing if any historical value is not one of the known
-- SmartMart states.
DO $$
DECLARE
    status_schema text;
    status_type text;
    invalid_values text[];
BEGIN
    SELECT n.nspname, t.typname
      INTO status_schema, status_type
      FROM pg_attribute a
      JOIN pg_class c ON c.oid = a.attrelid
      JOIN pg_namespace cn ON cn.oid = c.relnamespace
      JOIN pg_type t ON t.oid = a.atttypid
      JOIN pg_namespace n ON n.oid = t.typnamespace
     WHERE cn.nspname = 'public'
       AND c.relname = 'goods_receipts'
       AND a.attname = 'status'
       AND NOT a.attisdropped;

    IF status_schema IS DISTINCT FROM 'public'
       OR status_type IS DISTINCT FROM 'goods_receipt_status' THEN
        SELECT array_agg(DISTINCT status::text ORDER BY status::text)
          INTO invalid_values
          FROM public.goods_receipts
         WHERE status IS NOT NULL
           AND status::text NOT IN ('draft', 'completed', 'cancelled');

        IF invalid_values IS NOT NULL THEN
            RAISE EXCEPTION
                'Cannot migrate goods_receipts.status; unsupported existing values: %',
                invalid_values;
        END IF;

        ALTER TABLE public.goods_receipts
            ALTER COLUMN status DROP DEFAULT;
        ALTER TABLE public.goods_receipts
            ALTER COLUMN status TYPE public.goods_receipt_status
            USING status::text::public.goods_receipt_status;
    END IF;
END
$$;

ALTER TABLE public.goods_receipts
    ALTER COLUMN status SET DEFAULT 'draft'::public.goods_receipt_status;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM pg_constraint
         WHERE conrelid = 'public.goods_receipts'::regclass
           AND conname = 'goods_receipts_status_check'
    ) THEN
        ALTER TABLE public.goods_receipts
            ADD CONSTRAINT goods_receipts_status_check
            CHECK (status IS NULL OR status IN ('draft', 'completed', 'cancelled'))
            NOT VALID;
    END IF;
END
$$;

ALTER TABLE public.goods_receipts
    VALIDATE CONSTRAINT goods_receipts_status_check;

-- Stock is applied by the Spring transaction when a receipt is completed.
-- Remove known legacy/database-side stock triggers to avoid adding it twice.
DROP TRIGGER IF EXISTS goods_receipt_item_after_insert ON public.goods_receipt_items;
DROP TRIGGER IF EXISTS goods_receipt_after_status_complete ON public.goods_receipts;

-- Fail with a useful diagnostic if any other mapped columns are absent or
-- have incompatible PostgreSQL types. No historical business data is changed.
DO $$
DECLARE
    expected record;
    actual_type text;
BEGIN
    FOR expected IN
        SELECT *
          FROM (VALUES
              ('goods_receipts', 'id', 'uuid'),
              ('goods_receipts', 'warehouse_id', 'uuid'),
              ('goods_receipts', 'supplier_id', 'uuid'),
              ('goods_receipts', 'created_by', 'uuid'),
              ('goods_receipts', 'status', 'goods_receipt_status'),
              ('goods_receipts', 'total_amount', 'numeric'),
              ('goods_receipts', 'note', 'text'),
              ('goods_receipts', 'created_at', 'timestamptz'),
              ('goods_receipts', 'completed_at', 'timestamptz'),
              ('goods_receipts', 'cancelled_at', 'timestamptz'),
              ('goods_receipt_items', 'receipt_id', 'uuid'),
              ('goods_receipt_items', 'product_id', 'uuid'),
              ('goods_receipt_items', 'quantity', 'integer'),
              ('goods_receipt_items', 'import_price', 'numeric'),
              ('goods_receipt_items', 'lot_no', 'text'),
              ('goods_receipt_items', 'expiry_date', 'date')
          ) AS columns_to_check(table_name, column_name, expected_type)
    LOOP
        SELECT CASE
                   WHEN c.udt_name = 'timestamptz' THEN 'timestamptz'
                   WHEN c.udt_name = 'int4' THEN 'integer'
                   ELSE c.udt_name
               END
          INTO actual_type
          FROM information_schema.columns c
         WHERE c.table_schema = 'public'
           AND c.table_name = expected.table_name
           AND c.column_name = expected.column_name;

        IF actual_type IS NULL THEN
            RAISE EXCEPTION 'Missing required mapped column public.%.%',
                expected.table_name, expected.column_name;
        END IF;
        IF actual_type IS DISTINCT FROM expected.expected_type THEN
            RAISE EXCEPTION
                'Incompatible type for public.%.%: expected %, found %',
                expected.table_name, expected.column_name,
                expected.expected_type, actual_type;
        END IF;
    END LOOP;
END
$$;

COMMIT;

SELECT table_name,
       column_name,
       data_type,
       udt_name,
       is_nullable,
       column_default
  FROM information_schema.columns
 WHERE table_schema = 'public'
   AND table_name IN ('goods_receipts', 'goods_receipt_items')
 ORDER BY table_name, ordinal_position;

SELECT tc.table_name,
       tc.constraint_name,
       tc.constraint_type,
       pg_get_constraintdef(c.oid) AS definition
  FROM information_schema.table_constraints tc
  JOIN pg_constraint c
    ON c.conname = tc.constraint_name
   AND c.conrelid = (quote_ident(tc.table_schema) || '.' || quote_ident(tc.table_name))::regclass
 WHERE tc.table_schema = 'public'
   AND tc.table_name IN ('goods_receipts', 'goods_receipt_items')
 ORDER BY tc.table_name, tc.constraint_type, tc.constraint_name;

-- Review unclassified legacy receipts before assigning a state.
SELECT status, count(*) AS receipt_count
  FROM public.goods_receipts
 GROUP BY status
 ORDER BY status NULLS FIRST;
