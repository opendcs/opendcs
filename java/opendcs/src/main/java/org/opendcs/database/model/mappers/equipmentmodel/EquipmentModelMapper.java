package org.opendcs.database.model.mappers.equipmentmodel;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.opendcs.database.model.mappers.PrefixRowMapper;
import org.opendcs.utils.sql.SqlErrorMessages;

import decodes.db.DatabaseException;
import decodes.db.EquipmentModel;
import decodes.sql.DbKey;

public class EquipmentModelMapper extends PrefixRowMapper<EquipmentModel> 
{
  private EquipmentModelMapper(String prefix)
    {
        super(prefix);
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
            em.setId(columnMapperForKey.map(rs, prefix+"id", ctx));
            em.name = rs.getString(prefix+"name");
            em.company = rs.getString(prefix+"company");
            em.description = rs.getString(prefix+"description");
            em.equipmentType = rs.getString(prefix+"equipmentType");
            em.model = rs.getString(prefix+"model");
        }
        catch (DatabaseException ex)
        {
            throw new SQLException("Unable to set id field on fresh EquipmentModel instance.", ex);
        }
        return null;
    }
    
}
