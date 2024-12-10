package org.opendcs.database;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.api.OpenDcsDatabase;
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
    private final Map<Class<? extends OpenDcsDao>, DaoWrapper<? extends OpenDcsDao>> daoMap = new HashMap<>();

    public SimpleOpenDcsDatabaseWrapper(DecodesSettings settings, Database decodesDb, TimeSeriesDb timeSeriesDb)
    {
        this.settings = settings;
        this.decodesDb = decodesDb;
        this.timeSeriesDb = timeSeriesDb;
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
                DatabaseIO dbIo = this.decodesDb.getDbIo();
                Optional<Method> daoMakeMethod = findDaoMaker(dbIo.getClass(), dao);
                if (daoMakeMethod.isPresent())
                {
                    final Method m = daoMakeMethod.get();
                    return new DaoWrapper<>(() ->
                    {
                        try
                        {
                            return (T)m.invoke(decodesDb.getDbIo());
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

                daoMakeMethod = findDaoMaker(timeSeriesDb.getClass(), dao);
                if (!daoMakeMethod.isPresent())
                {
                    final Method m = daoMakeMethod.get();
                    return new DaoWrapper<>(() ->
                    {
                        try
                        {
                            return (T)m.invoke(timeSeriesDb);
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
        return null;
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
}
