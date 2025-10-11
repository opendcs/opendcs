package org.opendcs.database.model;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import decodes.sql.DbKey;

/**
 * Holds information about a particular user
 */
public class User
{
    /**
     * Unique identifier of user within OpenDCS itself
     */
    public final DbKey id;
    /**
     * User preferences, generally key -> value,
     * but can be nested objects or arrays depending on needs.
     */
    public final Map<String, Object> preferences;

    public final String email;
    public final ZonedDateTime createdAt;
    public final ZonedDateTime updatedAt;
    public final List<Role> roles;
    public final String password;
    /**
     * List of external identity providers that are tied to this user account.
     */
    public final List<IdentityProviderMapping> identityProviders;


    public User(DbKey id, Map<String, Object> preferences, String email,
                ZonedDateTime createdAt, ZonedDateTime updatedAt, List<Role> roles,
                List<IdentityProviderMapping> identityProviders, String password)
    {
        this.id = id;
        this.preferences = preferences;
        this.email = email;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.roles = Collections.unmodifiableList(roles);
        this.password = password;
        this.identityProviders = Collections.unmodifiableList(identityProviders);
    }

    public static final class IdentityProviderMapping
    {
        /**
         * Identity Provider associated to this user+subject.
         */
        final IdentityProvider provider;
        /**
         * unique identifier used for this user in the given identity provider..
         */
        final String subject;

        public IdentityProviderMapping(IdentityProvider provider, String subject)
        {
            this.provider = provider;
            this.subject = subject;
        }
    }
}
