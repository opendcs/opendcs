package org.opendcs.database.model.mappers.equipmentmodel;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.database.sql.TableColumnDefinition;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.utils.sql.SqlErrorMessages;

import decodes.db.DatabaseException;
import decodes.db.EquipmentModel;
import decodes.sql.DbKey;

public final class EquipmentModelMapper extends PrefixRowMapper<EquipmentModel,EquipmentModelMapper.Columns>
{
    private EquipmentModelMapper(String prefix)
    {
        super(prefix, Columns.class);
    }

    public static EquipmentModelMapper withPrefix(String prefix)
    {
        return new EquipmentModelMapper(prefix);
    }

    @Override
    public EquipmentModel map(ResultSet rs, StatementContext ctx) throws SQLException
    {
        EquipmentModel em = new EquipmentModel();
        ColumnMapper<DbKey> columnMapperForKey = ctx.findColumnMapperFor(DbKey.class)
                                                    .orElseThrow(() -> new SQLException(SqlErrorMessages.DBKEY_MAPPER_NOT_FOUND));
        try
        {
            em.setId(columnMapperForKey.map(rs, column(Columns.ID), ctx));
            em.name = rs.getString(column(Columns.NAME));
            em.company = rs.getString(column(Columns.COMPANY));
            em.description = rs.getString(column(Columns.DESCRIPTION));
            em.equipmentType = rs.getString(column(Columns.EQUIPMENT_TYPE));
            em.model = rs.getString(column(Columns.MODEL));
        }
        catch (DatabaseException ex)
        {
            throw new SQLException("Unable to set id field on fresh EquipmentModel instance.", ex);
        }
        return em;
    }

    public enum Columns implements TableColumnDefinition
    {
        ID(GenericColumns.ID),
        NAME(GenericColumns.NAME),
        COMPANY("company"),
        DESCRIPTION(GenericColumns.DESCRIPTION),
        EQUIPMENT_TYPE("equipmentType"),
        MODEL("model")
        ;

        private final String column;

        Columns(String column)
        {
            this.column = column;
        }

        Columns(GenericColumns other)
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
