package com.testlyflow.ui.support;

import com.vaadin.flow.server.VaadinRequest;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Single source of truth for client IP / User-Agent, shared by the REST start-attempt
 * endpoint and the Vaadin home view. Honours {@code X-Forwarded-For} (first hop).
 */
public final class ClientInfoResolver {

    private ClientInfoResolver() {
    }

    public static String ip() {
        VaadinRequest request = VaadinRequest.getCurrent();
        if (request == null) {
            return null;
        }
        return ip(request.getHeader("X-Forwarded-For"), request.getRemoteAddr());
    }

    public static String ip(HttpServletRequest request) {
        return ip(request.getHeader("X-Forwarded-For"), request.getRemoteAddr());
    }

    public static String ip(String forwardedFor, String remoteAddr) {
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return remoteAddr;
    }

    public static String userAgent() {
        VaadinRequest request = VaadinRequest.getCurrent();
        return request == null ? null : request.getHeader("User-Agent");
    }

    public static String userAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }
}
