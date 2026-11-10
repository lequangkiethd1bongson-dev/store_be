package vn.team4.store.supplier.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import vn.team4.store.supplier.service.SupplierService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SupplierControllerTest {
    private final SupplierService supplierService = mock(SupplierService.class);
    private final SupplierController controller = new SupplierController(supplierService);
    private final Authentication authentication = mock(Authentication.class);
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setMethod("GET");
        httpRequest.setRequestURI("/api/suppliers");
        request = httpRequest;
        when(supplierService.findAll(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
    }

    @Test
    void missingFilterMeansAllSuppliers() {
        assertEquals(List.of(), controller.findAll(
                false, null, null, null, null, null, null, authentication, request));

        verify(supplierService).findAll(isNull(), isNull(), isNull(), isNull());
    }

    @Test
    void supportsFlutterActiveAndStatusParameters() {
        controller.findAll(false, null, true, "active", null, null, null, authentication, request);

        verify(supplierService).findAll(true, null, null, null);
    }

    @Test
    void rejectsConflictingActiveParametersAsBadRequest() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.findAll(
                        false, true, false, null, null, null, null, authentication, request));

        assertEquals(400, exception.getStatusCode().value());
    }
}
