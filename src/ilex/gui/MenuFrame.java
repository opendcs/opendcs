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
*  Revision 1.18  2008/01/18 21:22:16  mmaloney
*  modified files
*
*  Revision 1.17  2004/08/31 16:35:47  mjmaloney
*  javadoc
*
*  Revision 1.16  2004/08/30 14:50:20  mjmaloney
*  Javadocs
*
*  Revision 1.15  2003/05/01 23:10:12  mjmaloney
*  dev
*
*  Revision 1.14  2003/04/30 00:48:16  mjmaloney
*  Make help URL dynamic
*
*  Revision 1.13  2001/03/05 20:00:44  mike
*  Added event handler for moving a frame. LRGS gui uses this to remember
*  new position in properties.
*
*  Revision 1.12  2001/03/03 03:15:29  mike
*  GUI mods for 3.2
*
*  Revision 1.11  2000/10/23 13:17:02  mike
*  New scheme for help urls.
*
*  Revision 1.10  2000/09/08 19:06:05  mike
*  Call AsciiUtil.wrapString to wrap long error messages in showError dialogs.
*
*  Revision 1.9  2000/06/07 13:49:09  mike
*  Removed diagnostics
*
*  Revision 1.8  2000/05/19 16:40:42  mike
*  Button behavior updates (not finished)
*
*  Revision 1.7  2000/05/17 17:56:37  mike
*  Added feature so that ENTER key would cause button presses.
*
*  Revision 1.6  2000/04/05 20:35:05  mike
*  dev
*
*  Revision 1.5  2000/04/04 19:07:25  mike
*  dev
*
*  Revision 1.4  2000/04/01 02:16:28  mike
*  dev
*
*  Revision 1.3  2000/03/28 00:43:18  mike
*  dev
*
*  Revision 1.2  2000/03/25 22:03:25  mike
*  dev
*
*  Revision 1.1  2000/03/23 21:07:53  mike
*  Created
*
*/
package ilex.gui;

import java.net.URL;
import java.net.MalformedURLException;
import java.util.ResourceBundle;
import java.awt.*;
import ilex.util.AsciiUtil;
import ilex.util.LoadResourceBundle;

import java.awt.event.*;
import javax.swing.border.BevelBorder;
import javax.swing.*;

import decodes.util.ResourceFactory;

/**
* MenuFrame provides a menu bar and other special features that
* are appropriate in the top-level frame of an applications.
* <p>
* Create a sub-class of MenuFrame for your top-level frame and
* supply implementations for the stub methods.
*/
public abstract class MenuFrame extends JFrame
{
	private static ResourceBundle labels = null;
	JMenuBar menuBar;
	ExitAction exitAction;
	protected int x, y;

	/**
	* Constructor.
	* @param title the frame title
	*/
	public MenuFrame( String title )
	{
		super(title);
		//Load the labels properties file
		getLabelDescriptions();
		menuBar = null;
		exitAction = new ExitAction(this);
		x = 0;
		y = 0;
		ImageIcon tkIcon = new ImageIcon(
			ResourceFactory.instance().getIconPath());
		setIconImage(tkIcon.getImage());
	}

	public static void getLabelDescriptions()
	{
		//Load the labels properties file
		labels = LoadResourceBundle.getLabelDescriptions(
				"ilex/resources/gui", null);
	}
	
	public static ResourceBundle getLabels() 
	{
		if (labels == null)
			getLabelDescriptions();
		return labels;
	}
	
	/**
	* Builds the menu bar and adds the menu items specified
	* by the sub-class.
	* Your sub-class should call this method if it wants a menu
	* bar to be associated with this frame. Do not call this method
	* if you don't want a menu.
	*/
	public final void buildMenuBar( )
	{
		menuBar = new JMenuBar();
		menuBar.setBorder(new BevelBorder(BevelBorder.RAISED));

		AbstractAction[] actions = getFileMenuActions();
		if (actions != null)
		{
			JMenu filemenu = new JMenu(labels.getString("MenuFrame.file"));
			buildMenu(filemenu, actions);
			filemenu.add(exitAction);
		}
		actions = getEditMenuActions();
		if (actions != null)
			buildMenu(new JMenu(
					labels.getString("MenuFrame.edit")), actions);
		if (isHelpMenuEnabled())
		{
			JMenu menu = new JMenu(
					labels.getString("MenuFrame.help"));
			int n=0;
			menu.add(new ViewHelpAction("contents.html", 
					labels.getString("MenuFrame.contents")));
			n++;
			menu.add(new ViewHelpAction(getHelpFileName(), 
				labels.getString("MenuFrame.thisScreen")));
			n++;
			menu.insertSeparator(n);
			menu.add(new ViewHelpAction("about.html", 
				labels.getString("MenuFrame.about") + GuiApp.getAppName()));
			menuBar.add(menu);
		}
		setJMenuBar(menuBar);
	}

	/**
	* Displays this frame.
	* @param x upper left X
	* @param y upper left Y
	* @param w width
	* @param h height
	*/
	public void launch( int x, int y, int w, int h )
	{
		this.x = x;
		this.y = y;
		setBounds(x,y,w,h);
		setVisible(true);

		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		final MenuFrame myframe = this;
		addWindowListener(
			new WindowAdapter()
			{
				public void windowClosed(WindowEvent e)
				{
					myframe.cleanupBeforeExit();
					if (GuiApp.getTopFrame() == myframe)
						System.exit(0);
//					exitAction.actionPerformed(null); Can't do this - causes recursion!
				}
			});

		addComponentListener(
			new ComponentAdapter()
			{
				public void componentResized(ComponentEvent e)
				{
					Dimension d = e.getComponent().getSize();
					myframe.setSize(d);
					//myframe.resize(d);
				}
				public void componentMoved(ComponentEvent e)
				{
					Point p = e.getComponent().getLocation();
					myframe.movedTo(p);
				}
			});

		// This key listener checks for ENTER key presses. If one occurs over
		// a JButton, call its doClick() method.
		addKeyListener(
			new KeyAdapter()
			{
				public void keyPressed(KeyEvent event)
				{
					if (event.getKeyChar() == KeyEvent.VK_ENTER)
					{
						Component c = getFocusOwner();
						if (c != null && c instanceof JButton)
						{
							JButton jb = (JButton)c;
							jb.doClick();
						}
					}
				}
			});
//		addMouseListener(
//			new MouseAdapter()
//			{
//				public void mouseClicked(MouseEvent event)
//				{
//					System.out.println("Mouse click, count="+event.getClickCount());
//				}
//			});
	}


	/**
	* Builds & adds a menu.
	* @param menu the menu
	* @param actions the actions
	*/
	private void buildMenu( JMenu menu, AbstractAction[] actions )
	{
		menuBar.add(menu);
		int n = 0;
		for(int i = 0; i < actions.length; i++)
		{
			if (actions[i] != null)
			{
				menu.add(actions[i]);
				n++;
			}
//			else
//				menu.insertSeparator(n);
		}
	}

	/**
	* Shows an error message in a modal dialog and prints it to stderr.
	* This is a convenience method.
	* @param msg the message
	*/
	public void showError( String msg )
	{
		System.err.println(msg);
		JOptionPane.showMessageDialog(this,
			AsciiUtil.wrapString(msg, 60), "Error!", JOptionPane.ERROR_MESSAGE);
	}

	//============== Stub Methods - Override in Sub-Class ===========

	/**
	* @return array of action objects to be placed in file menu.
	* Override this method in your sub-class. The default implementation
	* here returns null, meaning that no File menu is to be displayed.
	* <p>
	* You do not need to include the 'Exit' action. This will be included
	* automatically as the last item on the Exit menu. To display a File
	* menu with just the Exit action, return an array of 1 AbstractAction,
	* set to null.
	* <p>
	* In the array, a null action indicates a separator in the menu.
	*/
	protected AbstractAction[] getFileMenuActions( )
	{
		return null;
	}

	/**
	* @return array of action objects to be placed in Edit menu.
	* Override this method in your sub-class. The default implementation
	* here returns null, meaning that no Edit menu is to be displayed.
	* In the array, a null action indicates a separator in the menu.
	*/
	protected AbstractAction[] getEditMenuActions( )
	{
		return null;
	}

	/**
	* @return true if a 'Help' menu is to be included.
	* The default implementation here returns false.
	*/
	protected boolean isHelpMenuEnabled( )
	{
		return false;
	}

//	/**
//	 * Returns an URL to be displayed when the user selects the Help-About
//	 * menu item. The default implementation here looks for a property labeled
//	 * General.HelpAbout containing the URL. This is appropriate for most screens.
//	 */
/*
*	protected URL getHelpAbout()
*	{
*		//String s = GuiApp.getProperty("General.HelpAbout");
*		String s = GuiApp.getProperty("General.HelpRoot") + "about.html";
*		if (s != null)
*			try { return new URL(s); }
*			catch(MalformedURLException e)
*			{
*				System.err.println(s);
*				return null;
*			}
*
*		return null;
*	}
*/

//	/**
//	 * Returns an URL to be displayed when the user selects the Help-Contents
//	 * menu item. The default implementation here looks for a property labeled
//	 * General.HelpContents containing the URL. This is appropriate for most screens.
//
//	 */
/*	protected URL getHelpContents()
*	{
*		//String s = GuiApp.getProperty("General.HelpContents");
*		String s = GuiApp.getProperty("General.HelpRoot") + "contents.html";
*		if (s != null)
*			try { return new URL(s); }
*			catch(MalformedURLException e)
*			{
*				System.err.println(s);
*				return null;
*			}
*
*		return null;
*	}
*/

	/**
	* @return an URL to be displayed when the user selects the Help-ThisScreen
	* menu item.
	*/
	protected String getHelpFileName( )
	{
		return null;
	}

	/**
	* Performs clean up before TopLevel exits.
	* Override this method in your subclass. It is called when the user selects
	* 'Exit' from the file menu. Do cleanup and resource-releasing.
	* The default implementation here does nothing.
	*/
	public void cleanupBeforeExit( )
	{
	}

	/** Programatically execute event as if user had closed the window. */
	public void generateCloseEvent( )
	{
		processEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
	}

	/**
	* Allows application to know when frame was moved.
	* @param p the new origin
	*/
	public void movedTo( Point p )
	{
	}
}


class ViewHelpAction extends AbstractAction
{
	String filename;
	String text;

	/**
	* @param filename
	* @param text
	*/
	ViewHelpAction( String filename, String text )
	{
		super(text);
		//this.url = url;
		this.filename = filename;
		this.text = text;
	}

	/**
	* @param e
	*/
	public void actionPerformed( ActionEvent e )
	{
		String helpUrl = GuiApp.getProperty("General.HelpRoot");
		if (!helpUrl.endsWith("/"))
			helpUrl = helpUrl + "/";
		helpUrl = helpUrl + filename;

		String cmd = GuiApp.getProperty("General.Browser", "mozilla")
			+ " " + helpUrl;
		try { Runtime.getRuntime().exec(cmd); }
		catch(Exception ex)
		{
			System.err.println("Could not execute command '" + cmd + "':" + ex);
		}
	}
}

class ExitAction extends AbstractAction
{
	MenuFrame menuframe;

	/**
	* @param menuframe
	*/
	ExitAction( MenuFrame menuframe )
	{
		super(MenuFrame.getLabels().getString("MenuFrame.exit"));
		this.menuframe = menuframe;
	}

	/**
	* @param e
	*/
	public void actionPerformed( ActionEvent e )
	{
//		if (menuframe != null)
//			menuframe.cleanupBeforeExit();
//		menuframe.generateCloseEvent();  This doesn't work either.

		menuframe.dispose();
//		if (GuiApp.getTopFrame() == menuframe)
//			System.exit(0);
	}
}
