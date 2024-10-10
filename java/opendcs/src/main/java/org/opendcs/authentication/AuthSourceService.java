package org.opendcs.authentication;

import java.util.Iterator;
import java.util.ServiceLoader;

import org.opendcs.spi.authentication.AuthSource;
import org.opendcs.spi.authentication.AuthSourceProvider;

import ilex.util.AuthException;

/**
 * Retrieve and setups a authorization source for databases to use
 */
public class AuthSourceService
{
    private static ServiceLoader<AuthSourceProvider> loader = ServiceLoader.load(AuthSourceProvider.class);

    /**
     * dbAuthFile string from the properties file.
     * The line can now be in the form type:config information
     *
     * The default behavior if no ':' is present is to assume the original UserAuthFile
     *
     * @param configString
     * @return An AuthSource from which properties can be retrieved.
     * @throws AuthException Thrown on the following:
     * <ul>
     *  <li>Unable to find a provider.</li>
     *  <li>Unable instantiate the provider with a given configuration.</li>
     * </ul>
     *
     */
    public static AuthSource getFromString(String configString) throws AuthException
    {
        int colon = configString.indexOf(":");
        String type = "UserAuthFile";
        String actualConfig = configString;
        if ( colon > 1 )
        {
            type = configString.substring(0, colon);
            actualConfig = configString.substring(colon+1);
        }

        Iterator<AuthSourceProvider> services = loader.iterator();
        while(services.hasNext())
        {
            AuthSourceProvider provider = services.next();
            if (provider.getType().equalsIgnoreCase(type) )
            {
                return provider.process(actualConfig);
            }
        }

        throw new AuthException("No Auth provider available for type: " + type);
    }
}
