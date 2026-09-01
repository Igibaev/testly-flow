package com.testlyflow.ui.support;

import com.vaadin.flow.server.VaadinSession;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class AdminSession {

    public static final String AUTHORIZED_KEY = "adminAuthorized";
    public static final String REDIRECT_KEY = "adminRedirect";

    private AdminSession() {
    }

    public static boolean isAuthorized() {
        VaadinSession session = VaadinSession.getCurrent();
        return session != null && Boolean.TRUE.equals(session.getAttribute(AUTHORIZED_KEY));
    }

    public static void authorize() {
        VaadinSession.getCurrent().setAttribute(AUTHORIZED_KEY, Boolean.TRUE);
    }

    public static void clear() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(AUTHORIZED_KEY, null);
            session.setAttribute(REDIRECT_KEY, null);
        }
    }

    public static void setRedirect(String path) {
        VaadinSession.getCurrent().setAttribute(REDIRECT_KEY, path);
    }

    public static String takeRedirect() {
        VaadinSession session = VaadinSession.getCurrent();
        Object value = session.getAttribute(REDIRECT_KEY);
        session.setAttribute(REDIRECT_KEY, null);
        return value instanceof String s && !s.isBlank() ? s : null;
    }

    public static boolean passwordMatches(String provided, String expected) {
        if (provided == null || expected == null) {
            return false;
        }
        byte[] a = provided.getBytes(StandardCharsets.UTF_8);
        byte[] b = expected.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }
}
