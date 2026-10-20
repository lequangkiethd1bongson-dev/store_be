package vn.team4.store.config;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.team4.store.model.User;
import vn.team4.store.repository.UserRepository;

import java.io.IOException;
import java.util.List;

public class FirebaseAuthenticationFilter extends OncePerRequestFilter {
    private final FirebaseAuth firebaseAuth;
    private final UserRepository userRepository;

    public FirebaseAuthenticationFilter(
            FirebaseAuth firebaseAuth,
            UserRepository userRepository
    ) {
        this.firebaseAuth = firebaseAuth;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String idToken = authorization.substring("Bearer ".length()).trim();
            try {
                FirebaseToken token = firebaseAuth.verifyIdToken(idToken);
                User user = userRepository.findById(token.getUid());
                if (user != null) {
                    List<SimpleGrantedAuthority> authorities = authoritiesFor(user);
                    if (!authorities.isEmpty()) {
                        var authentication = new UsernamePasswordAuthenticationToken(
                                token.getUid(),
                                null,
                                authorities
                        );
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            } catch (FirebaseAuthException exception) {
                SecurityContextHolder.clearContext();
                response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Firebase ID token is invalid or expired"
                );
                return;
            } catch (Exception exception) {
                throw new ServletException("Could not load the Firebase user profile", exception);
            }
        }

        filterChain.doFilter(request, response);
    }

    private List<SimpleGrantedAuthority> authoritiesFor(User user) {
        if (user.isWarehouseStaff()) {
            return List.of(new SimpleGrantedAuthority("ROLE_WAREHOUSE_STAFF"));
        }
        if (user.isStaff()) {
            return List.of(new SimpleGrantedAuthority("ROLE_STAFF"));
        }
        if (user.isSalesStaff()) {
            return List.of(new SimpleGrantedAuthority("ROLE_SALES_STAFF"));
        }
        return List.of();
    }
}
