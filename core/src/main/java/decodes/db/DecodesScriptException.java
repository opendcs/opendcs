package decodes.db;

/**
 * Any errors processing DecodesScript information getting it ready for use.
 * @since 2022-11-05
 */
public class DecodesScriptException extends Exception
{
    /**
     * Message only constructor
     * @param msg description of the error
     */
    public DecodesScriptException(String msg)
    {
        super(msg);
    }

    /**
     * Message with cause
     * @param msg description of why this cause happened
     * @param cause additional information about the error
     */
    public DecodesScriptException(String msg, Throwable cause)
    {
        super(msg,cause);
    }
}
