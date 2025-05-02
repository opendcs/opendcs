package lrgs.rtstat.hosts;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.Objects;
import java.util.function.Predicate;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import nl.altindag.ssl.SSLFactory;
import nl.altindag.ssl.model.TrustManagerParameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ilex.util.AuthException;
import ilex.util.DesEncrypter;
import ilex.util.EnvExpander;

/**
 * Keep the information about a connection to an LRGS for the ComboBox model.
 */
public final class LrgsConnection
{
    private static final Logger log = LoggerFactory.getLogger(LrgsConnection.class);
    public static final LrgsConnection BLANK = new LrgsConnection("", -1, "", "", null, false);

    private final String hostName;
    private final int port;
    private final boolean tls;
    private final String username;
    private final String password;
    private final Date lastUsed;


    public LrgsConnection(String hostName, int port, String username, String password, Date lastUsed, boolean tls)
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

    public boolean getTls()
    {
        return tls;
    }

    public SocketFactory getSocketFactory()
    {
        return getSocketFactory(p -> false);
    }

    public SocketFactory getSocketFactory(Predicate<TrustManagerParameters> certTest)
    {
        if (tls)
        {
           return socketFactory(certTest);
		}  
        else
        {
            return null;
        }
    }

    public static SocketFactory socketFactory(Predicate<TrustManagerParameters> certTest)
    {
        SSLFactory sslFactory = SSLFactory.builder()
                                          .withDefaultTrustMaterial()
                                          .withSystemTrustMaterial()
                                          .withInflatableTrustMaterial(
                                            Paths.get(EnvExpander.expand("$DCSTOOL_USERDIR/local_trust.p12")),
                                            "local_trust".toCharArray(),
                                            "PKCS12", 
                                            certTest)
                                          .build();
        return sslFactory.getSslContext().getSocketFactory();
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
                                (tls ? " TLS" : ""));
    }


    public static LrgsConnection fromDdsFile(String host, String input)
    {
        final String parts[] = input.split("\\s+");
        boolean tls = false;
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
        if (parts.length > 4 && parts[4].equals("TLS"))
        {
            tls = true;
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
