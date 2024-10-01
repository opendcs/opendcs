package decodes.db;

import java.io.IOException;
import java.util.Optional;

/**
 * A reader that provides no actual data.
 * Primarily for use with editors for starting new scripts
 * 
 * NOTE: At least until the editors can be moved to using their own "reader" implementation
 * @since 2022-11-05
 */
public final class EmptyDecodesScriptReader implements DecodesScriptReader
{

    @Override
    public Optional<FormatStatement> nextStatement(DecodesScript script) throws IOException
    {
        return Optional.empty();
    }
    
}
