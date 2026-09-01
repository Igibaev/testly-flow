package com.testlyflow.ui.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminSessionTest {

    @Test
    void passwordMatchesIsConstantTimeEquality() {
        assertTrue(AdminSession.passwordMatches("admin", "admin"));
        assertFalse(AdminSession.passwordMatches("admin", "other"));
        assertFalse(AdminSession.passwordMatches(null, "admin"));
        assertFalse(AdminSession.passwordMatches("admin", null));
    }

    @Test
    void adminPathDetection() {
        assertTrue(com.testlyflow.ui.admin.AdminAccessControl.isAdminPath("admin"));
        assertTrue(com.testlyflow.ui.admin.AdminAccessControl.isAdminPath("admin/metrics"));
        assertFalse(com.testlyflow.ui.admin.AdminAccessControl.isAdminPath("attempt/1"));
        assertTrue(com.testlyflow.ui.admin.AdminAccessControl.isLoginPath("admin/login"));
        assertFalse(com.testlyflow.ui.admin.AdminAccessControl.isLoginPath("admin/metrics"));
    }
}
