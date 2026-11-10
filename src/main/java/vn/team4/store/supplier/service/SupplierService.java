package vn.team4.store.supplier.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.team4.store.supplier.dto.SupplierRequest;
import vn.team4.store.supplier.dto.SupplierUpdateRequest;
import vn.team4.store.supplier.entity.SupplierEntity;
import vn.team4.store.supplier.repository.SupplierRepository;
import vn.team4.store.supplier.repository.SupplierSpecifications;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class SupplierService {

    private static final Logger log = LoggerFactory.getLogger(SupplierService.class);
    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @Transactional(readOnly = true)
    public List<SupplierEntity> findAll(Boolean isActive, String keyword, Integer page, Integer size) {
        Instant startedAt = Instant.now();
        int keywordLength = keyword == null ? 0 : keyword.trim().length();
        log.debug("Supplier search started isActive={} keywordLength={} page={} size={}",
                isActive, keywordLength, page, size);
        try {
            if ((page != null && page < 0)
                    || (size != null && (size < 1 || size > 100))) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Tham số phân trang không hợp lệ");
            }
            Pageable pageable = page == null && size == null
                    ? Pageable.unpaged(Sort.by(Sort.Direction.ASC, "name"))
                    : PageRequest.of(
                            page == null ? 0 : page,
                            size == null ? 20 : size,
                            Sort.by(Sort.Direction.ASC, "name"));
            List<SupplierEntity> suppliers = supplierRepository.findAll(
                    SupplierSpecifications.matching(keyword, isActive), pageable).getContent();
            log.debug("Supplier search completed resultCount={} durationMs={}",
                    suppliers.size(), Duration.between(startedAt, Instant.now()).toMillis());
            return suppliers;
        } catch (RuntimeException exception) {
            SQLException sqlException = findSqlException(exception);
            log.error("Supplier search failed isActive={} keywordLength={} page={} size={} "
                            + "durationMs={} sqlState={} rootCauseClass={} rootCauseMessage={}",
                    isActive, keywordLength, page, size,
                    Duration.between(startedAt, Instant.now()).toMillis(),
                    sqlException == null ? null : sqlException.getSQLState(),
                    rootCause(exception).getClass().getName(),
                    rootCause(exception).getMessage(), exception);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public SupplierEntity findById(UUID id, boolean includeHidden) {
        SupplierEntity supplier = supplierRepository.findById(id)
                .orElseThrow(() -> notFound(id));
        if (!includeHidden && !supplier.isActive()) {
            throw notFound(id);
        }
        return supplier;
    }

    @Transactional
    public SupplierEntity create(SupplierRequest request) {
        SupplierEntity supplier = new SupplierEntity();
        supplier.setName(request.name());
        supplier.setAddress(request.address());
        supplier.setPhone(request.phone());
        supplier.setEmail(normalizeNullable(request.email()));
        return supplierRepository.saveAndFlush(supplier);
    }

    @Transactional
    public SupplierEntity update(UUID id, SupplierUpdateRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Tên nhà cung cấp không được để trống");
        }
        if (request.address() == null || request.address().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Địa chỉ không được để trống");
        }
        return patch(id, request);
    }

    @Transactional
    public SupplierEntity patch(UUID id, SupplierUpdateRequest request) {
        if (request.name() == null
                && request.address() == null
                && request.phone() == null
                && request.email() == null
                && request.isActive() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Cần truyền ít nhất một trường để cập nhật nhà cung cấp");
        }
        SupplierEntity supplier = supplierRepository.findById(id)
                .orElseThrow(() -> notFound(id));
        if (request.name() != null) supplier.setName(request.name());
        if (request.address() != null) supplier.setAddress(request.address());
        if (request.phone() != null) supplier.setPhone(request.phone());
        if (request.email() != null) supplier.setEmail(normalizeNullable(request.email()));
        if (request.isActive() != null) supplier.setActive(request.isActive());
        supplierRepository.saveAndFlush(supplier);
        return supplierRepository.findById(id).orElseThrow(() -> notFound(id));
    }

    @Transactional
    public SupplierEntity updateStatus(UUID id, boolean isActive) {
        SupplierEntity supplier = supplierRepository.findById(id)
                .orElseThrow(() -> notFound(id));
        supplier.setActive(isActive);
        supplierRepository.saveAndFlush(supplier);
        return supplierRepository.findById(id).orElseThrow(() -> notFound(id));
    }

    private String normalizeNullable(String value) {
        if (value.isBlank()) {
            return null;
        }

        return value;
    }

    private ResponseStatusException notFound(UUID id) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Không tìm thấy nhà cung cấp: " + id);
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
}
