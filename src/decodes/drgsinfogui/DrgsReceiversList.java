package decodes.drgsinfogui;

import java.awt.Dimension;
import java.awt.Toolkit;

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
		// Center the window
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension frameSize = drgsFrame.getSize();
		if (frameSize.height > screenSize.height)
		{
			frameSize.height = screenSize.height;
		}
		if (frameSize.width > screenSize.width)
		{
			frameSize.width = screenSize.width;
		}
		drgsFrame.setLocation((screenSize.width - frameSize.width) / 2,
				(screenSize.height - frameSize.height) / 2);
		drgsFrame.setVisible(true);
	}

	/** Main method */
	public static void main(String[] args)
	{
		// Create GUI Application
		DrgsReceiversList drgsR = new DrgsReceiversList();
		drgsR.init();
	}
}
