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
package org.opendcs.database;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import javax.sql.DataSource;

import decodes.cwms.CwmsLocationLevelDAO;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.JdbiException;
import org.jdbi.v3.core.statement.SqlStatements;
import org.opendcs.annotations.api.InjectDao;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.DatabaseEngine;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDaoConfigurationException;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.api.OpenDcsDataRuntimeException;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.impl.opendcs.jdbi.column.chrono.OpenDcsTimeColumn;
import org.opendcs.database.impl.opendcs.jdbi.column.chrono.OpenDcsTimeColumnArgumentFactory;
import org.opendcs.database.impl.opendcs.jdbi.column.databasekey.DatabaseKeyArgumentFactory;
import org.opendcs.database.impl.opendcs.jdbi.column.databasekey.DatabaseKeyColumnMapper;
import org.opendcs.settings.api.OpenDcsSettings;
import org.opendcs.utils.AnnotationHelpers;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.slf4j.Logger;

import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.DatabaseIO;
import decodes.sql.KeyGenerator;
import decodes.sql.KeyGeneratorFactory;
import decodes.tsdb.TimeSeriesDb;
import decodes.util.DecodesSettings;

public abstract class AbstractJdbiOpenDcsDatabaseWrapper implements OpenDcsDatabase
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
    private final Database decodesDb;
    private final TimeSeriesDb timeSeriesDb;
    protected final DataSource dataSource;
    protected final Jdbi jdbi;
    protected final DatabaseEngine dbEngine;
    protected final KeyGenerator keyGenerator;
    protected final Map<Class<? extends OpenDcsSettings>, OpenDcsSettings> settingsMap = new HashMap<>();
    private final Map<Class<? extends OpenDcsDao>, DaoWrapper<? extends OpenDcsDao>> daoMap = new HashMap<>();

    protected AbstractJdbiOpenDcsDatabaseWrapper(
            Map<Class<? extends OpenDcsSettings>, OpenDcsSettings> settings, Database decodesDb, TimeSeriesDb timeSeriesDb, DataSource dataSource)
    {
        Objects.requireNonNull(settings.get(DecodesSettings.class), 
                               "All implementations are required to provide a `DecodesSettings` instance.");
        this.settingsMap.putAll(settings);
        this.decodesDb = decodesDb;
        this.timeSeriesDb = timeSeriesDb;
        this.dataSource = dataSource;
        this.jdbi = Jdbi.create(dataSource);
        jdbi.registerArgument(new DatabaseKeyArgumentFactory())
            .registerColumnMapper(new DatabaseKeyColumnMapper())
            .registerArgument(new OpenDcsTimeColumnArgumentFactory())
            .registerColumnMapper(new OpenDcsTimeColumn());

        // Default behavior
        // allow/exepect implementations to refine. Deriving things like
        // constraint errors the SQLException and passing them on as specific OpenDcsExceptions.
        // handlers are attempted in reverse order of operation: https://jdbi.org/#_exception_handling
        jdbi.getConfig(SqlStatements.class).addExceptionHandler((ex, ctx) ->
        {
            throw new OpenDcsDataRuntimeException("Error during query operation", ex);
        });

        if (this.timeSeriesDb != null)
        {
            this.timeSeriesDb.setDcsDatabase(this);
        }

        try (var tx = newTransaction();
             var conn = tx.connection(Connection.class)
                          .orElseThrow(() -> new IllegalStateException("Unable to retrieve connection object.")))
        {
            dbEngine = DatabaseEngine.from(conn.getMetaData().getDatabaseProductName());
        }
        catch (SQLException | OpenDcsDataException ex)
        {
            throw new IllegalStateException("Unable to determine database type", ex);
        }

        DecodesSettings decodesSettings = (DecodesSettings)settingsMap.get(DecodesSettings.class);
        try
        {
            keyGenerator = KeyGeneratorFactory.makeKeyGenerator(decodesSettings.sqlKeyGenerator);
        }
        catch (DatabaseException ex)
        {
            throw new IllegalStateException("Unable to create key generator of type '" + decodesSettings.sqlKeyGenerator + "'", ex);
        }
        initialSetup();
    }

    /**
     * Handle any additional setup or overrides to what was previously done.
     */
    protected abstract void initialSetup();

    @SuppressWarnings("unchecked") // class is checked before casting
    @Override
    public <T> Optional<T> getLegacyDatabase(Class<T> legacyDatabaseType)
    {
        if (Database.class.equals(legacyDatabaseType))
        {
            return Optional.of((T)decodesDb);
        }
        else if (TimeSeriesDb.class.isAssignableFrom(legacyDatabaseType))
        {
            // The XML database does not currently have timeseries.
            return Optional.ofNullable((T)timeSeriesDb);
        }
        else
        {
            return Optional.empty();
        }
    }

    public DataSource getDataSource()
    {
        return dataSource;
    }

    /**
     * Allow implementation to manually add specific types that
     * may need some custom setup.
     * @param type
     * @param wrapper
     */
    protected void mapDao(Class<? extends OpenDcsDao> type, DaoWrapper<? extends OpenDcsDao> wrapper)
    {
        this.daoMap.put(type, wrapper);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends OpenDcsDao> Optional<T> getDao(Class<T> dao)
    {
        DaoWrapper<?> wrapper =
            daoMap.computeIfAbsent(dao, daoDesired ->
            {
                Optional<DaoWrapper<T>> tmp = fromLookup(dao);

                if (tmp.isPresent())
                {
                    return tmp.get();
                }

                Optional<Method> daoMakeMethod;
                if (timeSeriesDb != null)
                {
                    daoMakeMethod = findDaoMaker(timeSeriesDb.getClass(), dao);
                    if (daoMakeMethod.isPresent())
                    {
                        final Method m = daoMakeMethod.get();
                        return makeDaoWrapper(() -> timeSeriesDb, m);
                    }
                }
                DatabaseIO dbIo = this.decodesDb.getDbIo();
                daoMakeMethod = findDaoMaker(dbIo.getClass(), dao);
                if (daoMakeMethod.isPresent())
                {
                    final Method m = daoMakeMethod.get();
                    return makeDaoWrapper(() -> this.decodesDb.getDbIo(), m);
                }

                return new DaoWrapper<>(() -> null);
            });
        return Optional.ofNullable((T)wrapper.create());
    }

    @SuppressWarnings("unchecked")
    private <T,K> DaoWrapper<T> makeDaoWrapper(Supplier<K> instance, Method method)
    {
        return new DaoWrapper<T>(() ->
        {
            try
            {
                T ret = (T)method.invoke(instance.get());
                if (ret == null)
                {
                    log.atError()
                       .log("retrieval of DAO returned null instead of the expected DAO. {}::{}",
                            instance.get().getClass().getName(),
                            method.toGenericString());
                }
                return ret;
            }
            catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex)
            {
                log.atError()
                    .setCause(ex)
                    .log("Unable to retrieve DAO we should be able to get.");
                return null;
            }
        });
    }

    /**
     * Does lookup for the current implementation first, and then attempts to retrieve a
     * generic implementation.
     * @param <T>
     * @param dao
     * @return
     */
    private <T extends OpenDcsDao> Optional<DaoWrapper<T>> fromLookup(Class<T> dao)
    {
        final String impl = ((DecodesSettings)this.settingsMap.get(DecodesSettings.class)).editDatabaseType;
        final var implLookup = Lookups.forPath("dao/"+impl);
        var instance = implLookup.lookup(dao);
        if (instance == null)
        {
            instance = Lookup.getDefault().lookup(dao);
        }

        if (instance != null)
        {
            final var tmp = instance;
            injectDaos(tmp);
            return Optional.of(new DaoWrapper<>(() -> tmp));
        }
        else
        {
            log.trace("No DAO instance of '{}' found for implementation '{}' or as default", dao.getName(), impl);
            return Optional.empty();
        }
    }

    /**
     * Wait, but can't recursion happen?
     * @param dao
     * @throws OpenDcsDaoConfigurationException if unable to inject dependency for any reason. This is a runtime exception as
     * it should be rare and caught during testing.
     */
    @SuppressWarnings({"unchecked","java:S3011"}) // unchecked -> we check manually, java:S3001 -> Our current setup does not allow for constructor based injection and
                                                  // we want to allow for private/protected fields.
    private void injectDaos(OpenDcsDao dao)
    {
        var fieldpairs = AnnotationHelpers.getFieldsWithAnnotation(dao.getClass(), InjectDao.class);
        for (var pair: fieldpairs)
        {
            var anno = pair.second;
            var field = pair.first;

            var fieldClass = field.getType();
            if (!OpenDcsDao.class.isAssignableFrom(fieldClass))
            {
                throw new OpenDcsDaoConfigurationException("Field " + field.getName() +
                                               " in class" + fieldClass.getName() +
                                               " is not a type of " + OpenDcsDao.class.getName());
            }
            final var daoClass = (Class<? extends OpenDcsDao>)fieldClass;
            var instance = fromLookup(daoClass);
            if (instance.isPresent())
            {
                field.setAccessible(true);
                try
                {
                    field.set(dao, instance.get().create());
                }
                catch (IllegalArgumentException | IllegalAccessException ex)
                {
                    throw new OpenDcsDaoConfigurationException("Unable to assign DAO to this instance", ex);
                }
            }
            else if (!anno.optional())
            {
                throw new OpenDcsDaoConfigurationException("An instance Required DAO " + daoClass.getName() + " is not available.");
            }


        }


    }

    @Override
    public DataTransaction newTransaction() throws OpenDcsDataException
    {
        try
        {
            // This DataTransaction is auto closable and handles the closing of the
            // Jdbi Handle instance.
            return new JdbiTransaction(jdbi.open().begin(),
                                       new TransactionContextImpl(keyGenerator, settingsMap, dbEngine)); // NOSONAR
        }
        catch (JdbiException ex)
        {
            throw new OpenDcsDataException("Unable to get JDBC Connection.", ex);
        }
    }

    private Optional<Method> findDaoMaker(Class<?> implClass, Class<? extends OpenDcsDao> daoType)
    {
        Optional<Method> makerMethod = Optional.empty();
        for(Method method: implClass.getMethods())
        {
            if (method.getReturnType().equals(daoType))
            {
                makerMethod = Optional.of(method);
                break;
            }
        }
        return makerMethod;
    }

    protected static class DaoWrapper<T>
    {
        private final Supplier <T> daoSupplier;
        public DaoWrapper(Supplier<T> daoSupplier)
        {
            this.daoSupplier = daoSupplier;
        }
        public T create()
        {
            return daoSupplier.get();
        }
    }

    @SuppressWarnings("unchecked") // Types are checked manually in this function
    @Override
    public <T extends OpenDcsSettings> Optional<T> getSettings(Class<T> settingsClass)
    {
        return Optional.ofNullable((T)settingsMap.get(settingsClass));
    }

    @Override
    public DatabaseEngine getDatabaseEngine()
    {
        return this.dbEngine;
    }

}
