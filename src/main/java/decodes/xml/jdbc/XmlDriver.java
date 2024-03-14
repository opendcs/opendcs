package decodes.xml.jdbc;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

import org.slf4j.LoggerFactory;

public class XmlDriver implements Driver
{
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(XmlDriver.class);
    private static final Driver INSTANCE = new XmlDriver();
    private static boolean registered;

    @Override
    public Connection connect(String url, Properties info) throws SQLException
    {
        return new XmlConnection(url);
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException
    {
        return url.startsWith("jdbc:xml");
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion() 
    {
        return 1;
    }

    @Override
    public int getMinorVersion() 
    {
        return 0;
    }

    @Override
    public boolean jdbcCompliant()
    {
        return false;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getParentLogger'");
    }
    
    static
    {
        if (!registered)
        {
            registered = true;
            try
            {
                DriverManager.registerDriver(INSTANCE);
            }
            catch (SQLException ex)
            {
                log.atError()
                   .setCause(ex)
                   .log("Unable to load XML driver.");
            }
        }
    }
}
