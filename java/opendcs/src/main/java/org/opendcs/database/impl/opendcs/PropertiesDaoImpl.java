package org.opendcs.database.impl.opendcs;

import java.util.Objects;
import java.util.Properties;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.JdbiException;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.core.statement.Update;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.DatabaseKey;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.api.dao.PropertiesDAO;

import ilex.util.Pair;

/**
 * New implementation of Properties DAO that handle stateless behavior.
 * NOTE still requires passing in the table name. Future work may have this
 * be more of a "DAO generator" to simplify downstream usage.
 */
public class PropertiesDaoImpl implements PropertiesDAO
{

    private static final String UNABLE_TO_ACQUIRE_JDBI_HANDLE_FROM_TRANSACTION_OBJECT = "Unable to acquire JDBI Handle From transaction object.";
    private static final String THE_ID_COLUMN_MUST_BE_VALID = "The id column must be valid.";
    private static final String A_VALID_DATABASE_IDENTIFIER_IS_REQUIRED = "A valid Database Identifier is required.";
    private static final String A_VALID_PROPERTIES_OBJECT_MUST_BE_PASSED_INTO_THIS_FUNCTION = "A valid properties object must be passed into this function.";
    private static final String A_VALID_TABLE_NAME_IS_REQUIRED = "A valid table name is required.";

    @Override
    public void writeProperties(DataTransaction tx, String tableName, String idColumn, DatabaseKey parentKey,
            Properties props) throws OpenDcsDataException
    {
        Objects.requireNonNull(props, A_VALID_PROPERTIES_OBJECT_MUST_BE_PASSED_INTO_THIS_FUNCTION);
        Objects.requireNonNull(tableName, A_VALID_TABLE_NAME_IS_REQUIRED);
        Objects.requireNonNull(parentKey, A_VALID_DATABASE_IDENTIFIER_IS_REQUIRED);
        Objects.requireNonNull(idColumn, THE_ID_COLUMN_MUST_BE_VALID);
        final String columnNames = " ("+idColumn +",prop_name,prop_value) ";
        final String q = "insert into " + tableName + columnNames+ " values(:id,:prop,:value)";
        
        
        Handle handle = tx.connection(Handle.class)
                          .orElseThrow(() -> new OpenDcsDataException(UNABLE_TO_ACQUIRE_JDBI_HANDLE_FROM_TRANSACTION_OBJECT));

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
            DatabaseKey parentKey, int key2, Properties props) throws OpenDcsDataException
    {
        throw new UnsupportedOperationException("Unimplemented method 'writeProperties' with sorting key");
    }

    @Override
    public Properties readProperties(DataTransaction tx, String tableName, String idColumn, DatabaseKey parentKey)
            throws OpenDcsDataException
    {
        Objects.requireNonNull(tableName, A_VALID_TABLE_NAME_IS_REQUIRED);
        Objects.requireNonNull(parentKey, A_VALID_DATABASE_IDENTIFIER_IS_REQUIRED);
        Objects.requireNonNull(idColumn, THE_ID_COLUMN_MUST_BE_VALID);
        String q = "select prop_name, prop_value from " + tableName + " where " + idColumn + " = :id";
        final Properties props = new Properties();

        Handle handle = tx.connection(Handle.class)
                          .orElseThrow(() -> new OpenDcsDataException(UNABLE_TO_ACQUIRE_JDBI_HANDLE_FROM_TRANSACTION_OBJECT));

        try (Query jdbiQuery = handle.createQuery(q).bind("id", parentKey))
        {
            jdbiQuery.map( (rs, ctx) -> 
                  {
                    String name = rs.getString("prop_name");
                    String value = rs.getString("prop_value");
                    if (value == null)
                    {
                        value = "";
                    }
                    return Pair.of(name, value);
                  })
                  .list().forEach(pair -> props.setProperty(pair.first, pair.second));
                    

            return props;
        }
        catch (JdbiException ex)
        {
            throw new OpenDcsDataException("Unable to read properties", ex);
        }
    }

    @Override
    public Properties readProperties(DataTransaction tx, String tableName, String idColumn, String id2Column,
            DatabaseKey parentKey, int key2) throws OpenDcsDataException
    {
        throw new UnsupportedOperationException("Unimplemented method 'readProperties' with sorting key");
    }

    @Override
    public void deleteProperties(DataTransaction tx, String tableName, String idColumn, DatabaseKey parentKey)
            throws OpenDcsDataException
    {
        Objects.requireNonNull(tableName, A_VALID_TABLE_NAME_IS_REQUIRED);
        Objects.requireNonNull(parentKey, A_VALID_DATABASE_IDENTIFIER_IS_REQUIRED);
        Objects.requireNonNull(idColumn, THE_ID_COLUMN_MUST_BE_VALID);
        final String q = "delete from " + tableName + " where " + idColumn + " = :id";
        
        
        Handle handle = tx.connection(Handle.class)
                          .orElseThrow(() -> new OpenDcsDataException(UNABLE_TO_ACQUIRE_JDBI_HANDLE_FROM_TRANSACTION_OBJECT));

        try (Update update = handle.createUpdate(q))
        {
            update.bind("id", parentKey).execute();
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
        throw new UnsupportedOperationException("Unimplemented method 'deleteProperties' with sorting key");
    }
}
