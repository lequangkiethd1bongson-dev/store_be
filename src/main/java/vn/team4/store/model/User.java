package vn.team4.store.model;

import com.google.cloud.firestore.annotation.PropertyName;

public class User {
    public static final String ROLE_KHO = "kho";
    public static final String ROLE_SALE = "sale";

    // private String id;
    private String email;
    private String role;
    private String userName;

    public User() {
    }

    public User(String email, String role, String userName) {
        //this.id = id;
        this.email = email;
        setRole(role);
        this.userName = userName;
    }

    // public String getId() {
    //     return id;
    // }

    // public void setId(String id) {
    //     this.id = id;
    // }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        if (role != null && !ROLE_KHO.equals(role) && !ROLE_SALE.equals(role)) {
            throw new IllegalArgumentException("Role must be 'kho' or 'sale'");
        }
        this.role = role;
    }

    @PropertyName("user_name")
    public String getUserName() {
        return userName;
    }

    @PropertyName("user_name")
    public void setUserName(String userName) {
        this.userName = userName;
    }
}
