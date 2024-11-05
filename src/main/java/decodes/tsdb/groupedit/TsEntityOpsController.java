package decodes.tsdb.groupedit;

/**
 Panels that use the TsEntityOpsPanel of buttons must provide a controller
 that implements this interface.
 */
public interface TsEntityOpsController
{
	/** @return a string representing the type of entity for use in messages. */
	public String getEntityName();

	/** Called when user presses the 'Save' button. */
	public void saveEntity();

	/** Called when user presses the 'Close' button. */
	public void closeEntity();

	/** Called when user presses the 'Corrections' button. */
	public void evaluateEntity();
}
