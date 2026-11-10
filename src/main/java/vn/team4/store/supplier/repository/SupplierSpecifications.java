package vn.team4.store.supplier.repository;

import org.springframework.data.jpa.domain.Specification;
import vn.team4.store.supplier.entity.SupplierEntity;

import java.util.ArrayList;
import java.util.Locale;

public final class SupplierSpecifications {
    private SupplierSpecifications() {
    }

    public static Specification<SupplierEntity> matching(String keyword, Boolean isActive) {
        String normalizedKeyword = keyword == null || keyword.isBlank()
                ? null
                : keyword.trim().toLowerCase(Locale.ROOT);

        return (root, query, criteriaBuilder) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (normalizedKeyword != null) {
                String pattern = "%" + escapeLike(normalizedKeyword) + "%";
                var nameMatches = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")), pattern, '\\');
                var phoneMatches = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("phone")), pattern, '\\');
                var emailMatches = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("email")), pattern, '\\');
                predicates.add(criteriaBuilder.or(nameMatches, phoneMatches, emailMatches));
            }
            if (isActive != null) {
                predicates.add(criteriaBuilder.equal(root.get("active"), isActive));
            }
            return predicates.isEmpty()
                    ? criteriaBuilder.conjunction()
                    : criteriaBuilder.and(predicates.toArray(
                            jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
