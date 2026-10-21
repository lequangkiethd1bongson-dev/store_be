package vn.team4.store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.team4.store.model.CategoryEntity;

import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<CategoryEntity, UUID> {
    Optional<CategoryEntity> findByNameIgnoreCase(String name);
}
