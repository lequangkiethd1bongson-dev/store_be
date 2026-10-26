package vn.team4.store.controller;

import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import vn.team4.store.dto.SupplierRequest;
import vn.team4.store.dto.SupplierResponse;
import vn.team4.store.dto.SupplierUpdateRequest;
import vn.team4.store.service.SupplierService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public List<SupplierResponse> findAll(
            @RequestParam(defaultValue = "false") boolean includeHidden,
            Authentication authentication) {
        requireAdminForHidden(includeHidden, authentication);
        return supplierService.findAll(includeHidden).stream()
                .map(SupplierResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public SupplierResponse findById(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean includeHidden,
            Authentication authentication) {
        requireAdminForHidden(includeHidden, authentication);
        return SupplierResponse.from(supplierService.findById(id, includeHidden));
    }

    @PostMapping
    public ResponseEntity<SupplierResponse> create(
            @Valid @RequestBody SupplierRequest request) {
        SupplierResponse response = SupplierResponse.from(supplierService.create(request));
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public SupplierResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody SupplierRequest request) {
        return SupplierResponse.from(supplierService.update(id, request));
    }

    @PatchMapping("/{id}")
    public SupplierResponse patch(
            @PathVariable UUID id,
            @Valid @RequestBody SupplierUpdateRequest request) {
        return SupplierResponse.from(supplierService.patch(id, request));
    }

    private void requireAdminForHidden(boolean includeHidden, Authentication authentication) {
        if (includeHidden && (authentication == null
                || authentication.getAuthorities().stream()
                        .noneMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")))) {
            throw new AccessDeniedException("Chỉ Admin được xem nhà cung cấp đã ẩn");
        }
    }
}
