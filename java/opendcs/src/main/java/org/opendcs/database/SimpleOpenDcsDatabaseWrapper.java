package org.opendcs.database;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import javax.sql.DataSource;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.settings.api.OpenDcsSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import decodes.db.Database;
import decodes.db.DatabaseIO;
import decodes.tsdb.TimeSeriesDb;
import decodes.util.DecodesSettings;

public class SimpleOpenDcsDatabaseWrapper implements OpenDcsDatabase
{
    private static final Logger log = LoggerFactory.getLogger(SimpleOpenDcsDatabaseWrapper.class);
    private final DecodesSettings settings;
    private final Database decodesDb;
    private final TimeSeriesDb timeSeriesDb;
    private final DataSource dataSource;
    private final Map<Class<? extends OpenDcsDao>, DaoWrapper<? extends OpenDcsDao>> daoMap = new HashMap<>();

    public SimpleOpenDcsDatabaseWrapper(DecodesSettings settings, Database decodesDb, TimeSeriesDb timeSeriesDb, DataSource dataSource)
    {
        this.settings = settings;
        this.decodesDb = decodesDb;
        this.timeSeriesDb = timeSeriesDb;
        this.dataSource = dataSource;
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

    @Override
    public <T extends OpenDcsDao> Optional<T> getDao(Class<T> dao)
    {
        @SuppressWarnings("unchecked")
        DaoWrapper<?> wrapper =
            daoMap.computeIfAbsent(dao, daoDesired ->
            {
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

    @Override
    public DataTransaction newTransaction() throws OpenDcsDataException
    {
        try
        {
            return new SimpleTransaction(this.dataSource.getConnection());
        }
        catch (SQLException ex)
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
}
