package decodes.cwms.rating;

/**
* Panels that use the TsListControlsPanel of buttons must provide a controller
* that implements this interface.
*/
public interface RatingController
{
	/**
	 * Called when user presses the 'refresh' Button.
	 */
	public void refreshPressed();

	/**
	 * Called when user presses the 'Export XML' Button.
	 */
	public void exportXmlPressed();

	/**
	 * Called when user presses the 'Delete' Button.
	 */
	public void deletePressed();

	/**
	 * Called when user presses Import XML Button.
	 */
	public void importXmlPressed();
	
	/**
	 * Called when user presses Search USGS Button.
	 */
	public void searchUsgsPressed();
}
