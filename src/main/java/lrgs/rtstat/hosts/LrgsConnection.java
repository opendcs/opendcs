package lrgs.rtstat.hosts;

import java.util.Date;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ilex.util.AuthException;
import ilex.util.DesEncrypter;

/**
 * Keep the information about a connection to an LRGS for the ComboBox model.
 */
public final class LrgsConnection
{
    private static final Logger log = LoggerFactory.getLogger(LrgsConnection.class);
    public static final LrgsConnection BLANK = new LrgsConnection("", -1, "", "", null);

    private final String hostName;
    private final int port;
    private final String username;
    private final String password;
    private final Date lastUsed;


    public LrgsConnection(String hostName, int port, String username, String password, Date lastUsed)
    {
        this.hostName = hostName;
        this.port = port;
        this.username = username;
        this.password = password;
        this.lastUsed = lastUsed;
    }

    public LrgsConnection(LrgsConnection c, Date lastUsed)
    {
        this.hostName = c.hostName;
        this.port = c.port;
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
        return String.format("%d %s %d %s",
                                port, username,
                                (lastUsed == null ? 0 : lastUsed.getTime()),
                                password);
    }


    public static LrgsConnection fromDdsFile(String host, String input)
    {
        final String parts[] = input.split("\\s+");
        final int port = Integer.parseInt(parts[0]);
        final String username = parts[1];
        final Long lastUsed = Long.parseLong(parts[2]);
        final String password = parts[3];

        return new LrgsConnection(host, port,
                                  username, password, new Date(lastUsed));
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
