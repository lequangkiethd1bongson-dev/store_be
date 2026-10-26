package vn.team4.store.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.team4.store.dto.BrandRequest;
import vn.team4.store.dto.BrandUpdateRequest;
import vn.team4.store.model.BrandEntity;
import vn.team4.store.repository.BrandRepository;

import java.util.List;
import java.util.UUID;

@Service
public class BrandService {

    private final BrandRepository brandRepository;

    public BrandService(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    @Transactional(readOnly = true)
    public List<BrandEntity> findAll(boolean includeHidden) {
        return includeHidden
                ? brandRepository.findAllByOrderByNameAsc()
                : brandRepository.findAllByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public BrandEntity findById(UUID id, boolean includeHidden) {
        BrandEntity brand = brandRepository.findById(id)
                .orElseThrow(() -> notFound(id));
        if (!includeHidden && !brand.isActive()) {
            throw notFound(id);
        }
        return brand;
    }

    @Transactional
    public BrandEntity create(BrandRequest request) {
        String name = normalizeName(request.name());
        ensureNameAvailable(name, null);

        BrandEntity brand = new BrandEntity(name);
        brand.setActive(request.isActive() == null || request.isActive());
        return save(brand, name);
    }

    @Transactional
    public BrandEntity update(UUID id, BrandRequest request) {
        BrandEntity brand = brandRepository.findById(id)
                .orElseThrow(() -> notFound(id));
        String name = normalizeName(request.name());
        ensureNameAvailable(name, id);
        brand.setName(name);
        if (request.isActive() != null) {
            brand.setActive(request.isActive());
        }
        return save(brand, name);
    }

    @Transactional
    public BrandEntity patch(UUID id, BrandUpdateRequest request) {
        if (request.name() == null && request.isActive() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Cần truyền tên hoặc trạng thái thương hiệu để cập nhật");
        }
        BrandEntity brand = brandRepository.findById(id)
                .orElseThrow(() -> notFound(id));
        if (request.name() != null) {
            String name = normalizeName(request.name());
            ensureNameAvailable(name, id);
            brand.setName(name);
        }
        if (request.isActive() != null) {
            brand.setActive(request.isActive());
        }
        return save(brand, brand.getName());
    }

    private BrandEntity save(BrandEntity brand, String name) {
        try {
            return brandRepository.saveAndFlush(brand);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Tên thương hiệu đã tồn tại: " + name, ex);
        }
    }

    private void ensureNameAvailable(String name, UUID excludedId) {
        brandRepository.findByNameIgnoreCase(name)
                .filter(existing -> !existing.getId().equals(excludedId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT, "Tên thương hiệu đã tồn tại: " + name);
                });
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Tên thương hiệu không được để trống");
        }
        return name.trim();
    }

    private ResponseStatusException notFound(UUID id) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Không tìm thấy thương hiệu: " + id);
    }
}
