/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:14  cvs
*  Added legacy code to repository
*
*  Revision 1.1  2000/05/15 21:26:21  mike
*  Modified LrgsControl so that all CORBA operations are done in the CORBA
*  thread. Error popups which may result are displayed back in the UI thread.
*  PopupDisplayer is a new helper class that facilitates this.
*
*/
package lrgs.gui;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * This class provides a public static method that can be called
 * from any thread to display a pop-up dialod with an error message
 * in it. The display will be invoked in the user-interface event-
 * handling thread.
 */
public class PopupDisplayer
{
	/**
	 * Displays an error message in a modal popup in the
	 * center of the screen. 
	 * The popup is displayed during the user-interface event-handling
	 * thread at some point in the near future. Hence this method returns
	 * immediately. Do not depend on this method blocking.
	 */
	public static void displayError(final String errmsg)
	{
		SwingUtilities.invokeLater(
			new Runnable()
			{
				public void run()
				{
					System.err.println(errmsg);
					JOptionPane.showMessageDialog(null, 
						errmsg, "Error!", JOptionPane.ERROR_MESSAGE);
				}
			});
	}
}