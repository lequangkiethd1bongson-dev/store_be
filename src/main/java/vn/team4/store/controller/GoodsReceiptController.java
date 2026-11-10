package vn.team4.store.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.team4.store.model.GoodsReceiptEntity;
import vn.team4.store.repository.GoodsReceiptRepository;

import java.util.List;

@RestController
@RequestMapping("/warehouse/goods-receipts")
@CrossOrigin(origins = "*")
public class GoodsReceiptController {

    @Autowired
    private GoodsReceiptRepository goodsReceiptRepository;

    @GetMapping
    public List<GoodsReceiptEntity> getAllGoodsReceipts() {
        return goodsReceiptRepository.findAll();
    }

    @PostMapping
    public GoodsReceiptEntity createGoodsReceipt(@RequestBody GoodsReceiptEntity receipt) {
        return goodsReceiptRepository.save(receipt);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GoodsReceiptEntity> getGoodsReceiptById(@PathVariable Long id) {
        return goodsReceiptRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<GoodsReceiptEntity> updateGoodsReceipt(@PathVariable Long id, @RequestBody GoodsReceiptEntity details) {
        return goodsReceiptRepository.findById(id)
                .map(gr -> {
                    gr.setSupplierId(details.getSupplierId());
                    gr.setSupplierName(details.getSupplierName());
                    gr.setTotalAmount(details.getTotalAmount());
                    gr.setStatus(details.getStatus());
                    return ResponseEntity.ok(goodsReceiptRepository.save(gr));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<GoodsReceiptEntity> completeGoodsReceipt(@PathVariable Long id) {
        return goodsReceiptRepository.findById(id)
                .map(gr -> {
                    gr.setStatus("COMPLETED");
                    return ResponseEntity.ok(goodsReceiptRepository.save(gr));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<GoodsReceiptEntity> cancelGoodsReceipt(@PathVariable Long id) {
        return goodsReceiptRepository.findById(id)
                .map(gr -> {
                    gr.setStatus("CANCELLED");
                    return ResponseEntity.ok(goodsReceiptRepository.save(gr));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
