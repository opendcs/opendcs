package org.opendcs.database;

import java.util.Map;
import java.util.Optional;

import org.opendcs.database.api.DatabaseEngine;
import org.opendcs.database.api.Generator;
import org.opendcs.database.api.TransactionContext;
import org.opendcs.settings.api.OpenDcsSettings;

import decodes.sql.KeyGenerator;

public class TransactionContextImpl implements TransactionContext
{
    private final KeyGenerator keyGenerator;
    private final DatabaseEngine databaseEngine;
    private final Map<Class<? extends OpenDcsSettings>, OpenDcsSettings> settingsMap;

    public TransactionContextImpl(KeyGenerator keyGenerator,
            Map<Class<? extends OpenDcsSettings>, OpenDcsSettings> settingsMap, DatabaseEngine databaseEngine)
    {
        this.keyGenerator = keyGenerator;
        this. settingsMap = settingsMap;
        this.databaseEngine = databaseEngine;
        
    }


    public KeyGenerator getKeyGenerator()
    {
        return this.keyGenerator;
    }

    public DatabaseEngine getDatabaseEngine()
    {
        return this.databaseEngine;
    }


    @SuppressWarnings("unchecked")
    @Override
    public <T extends Generator> Optional<T> getGenerator(Class<T> generatorClass)
    {
        if (KeyGenerator.class.equals(generatorClass))
        {
            return Optional.of((T)this.keyGenerator);
        }
        else
        {
            return Optional.empty();
        }
    }


    @SuppressWarnings("unchecked")
    @Override
    public <T extends OpenDcsSettings> Optional<T> getSettings(Class<T> settingsClass)
    {
        return Optional.ofNullable((T)settingsMap.get(settingsClass));
    }


    @Override
    public DatabaseEngine getDatabase()
    {
        return this.databaseEngine;
    }


}
