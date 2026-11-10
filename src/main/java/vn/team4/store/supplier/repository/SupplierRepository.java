package vn.team4.store.supplier.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.team4.store.supplier.entity.SupplierEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplierRepository extends JpaRepository<SupplierEntity, UUID>,
        JpaSpecificationExecutor<SupplierEntity> {

    Optional<SupplierEntity> findByNameIgnoreCase(String name);
    Optional<SupplierEntity> findByPhone(String phone);
    Optional<SupplierEntity> findByEmailIgnoreCase(String email);
    List<SupplierEntity> findAllByActiveTrueOrderByNameAsc();
}
