package vn.team4.store.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.team4.store.service.StoreRestockService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/store")
public class StoreRestockController {

    private final StoreRestockService storeRestockService;

    public StoreRestockController(StoreRestockService storeRestockService) {
        this.storeRestockService = storeRestockService;
    }

    @PostMapping("/restock-requests")
    public ResponseEntity<StoreRestockService.RestockRequestCreateResponse> createRequest(
            Authentication authentication,
            @Valid @RequestBody StoreRestockService.RestockRequestCreateRequest request
    ) {
        return ResponseEntity.ok(storeRestockService.createRequest(authentication, request));
    }

    @GetMapping("/restock-requests")
    public List<StoreRestockService.RestockRequestSummaryResponse> listRequests(
            Authentication authentication,
            @RequestParam(required = false) String status
    ) {
        return storeRestockService.listRequests(authentication, status);
    }

    @GetMapping("/restock-requests/{requestId}")
    public StoreRestockService.RestockRequestDetailResponse getRequestDetail(
            Authentication authentication,
            @PathVariable UUID requestId
    ) {
        return storeRestockService.getRequestDetail(authentication, requestId);
    }

    @PostMapping("/stock-exports/{exportId}/confirm-receipt")
    public ResponseEntity<StoreRestockService.ReceiptConfirmationResponse> confirmReceipt(
            Authentication authentication,
            @PathVariable UUID exportId
    ) {
        return ResponseEntity.ok(storeRestockService.confirmReceipt(authentication, exportId));
    }
}
