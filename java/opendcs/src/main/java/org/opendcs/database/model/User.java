package org.opendcs.database.model;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class User
{
    final UUID id;
    // name/provider
    final Map<String, Object> preferences;
    final String email;
    final ZonedDateTime createdAt;
    final ZonedDateTime updatedAt;
    final List<Role> roles;
    final String password;
    final List<IdentityProvider> identityProviders;


    public User(UUID id, Map<String, Object> preferences, String email,
                ZonedDateTime createdAt, ZonedDateTime updatedAt, List<Role> roles,
                List<IdentityProvider> identityProviders, String password)
    {
        this.id = id;
        this.preferences = preferences;
        this.email = email;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.roles = roles;
        this.password = password;
        this.identityProviders = identityProviders;
    }
}
