package decodes.util;

/**
 * Interface for notifying when the initial PDT load is complete.
 * Note that this is only called once after Pdt.instance().setPdtLoadListener()
 * is called. Subsequent loads (updates) will not call the listener.
 */
public interface PdtLoadListener
{
	public void pdtLoaded();
}
