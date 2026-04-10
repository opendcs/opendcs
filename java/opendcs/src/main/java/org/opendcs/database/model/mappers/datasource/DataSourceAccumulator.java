package org.opendcs.database.model.mappers.datasource;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.result.ResultSetAccumulator;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.utils.sql.GenericColumns;

import decodes.db.DataSource;
import decodes.sql.DbKey;

import static org.opendcs.database.model.mappers.PrefixRowMapper.addUnderscoreIfMissing;

public final class DataSourceAccumulator implements ResultSetAccumulator<Map<DbKey,DataSource>>
{
    public static final DataSourceAccumulator DATA_SOURCE_ACCUMULATOR = new DataSourceAccumulator("ds", "dsm");

    private final String primaryPrefix;
    private final String memberPrefix;

    private final DataSourceMapper primaryMapper;
    private final DataSourceMapper memberMapper;

    private DataSourceAccumulator(String primaryPrefix, String memberPrefix)
    {
        this.primaryPrefix = primaryPrefix == null ? "" : addUnderscoreIfMissing(primaryPrefix);
        this.memberPrefix = memberPrefix == null ? "" : addUnderscoreIfMissing(memberPrefix);
        primaryMapper = DataSourceMapper.withPrefix(primaryPrefix);
        memberMapper = DataSourceMapper.withPrefix(memberPrefix);

    }

    @Override
    public Map<DbKey,DataSource> apply(Map<DbKey,DataSource> previous, ResultSet rs, StatementContext ctx) throws SQLException
    {
        ColumnMapper<DbKey> dbKeyMapper = ctx.findColumnMapperFor(DbKey.class)
                                             .orElseThrow(() -> new SQLException("No mapper registered for DbKey class."));
        var primaryId = dbKeyMapper.map(rs, primaryPrefix+GenericColumns.ID, ctx);
        DataSource primaryDs = null;
        try
        {
            primaryDs = previous.computeIfAbsent(primaryId, key ->
            {
                try
                {
                    return primaryMapper.map(rs, ctx);
                }
                catch (SQLException ex)
                {
                    // We have to use RuntimeException to escape the computeIfAbsent when there
                    // is an error.
                    throw new RuntimeException(ex); // NOSONAR
                }
            });
        }
        catch (RuntimeException ex)
        {
            if (ex.getCause() instanceof SQLException sqlException)
            {
                throw sqlException;
            }
            throw ex;
        }
        int sequence = rs.getInt(memberPrefix+"sequencenum");
        if (!rs.wasNull())
        {
            primaryDs.addGroupMember(sequence, memberMapper.map(rs, ctx));
        }

        return previous;
    }
}
