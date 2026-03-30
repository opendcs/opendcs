package org.opendcs.authentication.identityprovider.impl.oidc;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.opendcs.utils.json.Helpers.getTextField;

public final class OpenIdConfiguration
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();

    public final URI wellKnownUri;
    private String issuer = null;
    private URI authUri;
    private URI tokenUri;
    private URI userInfoUri;
    private URI logoutUri;
    private URI jwksUri;

    /**
     * Establish the required set of well known configuration data.
     * Data will not be retrieved from the URL until use is requested.
     * @param wellKnown
     */
    public OpenIdConfiguration(URI wellKnown)
    {
        this.wellKnownUri = wellKnown;
    }

    /**
     * Get the defined issuer
     * @return
     * @throws OidcConfigurationException if the well known data could not be processed.
     */
    public String getIssuer()
    {
        retrieveData();
        return issuer;
    }

    /**
     *
     * @return
     * @throws OidcConfigurationException if the well known data could not be processed.
     */
    public URI getAuthUri()
    {
        retrieveData();
        return authUri;
    }

    /**
     *
     * @return
     * @throws OidcConfigurationException if the well known data could not be processed.
     */
    public URI getTokenUri()
    {
        retrieveData();
        return tokenUri;
    }

    /**
     *
     * @return
     * @throws OidcConfigurationException if the well known data could not be processed.
     */
    public URI getUserInfoUri()
    {
        retrieveData();
        return userInfoUri;
    }

    /**
     *
     * @return
     * @throws OidcConfigurationException if the well known data could not be processed.
     */
    public URI getJwksUri()
    {
        retrieveData();
        return jwksUri;
    }

    /**
     *
     * @return
     * @throws OidcConfigurationException if the well known data could not be processed.
     */
    public URI getLogoutUri()
    {
        retrieveData();
        return logoutUri;
    }

    /**
     * Retrieve and process the well-known configuration data
     * @return
     * @throws OidcConfigurationException if the well known data could not be processed.
     */
    private void retrieveData()
    {
        if (issuer != null)
        {
            return;
        }
        HttpURLConnection http = null;
        try
        {
            http = (HttpURLConnection)wellKnownUri.toURL().openConnection();
            http.setRequestMethod("GET");
            http.setInstanceFollowRedirects(true);
            http.setConnectTimeout(5_000);
            http.setReadTimeout(5_000);
            int status = http.getResponseCode();
            if (status == 200)
            {
                ObjectMapper mapper = new ObjectMapper();
                final JsonNode root = mapper.readTree(http.getInputStream());
                jwksUri = URI.create(getTextField(root, "jwks_uri"));
                issuer = getTextField(root, "issuer");
                tokenUri = URI.create(getTextField(root, "token_endpoint"));
                userInfoUri = URI.create(getTextField(root, "userinfo_endpoint"));
                logoutUri = URI.create(getTextField(root, "end_session_endpoint"));
                authUri = URI.create(getTextField(root, "authorization_endpoint"));
            }
            else
            {
                log.atError().log("Unable to retrieve data from realm. Response code {}", status);
                throw new IOException("Unable to retrieve OpenIdConnect configuration");
            }
        }
        catch (IOException ex)
        {
            throw new OidcConfigurationException("Unable to retrieve or process required OpenID information", ex);
        }
        finally
        {
            if (http != null)
            {
                http.disconnect();
            }
        }
    }
}
