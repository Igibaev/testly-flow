package com.testlyflow.ui.admin;

import com.github.mvysny.kaributesting.v10.LocatorJ;
import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.testlyflow.ui.support.AdminSession;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminLoginViewTest {

    @BeforeEach
    void setUp() {
        MockVaadin.setup();
    }

    @AfterEach
    void tearDown() {
        AdminSession.clear();
        MockVaadin.tearDown();
    }

    @Test
    void wrongPasswordDoesNotAuthorize() {
        UI.getCurrent().add(new AdminLoginView("secret"));
        LocatorJ._get(Input.class, spec -> spec.withId("admin-password")).setValue("nope");
        LocatorJ._click(LocatorJ._get(NativeButton.class, spec -> spec.withText("Войти")));
        assertFalse(AdminSession.isAuthorized());
    }

    @Test
    void correctPasswordAuthorizes() {
        UI.getCurrent().add(new AdminLoginView("secret"));
        LocatorJ._get(Input.class, spec -> spec.withId("admin-password")).setValue("secret");
        LocatorJ._click(LocatorJ._get(NativeButton.class, spec -> spec.withText("Войти")));
        assertTrue(AdminSession.isAuthorized());
    }
}
