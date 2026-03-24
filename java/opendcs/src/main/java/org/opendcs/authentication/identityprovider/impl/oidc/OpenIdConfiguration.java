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
    public final String issuer;
    public final URI authUri;
    public final URI tokenUri;
    public final URI userInfoUri;
    public final URI logoutUri;
    public final URI jwksUri;

    public OpenIdConfiguration(URI wellKnown) throws IOException
    {
        this.wellKnownUri = wellKnown;

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
        finally 
        {
            if (http != null)
            {
                http.disconnect();
            }
        }
    }

    
}
