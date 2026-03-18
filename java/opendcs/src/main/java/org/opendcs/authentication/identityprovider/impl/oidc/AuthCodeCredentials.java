package org.opendcs.authentication.identityprovider.impl.oidc;

import org.opendcs.authentication.IdentityProviderCredentials;

public record AuthCodeCredentials(String code) implements IdentityProviderCredentials
{
    
}
