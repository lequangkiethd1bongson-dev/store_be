package vn.team4.store.dto;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {
    @NotBlank
    private String idToken;

    public LoginRequest() {
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }
}
