-- Keep the four known legacy receipts unclassified until their inventory
-- history is reconciled, while preventing any new NULL status values.
-- This migration does not change receipt status or warehouse stock.

BEGIN;

ALTER TABLE public.goods_receipts
    ALTER COLUMN status SET DEFAULT 'draft'::public.goods_receipt_status;

DO $$
DECLARE
    unexpected_null_count bigint;
BEGIN
    SELECT count(*)
      INTO unexpected_null_count
      FROM public.goods_receipts
     WHERE status IS NULL
        AND NOT (
            id IN (
                '719f83c0-f7d6-4ff7-a475-67b3007c109f'::uuid,
                '09cf40b6-f8c9-4afb-9599-fce77fda433c'::uuid,
                '28ebdb3b-c615-4b87-87a5-c21a9ff4898a'::uuid,
                'c7064587-0774-4831-87f5-8247e0a6fc73'::uuid
            )
            AND created_at = TIMESTAMPTZ '2026-10-21 08:45:37.800198+00'
        );

    IF unexpected_null_count > 0 THEN
        RAISE EXCEPTION
            'Found % unclassified goods receipts outside the approved legacy ID list; inspect before applying the NULL guard',
            unexpected_null_count;
    END IF;
END
$$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM pg_constraint
         WHERE conrelid = 'public.goods_receipts'::regclass
           AND conname = 'goods_receipts_status_null_guard'
    ) THEN
        ALTER TABLE public.goods_receipts
            ADD CONSTRAINT goods_receipts_status_null_guard
            CHECK (
                status IS NOT NULL
                OR (
                    id IN (
                        '719f83c0-f7d6-4ff7-a475-67b3007c109f'::uuid,
                        '09cf40b6-f8c9-4afb-9599-fce77fda433c'::uuid,
                        '28ebdb3b-c615-4b87-87a5-c21a9ff4898a'::uuid,
                        'c7064587-0774-4831-87f5-8247e0a6fc73'::uuid
                    )
                    AND created_at = TIMESTAMPTZ '2026-10-21 08:45:37.800198+00'
                )
            )
            NOT VALID;
    END IF;
END
$$;

ALTER TABLE public.goods_receipts
    VALIDATE CONSTRAINT goods_receipts_status_null_guard;

-- Completion stock changes are performed by the backend transaction. Retire
-- known database triggers from either historical trigger design to prevent a
-- second stock increment. No stock or receipt data is modified here.
DROP TRIGGER IF EXISTS goods_receipt_item_after_insert ON public.goods_receipt_items;
DROP TRIGGER IF EXISTS goods_receipt_after_status_complete ON public.goods_receipts;

-- A NOT NULL constraint is intentionally deferred while legacy NULL rows remain.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM public.goods_receipts WHERE status IS NULL
    ) THEN
        ALTER TABLE public.goods_receipts
            ALTER COLUMN status SET NOT NULL;
    ELSE
        RAISE NOTICE
            'goods_receipts.status remains nullable because legacy NULL receipts require manual reconciliation';
    END IF;
END
$$;

COMMIT;

SELECT id, created_at, status, completed_at, cancelled_at
  FROM public.goods_receipts
 WHERE id IN (
       '719f83c0-f7d6-4ff7-a475-67b3007c109f'::uuid,
       '09cf40b6-f8c9-4afb-9599-fce77fda433c'::uuid,
       '28ebdb3b-c615-4b87-87a5-c21a9ff4898a'::uuid,
       'c7064587-0774-4831-87f5-8247e0a6fc73'::uuid
   )
 ORDER BY id;

SELECT event_object_table AS table_name,
       trigger_name,
       action_timing,
       event_manipulation,
       action_statement
  FROM information_schema.triggers
 WHERE event_object_schema = 'public'
   AND event_object_table IN ('goods_receipts', 'goods_receipt_items')
 ORDER BY event_object_table, trigger_name, event_manipulation;
