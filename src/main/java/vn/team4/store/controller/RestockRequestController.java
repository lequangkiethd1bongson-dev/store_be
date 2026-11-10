package vn.team4.store.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.team4.store.model.RestockRequestEntity;
import vn.team4.store.repository.RestockRequestRepository;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
public class RestockRequestController {

    @Autowired
    private RestockRequestRepository restockRepository;

    @GetMapping("/store/restock-requests")
    public List<RestockRequestEntity> getStoreRestockRequests(@RequestParam(required = false) Long storeId) {
        if (storeId != null) {
            return restockRepository.findByStoreId(storeId);
        }
        return restockRepository.findAll();
    }

    @PostMapping("/store/restock-requests")
    public RestockRequestEntity createRestockRequest(@RequestBody RestockRequestEntity request) {
        return restockRepository.save(request);
    }

    @GetMapping("/store/restock-requests/{id}")
    public ResponseEntity<RestockRequestEntity> getStoreRestockRequestById(@PathVariable Long id) {
        return restockRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/warehouse/restock-requests")
    public List<RestockRequestEntity> getWarehouseRestockRequests(@RequestParam(required = false) String status) {
        if (status != null && !status.isEmpty()) {
            return restockRepository.findByStatus(status);
        }
        return restockRepository.findAll();
    }

    @GetMapping("/warehouse/restock-requests/{id}")
    public ResponseEntity<RestockRequestEntity> getWarehouseRestockRequestById(@PathVariable Long id) {
        return restockRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/warehouse/restock-requests/{id}/approve")
    public ResponseEntity<RestockRequestEntity> approveRestockRequest(@PathVariable Long id) {
        return restockRepository.findById(id)
                .map(req -> {
                    req.setStatus("APPROVED");
                    return ResponseEntity.ok(restockRepository.save(req));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/warehouse/restock-requests/{id}/reject")
    public ResponseEntity<RestockRequestEntity> rejectRestockRequest(@PathVariable Long id) {
        return restockRepository.findById(id)
                .map(req -> {
                    req.setStatus("REJECTED");
                    return ResponseEntity.ok(restockRepository.save(req));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/warehouse/restock-requests/{id}/export")
    public ResponseEntity<RestockRequestEntity> exportRestockRequest(@PathVariable Long id) {
        return restockRepository.findById(id)
                .map(req -> {
                    req.setStatus("EXPORTED");
                    return ResponseEntity.ok(restockRepository.save(req));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/store/stock-exports/{exportId}/confirm-receipt")
    public ResponseEntity<RestockRequestEntity> confirmStockReceipt(@PathVariable Long exportId) {
        return restockRepository.findById(exportId)
                .map(req -> {
                    req.setStatus("RECEIVED");
                    return ResponseEntity.ok(restockRepository.save(req));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
