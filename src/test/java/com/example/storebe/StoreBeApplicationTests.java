package com.example.storebe;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import com.example.storebe.StoreBeApplication;

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

    @Test
    void contextLoads() {
    }
}