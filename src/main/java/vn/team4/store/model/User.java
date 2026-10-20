package vn.team4.store.model;

import com.google.cloud.firestore.annotation.PropertyName;

public class User {
    public static final String ROLE_KHO = "kho";
    public static final String ROLE_SALE = "sale";
    public static final String ROLE_STAFF = "staff";
    public static final String ROLE_WAREHOUSE_STAFF = "warehouse_staff";

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
        if (role != null
                && !ROLE_KHO.equalsIgnoreCase(role)
                && !ROLE_SALE.equalsIgnoreCase(role)
                && !ROLE_STAFF.equalsIgnoreCase(role)
                && !ROLE_WAREHOUSE_STAFF.equalsIgnoreCase(role)) {
            throw new IllegalArgumentException(
                    "Role must be 'kho', 'sale', 'staff' or 'warehouse_staff'"
            );
        }
        this.role = role;
    }

    public boolean isWarehouseStaff() {
        return ROLE_KHO.equalsIgnoreCase(role)
                || ROLE_WAREHOUSE_STAFF.equalsIgnoreCase(role);
    }

    public boolean isStaff() {
        return ROLE_STAFF.equalsIgnoreCase(role);
    }

    public boolean isSalesStaff() {
        return ROLE_SALE.equalsIgnoreCase(role);
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
