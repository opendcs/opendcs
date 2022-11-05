package decodes.db;

import java.io.IOException;

/**
 * A reader that provides no actual data.
 * Primarily for use with editors for starting new scripts
 * 
 * NOTE: At least until the editors can be moved to using their own "reader" implementation
 * @since 2022-11-05
 */
public class EmptyDecodesScriptReader implements DecodesScriptReader
{

    @Override
    public FormatStatement nextStatement(DecodesScript script) throws IOException
    {
        return null;
    }
    
}
