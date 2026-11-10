package org.opendcs.database.impl.opendcs.sqlite;

import java.sql.Connection;

import decodes.db.DatabaseException;
import decodes.sql.DbKey;
import decodes.sql.KeyGenerator;

/**
 * Is this a terrible idea, yes. But SQlite doesn't have sequences
 * and we haven't modified DbKey to be an interface such that we could use a UUID yet.
 */
public class RandomKeyGenerator implements KeyGenerator
{
    java.util.Random keyGen = new java.security.SecureRandom();

    @Override
    public DbKey getKey(String tableName, Connection conn) throws DatabaseException
    {
        return DbKey.createDbKey(keyGen.nextLong());
    }

    @Override
    public void reset(String tableName, Connection conn) throws DatabaseException
    {
    
    }
    
}
