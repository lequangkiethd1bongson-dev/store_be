package vn.team4.store.supplier.controller;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.server.ResponseStatusException;
import vn.team4.store.supplier.dto.SupplierRequest;
import vn.team4.store.supplier.dto.SupplierResponse;
import vn.team4.store.supplier.dto.SupplierStatusRequest;
import vn.team4.store.supplier.dto.SupplierUpdateRequest;
import vn.team4.store.supplier.service.SupplierService;

import java.util.List;
import java.util.UUID;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private static final Logger log = LoggerFactory.getLogger(SupplierController.class);
    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public List<SupplierResponse> findAll(
            @RequestParam(defaultValue = "false") boolean includeHidden,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        return withRequestLogging(httpRequest, () -> {
            Boolean activeFilter = resolveActiveFilter(isActive, active, status);
            requireAdminForHidden(
                    includeHidden || Boolean.FALSE.equals(activeFilter), authentication);
            return supplierService.findAll(activeFilter, keyword, page, size).stream()
                    .map(SupplierResponse::from)
                    .toList();
        });
    }

    @GetMapping("/active")
    public List<SupplierResponse> findActive(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            HttpServletRequest httpRequest) {
        return withRequestLogging(httpRequest, () ->
                supplierService.findAll(Boolean.TRUE, keyword, page, size).stream()
                        .map(SupplierResponse::from)
                        .toList());
    }

    @GetMapping("/{id}")
    public SupplierResponse findById(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean includeHidden,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        return withRequestLogging(httpRequest, () -> {
            requireAdminForHidden(includeHidden, authentication);
            return SupplierResponse.from(supplierService.findById(id, includeHidden));
        });
    }

    @PostMapping
    public ResponseEntity<SupplierResponse> create(
            @Valid @RequestBody SupplierRequest request,
            HttpServletRequest httpRequest) {
        return withRequestLogging(httpRequest, () -> {
            SupplierResponse response = SupplierResponse.from(supplierService.create(request));
            var location = ServletUriComponentsBuilder.fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(response.id())
                    .toUri();
            return ResponseEntity.created(location).body(response);
        });
    }

    @PutMapping("/{id}")
    public SupplierResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody SupplierUpdateRequest request,
            HttpServletRequest httpRequest) {
        log.info("Supplier update requested id={} isActive={}", id, request.isActive());
        return withRequestLogging(httpRequest,
                () -> SupplierResponse.from(supplierService.update(id, request)));
    }

    @PatchMapping("/{id}")
    public SupplierResponse patch(
            @PathVariable UUID id,
            @Valid @RequestBody SupplierUpdateRequest request,
            HttpServletRequest httpRequest) {
        return withRequestLogging(httpRequest,
                () -> SupplierResponse.from(supplierService.patch(id, request)));
    }

    @PatchMapping("/{id}/status")
    public SupplierResponse updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody SupplierStatusRequest request,
            HttpServletRequest httpRequest) {
        log.info("Supplier status update requested id={} isActive={}", id, request.isActive());
        return withRequestLogging(httpRequest,
                () -> SupplierResponse.from(supplierService.updateStatus(id, request.isActive())));
    }

    private Boolean resolveActiveFilter(Boolean isActive, Boolean active, String status) {
        if (isActive != null && active != null && !isActive.equals(active)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Tham số isActive và active không thống nhất");
        }
        Boolean activeFilter = isActive != null ? isActive : active;
        if (status == null || status.isBlank()) {
            return activeFilter;
        }
        Boolean statusFilter = switch (status.trim().toLowerCase()) {
            case "active" -> true;
            case "inactive", "suspended" -> false;
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Trạng thái nhà cung cấp không hợp lệ");
        };
        if (activeFilter != null && !activeFilter.equals(statusFilter)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Tham số trạng thái không thống nhất");
        }
        return statusFilter;
    }

    private <T> T withRequestLogging(
            HttpServletRequest request,
            Supplier<T> action) {
        Instant startedAt = Instant.now();
        try {
            T response = action.get();
            return response;
        } catch (RuntimeException exception) {
            int status = exception instanceof ResponseStatusException responseStatusException
                    ? responseStatusException.getStatusCode().value()
                    : HttpStatus.INTERNAL_SERVER_ERROR.value();
            SQLException sqlException = findSqlException(exception);
            Throwable rootCause = rootCause(exception);
            log.error("Supplier request failed method={} uri={} query={} status={} "
                            + "durationMs={} exceptionClass={} message={} sqlState={} "
                            + "rootCauseClass={} rootCauseMessage={}",
                    request.getMethod(), request.getRequestURI(),
                    SupplierRequestLoggingInterceptor.querySummary(request), status,
                    Duration.between(startedAt, Instant.now()).toMillis(),
                    exception.getClass().getName(), exception.getMessage(),
                    sqlException == null ? null : sqlException.getSQLState(),
                    rootCause.getClass().getName(), rootCause.getMessage(), exception);
            throw exception;
        }
    }

    private SQLException findSqlException(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException) {
                return sqlException;
            }
        }
        return null;
    }

    private Throwable rootCause(Throwable exception) {
        Throwable cause = exception;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }

    private void requireAdminForHidden(boolean includeHidden, Authentication authentication) {
        if (includeHidden && (authentication == null
                || authentication.getAuthorities().stream()
                        .noneMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")))) {
            throw new AccessDeniedException("Chỉ Admin được xem nhà cung cấp đã ẩn");
        }
    }
}
