package vn.team4.store.supplier.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import vn.team4.store.supplier.entity.SupplierEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:supplier-search;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "SUPABASE_DB_URL=jdbc:h2:mem:supplier-search;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "SUPABASE_DB_USERNAME=sa",
        "SUPABASE_DB_PASSWORD=",
        "SUPABASE_JWT_ISSUER=https://test.supabase.co/auth/v1",
        "SUPABASE_JWKS_URI=https://test.supabase.co/auth/v1/.well-known/jwks.json"
})
class SupplierRepositoryTest {
    @Autowired
    private SupplierRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        repository.saveAll(List.of(
                supplier("Needle Name", null, null, true),
                supplier("Contact", "needle-555", null, false),
                supplier("Mail", null, "Needle@example.com", false),
                supplier("Other", null, null, true)));
    }

    @Test
    void nullAndBlankKeywordDoNotAddSearchPredicate() {
        assertEquals(4, search(null, null).size());
        assertEquals(4, search("   ", null).size());
    }

    @Test
    void keywordSearchMatchesNamePhoneOrEmailAndAllowsNullColumns() {
        assertEquals(3, search(" needle ", null).size());
    }

    @Test
    void activeFilterCanBeOmittedOrSetToEitherBoolean() {
        assertEquals(4, search(null, null).size());
        assertEquals(2, search(null, true).size());
        assertEquals(2, search(null, false).size());
    }

    @Test
    void keywordAndStatusFiltersCombineAndReturnEmptyListWhenNoMatch() {
        assertEquals(1, search("needle", true).size());
        assertEquals(2, search("needle", false).size());
        assertEquals(0, search("missing", null).size());
    }

    @Test
    void pageableQueryKeepsNameSort() {
        var page = repository.findAll(
                SupplierSpecifications.matching(null, null),
                PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "name")));

        assertEquals(4, page.getTotalElements());
        assertEquals(2, page.getContent().size());
        assertEquals("Contact", page.getContent().get(0).getName());
    }

    private List<SupplierEntity> search(String keyword, Boolean isActive) {
        return repository.findAll(SupplierSpecifications.matching(keyword, isActive));
    }

    private SupplierEntity supplier(String name, String phone, String email, boolean active) {
        SupplierEntity supplier = new SupplierEntity();
        supplier.setName(name);
        supplier.setAddress(null);
        supplier.setPhone(phone);
        supplier.setEmail(email);
        supplier.setActive(active);
        return supplier;
    }
}
