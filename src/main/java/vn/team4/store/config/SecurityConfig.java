package vn.team4.store.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import jakarta.servlet.DispatcherType;

@Configuration
public class SecurityConfig {

    /**
     * Tạo filter kiểm tra trạng thái và vai trò nhân viên
     * dựa trên dữ liệu trong database.
     */
    @Bean
    public EmployeeAuthorizationFilter employeeAuthorizationFilter(
            JdbcTemplate jdbcTemplate
    ) {
        return new EmployeeAuthorizationFilter(jdbcTemplate);
    }

    /**
     * Không đăng ký EmployeeAuthorizationFilter thành servlet filter riêng.
     * Filter chỉ được chạy trong Spring Security chain.
     */
    @Bean
    public FilterRegistrationBean<EmployeeAuthorizationFilter>
    employeeAuthorizationFilterRegistration(
            EmployeeAuthorizationFilter filter
    ) {
        FilterRegistrationBean<EmployeeAuthorizationFilter> registration =
                new FilterRegistrationBean<>(filter);

        registration.setEnabled(false);

        return registration;
    }

    /**
     * Security configuration chính.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            EmployeeAuthorizationFilter employeeAuthorizationFilter
    ) throws Exception {

        http
                // Cấu hình CORS
                .cors(Customizer.withDefaults())

                // API REST không sử dụng CSRF token
                .csrf(csrf -> csrf.disable())

                // Không sử dụng HTTP session để xác thực
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Trả về HTTP status rõ ràng khi xác thực/phân quyền thất bại
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
                        )
                        .accessDeniedHandler((request, response, exception) ->
                                response.sendError(HttpStatus.FORBIDDEN.value())
                        )
                )

                .authorizeHttpRequests(authorize -> authorize

                        // Cho phép preflight request
                        .requestMatchers(HttpMethod.OPTIONS, "/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        /*
                         * API đọc công khai.
                         * Phải đặt trước các quy tắc yêu cầu vai trò Admin.
                         */
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/products",
                                "/api/products/**",
                                "/api/v1/products",
                                "/api/v1/products/**",
                                "/api/categories",
                                "/api/categories/**",
                                "/api/brands",
                                "/api/brands/**"
                        )
                        .permitAll()

                        // Chỉ Admin được tạo nhà cung cấp
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/suppliers",
                                "/api/suppliers/**"
                        )
                        .hasRole("ADMIN")

                        // Chỉ Admin được sửa nhà cung cấp
                        .requestMatchers(HttpMethod.PUT, "/api/suppliers/**")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.PATCH, "/api/suppliers/**")
                        .hasRole("ADMIN")

                        /*
                         * Những thao tác không phải GET trên danh mục
                         * và thương hiệu chỉ dành cho Admin.
                         * GET đã được permitAll ở quy tắc phía trên.
                         */
                        .requestMatchers(
                                "/api/categories",
                                "/api/categories/**"
                        )
                        .hasRole("ADMIN")

                        .requestMatchers(
                                "/api/brands",
                                "/api/brands/**"
                        )
                        .hasRole("ADMIN")

                        // Thông tin nhân viên đang đăng nhập
                        .requestMatchers("/api/auth/me")
                        .authenticated()

                        // API kho hàng
                        .requestMatchers(HttpMethod.GET, "/api/warehouses")
                        .hasAnyRole(
                                "ADMIN",
                                "STORE_MANAGER",
                                "KHO_MANAGER",
                                "KHO",
                                "WAREHOUSE_STAFF"
                        )

                        // Tạo sản phẩm
                        .requestMatchers(HttpMethod.POST, "/api/products")
                        .hasAnyRole(
                                "ADMIN",
                                "STORE_MANAGER",
                                "KHO_MANAGER",
                                "KHO"
                        )

                        // API nghiệp vụ dành cho nhân viên
                        .requestMatchers("/api/staff/**")
                        .hasAnyRole(
                                "ADMIN",
                                "STAFF",
                                "SALES_STAFF",
                                "WAREHOUSE_STAFF"
                        )

                        // API người dùng yêu cầu xác thực
                        .requestMatchers("/api/users/**")
                        .authenticated()

                        // Mặc định: phải đăng nhập
                        .anyRequest()
                        .authenticated()
                )

                // Xác thực Bearer token bằng JWT của Supabase
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                )

                /*
                 * Chạy filter kiểm tra hồ sơ nhân viên sau khi Spring
                 * Security xác thực Bearer token.
                 */
                .addFilterAfter(
                        employeeAuthorizationFilter,
                        BearerTokenAuthenticationFilter.class
                );

        return http.build();
    }

    /**
     * Cấu hình CORS cho web, Android Emulator và mạng LAN.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "http://10.0.2.2:*",
                "http://10.*.*.*:*",
                "http://172.16.*.*:*",
                "http://192.168.*.*:*"
        ));

        configuration.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of(
                HttpHeaders.AUTHORIZATION,
                HttpHeaders.CONTENT_TYPE,
                HttpHeaders.ACCEPT
        ));

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}