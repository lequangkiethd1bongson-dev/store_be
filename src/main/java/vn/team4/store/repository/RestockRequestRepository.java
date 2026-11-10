package vn.team4.store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.team4.store.model.RestockRequestEntity;

import java.util.List;

@Repository
public interface RestockRequestRepository extends JpaRepository<RestockRequestEntity, Long> {
    List<RestockRequestEntity> findByStoreId(Long storeId);
    List<RestockRequestEntity> findByStatus(String status);
}
