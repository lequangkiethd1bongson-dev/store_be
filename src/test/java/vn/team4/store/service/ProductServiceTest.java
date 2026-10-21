
package vn.team4.store.service;

import org.junit.jupiter.api.Test;
import vn.team4.store.dto.ProductRequest;
import vn.team4.store.model.BrandEntity;
import vn.team4.store.model.CategoryEntity;
import vn.team4.store.model.Product;
import vn.team4.store.model.ProductEntity;
import vn.team4.store.model.WarehouseEntity;
import vn.team4.store.model.WarehouseStockEntity;
import vn.team4.store.repository.BrandRepository;
import vn.team4.store.repository.CategoryRepository;
import vn.team4.store.repository.ProductRepository;
import vn.team4.store.repository.WarehouseRepository;
import vn.team4.store.repository.WarehouseStockRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ProductServiceTest {

    @Test
    void createPersistsProductAndInitialWarehouseStock() {
        // Mock 5 repository mà ProductService hiện yêu cầu
        ProductRepository productRepository = mock(ProductRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        BrandRepository brandRepository = mock(BrandRepository.class);
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        WarehouseStockRepository warehouseStockRepository =
                mock(WarehouseStockRepository.class);

        ProductService service = new ProductService(
                productRepository,
                categoryRepository,
                brandRepository,
                warehouseRepository,
                warehouseStockRepository
        );

        // Tạo kho thử nghiệm
        UUID warehouseId = UUID.randomUUID();

        WarehouseEntity warehouse = new WarehouseEntity();
        warehouse.setId(warehouseId);
        warehouse.setName("Kho chính");

        when(warehouseRepository.findById(warehouseId))
                .thenReturn(Optional.of(warehouse));

        // Mock danh mục
        CategoryEntity category = new CategoryEntity("Sữa");
        when(categoryRepository.findByNameIgnoreCase("Sữa"))
                .thenReturn(Optional.of(category));

        // Mock thương hiệu
        BrandEntity brand = new BrandEntity("Vinamilk");
        when(brandRepository.findByNameIgnoreCase("Vinamilk"))
                .thenReturn(Optional.of(brand));

        // Mã vạch chưa tồn tại
        when(productRepository.existsByBarcode("8930000000001"))
                .thenReturn(false);

        // Tạo request
        ProductRequest request = new ProductRequest();
        request.setId("prod_17739783938"); // ID cũ được giữ để tương thích API
        request.setName("Sữa tươi Vinamilk 100% 1L");
        request.setPrice(36000);
        request.setOldPrice(40000.0);
        request.setImportPrice(28000);
        request.setBarcode("8930000000001");
        request.setRating(4.5);
        request.setCategory("Sữa");
        request.setBrand("Vinamilk");
        request.setPackageSize("Hộp 1L");
        request.setDescription("Sữa tươi");
        request.setStock(50);
        request.setWarehouseId(warehouseId.toString());

        // Mock lưu ProductEntity vào PostgreSQL qua JPA
        when(productRepository.saveAndFlush(any(ProductEntity.class)))
                .thenAnswer(invocation -> {
                    ProductEntity entity = invocation.getArgument(0);

                    // Mô phỏng ID UUID do database tạo
                    if (entity.getId() == null) {
                        entity.setId(UUID.randomUUID());
                    }

                    return entity;
                });

        // Chưa có bản ghi tồn kho cho sản phẩm mới
        when(warehouseStockRepository
                .findByWarehouseIdAndProductId(eq(warehouseId), any(UUID.class)))
                .thenReturn(Optional.empty());

        when(warehouseStockRepository.saveAndFlush(
                any(WarehouseStockEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Thực thi
        Product created = service.create(request);

        // Kiểm tra dữ liệu sản phẩm trả về
        assertNotNull(created.getId());
        assertDoesNotThrow(() -> UUID.fromString(created.getId()));
        assertNotEquals(request.getId(), created.getId());

        assertEquals(request.getName(), created.getName());
        assertEquals(36000.0, created.getPrice());
        assertEquals(40000.0, created.getOldPrice());
        assertEquals(28000.0, created.getImportPrice());
        assertEquals(50, created.getStock());
        assertEquals("Sữa", created.getCategory());
        assertEquals("Vinamilk", created.getBrand());

        // Xác nhận sản phẩm được lưu qua repository JPA
        verify(productRepository).saveAndFlush(any(ProductEntity.class));

        // Xác nhận số lượng ban đầu được ghi vào đúng kho
        verify(warehouseStockRepository)
                .saveAndFlush(argThat(stock ->
                        stock.getWarehouseId().equals(warehouseId)
                        && stock.getProductId().equals(
                                UUID.fromString(created.getId()))
                        && stock.getQuantity() == 50
                ));
    }
}
