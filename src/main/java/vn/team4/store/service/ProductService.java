package vn.team4.store.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service("storeProductService")
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseStockRepository warehouseStockRepository;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            BrandRepository brandRepository,
            WarehouseRepository warehouseRepository,
            WarehouseStockRepository warehouseStockRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.warehouseRepository = warehouseRepository;
        this.warehouseStockRepository = warehouseStockRepository;
    }

    /**
     * Backward compatible behavior: without warehouseId, stock is the sum across warehouses.
     * With warehouseId, stock represents only that warehouse.
     */
    @Transactional(readOnly = true)
    public List<Product> getAllProducts(UUID warehouseId) {
        Map<UUID, Integer> stockByProduct = loadStockByProduct(warehouseId);

        return productRepository.findAllActiveWithCategoryAndBrand().stream()
                .map(entity -> toApiProduct(
                        entity, stockByProduct.getOrDefault(entity.getId(), 0)))
                .toList();
    }

    @Transactional
    public Product create(ProductRequest request) {
        validateRequest(request);

        UUID warehouseId = parseWarehouseId(request.getWarehouseId());
        WarehouseEntity warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy kho với warehouseId: " + warehouseId));

        String barcode = normalizeNullableText(request.getBarcode());
        if (barcode != null && productRepository.existsByBarcode(barcode)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Mã vạch sản phẩm đã tồn tại: " + barcode);
        }

        ProductEntity entity = new ProductEntity();
        // New product IDs are generated as UUIDs; the legacy request id is intentionally ignored.
        entity.setName(request.getName().trim());
        entity.setBarcode(barcode);
        entity.setDescription(normalizeNullableText(request.getDescription()));
        entity.setImageUrl(normalizeNullableText(request.getImageUrl()));
        entity.setPrice(BigDecimal.valueOf(request.getPrice()));
        entity.setOldPrice(request.getOldPrice() == null
                ? null : BigDecimal.valueOf(request.getOldPrice()));
        entity.setImportPrice(BigDecimal.valueOf(request.getImportPrice()));
        entity.setPackageSize(normalizeNullableText(request.getPackageSize()));
        entity.setRating(BigDecimal.valueOf(request.getRating()));
        entity.setCategory(findOrCreateCategory(request.getCategory()));
        entity.setBrand(findOrCreateBrand(request.getBrand()));
        entity.setActive(true);

        try {
            ProductEntity saved = productRepository.saveAndFlush(entity);

            // Initial stock is stored against the selected warehouse, never in products.stock.
            WarehouseStockEntity stock = warehouseStockRepository
                    .findByWarehouseIdAndProductId(warehouse.getId(), saved.getId())
                    .orElseGet(WarehouseStockEntity::new);
            stock.setWarehouseId(warehouse.getId());
            stock.setProductId(saved.getId());
            stock.setQuantity(stock.getQuantity() + request.getStock());
            stock.setUpdatedAt(OffsetDateTime.now());
            warehouseStockRepository.saveAndFlush(stock);

            return toApiProduct(saved, stock.getQuantity());
        } catch (DataIntegrityViolationException ex) {
            // Catches uniqueness / foreign-key races not detected by the pre-checks above.
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Không thể tạo sản phẩm. Hãy kiểm tra mã vạch, danh mục, thương hiệu và kho.",
                    ex);
        }
    }

    private Map<UUID, Integer> loadStockByProduct(UUID warehouseId) {
        Map<UUID, Integer> result = new HashMap<>();

        if (warehouseId != null) {
            if (!warehouseRepository.existsById(warehouseId)) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy kho với warehouseId: " + warehouseId);
            }
            for (WarehouseStockEntity stock :
                    warehouseStockRepository.findAllByWarehouseId(warehouseId)) {
                result.put(stock.getProductId(), stock.getQuantity());
            }
            return result;
        }

        for (Object[] row : warehouseStockRepository.sumQuantityByProduct()) {
            UUID productId = (UUID) row[0];
            Number total = (Number) row[1];
            result.put(productId, total.intValue());
        }
        return result;
    }

    private CategoryEntity findOrCreateCategory(String rawName) {
        String name = normalizeNullableText(rawName);
        if (name == null) return null;

        return categoryRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> categoryRepository.saveAndFlush(new CategoryEntity(name)));
    }

    private BrandEntity findOrCreateBrand(String rawName) {
        String name = normalizeNullableText(rawName);
        if (name == null) return null;

        return brandRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> brandRepository.saveAndFlush(new BrandEntity(name)));
    }

    private void validateRequest(ProductRequest request) {
        if (request == null || request.getName() == null || request.getName().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Tên sản phẩm không được để trống");
        }
        if (request.getWarehouseId() == null || request.getWarehouseId().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cần truyền warehouseId để ghi số lượng ban đầu vào kho");
        }
        if (!Double.isFinite(request.getPrice())
                || !Double.isFinite(request.getImportPrice())
                || request.getPrice() < 0
                || request.getImportPrice() < 0
                || request.getStock() < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Giá và số lượng phải là giá trị hợp lệ >= 0");
        }
        if (request.getOldPrice() != null
                && (!Double.isFinite(request.getOldPrice()) || request.getOldPrice() < 0)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Giá cũ phải >= 0");
        }
        if (!Double.isFinite(request.getRating())
                || request.getRating() < 0 || request.getRating() > 5) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Rating phải nằm trong khoảng từ 0 đến 5");
        }
        if (request.getPrice() < request.getImportPrice()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Giá bán phải >= giá nhập");
        }
    }

    private UUID parseWarehouseId(String rawId) {
        try {
            return UUID.fromString(rawId.trim());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "warehouseId phải là UUID hợp lệ");
        }
    }

    private String normalizeNullableText(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private Product toApiProduct(ProductEntity entity, int stock) {
        return new Product(
                entity.getId().toString(),
                entity.getName(),
                entity.getPrice().doubleValue(),
                entity.getOldPrice() == null ? null : entity.getOldPrice().doubleValue(),
                entity.getImportPrice() == null ? 0 : entity.getImportPrice().doubleValue(),
                entity.getBarcode(),
                entity.getRating() == null ? 0 : entity.getRating().doubleValue(),
                entity.getCategory() == null ? null : entity.getCategory().getName(),
                entity.getImageUrl(),
                entity.getBrand() == null ? null : entity.getBrand().getName(),
                entity.getPackageSize(),
                entity.getDescription(),
                stock);
    }
}
