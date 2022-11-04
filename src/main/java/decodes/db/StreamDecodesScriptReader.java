package decodes.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Read a decodes script from a text file.
 * Primarily used for testing decodes scripts.
 */
public class StreamDecodesScriptReader implements DecodesScriptReader
{
    private int lineNumber = 1;
    private BufferedReader reader = null;

    public StreamDecodesScriptReader(InputStream stream)
    {
        reader = new BufferedReader(new InputStreamReader(stream));
    }

    @Override
    public FormatStatement nextStatement(DecodesScript script) throws IOException
    {
        FormatStatement fs = null;
        String line = reader.readLine();
        if ( line != null)
        {
            int firstColon = line.indexOf(":");
            if (firstColon < 0 )
            {
                throw new IOException("Statement on line " + lineNumber + " is not a label separated by a colon.");
            }
            String label = line.substring(0, firstColon);
            String statement = line.substring(firstColon+1);
            fs = new FormatStatement(script, lineNumber);
            lineNumber++;
            fs.label = label;
            fs.format = statement.trim();
        }
        return fs;
    }
}
