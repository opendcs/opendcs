package org.opendcs.implementations.xml;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import decodes.db.DatabaseException;
import decodes.sql.DbKey;
import decodes.sql.KeyGenerator;

public class MemoryKeyGenerator implements KeyGenerator
{

    final Map<String,AtomicLong> keysByTable = new HashMap<>();

    @Override
    public DbKey getKey(String tableName, Connection conn) throws DatabaseException
    {
        Long value = keysByTable.computeIfAbsent(tableName, t -> {
            return new AtomicLong();
        }).incrementAndGet();
        return DbKey.createDbKey(value);
    }

    @Override
    public void reset(String tableName, Connection conn) throws DatabaseException
    {
        keysByTable.computeIfAbsent(tableName, (t) -> new AtomicLong(0))
                   .set(0);
    }
    
}
