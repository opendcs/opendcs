package org.opendcs.database.model.mappers.datasource;

import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;

import decodes.db.DataSource;
import decodes.sql.DbKey;

public final class DataSourceAccumulator implements LinkedHashMapRowReducer<DbKey,DataSource>
{
    // NOTE, this doesn't actually work due to type erasure.
    public static final GenericType<DataSource> PRIMARY_SOURCE = new GenericType<>()
    {
        /* marker type */    
    };
    public static final GenericType<DataSource> MEMBER_SOURCE = new GenericType<>()
    {
        /* marker type */
    };

    public final DataSourceMapper primaryMapper;
    public final DataSourceMapper memberMapper;

    public DataSourceAccumulator(DataSourceMapper primaryMapper, DataSourceMapper memberMapper)
    {
        this.primaryMapper = primaryMapper;
        this.memberMapper = memberMapper;

    }

    @Override
    public void accumulate(Map<DbKey,DataSource> previous, RowView rowView)
    {
        try
        {
            final var primaryDs = previous.computeIfAbsent(
                rowView.getColumn(primaryMapper.column(DataSourceMapper.Columns.ID), DbKey.class),
                newKey -> rowView.getRow(DataSource.class, primaryMapper.getPrefix())
            );

            var sequence = rowView.getColumn(memberMapper.column(DataSourceMapper.Columns.SEQUENCE_NUMBER),
                                             Integer.class);
            if (sequence != null)
            {
                var member = rowView.getRow(DataSource.class, memberMapper.getPrefix());
                primaryDs.addGroupMember(sequence, member);
            }
        }
        catch (SQLException ex)
        {
            throw new UnableToExecuteStatementException("Unable to map ID column to prefix", ex, null);
        }
    }
}
