package vn.team4.store.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.team4.store.dto.CategoryRequest;
import vn.team4.store.model.CategoryEntity;
import vn.team4.store.repository.CategoryRepository;

import java.util.List;
import java.util.UUID;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryEntity> findAll() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public CategoryEntity findById(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> notFound(id));
    }

    @Transactional
    public CategoryEntity create(CategoryRequest request) {
        String name = normalizeName(request.name());
        ensureNameAvailable(name, null);

        try {
            return categoryRepository.saveAndFlush(new CategoryEntity(name));
        } catch (DataIntegrityViolationException ex) {
            throw duplicateName(name, ex);
        }
    }

    @Transactional
    public CategoryEntity update(UUID id, CategoryRequest request) {
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> notFound(id));
        String name = normalizeName(request.name());
        ensureNameAvailable(name, id);
        category.setName(name);

        try {
            return categoryRepository.saveAndFlush(category);
        } catch (DataIntegrityViolationException ex) {
            throw duplicateName(name, ex);
        }
    }

    @Transactional
    public void delete(UUID id) {
        CategoryEntity category = categoryRepository.findById(id)
                .orElseThrow(() -> notFound(id));
        categoryRepository.delete(category);
    }

    private void ensureNameAvailable(String name, UUID excludedId) {
        categoryRepository.findByNameIgnoreCase(name)
                .filter(existing -> !existing.getId().equals(excludedId))
                .ifPresent(existing -> {
                    throw duplicateName(name, null);
                });
    }

    private String normalizeName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Tên danh mục không được để trống");
        }
        return rawName.trim();
    }

    private ResponseStatusException notFound(UUID id) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Không tìm thấy danh mục: " + id);
    }

    private ResponseStatusException duplicateName(String name, Throwable cause) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT, "Tên danh mục đã tồn tại: " + name, cause);
    }
}
