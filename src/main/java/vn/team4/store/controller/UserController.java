package vn.team4.store.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.team4.store.dto.LoginRequest;
import vn.team4.store.dto.UserResponse;
import vn.team4.store.service.UserService;

@RestController("firestoreUserController")
@RequestMapping("/api/users")
public class UserController {
    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest request) throws Exception {
        System.out.println("[LOGIN] Backend đã nhận request");
        return UserResponse.from(service.login(request));
    } 
}
