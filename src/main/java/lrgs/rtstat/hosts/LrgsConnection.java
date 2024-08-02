package lrgs.rtstat.hosts;

import java.util.Date;
import java.util.Objects;

/**
 * Keep the information about a connection to an LRGS for the ComboBox model.
 */
public class LrgsConnection
{
    public static LrgsConnection BLANK = new LrgsConnection("", -1, "", "", null);

    final String hostName;
    final int port;
    final String username;
    final String password;
    final Date lastUsed;


    public LrgsConnection(String hostName, int port, String username, String password, Date lastUsed)
    {
        this.hostName = hostName;
        this.port = port;
        this.username = username;
        this.password = password;
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
        return Objects.equals(this, other);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(hostName, port, username, password);
    }

    @Override
    public String toString()
    {
        return String.format("%s=%d %s %d %s", hostName, port, username, lastUsed.getTime(), password);
    }


    public static LrgsConnection fromDdsFile(String input)
    {
        final String parts[] = input.split("\\s+");
        final String hostNameAndPort = parts[0];
        final String username = parts[1];
        final Long lastUsed = Long.parseLong(parts[2]);
        final String password = parts[3];

        final String parts2[] = hostNameAndPort.split("=");

        return new LrgsConnection(parts2[0], Integer.parseInt(parts2[1]), 
                                  username, password, new Date(lastUsed));
    }

    public static String decryptPassword(LrgsConnection c, String key)
    {
        return null;
    }

    public String encryptPassword(LrgsConnection c, String key)
    {
        return null;
    }
}
