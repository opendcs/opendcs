package org.opendcs.database.impl.opendcs.jdbi.decodesscript;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

import decodes.db.DecodesScript;
import decodes.db.DecodesScriptReader;
import decodes.db.FormatStatement;

public class InMemoryDecodesScriptReader implements DecodesScriptReader
{
    private int current = 0;

    private ArrayList<FormatStatement> statements = new ArrayList<>();


    public void addStatement(FormatStatement statement)
    {

    }

    @Override
    public Optional<FormatStatement> nextStatement(DecodesScript script) throws IOException
    {
        if (current >= statements.size())
        {
            return Optional.empty();
        }
        
        
        var ret = new FormatStatement(script, current);
        var fs = statements.get(current);
        ret.format = fs.format;
        ret.label = fs.label;

        current++;
        return Optional.of(ret);
    }
    
}
