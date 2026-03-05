package org.opendcs.authentication.identityprovider.impl.oidc;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
            int status = http.getResponseCode();
            if (status == 200) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(http.getInputStream());
                jwksUri = URI.create(node.get("jwks_uri").asText());
                issuer = node.get("issuer").asText();
                tokenUri = URI.create(node.get("token_endpoint").asText());
                userInfoUri = URI.create(node.get("userinfo_endpoint").asText());
                logoutUri = URI.create(node.get("end_session_endpoint").asText());
                authUri = URI.create(node.get("authorization_endpoint").asText());
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
