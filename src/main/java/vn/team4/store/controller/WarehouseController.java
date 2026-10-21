package vn.team4.store.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.team4.store.model.WarehouseEntity;
import vn.team4.store.repository.WarehouseRepository;

import java.util.List;

@RestController
@RequestMapping("/api/warehouses")
public class WarehouseController {

    private final WarehouseRepository warehouseRepository;

    public WarehouseController(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }

    @GetMapping
    public List<WarehouseResponse> getAll() {
        return warehouseRepository.findAll()
                .stream()
                .map(warehouse -> new WarehouseResponse(
                        warehouse.getId().toString(),
                        warehouse.getName()
                ))
                .toList();
    }

    public record WarehouseResponse(String id, String name) {
    }
}