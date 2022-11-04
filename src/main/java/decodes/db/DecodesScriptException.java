package decodes.db;

public class DecodesScriptException extends Exception
{
    public DecodesScriptException(String msg)
    {
        super(msg);
    }

    public DecodesScriptException(String msg, Throwable cause)
    {
        super(msg,cause);
    }
}
