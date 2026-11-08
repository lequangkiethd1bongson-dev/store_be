package vn.team4.store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.team4.store.model.GoodsReceiptEntity;
import vn.team4.store.model.GoodsReceiptStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GoodsReceiptRepository extends JpaRepository<GoodsReceiptEntity, UUID> {

    List<GoodsReceiptEntity> findByWarehouseIdOrderByCreatedAtDesc(UUID warehouseId);

    List<GoodsReceiptEntity> findByWarehouseIdAndStatusOrderByCreatedAtDesc(UUID warehouseId, GoodsReceiptStatus status);

    Optional<GoodsReceiptEntity> findByIdAndWarehouseId(UUID id, UUID warehouseId);
}
