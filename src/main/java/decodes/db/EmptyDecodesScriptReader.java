package decodes.db;

import java.io.IOException;

public class EmptyDecodesScriptReader implements DecodesScriptReader
{

    @Override
    public FormatStatement nextStatement(DecodesScript script) throws IOException
    {
        return null;
    }
    
}
