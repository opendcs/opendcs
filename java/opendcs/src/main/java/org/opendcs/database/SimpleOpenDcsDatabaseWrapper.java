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
import java.util.Optional;
import java.util.function.Supplier;

import javax.sql.DataSource;

import decodes.cwms.CwmsLocationLevelDAO;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.JdbiException;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.DatabaseEngine;
import org.opendcs.database.api.Generator;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.EquipmentModelDao;
import org.opendcs.database.dai.UserManagementDao;
import org.opendcs.database.impl.opendcs.dao.EquipmentModelImpl;
import org.opendcs.database.impl.opendcs.dao.UserManagementImpl;
import org.opendcs.database.impl.opendcs.jdbi.column.databasekey.DatabaseKeyArgumentFactory;
import org.opendcs.database.impl.opendcs.jdbi.column.databasekey.DatabaseKeyColumnMapper;
import org.opendcs.settings.api.OpenDcsSettings;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.openide.util.Lookup;
import org.slf4j.Logger;

import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.DatabaseIO;
import decodes.sql.KeyGenerator;
import decodes.sql.KeyGeneratorFactory;
import decodes.tsdb.TimeSeriesDb;
import decodes.util.DecodesSettings;
import opendcs.dai.EnumDAI;
import opendcs.dao.EnumSqlDao;

public class SimpleOpenDcsDatabaseWrapper implements OpenDcsDatabase
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
    protected final DecodesSettings settings;
    private final Database decodesDb;
    private final TimeSeriesDb timeSeriesDb;
    protected final DataSource dataSource;
    protected final Jdbi jdbi;
    protected final DatabaseEngine dbEngine;
    protected final KeyGenerator keyGenerator;
    private final Map<Class<? extends OpenDcsDao>, DaoWrapper<? extends OpenDcsDao>> daoMap = new HashMap<>();

    public SimpleOpenDcsDatabaseWrapper(DecodesSettings settings, Database decodesDb, TimeSeriesDb timeSeriesDb, DataSource dataSource)
    {
        this.settings = settings;
        this.decodesDb = decodesDb;
        this.timeSeriesDb = timeSeriesDb;
        this.dataSource = dataSource;
        this.jdbi = Jdbi.create(dataSource);
        jdbi.registerArgument(new DatabaseKeyArgumentFactory())
            .registerColumnMapper(new DatabaseKeyColumnMapper());
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

        try
        {
            keyGenerator = KeyGeneratorFactory.makeKeyGenerator(settings.sqlKeyGenerator);
        }
        catch (DatabaseException ex)
        {
            throw new IllegalStateException("Unable to create key generator of type '" + settings.sqlKeyGenerator + "'", ex);
        }
    }

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
                if (dao.isAssignableFrom(CwmsLocationLevelDAO.class))
                {
                    return new DaoWrapper<>(() -> new CwmsLocationLevelDAO(this.timeSeriesDb));
                }

                Optional<Method> daoMakeMethod;
                if (timeSeriesDb != null)
                {
                    daoMakeMethod = findDaoMaker(timeSeriesDb.getClass(), dao);
                    if (daoMakeMethod.isPresent())
                    {
                        final Method m = daoMakeMethod.get();
                        return new DaoWrapper<>(() ->
                        {
                            try
                            {
                                T ret = (T)m.invoke(timeSeriesDb);
                                if (ret == null)
                                {
                                    log.atError().log("retrieval of DAO returned null instead of the expected DAO." + timeSeriesDb + " " + m.toGenericString());
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
                }
                DatabaseIO dbIo = this.decodesDb.getDbIo();
                daoMakeMethod = findDaoMaker(dbIo.getClass(), dao);
                if (daoMakeMethod.isPresent())
                {
                    final Method m = daoMakeMethod.get();
                    return new DaoWrapper<>(() ->
                    {
                        try
                        {
                            T ret = (T)m.invoke(this.decodesDb.getDbIo());
                            if (ret == null)
                            {
                                log.atError().log("retrieval of DAO returned null instead of the expected DAO." + this.decodesDb.getDbIo() + " " + m.toGenericString());
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
                return new DaoWrapper<>(() -> null);
            });
        return Optional.ofNullable((T)wrapper.create());
    }

    private <T extends OpenDcsDao> Optional<DaoWrapper<T>> fromLookup(Class<T> dao)
    {
        final var instance = Lookup.getDefault().lookup(dao);
        if (instance != null)
        {
            return Optional.of(new DaoWrapper<>(() -> instance));
        }
        else
        {
            return Optional.empty();
        }
    }

    @Override
    public DataTransaction newTransaction() throws OpenDcsDataException
    {
        try
        {
            // This DataTransaction is auto closable and handles the closing of the
            // Jdbi Handle instance.
            return new JdbiTransaction(jdbi.open().begin(), new TransactionContextImpl(keyGenerator, settings, dbEngine)); // NOSONAR
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

    private static class DaoWrapper<T>
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
        return this.dbEngine;
    }

}
