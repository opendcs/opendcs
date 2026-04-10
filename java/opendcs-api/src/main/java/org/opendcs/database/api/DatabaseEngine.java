package org.opendcs.database.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum DatabaseEngine
{
    GENERIC_SQL("ANSI SQL"), // If engine unknown, assume ANSI SQL standards are followed
    ORACLE("Oracle"),
    POSTGRES("PostgreSQL"),
    SQLITE("SQLite"),
    H2("H2"),
    HSQLDB("HSQLDB"),
    MYSQL("MYSQL"),
    MICROSOFT_SQL("Microsoft SQL Server");

    private static final Logger log = LoggerFactory.getLogger(DatabaseEngine.class);

    private final String productString;


    DatabaseEngine(String productString)
    {
        this.productString = productString;
    }

    public static DatabaseEngine from(String text)
    {
        for (var tmp: values())
        {
            if (tmp.productString.equalsIgnoreCase(text))
            {
                return tmp;
            }
        }
        log.warn("Unknown database type '{}', will default to ANSI SQL Standard queries", text);
        return GENERIC_SQL;
    }

    public String getProductString()
    {
        return productString;
    }
}
