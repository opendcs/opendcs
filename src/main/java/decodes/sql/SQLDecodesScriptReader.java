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

public class SQLDecodesScriptReader implements DecodesScriptReader, AutoCloseable
{
    ResultSet rs = null;
    PreparedStatement query = null;
    
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
