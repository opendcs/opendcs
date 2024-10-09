package decodes.drgsinfogui;

import ilex.gui.WindowUtility;

/**
 * This is the main class for the DRGS Receiver GUI. This GUI
 * will generate an XML file with all the info entered by the user.
 * It also converts that XML to HTML to be used by the Dcp Monitor.
 *
 */
public class DrgsReceiversList
{
	private String module = "DrgsReceiversList";
	private DrgsReceiversListFrame drgsFrame;
	private boolean packFrame = false;
	
	/** Constructor */
	public DrgsReceiversList()
	{
		
	}
	
	/** Initialize the application. Creates the SWING GUI. */
	public void init()
	{
		drgsFrame = new DrgsReceiversListFrame();
		// Validate frames that have preset sizes
		//Pack frames that have useful preferred size info, 
		//e.g. from their layout
		if (packFrame)
		{
			drgsFrame.pack();
		} else
		{
			drgsFrame.validate();
		}
		WindowUtility.center(drgsFrame).setVisible(true);
	}

	/** Main method */
	public static void main(String[] args)
	{
		// Create GUI Application
		DrgsReceiversList drgsR = new DrgsReceiversList();
		drgsR.init();
	}
}
