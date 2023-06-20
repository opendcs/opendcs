package decodes.dbeditor;

/**
* Panels that use the ListOpsPanel of buttons must provide a controller
* that implements this interface.
*/
public interface ListOpsController
{
	/** @return the type of entity being listed. */
	public String getEntityType();

	/**
	  Called when user presses the 'Open' Button.
	*/
	public void openPressed();

	/**
	  Called when user presses the 'New' Button.
	*/
	public void newPressed();

	/**
	  Called when user presses the 'Copy' Button.
	*/
	public void copyPressed();

	/**
	  Called when user presses the 'Delete' Button.
	*/
	public void deletePressed();

	/**
	  Called when user presses the 'Help' Button.
	*/
	public void refreshPressed();
}
