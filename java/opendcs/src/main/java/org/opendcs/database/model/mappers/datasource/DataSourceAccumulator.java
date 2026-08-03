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
                newKey -> rowView.getRow(PRIMARY_SOURCE)
            );

            var sequence = rowView.getColumn(memberMapper.column(DataSourceMapper.Columns.SEQUENCE_NUMBER),
                                             Integer.class);
            if (sequence != null)
            {
                var member = rowView.getRow(MEMBER_SOURCE);
                primaryDs.addGroupMember(sequence, member);
            }
        }
        catch (SQLException ex)
        {
            throw new UnableToExecuteStatementException("Unable to map ID column to prefix", ex, null);
        }
    }

    public static class DataSourceMapperFactory implements RowMapperFactory
    {
        private final DataSourceMapper primaryMapper;
        private final DataSourceMapper memberMapper;

        public DataSourceMapperFactory(DataSourceMapper primaryMapper, DataSourceMapper memberMapper)
        {
            this.primaryMapper = primaryMapper;
            this.memberMapper = memberMapper;
        }

        @Override
        public Optional<RowMapper<?>> build(Type type, ConfigRegistry config)
        {
            Optional<RowMapper<?>> ret = Optional.empty();
            if (type == PRIMARY_SOURCE)
            {
                ret = Optional.of(primaryMapper);
            }
            else if (type == MEMBER_SOURCE)
            {
                ret = Optional.of(memberMapper);
            }
            return ret;
        }
    }
}
