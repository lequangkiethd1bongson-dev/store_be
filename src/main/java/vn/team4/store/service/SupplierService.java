package vn.team4.store.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.team4.store.dto.SupplierRequest;
import vn.team4.store.dto.SupplierUpdateRequest;
import vn.team4.store.model.SupplierEntity;
import vn.team4.store.repository.SupplierRepository;

import java.util.List;
import java.util.UUID;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @Transactional(readOnly = true)
    public List<SupplierEntity> findAll(boolean includeHidden) {
        return includeHidden
                ? supplierRepository.findAllByOrderByNameAsc()
                : supplierRepository.findAllByActiveTrueOrderByNameAsc();
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
        apply(supplier, request);
        supplier.setActive(request.isActive() == null || request.isActive());
        return supplierRepository.saveAndFlush(supplier);
    }

    @Transactional
    public SupplierEntity update(UUID id, SupplierRequest request) {
        SupplierEntity supplier = supplierRepository.findById(id)
                .orElseThrow(() -> notFound(id));
        apply(supplier, request);
        if (request.isActive() != null) {
            supplier.setActive(request.isActive());
        }
        return supplierRepository.saveAndFlush(supplier);
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
        if (request.name() != null) {
            supplier.setName(normalizeRequired(request.name()));
        }
        if (request.address() != null) {
            supplier.setAddress(normalizeNullable(request.address()));
        }
        if (request.phone() != null) {
            supplier.setPhone(normalizeNullable(request.phone()));
        }
        if (request.email() != null) {
            supplier.setEmail(normalizeNullable(request.email()));
        }
        if (request.isActive() != null) {
            supplier.setActive(request.isActive());
        }
        return supplierRepository.saveAndFlush(supplier);
    }

    private void apply(SupplierEntity supplier, SupplierRequest request) {
        supplier.setName(normalizeRequired(request.name()));
        supplier.setAddress(normalizeNullable(request.address()));
        supplier.setPhone(normalizeNullable(request.phone()));
        supplier.setEmail(normalizeNullable(request.email()));
    }

    private String normalizeRequired(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Tên nhà cung cấp không được để trống");
        }
        return value.trim();
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private ResponseStatusException notFound(UUID id) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Không tìm thấy nhà cung cấp: " + id);
    }
}
