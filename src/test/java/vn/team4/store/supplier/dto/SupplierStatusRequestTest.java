package vn.team4.store.supplier.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SupplierStatusRequestTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void acceptsOnlyCanonicalStatusPropertyAndIgnoresLegacyUnknownField() throws Exception {
        assertFalse(objectMapper.readValue(
                "{\"isActive\":false}", SupplierStatusRequest.class).isActive());
        assertTrue(objectMapper.readValue(
                "{\"isActive\":true}", SupplierStatusRequest.class).isActive());
        assertFalse(objectMapper.readValue(
                "{\"isActive\":false,\"is_active\":true}", SupplierStatusRequest.class)
                .isActive());
    }

    @Test
    void updateRequestAcceptsCanonicalStatusAndPreservesAbsentStatus() throws Exception {
        SupplierUpdateRequest update = objectMapper.readValue(
                "{\"name\":\"Supplier\",\"address\":\"Address\","
                        + "\"isActive\":false,\"is_active\":true}",
                SupplierUpdateRequest.class);
        SupplierUpdateRequest withoutStatus = objectMapper.readValue(
                "{\"name\":\"Supplier\",\"address\":\"Address\"}",
                SupplierUpdateRequest.class);

        assertEquals(Boolean.FALSE, update.isActive());
        assertNull(withoutStatus.isActive());
    }

    @Test
    void deserializesFullPutPayloadWithCanonicalFalseState() throws Exception {
        SupplierUpdateRequest request = objectMapper.readValue(
                """
                {
                  "name": "Cong ty CP TM A Dong",
                  "phone": "07745408421",
                  "address": "Mai Dng, Bong Son, Gia Lai",
                  "email": "lequangkiethd@gmail.com",
                  "isActive": false
                }
                """,
                SupplierUpdateRequest.class);

        assertEquals(Boolean.FALSE, request.isActive());
    }

    @Test
    void rejectsStringStatusValueForStatusAndUpdateRequests() {
        assertThrows(JsonProcessingException.class, () -> objectMapper.readValue(
                "{\"isActive\":\"false\"}", SupplierStatusRequest.class));
        assertThrows(JsonProcessingException.class, () -> objectMapper.readValue(
                "{\"name\":\"Supplier\",\"address\":\"Address\","
                        + "\"isActive\":\"false\"}",
                SupplierUpdateRequest.class));
    }
}
