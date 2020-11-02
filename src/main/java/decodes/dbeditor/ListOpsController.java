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
	  @param e ignored.
	*/
	public void openPressed();

	/**
	  Called when user presses the 'New' Button.
	  @param e ignored.
	*/
	public void newPressed();

	/**
	  Called when user presses the 'Copy' Button.
	  @param e ignored.
	*/
	public void copyPressed();

	/**
	  Called when user presses the 'Delete' Button.
	  @param e ignored.
	*/
	public void deletePressed();

	/**
	  Called when user presses the 'Help' Button.
	  @param e ignored.
	*/
	public void refreshPressed();
}
