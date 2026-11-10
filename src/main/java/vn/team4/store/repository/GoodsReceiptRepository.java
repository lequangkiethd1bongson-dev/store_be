package vn.team4.store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.team4.store.model.GoodsReceiptEntity;

@Repository
public interface GoodsReceiptRepository extends JpaRepository<GoodsReceiptEntity, Long> {
}
