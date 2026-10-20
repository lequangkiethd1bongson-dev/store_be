package vn.team4.store.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.team4.store.dto.StockReceiptRequest;
import vn.team4.store.dto.StockReceiptResponse;
import vn.team4.store.service.StockReceiptService;

@RestController
@RequestMapping("/api/staff/stock-receipts")
public class StaffStockController {
    private final StockReceiptService service;

    public StaffStockController(StockReceiptService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<StockReceiptResponse> receive(
            @Valid @RequestBody StockReceiptRequest request
    ) throws Exception {
        return ResponseEntity.ok(service.receive(request));
    }
}
