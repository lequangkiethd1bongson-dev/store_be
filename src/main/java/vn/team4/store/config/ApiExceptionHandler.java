package vn.team4.store.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vn.team4.store.service.WarehouseRestockService;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(WarehouseRestockService.InsufficientStockException.class)
    public ResponseEntity<Map<String, String>> handleInsufficientStock(
            WarehouseRestockService.InsufficientStockException ex
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", ex.getMessage()));
    }
}
