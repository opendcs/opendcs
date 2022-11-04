package decodes.db;

import java.io.IOException;

public interface DecodesScriptReader
{
    public FormatStatement nextStatement(DecodesScript script) throws IOException;
}
