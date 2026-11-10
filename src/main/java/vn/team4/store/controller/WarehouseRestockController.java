package vn.team4.store.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.team4.store.service.WarehouseRestockService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/warehouse")
public class WarehouseRestockController {

    private final WarehouseRestockService warehouseRestockService;

    public WarehouseRestockController(WarehouseRestockService warehouseRestockService) {
        this.warehouseRestockService = warehouseRestockService;
    }

    @GetMapping("/restock-requests")
    public List<WarehouseRestockService.RestockRequestSummaryResponse> listRequests(
            Authentication authentication,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) UUID warehouseId
    ) {
        return warehouseRestockService.listRequests(authentication, status, page, size, warehouseId);
    }

    @GetMapping("/restock-requests/{requestId}")
    public WarehouseRestockService.RestockRequestDetailResponse getRequestDetail(
            Authentication authentication,
            @PathVariable UUID requestId
    ) {
        return warehouseRestockService.getRequestDetail(authentication, requestId);
    }

    @PostMapping("/restock-requests/{requestId}/approve")
    public ResponseEntity<WarehouseRestockService.ApproveResult> approveRequest(
            Authentication authentication,
            @PathVariable UUID requestId,
            @RequestBody(required = false) WarehouseRestockService.ApproveRequest request
    ) {
        return ResponseEntity.ok(warehouseRestockService.approveRequest(authentication, requestId, request));
    }

    @PostMapping("/restock-requests/{requestId}/reject")
    public ResponseEntity<WarehouseRestockService.RejectResult> rejectRequest(
            Authentication authentication,
            @PathVariable UUID requestId,
            @RequestBody(required = false) WarehouseRestockService.RejectRequest request,
            @RequestParam(required = false) String reason
    ) {
        String finalReason = request != null && request.reason() != null ? request.reason() : reason;
        return ResponseEntity.ok(warehouseRestockService.rejectRequest(authentication, requestId, finalReason));
    }

    @PostMapping("/restock-requests/{requestId}/export")
    public ResponseEntity<WarehouseRestockService.ExportResult> exportRequest(
            Authentication authentication,
            @PathVariable UUID requestId,
            @RequestBody(required = false) WarehouseRestockService.ExportRequest request
    ) {
        return ResponseEntity.ok(warehouseRestockService.exportRequest(authentication, requestId, request));
    }
}
