package vn.team4.store.service;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import vn.team4.store.dto.GoodsReceiptItemRequest;
import vn.team4.store.dto.GoodsReceiptRequest;
import vn.team4.store.model.EmployeeContext;
import vn.team4.store.model.GoodsReceiptEntity;
import vn.team4.store.model.GoodsReceiptItemEntity;
import vn.team4.store.model.GoodsReceiptStatus;
import vn.team4.store.model.ProductEntity;
import vn.team4.store.model.SupplierEntity;
import vn.team4.store.repository.GoodsReceiptItemRepository;
import vn.team4.store.repository.GoodsReceiptRepository;
import vn.team4.store.repository.ProductRepository;
import vn.team4.store.repository.SupplierRepository;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoodsReceiptServiceTest {
    private final GoodsReceiptRepository receiptRepository = mock(GoodsReceiptRepository.class);
    private final GoodsReceiptItemRepository itemRepository = mock(GoodsReceiptItemRepository.class);
    private final SupplierRepository supplierRepository = mock(SupplierRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final EmployeeContextService employeeContextService = mock(EmployeeContextService.class);
    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

    private final GoodsReceiptService service = new GoodsReceiptService(
            receiptRepository,
            itemRepository,
            supplierRepository,
            productRepository,
            employeeContextService,
            jdbcTemplate);

    private Authentication authentication;
    private UUID employeeId;
    private UUID warehouseId;
    private UUID supplierId;
    private UUID productId;
    private SupplierEntity supplier;
    private ProductEntity product;

    @BeforeEach
    void setUp() {
        authentication = mock(Authentication.class);
        employeeId = UUID.randomUUID();
        warehouseId = UUID.randomUUID();
        supplierId = UUID.randomUUID();
        productId = UUID.randomUUID();
        when(employeeContextService.requireWarehouseManagerOrStaff(authentication))
                .thenReturn(new EmployeeContext(employeeId, "kho", null, warehouseId, true));

        supplier = new SupplierEntity();
        supplier.setId(supplierId);
        supplier.setName("Supplier");
        supplier.setActive(true);
        when(supplierRepository.findById(supplierId)).thenReturn(Optional.of(supplier));

        product = new ProductEntity();
        product.setId(productId);
        product.setName("Product");
        product.setBarcode("123");
        product.setActive(true);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), eq(employeeId)))
                .thenReturn("Warehouse Employee");
    }

    @Test
    void createsDraftWithLowercaseStatusAndPersistsItsItems() {
        List<GoodsReceiptItemEntity> persistedItems = new ArrayList<>();
        when(receiptRepository.saveAndFlush(any(GoodsReceiptEntity.class)))
                .thenAnswer(invocation -> {
                    GoodsReceiptEntity receipt = invocation.getArgument(0);
                    if (receipt.getId() == null) {
                        receipt.setId(UUID.randomUUID());
                    }
                    return receipt;
                });
        when(itemRepository.saveAllAndFlush(anyList())).thenAnswer(invocation -> {
            persistedItems.addAll(invocation.getArgument(0));
            return invocation.getArgument(0);
        });
        when(itemRepository.findByReceiptIdOrderByProductId(any(UUID.class))).thenAnswer(invocation -> persistedItems);

        var response = service.createDraft(authentication, request(new BigDecimal("25")));

        assertEquals("draft", response.status());
        assertEquals(1, response.items().size());
        assertEquals(1, persistedItems.size());
        verify(receiptRepository, times(2)).saveAndFlush(org.mockito.ArgumentMatchers.argThat(
                receipt -> receipt.getStatus() == GoodsReceiptStatus.draft));
    }

    @Test
    void enumMappingUsesLowercasePostgresNamedEnum() throws ReflectiveOperationException {
        Field statusField = GoodsReceiptEntity.class.getDeclaredField("status");

        assertEquals(EnumType.STRING, statusField.getAnnotation(Enumerated.class).value());
        assertEquals(SqlTypes.NAMED_ENUM, statusField.getAnnotation(JdbcTypeCode.class).value());
        assertEquals(List.of("draft", "completed", "cancelled"),
                java.util.Arrays.stream(GoodsReceiptStatus.values()).map(Enum::name).toList());
    }

    @Test
    void refusesToCompleteAnUnclassifiedLegacyReceipt() {
        UUID receiptId = receiptId();
        when(jdbcTemplate.queryForMap(anyString(), eq(receiptId), eq(warehouseId)))
                .thenReturn(new java.util.HashMap<>(Map.of("id", receiptId, "warehouse_id", warehouseId)));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.complete(authentication, receiptId));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void completingDraftAddsStockOnceAndRejectsSecondCompletion() {
        UUID receiptId = receiptId();
        GoodsReceiptEntity receipt = receipt(receiptId, GoodsReceiptStatus.draft);
        GoodsReceiptItemEntity item = item(receiptId);
        when(jdbcTemplate.queryForMap(anyString(), eq(receiptId), eq(warehouseId)))
                .thenReturn(Map.of("id", receiptId, "warehouse_id", warehouseId, "status", "draft"))
                .thenReturn(Map.of("id", receiptId, "warehouse_id", warehouseId, "status", "completed"));
        when(itemRepository.findByReceiptIdOrderByProductId(receiptId)).thenReturn(List.of(item));
        when(receiptRepository.findById(receiptId)).thenReturn(Optional.of(receipt));
        when(receiptRepository.findByIdAndWarehouseId(receiptId, warehouseId)).thenReturn(Optional.of(receipt));
        when(receiptRepository.saveAndFlush(any(GoodsReceiptEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(itemRepository.findByReceiptIdOrderByProductId(any(UUID.class))).thenReturn(List.of(item));

        assertEquals("completed", service.complete(authentication, receiptId).status());
        assertEquals(GoodsReceiptStatus.completed, receipt.getStatus());
        assertNotNull(receipt.getCompletedAt());
        assertNull(receipt.getCancelledAt());
        verify(receiptRepository).saveAndFlush(org.mockito.ArgumentMatchers.argThat(
                savedReceipt -> savedReceipt.getStatus() == GoodsReceiptStatus.completed));
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.complete(authentication, receiptId));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(jdbcTemplate, times(1)).update(anyString(), eq(warehouseId), eq(productId), eq(5));
    }

    @Test
    void cancelsOnlyDraftAndUpdatesItsState() {
        UUID receiptId = receiptId();
        GoodsReceiptEntity receipt = receipt(receiptId, GoodsReceiptStatus.draft);
        when(jdbcTemplate.queryForMap(anyString(), eq(receiptId), eq(warehouseId)))
                .thenReturn(Map.of("status", "draft"));
        when(receiptRepository.findByIdAndWarehouseId(receiptId, warehouseId)).thenReturn(Optional.of(receipt));
        when(receiptRepository.saveAndFlush(any(GoodsReceiptEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(itemRepository.findByReceiptIdOrderByProductId(receiptId)).thenReturn(List.of());

        assertEquals("cancelled", service.cancel(authentication, receiptId).status());
        assertEquals(GoodsReceiptStatus.cancelled, receipt.getStatus());
        org.junit.jupiter.api.Assertions.assertNotNull(receipt.getCancelledAt());
    }

    @Test
    void rejectsEditingCompletedOrCancelledReceipts() {
        UUID receiptId = receiptId();
        GoodsReceiptRequest request = request(BigDecimal.TEN);
        for (GoodsReceiptStatus status : List.of(GoodsReceiptStatus.completed, GoodsReceiptStatus.cancelled)) {
            when(receiptRepository.findByIdAndWarehouseId(receiptId, warehouseId))
                    .thenReturn(Optional.of(receipt(receiptId, status)));

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> service.updateDraft(authentication, receiptId, request));

            assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        }
        verify(itemRepository, never()).saveAllAndFlush(anyList());
    }

    private GoodsReceiptRequest request(BigDecimal price) {
        return new GoodsReceiptRequest(
                supplierId,
                "draft",
                List.of(new GoodsReceiptItemRequest(productId, 5, price, null, null)));
    }

    private UUID receiptId() {
        return UUID.randomUUID();
    }

    private GoodsReceiptEntity receipt(UUID receiptId, GoodsReceiptStatus status) {
        GoodsReceiptEntity receipt = new GoodsReceiptEntity();
        receipt.setId(receiptId);
        receipt.setWarehouseId(warehouseId);
        receipt.setSupplierId(supplierId);
        receipt.setCreatedBy(employeeId);
        receipt.setStatus(status);
        receipt.setTotalAmount(BigDecimal.ZERO);
        return receipt;
    }

    private GoodsReceiptItemEntity item(UUID receiptId) {
        GoodsReceiptItemEntity item = new GoodsReceiptItemEntity();
        item.setReceiptId(receiptId);
        item.setProductId(productId);
        item.setQuantity(5);
        item.setImportPrice(BigDecimal.TEN);
        return item;
    }
}
