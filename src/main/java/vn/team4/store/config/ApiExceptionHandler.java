package vn.team4.store.config;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import vn.team4.store.service.WarehouseRestockService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(WarehouseRestockService.InsufficientStockException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientStock(
            WarehouseRestockService.InsufficientStockException ex
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "INSUFFICIENT_STOCK");
        body.put("message", ex.getMessage());
        body.put("details", List.of(Map.of(
                "productName", ex.getProductName(),
                "approvedQuantity", ex.getRequiredQuantity(),
                "availableQuantity", ex.getAvailableQuantity(),
                "missingQuantity", Math.max(ex.getRequiredQuantity() - ex.getAvailableQuantity(), 0)
        )));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(WarehouseRestockService.WarehouseExportValidationException.class)
    public ResponseEntity<Map<String, Object>> handleExportValidation(
            WarehouseRestockService.WarehouseExportValidationException ex
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", ex.getCode());
        body.put("message", ex.getMessage());
        body.put("details", ex.getDetails());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "VALIDATION_ERROR");
        body.put("message", "Dữ liệu nhập không hợp lệ");

        List<Map<String, Object>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        body.put("details", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, Object>> handleBadRequestPayload(Exception ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "INVALID_REQUEST");
        body.put("message", "Dữ liệu gửi lên không hợp lệ hoặc sai định dạng");
        body.put("details", List.of(Map.of("error", ex.getMessage())));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(
            ConstraintViolationException ex
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "VALIDATION_ERROR");
        body.put("message", ex.getMessage());
        body.put("details", List.of(Map.of("error", ex.getMessage())));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(
            ResponseStatusException ex
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "BUSINESS_ERROR");
        body.put("message", ex.getReason() == null ? "Yêu cầu không hợp lệ" : ex.getReason());
        body.put("details", List.of());
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(
            DataIntegrityViolationException ex
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "DATA_INTEGRITY_ERROR");
        body.put("message", "Dữ liệu nhập không hợp lệ hoặc trùng khóa");
        body.put("details", List.of(Map.of("error", ex.getMostSpecificCause() == null ? ex.getMessage() : ex.getMostSpecificCause().getMessage())));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    private Map<String, Object> toFieldError(FieldError fieldError) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("field", fieldError.getField());
        error.put("message", fieldError.getDefaultMessage());
        error.put("rejectedValue", fieldError.getRejectedValue());
        return error;
    }
}
