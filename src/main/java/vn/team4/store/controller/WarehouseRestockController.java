package vn.team4.store.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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
            @RequestParam(required = false) String status
    ) {
        return warehouseRestockService.listRequests(authentication, status);
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
            @PathVariable UUID requestId
    ) {
        return ResponseEntity.ok(warehouseRestockService.approveRequest(authentication, requestId));
    }

    @PostMapping("/restock-requests/{requestId}/reject")
    public ResponseEntity<WarehouseRestockService.RejectResult> rejectRequest(
            Authentication authentication,
            @PathVariable UUID requestId,
            @RequestParam(required = false) String reason
    ) {
        return ResponseEntity.ok(warehouseRestockService.rejectRequest(authentication, requestId, reason));
    }

    @PostMapping("/restock-requests/{requestId}/export")
    public ResponseEntity<WarehouseRestockService.ExportResult> exportRequest(
            Authentication authentication,
            @PathVariable UUID requestId
    ) {
        return ResponseEntity.ok(warehouseRestockService.exportRequest(authentication, requestId));
    }
}
