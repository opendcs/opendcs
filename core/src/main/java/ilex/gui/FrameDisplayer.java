/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:09  cvs
*  Added legacy code to repository
*
*  Revision 1.4  2004/08/30 14:50:19  mjmaloney
*  Javadocs
*
*  Revision 1.3  2000/03/25 22:03:25  mike
*  dev
*
*  Revision 1.2  2000/03/23 21:07:53  mike
*  Created
*
*/
package ilex.gui;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.WindowConstants;


/**
Utility class to display a frame.
DEPRECATED!
*/
public class FrameDisplayer extends WindowAdapter 
{
	/**
	* Utility class to display a frame -- DEPCRECATED.
	* @param f
	* @param title
	* @param x
	* @param y
	* @param w
	* @param h
	*/
	public static void launch( final JFrame f, String title, final int x, final int y, final int w, int h ) 
	{
		f.setTitle(title);
		if (w > 0 && h > 0)
			f.setBounds(x,y,w,h);
		f.setVisible(true);

		f.setDefaultCloseOperation(
							WindowConstants.DISPOSE_ON_CLOSE);

		f.addWindowListener(
			new WindowAdapter() 
			{
				public void windowClosed(WindowEvent e) 
				{
					System.exit(0);
				}
			});
	}
}
