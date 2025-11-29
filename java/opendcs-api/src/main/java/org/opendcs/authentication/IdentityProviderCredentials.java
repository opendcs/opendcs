package org.opendcs.authentication;

/**
 * All identity providers accept instance of this object to verify a given authentication request.
 * If the provider that receives it does not support the given type a InvalidCredentialsType should be thrown.
 */
public interface IdentityProviderCredentials
{
    
}
