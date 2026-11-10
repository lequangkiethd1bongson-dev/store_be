package vn.team4.store.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import vn.team4.store.dto.GoodsReceiptRequest;
import vn.team4.store.dto.GoodsReceiptResponse;
import vn.team4.store.service.GoodsReceiptService;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/warehouse")
public class GoodsReceiptController {

    private final GoodsReceiptService goodsReceiptService;

    public GoodsReceiptController(GoodsReceiptService goodsReceiptService) {
        this.goodsReceiptService = goodsReceiptService;
    }

    @GetMapping("/goods-receipts")
    public List<GoodsReceiptResponse> list(
            Authentication authentication,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID warehouseId) {
        return goodsReceiptService.list(authentication, status, warehouseId);
    }

    @GetMapping("/goods-receipts/{receiptId}")
    public GoodsReceiptResponse getById(
            Authentication authentication,
            @PathVariable UUID receiptId) {
        return goodsReceiptService.getById(authentication, receiptId);
    }

    @PostMapping("/goods-receipts")
    public ResponseEntity<GoodsReceiptResponse> createDraft(
            Authentication authentication,
            @Valid @RequestBody GoodsReceiptRequest request) {
        GoodsReceiptResponse response = goodsReceiptService.createDraft(authentication, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{receiptId}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/goods-receipts/{receiptId}")
    public GoodsReceiptResponse updateDraft(
            Authentication authentication,
            @PathVariable UUID receiptId,
            @Valid @RequestBody GoodsReceiptRequest request) {
        return goodsReceiptService.updateDraft(authentication, receiptId, request);
    }

    @PostMapping("/goods-receipts/{receiptId}/complete")
    public GoodsReceiptResponse complete(
            Authentication authentication,
            @PathVariable UUID receiptId) {
        return goodsReceiptService.complete(authentication, receiptId);
    }

    @PostMapping("/goods-receipts/{receiptId}/cancel")
    public GoodsReceiptResponse cancel(
            Authentication authentication,
            @PathVariable UUID receiptId) {
        return goodsReceiptService.cancel(authentication, receiptId);
    }
}
