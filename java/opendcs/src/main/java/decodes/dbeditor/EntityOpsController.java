package decodes.dbeditor;

/**
Panels that use the EntityOpsPanel of buttons must provide a controller
that implements this interface.
*/
public interface EntityOpsController
{
	/** @return a string representing the type of entity for use in messages. */
	public String getEntityName();

	/** Called when user presses the 'Commit' button. */
	public void commitEntity();

	/** Called when user presses the 'Close' button. */
	public void closeEntity();

	/** Called when user presses the 'Help' button. */
	public void help();
}
