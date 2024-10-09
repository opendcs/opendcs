package decodes.db;

import java.io.IOException;
import java.util.Optional;

/**
 * Provide format statements to a DecodesScriptBuilder
 * during the final creation of the DecodesScriptObject
 * @since 2022-11-05
 */
public interface DecodesScriptReader
{
    /**
     * Create a new FormatStatement for a given script.
     * 
     * @param script decodes script that the BuilderCreates, traditionally 
     *               passed into the FormatStatement constructor
     * @return a new FormatStatement or null when all statements have been read in
     * @throws IOException any error retrieving a statement from a source
     */
    public Optional<FormatStatement> nextStatement(DecodesScript script) throws IOException;
}
