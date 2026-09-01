package com.testlyflow.ui.admin;

import com.testlyflow.ui.support.AdminSession;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterListener;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.springframework.stereotype.Component;

@Component
public class AdminAccessControl implements VaadinServiceInitListener, BeforeEnterListener {

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(uiEvent -> uiEvent.getUI().addBeforeEnterListener(this));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String path = event.getLocation().getPath();
        if (!isAdminPath(path) || isLoginPath(path) || AdminSession.isAuthorized()) {
            return;
        }
        AdminSession.setRedirect(path);
        event.rerouteTo(AdminLoginView.class);
    }

    public static boolean isAdminPath(String path) {
        return path != null && (path.equals("admin") || path.startsWith("admin/"));
    }

    public static boolean isLoginPath(String path) {
        return "admin/login".equals(path);
    }
}
