package vn.team4.store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.team4.store.model.WarehouseEntity;

import java.util.UUID;

public interface WarehouseRepository extends JpaRepository<WarehouseEntity, UUID> {
}
