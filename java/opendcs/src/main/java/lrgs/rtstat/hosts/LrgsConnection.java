/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package lrgs.rtstat.hosts;

import java.util.Date;
import java.util.Objects;
import java.util.function.Predicate;

import javax.net.SocketFactory;

import nl.altindag.ssl.model.TrustManagerParameters;

import org.opendcs.tls.TlsMode;
import org.opendcs.utils.WebUtility;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.util.AuthException;
import ilex.util.DesEncrypter;

/**
 * Keep the information about a connection to an LRGS for the ComboBox model.
 */
public final class LrgsConnection
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();

    public static final LrgsConnection BLANK = new LrgsConnection("", -1, "", "", null, TlsMode.NONE);

    private final String hostName;
    private final int port;
    private final TlsMode tls;
    private final String username;
    private final String password;
    private final Date lastUsed;


    public LrgsConnection(String hostName, int port, String username, String password, Date lastUsed, TlsMode tls)
    {
        this.hostName = hostName;
        this.port = port;
        this.tls = tls;
        this.username = username;
        this.password = password;
        this.lastUsed = lastUsed;
    }

    public LrgsConnection(LrgsConnection c, Date lastUsed)
    {
        this.hostName = c.hostName;
        this.port = c.port;
        this.tls = c.tls;
        this.username = c.username;
        this.password = c.password;
        this.lastUsed = lastUsed;
    }

    public String getHostName()
    {
        return hostName;
    }

    public int getPort()
    {
        return port;
    }

    public String getUsername()
    {
        return username;
    }

    public String getPassword()
    {
        return password;
    }

    public Date getLastUsed()
    {
        return lastUsed;
    }

    public TlsMode getTls()
    {
        return tls;
    }

    /**
     * Get Default Socket Factory. If TLS mode is enabled a default "no additional trust" is used.
     * Certificates signed by The System and Java Trust stores or already in the "local_trust" will be accepted,
     * all others
     * will fail verification.
     * @return
     */
    public SocketFactory getSocketFactory()
    {
        return getSocketFactory(WebUtility.TRUST_EXISTING_CERTIFICATES);
    }

    /**
     * @see getSocketFactory
     * @param certTest Predicate callback that allows either invoking system to approve the certificate if desired.
     * @return
     */
    public SocketFactory getSocketFactory(Predicate<TrustManagerParameters> certTest)
    {
        if (tls != TlsMode.NONE)
        {
           return WebUtility.socketFactory(certTest);
		}
        else
        {
            return null;
        }
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof LrgsConnection))
        {
            return false;
        }
        LrgsConnection rhs = (LrgsConnection)other;
        if (port != rhs.port)
        {
            return false;
        }

        if (lastUsed != null && rhs.lastUsed == null)
        {
            return false;
        }
        else if (lastUsed == null && rhs.lastUsed != null)
        {
            return false;
        }
        else if ((lastUsed != null && rhs.lastUsed != null) && !lastUsed.equals(rhs.lastUsed))
        {
            return false;
        }
        else if((tls != rhs.tls))
        {
            return false;
        }

        if (!hostName.equals(rhs.hostName))
        {
            return false;
        }

        if (!username.equals(rhs.username))
        {
            return false;
        }

        if (!password.equals(rhs.password))
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(hostName, port, username, password);
    }

    @Override
    public String toString()
    {
        return hostName;
    }

    public String toPropertyEntry()
    {
        return String.format("%d %s %d %s%s",
                                port, username,
                                (lastUsed == null ? 0 : lastUsed.getTime()),
                                password,
                                (tls != TlsMode.NONE ? " " + tls.name() : ""));
    }


    public static LrgsConnection fromDdsFile(String host, String input)
    {
        final String parts[] = input.split("\\s+");
        TlsMode tls = TlsMode.NONE;
        final int port = Integer.parseInt(parts[0].replace("/TLS", ""));
        String username = "<set_me>";
        long lastUsed = 0;
        String password = "<set_me>";
        // Some older files only container the host portion.
        // this allows them to be successfully loaded and saved
        // while informing the user which values need
        // updating.
        if (parts.length > 1)
        {
            username = parts[1];
        }
        if (parts.length > 2)
        {
            lastUsed = Long.parseLong(parts[2]);
        }
        if (parts.length > 3)
        {
            password = parts[3];
        }
        if (parts.length > 4)
        {
            tls = TlsMode.valueOf(parts[4].trim());
        }

        return new LrgsConnection(host, port, username, password, new Date(lastUsed), tls);
    }

    public static String decryptPassword(LrgsConnection c, String key)
    {
        final String encrypted = c.getPassword();
        if (encrypted == null || encrypted.isEmpty())
        {
            return "";
        }
        try
        {
            final DesEncrypter de = new DesEncrypter(key);
            final String password = de.decrypt(encrypted);
            return password;
        }
        catch (AuthException ex)
        {
            log.atTrace()
               .setCause(ex)
               .log("Unable to decrypt password from LddsConnection files.");
            return "";
        }
    }

    public static String encryptPassword(String password, String key)
    {
        if (password == null || password.isEmpty())
        {
            return "";
        }
        try
        {
            final DesEncrypter de = new DesEncrypter(key);
            final String passwordEncrypted = de.encrypt(password);
            return passwordEncrypted;
        }
        catch (AuthException ex)
        {
            log.atTrace()
               .setCause(ex)
               .log("Unable to encrypt password for LddsConnection files.");
            return "";
        }
    }
}
