package org.opendcs.authentication.identityprovider.impl.oidc;

import org.opendcs.authentication.IdentityProviderCredentials;

public record JwtCredentials(String accessToken) implements IdentityProviderCredentials
{
    
}
