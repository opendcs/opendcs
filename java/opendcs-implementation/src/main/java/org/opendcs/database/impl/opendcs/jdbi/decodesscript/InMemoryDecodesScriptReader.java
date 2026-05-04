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
        if (!statements.stream().anyMatch(fs -> fs.sequenceNum == statement.sequenceNum))
        {
            statements.add(statement);
        }
    }

    @Override
    public Optional<FormatStatement> nextStatement(DecodesScript script) throws IOException
    {
        if (current >= statements.size())
        {
            return Optional.empty();
        }
        
        if (current == 0)
        {
            // make sure the statements are ordered by sequence number, just in case.
            statements.sort((a,b) -> Integer.compare(a.sequenceNum, b.sequenceNum));
        }
        
        var ret = new FormatStatement(script, current + 1); // statements are 1 based, not 0 based
        var fs = statements.get(current);
        ret.format = fs.format;
        ret.label = fs.label;

        current++;
        return Optional.of(ret);
    }
    
}
