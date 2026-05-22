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
        // see if I'm the parent to anyone and assign
        previous.values()
                .stream()
                .filter(ppg -> pg.groupName.equals(ppg.inheritsFrom))
                .forEach(ppg -> ppg.parent = pg);
        final var parentName = rs.getString(prefix+"inheritsfrom");

        // see if my child group needs parent assigned.
        if (!rs.wasNull() && pg.parent == null)
        {
            var parent = previous.values()
                                 .stream()
                                 .filter(ppg -> parentName.equals(ppg.groupName))
                                 .findAny()
                                 .orElse(null);
            if (parent != null)
            {
                pg.parent = parent;
                pg.inheritsFrom = pg.parent.groupName;
            } // other hasn't been loaded yet, will get picked up by the first parent/child check
        }

        return previous;
    }

      
}
