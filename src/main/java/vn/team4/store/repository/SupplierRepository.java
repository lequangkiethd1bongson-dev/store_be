package vn.team4.store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.team4.store.model.SupplierEntity;

import java.util.List;
import java.util.UUID;

public interface SupplierRepository extends JpaRepository<SupplierEntity, UUID> {
    List<SupplierEntity> findAllByActiveTrueOrderByNameAsc();
    List<SupplierEntity> findAllByOrderByNameAsc();
}
