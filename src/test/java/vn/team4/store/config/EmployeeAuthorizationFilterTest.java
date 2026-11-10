package vn.team4.store.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EmployeeAuthorizationFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void loadsRolesFromEmployeeRecordAndGivesAdminEveryBusinessRole() throws Exception {
        List<String> roles = List.of("admin", "store_manager", "sales", "kho_manager", "kho");

        for (String role : roles) {
            UUID employeeId = UUID.randomUUID();
            JdbcTemplate jdbcTemplate = employeeLookup(role, true);
            Jwt jwt = Jwt.withTokenValue("verified-token")
                    .header("alg", "ES256")
                    .subject(employeeId.toString())
                    .claim("role", "sales")
                    .build();
            SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/private");
            MockHttpServletResponse response = new MockHttpServletResponse();
            new EmployeeAuthorizationFilter(jdbcTemplate).doFilter(
                    request,
                    response,
                    mock(FilterChain.class));

            Set<String> authorities = SecurityContextHolder.getContext().getAuthentication()
                    .getAuthorities().stream()
                    .map(authority -> authority.getAuthority())
                    .collect(Collectors.toSet());

            if ("admin".equals(role)) {
                assertTrue(authorities.containsAll(Set.of(
                        "ROLE_ADMIN",
                        "ROLE_STORE_MANAGER",
                        "ROLE_SALES",
                        "ROLE_KHO_MANAGER",
                        "ROLE_KHO",
                        "ROLE_STAFF",
                        "ROLE_SALES_STAFF",
                        "ROLE_WAREHOUSE_STAFF")));
            } else {
                assertFalse(authorities.contains("ROLE_ADMIN"));
                assertEquals(expectedAuthorities(role), authorities);
            }
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void rejectsInactiveEmployeeProfile() throws Exception {
        UUID employeeId = UUID.randomUUID();
        JdbcTemplate jdbcTemplate = employeeLookup("admin", false);
        Jwt jwt = Jwt.withTokenValue("verified-token")
                .header("alg", "ES256")
                .subject(employeeId.toString())
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        MockHttpServletResponse response = new MockHttpServletResponse();
        new EmployeeAuthorizationFilter(jdbcTemplate).doFilter(
                new MockHttpServletRequest("GET", "/api/private"),
                response,
                mock(FilterChain.class));

        assertEquals(403, response.getStatus());
        assertTrue(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken);
    }

    @Test
    void rejectsJwtWithInvalidSubjectBeforeLookingUpAnEmployee() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        Jwt jwt = Jwt.withTokenValue("verified-token")
                .header("alg", "ES256")
                .subject("not-a-uuid")
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        MockHttpServletResponse response = new MockHttpServletResponse();
        new EmployeeAuthorizationFilter(jdbcTemplate).doFilter(
                new MockHttpServletRequest("GET", "/api/private"),
                response,
                mock(FilterChain.class));

        assertEquals(401, response.getStatus());
        verifyNoInteractions(jdbcTemplate);
    }

    private JdbcTemplate employeeLookup(String role, boolean active) throws SQLException {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        doAnswer(invocation -> {
            RowMapper<?> rowMapper = invocation.getArgument(1);
            ResultSet resultSet = mock(ResultSet.class);
            when(resultSet.getString("role")).thenReturn(role);
            when(resultSet.getBoolean("is_active")).thenReturn(active);
            return List.of(rowMapper.mapRow(resultSet, 0));
        }).when(jdbcTemplate).query(anyString(), any(RowMapper.class), any(Object[].class));
        return jdbcTemplate;
    }

    private Set<String> expectedAuthorities(String role) {
        return switch (role) {
            case "store_manager" -> Set.of(
                    "ROLE_STORE_MANAGER", "ROLE_STAFF", "ROLE_SALES_STAFF");
            case "sales" -> Set.of("ROLE_SALES", "ROLE_STAFF", "ROLE_SALES_STAFF");
            case "kho_manager" -> Set.of(
                    "ROLE_KHO_MANAGER", "ROLE_STAFF", "ROLE_WAREHOUSE_STAFF");
            case "kho" -> Set.of("ROLE_KHO", "ROLE_STAFF", "ROLE_WAREHOUSE_STAFF");
            default -> Set.of();
        };
    }
}
