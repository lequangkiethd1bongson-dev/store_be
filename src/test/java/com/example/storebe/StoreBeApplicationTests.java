package com.example.storebe;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import com.example.storebe.StoreBeApplication;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = StoreBeApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:storebe-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:54321/auth/v1",
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:54321/auth/v1/keys"
        }
)
class StoreBeApplicationTests {

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Test
    void contextLoads() {
    }

    @Test
    void storeAndWarehouseRestockRoutesAreRegistered() {
        Set<String> paths = requestMappingHandlerMapping.getHandlerMethods().keySet().stream()
                .flatMap(mapping -> mapping.getPatternValues().stream())
                .collect(Collectors.toSet());

        assertTrue(paths.contains("/api/store/products"));
        assertTrue(paths.contains("/api/store/restock-requests"));
        assertTrue(paths.contains("/api/store/restock-requests/{requestId}"));
        assertTrue(paths.contains("/api/store/stock-exports/{exportId}/confirm-receipt"));
        assertTrue(paths.contains("/api/warehouse/restock-requests"));
        assertTrue(paths.contains("/api/warehouse/restock-requests/{requestId}"));
        assertTrue(paths.contains("/api/warehouse/restock-requests/{requestId}/approve"));
        assertTrue(paths.contains("/api/warehouse/restock-requests/{requestId}/reject"));
        assertTrue(paths.contains("/api/warehouse/restock-requests/{requestId}/export"));
    }
}