package decodes.xml.jdbc;

import java.sql.SQLException;
import java.util.Optional;

import javax.sql.DataSource;

import org.opendcs.database.SimpleOpenDcsDatabaseWrapper;
import org.opendcs.database.SimpleTransaction;
import org.opendcs.database.TransactionContextImpl;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDao;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.impl.xml.dao.EnumXmlDao;

import decodes.db.Database;
import decodes.db.DbEnum;
import decodes.tsdb.TimeSeriesDb;
import decodes.util.DecodesSettings;
import opendcs.dai.EnumDAI;
import opendcs.dao.DbObjectCache;

public final class XmlOpenDcsDatabaseWrapper extends SimpleOpenDcsDatabaseWrapper
{
    private final DbObjectCache<DbEnum> enumCache;

    public XmlOpenDcsDatabaseWrapper(DecodesSettings settings, Database decodesDb, TimeSeriesDb tsDb, DataSource ds)
    {
        super(settings, decodesDb, tsDb, ds);
        enumCache = new DbObjectCache<>(1800_000L, false);
    }

    @SuppressWarnings("unchecked") // types are checked before any operations happen.
    @Override
    public <T extends OpenDcsDao> Optional<T> getDao(Class<T> dao)
    {
        if (dao.isAssignableFrom(EnumDAI.class))
        {
            return Optional.of((T)new EnumXmlDao(getSettings(DecodesSettings.class).get(), enumCache));
        }
        else
        {
            return super.getDao(dao);
        }
        
    }

    @Override
    public DataTransaction newTransaction() throws OpenDcsDataException
    {
        try
        {
            return new SimpleTransaction(this.dataSource.getConnection(),
                                         new TransactionContextImpl(keyGenerator, settings, dbEngine));
        }
        catch (SQLException ex)
        {
            throw new OpenDcsDataException("Unable to get JDBC Connection.", ex);
        }
    }
}
