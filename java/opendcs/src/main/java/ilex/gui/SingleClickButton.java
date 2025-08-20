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
*  Revision 1.4  2004/08/30 14:50:20  mjmaloney
*  Javadocs
*
*  Revision 1.3  2000/05/23 11:23:26  mike
*  SingleClickButton modification.
*
*  Revision 1.2  2000/05/19 21:13:29  mike
*  First working implementation. Processes both keyboard and mouse events.
*  Filters mouse for double click. Either space or ENTER will push the button.
*
*  Revision 1.1  2000/05/19 18:17:33  mike
*  This class processes keyboard and mouse events and also filters
*  out double clicks.
*
*/
package ilex.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
* This button prevents double-clicks.
*/
public abstract class SingleClickButton extends JButton
{
	long lastClickTime;
	
	/**
	* Constructor.
	* @param text the button text
	*/
	public SingleClickButton( String text )
	{
		super(text);
		addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent av)
				{
					buttonPressed(av);
				}
			});

/* The following stuff doesn't work very well. 
One problem is on Motif - unless there's at least one action
listener, the button never uses the 'pressed' icon. The main
problem seems to be missed clicks. Don't know why this is happening.

		addMouseListener(
			new MouseAdapter()
			{
				public void mouseClicked(MouseEvent event)
				{
					int count = event.getClickCount();
System.out.println("click, count="+count);
					if (count == 1)
						buttonPressed(event);
				}
			});
			
		addKeyListener(
			new KeyAdapter()
			{
				public void keyPressed(KeyEvent event)
				{
					if (event.getKeyChar() == KeyEvent.VK_ENTER
					 || event.getKeyChar() == KeyEvent.VK_SPACE)
					{
						buttonPressed(event);
					}
				}
			});
*/
			
	}

	/**
	* Called when button is pressed.
	* @param event passed to implementation.
	*/
	public abstract void buttonPressed( AWTEvent event );
}
	
