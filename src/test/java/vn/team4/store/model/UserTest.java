package vn.team4.store.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {
    @Test
    void recognizesWarehouseRoleAliases() {
        User user = new User();

        user.setRole("kho");
        assertTrue(user.isWarehouseStaff());

        user.setRole("WAREHOUSE_STAFF");
        assertTrue(user.isWarehouseStaff());
    }

    @Test
    void recognizesStaffRoleWithoutGrantingWarehouseRole() {
        User user = new User();
        user.setRole("STAFF");

        assertTrue(user.isStaff());
        assertFalse(user.isWarehouseStaff());
    }
}
