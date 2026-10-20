package vn.team4.store.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import vn.team4.store.dto.LoginRequest;
import vn.team4.store.model.User;
import vn.team4.store.repository.UserRepository;

@Service("firestoreUserService")
public class UserService {

    private static final Logger log =
            LoggerFactory.getLogger(UserService.class);

    private final UserRepository repository;
    private final FirebaseAuth firebaseAuth;

    public UserService(UserRepository repository, FirebaseApp firebaseApp) {
        this.repository = repository;
        this.firebaseAuth = FirebaseAuth.getInstance(firebaseApp);
    }

    public User login(LoginRequest request) throws Exception {

        // Bước 1: Kiểm tra Firebase ID token.
        FirebaseToken token;

        try {
            token = firebaseAuth.verifyIdToken(request.getIdToken());
        } catch (FirebaseAuthException exception) {
            log.warn("Firebase ID token verification failed: {}",
                    exception.getMessage());

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Firebase ID token is invalid or expired",
                    exception
            );
        }

        // Bước 2: Lấy UID từ token để tìm hồ sơ Firestore.
        String uid = token.getUid();

        log.info("Login request verified for Firebase UID: {}", uid);

        User user = repository.findById(uid);

        // Bước 3: Kiểm tra hồ sơ người dùng.
        if (user == null) {
            log.warn(
                    "Login denied: no Firestore document found in collection "
                    + "'users' with document ID = {}",
                    uid
            );

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No user profile is registered for this account"
            );
        }

        // Bước 4: Kiểm tra role.
        String role = user.getRole();

        log.info("Firestore profile found: UID={}, role={}", uid, role);

        if (!user.isWarehouseStaff()
                && !user.isStaff()
                && !user.isSalesStaff()) {

            log.warn(
                    "Login denied: unsupported role '{}' for UID={}",
                    role,
                    uid
            );

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "User account has an unsupported role"
            );
        }

        // Bước 5: Đăng nhập thành công.
        log.info("Login successful: UID={}, role={}", uid, role);

        return user;
    }
}