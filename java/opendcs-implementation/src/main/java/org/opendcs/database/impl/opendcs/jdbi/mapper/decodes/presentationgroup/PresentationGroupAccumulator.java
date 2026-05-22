package org.opendcs.database.impl.opendcs.jdbi.mapper.decodes.presentationgroup;

import static org.opendcs.database.model.mappers.PrefixRowMapper.addUnderscoreIfMissing;
import static org.opendcs.utils.ExceptionUtil.wrappedComputeIfAbsent;

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
    private final DataPresentationMapper dpMapper;

    public PresentationGroupAccumulator(String prefix, PresentationGroupMapper pgMapper, 
                                        DataPresentationMapper dpMapper)
    {
        this.prefix = addUnderscoreIfMissing(prefix);
        this.pgMapper = pgMapper;
        this.dpMapper = dpMapper;
    }

    @Override
    public Map<Long, PresentationGroup> apply(Map<Long, PresentationGroup> previous, ResultSet rs, StatementContext ctx)
            throws SQLException
    {
        final var id = rs.getLong(prefix + GenericColumns.ID);

        final var pg = wrappedComputeIfAbsent(previous, id, newId -> pgMapper.map(rs, ctx), SQLException.class);

        final var dataPresentation = dpMapper.map(rs, ctx);
        if (dataPresentation != null)
        {
            pg.addDataPresentation(dataPresentation);
        }

        final var parentId = rs.getLong(prefix+"inheritsfrom");
        if (!rs.wasNull() && pg.parent == null && previous.containsKey(parentId))
        {
            pg.parent = previous.get(parentId);
            pg.inheritsFrom = pg.parent.groupName;
        }
        return previous;
    }

      
}
