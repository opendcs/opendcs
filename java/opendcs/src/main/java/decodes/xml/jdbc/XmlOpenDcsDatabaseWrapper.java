package decodes.xml.jdbc;

import java.util.Optional;

import javax.sql.DataSource;

import org.opendcs.database.SimpleOpenDcsDatabaseWrapper;
import org.opendcs.database.api.OpenDcsDao;
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
}
