package com.voyageops.app.common.web;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * Temporary auth seam: resolves the current user from the {@code X-User-Id} header.
 * Replaced by an OAuth2-backed implementation in a later phase.
 */
@Component
public class HeaderCurrentUserProvider implements CurrentUserProvider {

    public static final String HEADER = "X-User-Id";

    @Override
    public UUID currentUserId() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        String value = attrs.getRequest().getHeader(HEADER);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}