package decodes.decoder;

import ilex.var.TimedVariable;

/** holds a TimedVariable and its formatted string value. */
public class TSSample
{
    TimedVariable tv;  // Contains time and numeric value
    String fv;        // Value formatted by presentation group (may be null)

    TSSample(TimedVariable tv)
    {
        this.tv = tv;
        fv = null;
    }
}
