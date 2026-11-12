package vn.team4.store.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final String DEFAULT_SUPABASE_URL = "https://imtbcjdrnvjbmdxvnunh.supabase.co";
    private static final String DEFAULT_SUPABASE_ANON_KEY = "sb_publishable_c8SigvWlTEVgHb4uBjhe3Q_HjmmTjPO";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${SUPABASE_URL:" + DEFAULT_SUPABASE_URL + "}")
    private String supabaseUrl;

    @Value("${SUPABASE_PUBLISHABLE_KEY:" + DEFAULT_SUPABASE_ANON_KEY + "}")
    private String supabaseAnonKey;

    public AuthController(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Proxy đăng nhập Supabase Auth qua Backend Host (10.0.2.2:8080).
     * Khắc phục triệt để lỗi máy ảo Android bị kẹt DNS ("Failed host lookup: *.supabase.co")
     * sau khi laptop sleep hoặc đổi mạng Wi-Fi.
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, Object> body) {
        String email = String.valueOf(body.getOrDefault("email", body.getOrDefault("account", ""))).trim();
        String password = String.valueOf(body.getOrDefault("password", ""));

        if (email.isEmpty() || password.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập đầy đủ email và mật khẩu.");
        }

        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "email", email,
                    "password", password
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(supabaseUrl + "/auth/v1/token?grant_type=password"))
                    .timeout(Duration.ofSeconds(12))
                    .header("apikey", supabaseAnonKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            Map<String, Object> authData = objectMapper.readValue(
                    response.body(),
                    new TypeReference<Map<String, Object>>() {}
            );

            if (response.statusCode() >= 400) {
                String errorCode = String.valueOf(authData.getOrDefault("error_code", ""));
                String msg = String.valueOf(authData.getOrDefault("msg", authData.getOrDefault("error_description", "Email hoặc mật khẩu không đúng.")));
                if ("invalid_credentials".equalsIgnoreCase(errorCode)) {
                    msg = "Email hoặc mật khẩu không đúng.";
                }
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, msg);
            }

            String accessToken = String.valueOf(authData.getOrDefault("access_token", ""));
            String refreshToken = String.valueOf(authData.getOrDefault("refresh_token", ""));
            @SuppressWarnings("unchecked")
            Map<String, Object> userObj = (Map<String, Object>) authData.get("user");
            if (accessToken.isEmpty() || userObj == null || userObj.get("id") == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không nhận được phiên đăng nhập từ Supabase.");
            }

            UUID employeeId = UUID.fromString(String.valueOf(userObj.get("id")));
            Map<String, Object> profile = queryEmployeeProfile(employeeId);

            boolean isActive = Boolean.TRUE.equals(profile.get("isActive"));
            if (!isActive) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Tài khoản chưa được Admin kích hoạt/cấp quyền. Vui lòng liên hệ Admin."
                );
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("accessToken", accessToken);
            result.put("refreshToken", refreshToken);
            result.put("user", profile);
            return result;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Không thể kết nối máy chủ xác thực Supabase: " + ex.getMessage()
            );
        }
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        UUID employeeId = UUID.fromString(authentication.getName());
        return queryEmployeeProfile(employeeId);
    }

    private Map<String, Object> queryEmployeeProfile(UUID employeeId) {
        List<Map<String, Object>> rows = jdbcTemplate.query("""
                SELECT id::text AS id,
                       full_name AS name,
                       COALESCE(email, '') AS email,
                       role::text AS role,
                       is_active AS "isActive",
                       store_id::text AS "storeId",
                       warehouse_id::text AS "warehouseId"
                FROM public.employees
                WHERE id = ?
                """, (rs, rowNum) -> {
            Map<String, Object> profile = new LinkedHashMap<>();
            profile.put("id", rs.getString("id"));
            profile.put("name", rs.getString("name"));
            profile.put("email", rs.getString("email"));
            profile.put("role", rs.getString("role"));
            profile.put("isActive", rs.getBoolean("isActive"));
            profile.put("storeId", rs.getString("storeId"));
            profile.put("warehouseId", rs.getString("warehouseId"));
            return profile;
        }, employeeId);

        if (rows.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tài khoản chưa có hồ sơ nhân viên. Vui lòng liên hệ Admin."
            );
        }
        return rows.get(0);
    }
}
