package vn.team4.store.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.team4.store.service.StoreInventoryService;

import java.util.List;

@RestController
@RequestMapping("/api/store")
public class StoreInventoryController {

    private final StoreInventoryService storeInventoryService;

    public StoreInventoryController(StoreInventoryService storeInventoryService) {
        this.storeInventoryService = storeInventoryService;
    }

    @GetMapping("/products")
    public List<StoreInventoryService.StoreProductResponse> getProducts(Authentication authentication) {
        return storeInventoryService.getProductsForCurrentStore(authentication);
    }
}
