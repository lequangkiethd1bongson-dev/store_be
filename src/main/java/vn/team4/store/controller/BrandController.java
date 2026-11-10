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
import vn.team4.store.dto.BrandRequest;
import vn.team4.store.dto.BrandResponse;
import vn.team4.store.dto.BrandUpdateRequest;
import vn.team4.store.service.BrandService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/brands")
public class BrandController {

    private final BrandService brandService;

    public BrandController(BrandService brandService) {
        this.brandService = brandService;
    }

    @GetMapping
    public List<BrandResponse> findAll(
            @RequestParam(defaultValue = "false") boolean includeHidden,
            Authentication authentication) {
        requireAdminForHidden(includeHidden, authentication);
        return brandService.findAll(includeHidden).stream()
                .map(BrandResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public BrandResponse findById(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean includeHidden,
            Authentication authentication) {
        requireAdminForHidden(includeHidden, authentication);
        return BrandResponse.from(brandService.findById(id, includeHidden));
    }

    @PostMapping
    public ResponseEntity<BrandResponse> create(@Valid @RequestBody BrandRequest request) {
        BrandResponse response = BrandResponse.from(brandService.create(request));
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public BrandResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody BrandRequest request) {
        return BrandResponse.from(brandService.update(id, request));
    }

    @PatchMapping("/{id}")
    public BrandResponse patch(
            @PathVariable UUID id,
            @Valid @RequestBody BrandUpdateRequest request) {
        return BrandResponse.from(brandService.patch(id, request));
    }

    private void requireAdminForHidden(boolean includeHidden, Authentication authentication) {
        if (includeHidden && (authentication == null
                || authentication.getAuthorities().stream()
                        .noneMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")))) {
            throw new AccessDeniedException("Chỉ Admin được xem thương hiệu đã ẩn");
        }
    }
}
