package vn.team4.store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.team4.store.model.ProductEntity;

import java.util.List;
import java.util.UUID;

@Repository("storeProductRepository")
public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {

    boolean existsByBarcode(String barcode);

    @Query("select p from ProductEntity p " +
           "left join fetch p.category " +
           "left join fetch p.brand " +
           "where p.active = true " +
           "order by p.name asc")
    List<ProductEntity> findAllActiveWithCategoryAndBrand();
}
