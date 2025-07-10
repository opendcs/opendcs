package org.opendcs.database.impl.opendcs;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.JdbiException;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.DatabaseKey;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.api.dao.PropertiesDAO;
import org.opendcs.utils.Property;

import decodes.tsdb.DbIoException;
import ilex.util.Logger;
import ilex.util.Pair;

public class PropertiesDaoImpl implements PropertiesDAO {

    @Override
    public void writeProperties(DataTransaction tx, String tableName, String idColumn, DatabaseKey parentKey,
            Properties props) throws OpenDcsDataException {
        Objects.requireNonNull(props, "A valid properties object must be passed into this function.");
        Objects.requireNonNull(tableName, "A valid table name is required.");
        Objects.requireNonNull(parentKey, "A valid Database Identifier is required.");
        Objects.requireNonNull(idColumn, "The id column must be valid.");
        final String columnNames = " ("+idColumn +",prop_name,prop_value) ";
        final String q = "insert into " + tableName + columnNames+ " values(:id,:prop,:value)";
        
        
        Handle handle = tx.connection(Handle.class)
                          .orElseThrow(() -> new OpenDcsDataException("Unable to acquire JDBI Handle From transaction object."));

        try
        {
            final PreparedBatch query = handle.prepareBatch(q);
            props.forEach((k,v) -> 
            {
                query.bind("id", parentKey);
                query.bind("prop", k);
                query.bind("value", v);
                query.add();
            });
            deleteProperties(tx, tableName, idColumn, parentKey);
            query.execute();
        }
        catch (JdbiException ex)
        {
            throw new OpenDcsDataException("Unable to write properties", ex);
        }
    }

    @Override
    public void writeProperties(DataTransaction tx, String tableName, String idColumn, String id2Column,
            DatabaseKey parentKey, int key2, Properties props) throws OpenDcsDataException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'writeProperties'");
    }

    @Override
    public Properties readProperties(DataTransaction tx, String tableName, String idColumn, DatabaseKey parentKey)
            throws OpenDcsDataException
    {
        Objects.requireNonNull(tableName, "A valid table name is required.");
        Objects.requireNonNull(parentKey, "A valid Database Identifier is required.");
        Objects.requireNonNull(idColumn, "The id column must be valid.");
        String q = "select prop_name, prop_value from " + tableName + " where " + idColumn + " = :id";
        final Properties props = new Properties();

        Handle handle = tx.connection(Handle.class)
                          .orElseThrow(() -> new OpenDcsDataException("Unable to acquire JDBI Handle From transaction object."));

        try
        {
            handle.createQuery(q).bind("id", parentKey)
                  .map( (rs, ctx) -> 
                  {
                    String name = rs.getString("prop_name");
                    String value = rs.getString("prop_value");
                    if (value == null)
                    {
                        value = "";
                    }
                    return Pair.of(name, value);
                  }
                  ).list().forEach(pair ->
                  {
                    props.setProperty(pair.first, pair.second);
                  });
                    

            return props;
        }
        catch (JdbiException ex)
        {
            throw new OpenDcsDataException("Unable to read properties", ex);
        }
    }

    @Override
    public Properties readProperties(DataTransaction tx, String tableName, String idColumn, String id2Column,
            DatabaseKey parentKey, int key2) throws OpenDcsDataException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'readProperties'");
    }

    @Override
    public void deleteProperties(DataTransaction tx, String tableName, String idColumn, DatabaseKey parentKey)
            throws OpenDcsDataException
    {
        Objects.requireNonNull(tableName, "A valid table name is required.");
        Objects.requireNonNull(parentKey, "A valid Database Identifier is required.");
        Objects.requireNonNull(idColumn, "The id column must be valid.");
        final String q = "delete from " + tableName + " where " + idColumn + " = :id";
        
        
        Handle handle = tx.connection(Handle.class)
                          .orElseThrow(() -> new OpenDcsDataException("Unable to acquire JDBI Handle From transaction object."));
        
        
        try
        {
            handle.createUpdate(q).bind("id", parentKey).execute();
        }
        catch (JdbiException ex)
        {
            throw new OpenDcsDataException("Unable to delete properties", ex);
        }
        
    }

    @Override
    public void deleteProperties(DataTransaction tx, String tableName, String idColumn, String id2Column,
            DatabaseKey parentKey, int key2) throws OpenDcsDataException
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteProperties'");
    }
}
