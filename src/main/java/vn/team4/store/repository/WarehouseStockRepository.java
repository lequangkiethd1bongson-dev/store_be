package vn.team4.store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.team4.store.model.WarehouseStockEntity;
import vn.team4.store.model.WarehouseStockId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WarehouseStockRepository
        extends JpaRepository<WarehouseStockEntity, WarehouseStockId> {

    List<WarehouseStockEntity> findAllByWarehouseId(UUID warehouseId);

    Optional<WarehouseStockEntity> findByWarehouseIdAndProductId(
            UUID warehouseId, UUID productId);

    @Query("select stock.productId, sum(stock.quantity) " +
           "from WarehouseStockEntity stock group by stock.productId")
    List<Object[]> sumQuantityByProduct();
}
