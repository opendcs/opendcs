package org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.configs;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.database.sql.TableColumnDefinition;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;

import decodes.db.PlatformConfig;
import decodes.sql.DbKey;

public final class DecodesConfigMapper extends PrefixRowMapper<PlatformConfig, org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.configs.DecodesConfigMapper.Columns>
{
    public static final String DEFAULT_PREFIX = "pc";
    public static final DecodesConfigMapper DEFAULT_MAPPER = new DecodesConfigMapper(DEFAULT_PREFIX);

    private DecodesConfigMapper(String prefix)
    {
        super(prefix, Columns.class);
    }

    @Override
    public PlatformConfig map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        ColumnMapper<DbKey> columnMapperForKey = ctx.findColumnMapperFor(DbKey.class)
                                                    .orElseThrow(() -> new SQLException(SqlErrorMessages.DBKEY_MAPPER_NOT_FOUND));
        final var pc = new PlatformConfig();
        final var id = columnMapperForKey.map(rs, prefix + GenericColumns.ID, ctx);
        pc.forceSetId(id);
        pc.configName = rs.getString(prefix + GenericColumns.NAME);
        pc.description = rs.getString(prefix + GenericColumns.DESCRIPTION);
        return pc;
    }

    public static DecodesConfigMapper withPrefix(String prefix)
    {
        if (DEFAULT_PREFIX.equalsIgnoreCase(prefix))
        {
            return DEFAULT_MAPPER;
        }
        else
        {
            return new DecodesConfigMapper(prefix);
        }
    }

    public static enum Columns implements TableColumnDefinition
    {
        ID(GenericColumns.ID),
        NAME(GenericColumns.NAME),
        DESCRIPTION(GenericColumns.DESCRIPTION);

        private final String column;

        Columns(TableColumnDefinition other)
        {
            this.column = other.column();
        }

        @Override
        public String column()
        {
            return column;
        }
        
    }
}
