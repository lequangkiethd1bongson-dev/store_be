package vn.team4.store.config;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import vn.team4.store.repository.UserRepository;

import java.util.List;

@Configuration
public class SecurityConfig {
    @Bean
    public FirebaseAuthenticationFilter firebaseAuthenticationFilter(
            FirebaseApp firebaseApp,
            UserRepository userRepository
    ) {
        return new FirebaseAuthenticationFilter(
                FirebaseAuth.getInstance(firebaseApp),
                userRepository
        );
    }

    @Bean
    public FilterRegistrationBean<FirebaseAuthenticationFilter>
    firebaseAuthenticationFilterRegistration(
            FirebaseAuthenticationFilter firebaseAuthenticationFilter
    ) {
        FilterRegistrationBean<FirebaseAuthenticationFilter> registration =
                new FilterRegistrationBean<>(firebaseAuthenticationFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            FirebaseAuthenticationFilter firebaseAuthenticationFilter
    ) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, exception) ->
                                response.sendError(HttpStatus.FORBIDDEN.value())))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/staff/**")
                        .hasAnyRole("STAFF", "WAREHOUSE_STAFF")
                        .requestMatchers(HttpMethod.POST, "/api/products")
                        .hasAnyRole("STAFF", "WAREHOUSE_STAFF")
                        .anyRequest().permitAll())
                .addFilterBefore(
                        firebaseAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );
        return http.build();
    }

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
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
        configuration.setAllowedHeaders(List.of(
                HttpHeaders.AUTHORIZATION,
                HttpHeaders.CONTENT_TYPE,
                HttpHeaders.ACCEPT
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
