package vn.team4.store.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "restock_requests")
public class RestockRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    private String status = "PENDING"; // PENDING, APPROVED, REJECTED, EXPORTED

    @Column(name = "requested_by")
    private String requestedBy;

    @Column(name = "total_items")
    private Integer totalItems = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public RestockRequestEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }

    public Integer getTotalItems() { return totalItems; }
    public void setTotalItems(Integer totalItems) { this.totalItems = totalItems; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
