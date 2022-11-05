package decodes.sql;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import decodes.db.DecodesScript;
import decodes.db.DecodesScriptReader;
import decodes.db.FormatStatement;

/**
 * Retrieves DecodesScript format statements from a database.
 * 
 * Instances of SQLDecodesScriptReader should be closed to avoid resource leaks.
 * 
 * @since 2022-11-05
 */
public class SQLDecodesScriptReader implements DecodesScriptReader, AutoCloseable
{
    ResultSet rs = null;
    PreparedStatement query = null;
    
    /**
     * Create a new SQLDecodesScriptReader. The constructor will prepare the sql statement
     * and call executeQuery.
     * @param conn An opened java.sql.Connection. This call will not close it.
     * @param id SQL Id of the script to retrieve
     * @throws SQLException errors either preparing the statement, setting the ID parameter, or executing the query.
     */
    public SQLDecodesScriptReader(Connection conn, DbKey id) throws SQLException
    {
        query = conn.prepareStatement("SELECT decodesScriptId, sequenceNum, " +
                                      "       label, format " +
                                      "       FROM FormatStatement " +
                                      "       WHERE DecodesScriptId = ?" +
                                      "       ORDER BY SequenceNum");
        query.setLong(1, id.getValue());
        rs = query.executeQuery();
    }

    /**
     * Returns the next statemetn from the query result.
     */
    @Override
    public FormatStatement nextStatement(DecodesScript script) throws IOException {        
        try
        {
            if(rs.next())
            {
                return fromRS(rs,script);
            }
            return null;
        }
        catch(SQLException ex)
        {
            throw new IOException("Unable to read row or element for DecodesScript",ex);
        }
    }
    
    /**
     * Turn the row into a FormatStatement
     * @param rs valid ResultSet
     * @param script DecodesScript this format will be associated with.
     * @return a valid format Statement
     * @throws SQLException any errors retrieving columns
     */
    private static FormatStatement fromRS(ResultSet rs, DecodesScript script) throws SQLException
    {
        int seqNum = rs.getInt("sequenceNum");
        String label = rs.getString("label");
        String format = rs.getString("format");
        if (format == null) format = "";

        FormatStatement fmt = new FormatStatement(script, seqNum);
        fmt.label = label;
        fmt.format = format;
        return fmt;
    }

    /**
     * Close the ResultSet and PreparedStatement.
     */
    @Override
    public void close() throws Exception {
        if(rs != null)
        {
            rs.close();
        }        
        if (query != null)
        {
            query.close();
        }
    }
}
