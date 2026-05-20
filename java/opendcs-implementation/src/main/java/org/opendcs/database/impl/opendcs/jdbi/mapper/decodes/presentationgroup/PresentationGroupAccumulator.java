package org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.presentationgroup;

import static org.opendcs.database.model.mappers.PrefixRowMapper.addUnderscoreIfMissing;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.jdbi.v3.core.result.ResultSetAccumulator;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.utils.sql.GenericColumns;

import decodes.db.PresentationGroup;

public class PresentationGroupAccumulator implements ResultSetAccumulator<Map<Long,PresentationGroup>>
{
    private final String prefix;
    private final PresentationGroupMapper pgMapper;

    public PresentationGroupAccumulator(String prefix, PresentationGroupMapper pgMapper)
    {
        this.prefix = addUnderscoreIfMissing(prefix);
        this.pgMapper = pgMapper;
    }

    @Override
    public Map<Long, PresentationGroup> apply(Map<Long, PresentationGroup> previous, ResultSet rs, StatementContext ctx)
            throws SQLException
    {
        final var id = rs.getLong(prefix + GenericColumns.ID);

        final var pg = previous.computeIfAbsent(id, newId -> pgMapper.map(rs, ctx));


        return previous;
    }

      
}
