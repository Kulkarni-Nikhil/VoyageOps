package com.voyageops.app.common.web;

import java.util.UUID;

/**
 * Port for resolving the authenticated actor.
 * <p>
 * Temporary implementation reads the {@code X-User-Id} header until OAuth2 login
 * is introduced; services stay decoupled from the resolution mechanism.
 */
public interface CurrentUserProvider {

    UUID currentUserId();
}