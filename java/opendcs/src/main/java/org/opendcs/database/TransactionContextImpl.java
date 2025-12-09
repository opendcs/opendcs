package org.opendcs.database;

import java.util.Optional;

import org.opendcs.database.api.DatabaseEngine;
import org.opendcs.database.api.Generator;
import org.opendcs.database.api.TransactionContext;
import org.opendcs.settings.api.OpenDcsSettings;

import decodes.sql.KeyGenerator;
import decodes.util.DecodesSettings;

public class TransactionContextImpl implements TransactionContext
{
    private final KeyGenerator keyGenerator;
    private final DecodesSettings settings;
    private final DatabaseEngine databaseEngine;

    public TransactionContextImpl(KeyGenerator keyGenerator, DecodesSettings settings, DatabaseEngine databaseEngine)
    {
        this.keyGenerator = keyGenerator;
        this.settings = settings;
        this.databaseEngine = databaseEngine;
    }


    public KeyGenerator getKeyGenerator()
    {
        return this.keyGenerator;
    }


    public DecodesSettings getSettings() {
        return this.settings;
    }


    public DatabaseEngine getDatabaseEngine() {
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
        if (DecodesSettings.class.equals(settingsClass))
        {
            return Optional.of((T)settings);
        }
        else
        {
            return Optional.empty();
        }
    }


    @Override
    public DatabaseEngine getDatabase()
    {
        return this.databaseEngine;
    }


}
