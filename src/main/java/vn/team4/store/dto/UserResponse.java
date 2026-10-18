package vn.team4.store.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import vn.team4.store.model.User;

public record UserResponse(
        //String id,
        String email,
        String role,
        @JsonProperty("user_name") String userName
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                //user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getUserName()
        );
    }
}
