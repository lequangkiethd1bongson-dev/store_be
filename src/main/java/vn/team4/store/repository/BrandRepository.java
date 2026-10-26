package vn.team4.store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.team4.store.model.BrandEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BrandRepository extends JpaRepository<BrandEntity, UUID> {
    Optional<BrandEntity> findByNameIgnoreCase(String name);
    Optional<BrandEntity> findByNameIgnoreCaseAndActiveTrue(String name);
    List<BrandEntity> findAllByActiveTrueOrderByNameAsc();
    List<BrandEntity> findAllByOrderByNameAsc();
}
