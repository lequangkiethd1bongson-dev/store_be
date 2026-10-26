package vn.team4.store.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import vn.team4.store.dto.CategoryRequest;
import vn.team4.store.model.CategoryEntity;
import vn.team4.store.repository.CategoryRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CategoryServiceTest {

    private final CategoryRepository repository = mock(CategoryRepository.class);
    private final CategoryService service = new CategoryService(repository);

    @Test
    void createTrimsNameAndRejectsExistingNameIgnoringCase() {
        when(repository.findByNameIgnoreCase("Đồ uống")).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(CategoryEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CategoryEntity created = service.create(new CategoryRequest("  Đồ uống  "));

        assertEquals("Đồ uống", created.getName());
        verify(repository).saveAndFlush(any(CategoryEntity.class));

        CategoryEntity existing = new CategoryEntity("Đồ uống");
        existing.setId(UUID.randomUUID());
        when(repository.findByNameIgnoreCase("đồ uống")).thenReturn(Optional.of(existing));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.create(new CategoryRequest("đồ uống")));
        assertEquals(409, exception.getStatusCode().value());
    }

    @Test
    void updateAllowsUnchangedNameAndReturnsNotFoundForMissingCategory() {
        UUID id = UUID.randomUUID();
        CategoryEntity category = new CategoryEntity("Đồ uống");
        category.setId(id);
        when(repository.findById(id)).thenReturn(Optional.of(category));
        when(repository.findByNameIgnoreCase("Đồ uống")).thenReturn(Optional.of(category));
        when(repository.saveAndFlush(category)).thenReturn(category);

        CategoryEntity updated = service.update(id, new CategoryRequest("Đồ uống"));

        assertEquals(id, updated.getId());
        assertEquals("Đồ uống", updated.getName());

        UUID missingId = UUID.randomUUID();
        when(repository.findById(missingId)).thenReturn(Optional.empty());
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.update(missingId, new CategoryRequest("Thực phẩm")));
        assertEquals(404, exception.getStatusCode().value());
    }
}
