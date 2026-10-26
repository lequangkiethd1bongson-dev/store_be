package vn.team4.store.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/** Runs after Spring Security has cryptographically validated the Supabase JWT. */
public class EmployeeAuthorizationFilter extends OncePerRequestFilter {
    private final JdbcTemplate jdbcTemplate;

    public EmployeeAuthorizationFilter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getServletPath();

    boolean isPublicReadApi =
            "GET".equalsIgnoreCase(request.getMethod())
            && (
                path.equals("/api/products")
                || path.startsWith("/api/products/")
                || path.equals("/api/v1/products")
                || path.startsWith("/api/v1/products/")
                || path.equals("/api/brands")
                || path.startsWith("/api/brands/")
                || path.equals("/api/categories")
                || path.startsWith("/api/categories/")
            );

    // 2. API GET công khai không cần kiểm tra hồ sơ nhân viên
    if (isPublicReadApi) {
        filterChain.doFilter(request, response);
        return;
    }
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            filterChain.doFilter(request, response);
            return;
        }

        final UUID userId;
        try {
            userId = UUID.fromString(jwtAuthentication.getToken().getSubject());
        } catch (IllegalArgumentException | NullPointerException ex) {
            reject(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Supabase access token has an invalid subject");
            return;
        }

        List<EmployeeRow> employees = jdbcTemplate.query(
                "SELECT role::text AS role, is_active FROM public.employees WHERE id = ?",
                (rs, rowNum) -> new EmployeeRow(rs.getString("role"), rs.getBoolean("is_active")),
                userId
        );

        if (employees.isEmpty()) {
            reject(response, HttpServletResponse.SC_FORBIDDEN,
                    "Tài khoản chưa có hồ sơ nhân viên. Vui lòng liên hệ Admin.");
            return;
        }

        EmployeeRow employee = employees.get(0);
        if (!employee.active()) {
            reject(response, HttpServletResponse.SC_FORBIDDEN,
                    "Tài khoản chưa được Admin kích hoạt/cấp quyền.");
            return;
        }

        List<SimpleGrantedAuthority> authorities = authoritiesFor(employee.role());
        if (authorities.isEmpty()) {
            reject(response, HttpServletResponse.SC_FORBIDDEN,
                    "Vai trò nhân viên chưa được hỗ trợ.");
            return;
        }

        // Use the Supabase auth.users UUID as the application principal.
        var employeeAuthentication = new UsernamePasswordAuthenticationToken(
                userId.toString(), null, authorities
        );
        employeeAuthentication.setDetails(request.getRemoteAddr());
        SecurityContextHolder.getContext().setAuthentication(employeeAuthentication);

        filterChain.doFilter(request, response);
    }
    
    private List<SimpleGrantedAuthority> authoritiesFor(String role) {
        return switch (role) {
            case "admin" -> List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
            case "store_manager" -> List.of(
                    new SimpleGrantedAuthority("ROLE_STORE_MANAGER"),
                    new SimpleGrantedAuthority("ROLE_STAFF"),
                    new SimpleGrantedAuthority("ROLE_SALES_STAFF"));
            case "sales" -> List.of(
                    new SimpleGrantedAuthority("ROLE_SALES"),
                    new SimpleGrantedAuthority("ROLE_STAFF"),
                    new SimpleGrantedAuthority("ROLE_SALES_STAFF"));
            case "kho_manager" -> List.of(
                    new SimpleGrantedAuthority("ROLE_KHO_MANAGER"),
                    new SimpleGrantedAuthority("ROLE_STAFF"),
                    new SimpleGrantedAuthority("ROLE_WAREHOUSE_STAFF"));
            case "kho" -> List.of(
                    new SimpleGrantedAuthority("ROLE_KHO"),
                    new SimpleGrantedAuthority("ROLE_STAFF"),
                    new SimpleGrantedAuthority("ROLE_WAREHOUSE_STAFF"));
            default -> List.of();
        };
    }

    private void reject(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        String safeMessage = message.replace("\\", "\\\\").replace("\"", "\\\"");
        response.getWriter().write("{\"message\":\"" + safeMessage + "\"}");
    }

    private record EmployeeRow(String role, boolean active) { }
}
