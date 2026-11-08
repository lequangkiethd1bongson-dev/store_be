package vn.team4.store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.team4.store.model.GoodsReceiptItemEntity;
import vn.team4.store.model.GoodsReceiptItemId;

import java.util.List;
import java.util.UUID;

@Repository
public interface GoodsReceiptItemRepository extends JpaRepository<GoodsReceiptItemEntity, GoodsReceiptItemId> {

    List<GoodsReceiptItemEntity> findByReceiptIdOrderByProductId(UUID receiptId);
}
