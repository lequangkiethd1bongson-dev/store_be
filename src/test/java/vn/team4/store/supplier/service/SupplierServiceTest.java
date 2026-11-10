package vn.team4.store.supplier.service;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import vn.team4.store.supplier.dto.SupplierRequest;
import vn.team4.store.supplier.dto.SupplierUpdateRequest;
import vn.team4.store.supplier.entity.SupplierEntity;
import vn.team4.store.supplier.repository.SupplierRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SupplierServiceTest {
    private static final jakarta.validation.ValidatorFactory VALIDATOR_FACTORY =
            Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();

    private final SupplierRepository repository = mock(SupplierRepository.class);
    private final SupplierService service = new SupplierService(repository);

    private UUID supplierId;
    private SupplierEntity supplier;

    @BeforeEach
    void setUp() {
        supplierId = UUID.randomUUID();
        supplier = new SupplierEntity();
        supplier.setId(supplierId);
        supplier.setName("Supplier");
        supplier.setAddress("Address");
        supplier.setPhone("01234567890");
        supplier.setEmail("old@example.com");
        supplier.setActive(true);
        when(repository.findById(supplierId)).thenReturn(Optional.of(supplier));
        when(repository.saveAndFlush(any(SupplierEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void trimsCreateFieldsAndAcceptsOnlyElevenDigitPhone() {
        SupplierRequest valid = new SupplierRequest(
                "  Supplier  ", "  Address  ", " 01234567890 ", "  ");
        SupplierRequest invalidPhone = new SupplierRequest(
                "Supplier", "Address", "123", null);

        assertEquals(0, VALIDATOR.validate(valid).size());
        assertEquals(1, VALIDATOR.validate(invalidPhone).size());
        assertEquals("Supplier", valid.name());
        assertEquals("Address", valid.address());
        assertEquals("01234567890", valid.phone());
    }

    @Test
    void updatesOnlySubmittedFieldsAndCanClearEmail() {
        SupplierEntity updated = service.update(
                supplierId, new SupplierUpdateRequest("  Updated  ", null, null, "", null));

        assertEquals("Updated", updated.getName());
        assertEquals("Address", updated.getAddress());
        assertEquals("01234567890", updated.getPhone());
        assertNull(updated.getEmail());
    }

    @Test
    void putUpdatePersistsRequestedInactiveAndActiveState() {
        SupplierEntity deactivated = service.update(
                supplierId,
                new SupplierUpdateRequest("Supplier", "Address", "01234567890",
                        "old@example.com", false));
        assertFalse(deactivated.isActive());

        supplier.setActive(false);
        SupplierEntity activated = service.update(
                supplierId,
                new SupplierUpdateRequest("Supplier", "Address", "01234567890",
                        "old@example.com", true));
        assertTrue(activated.isActive());
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.times(2))
                .saveAndFlush(any(SupplierEntity.class));
    }

    @Test
    void putUpdateWithoutStatusPreservesExistingStatus() {
        SupplierEntity updated = service.update(
                supplierId,
                new SupplierUpdateRequest("Updated", "New address", null, null, null));

        assertTrue(updated.isActive());
    }

    @Test
    void patchCanChangeOnlyStatusWithoutOverwritingOtherFields() {
        SupplierEntity updated = service.patch(
                supplierId, new SupplierUpdateRequest(null, null, null, null, false));

        assertFalse(updated.isActive());
        assertEquals("Supplier", updated.getName());
        assertEquals("Address", updated.getAddress());
        assertEquals("01234567890", updated.getPhone());
    }

    @Test
    void statusCanBeDisabledAndReactivated() {
        assertFalse(service.updateStatus(supplierId, false).isActive());
        assertTrue(service.updateStatus(supplierId, true).isActive());
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.times(4))
                .findById(supplierId);
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.times(2))
                .saveAndFlush(any(SupplierEntity.class));
    }

    @Test
    void rejectsMissingSupplier() {
        UUID missingId = UUID.randomUUID();
        when(repository.findById(missingId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.updateStatus(missingId, false));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void forwardsSearchFilterAndPaginationToRepository() {
        when(repository.findAll(
                org.mockito.ArgumentMatchers.<Specification<SupplierEntity>>any(),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(supplier)));

        List<SupplierEntity> results = service.findAll(false, " supplier ", 1, 10);

        assertEquals(List.of(supplier), results);
        org.mockito.Mockito.verify(repository).findAll(
                org.mockito.ArgumentMatchers.<Specification<SupplierEntity>>any(),
                PageRequest.of(1, 10, Sort.by(Sort.Direction.ASC, "name")));
    }

    @Test
    void acceptsAllFilterVariantsAndUsesSortedUnpagedQueryByDefault() {
        when(repository.findAll(
                org.mockito.ArgumentMatchers.<Specification<SupplierEntity>>any(),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.findAll(null, null, null, null);
        service.findAll(true, null, null, null);
        service.findAll(false, null, null, null);

        org.mockito.Mockito.verify(repository, org.mockito.Mockito.times(3)).findAll(
                org.mockito.ArgumentMatchers.<Specification<SupplierEntity>>any(),
                Pageable.unpaged(Sort.by(Sort.Direction.ASC, "name")));
    }
}
