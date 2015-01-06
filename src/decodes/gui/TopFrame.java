/*
*  $Id$
*/
package decodes.gui;

import java.awt.Point;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import decodes.dbeditor.DbEditorFrame;
import decodes.platwiz.PlatformWizard;
import decodes.util.DecodesSettings;
import decodes.util.ResourceFactory;
import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.AsciiUtil;

/**
* Common base class for top-level frames in DECODES GUI applications.
* Provides singleton-like access for various resources.
*/
public class TopFrame extends JFrame
{
	private static TopFrame _instance;
	
	protected boolean exitOnClose = false;
	private File changeTrackFile = null;
	private boolean trackingChanges = false;
	private Properties locSizeProps = null;

	/** 
	  Constructor sets the singleton instance on first call. 
	*/
    public TopFrame()
	{
		if (_instance == null)
			_instance = this;
		ImageIcon tkIcon = new ImageIcon(
			ResourceFactory.instance().getIconPath());
		setIconImage(tkIcon.getImage());
    }

	/** 
	  The constructor MUST be called prior to calling this method. Since
	  each GUI will have a different concrete subclass of TopFrame, we
	  can't construct the singleton here.
	  @return the singleton instance. 
	*/
	public static TopFrame instance()
	{
		return _instance;
	}

	/**
	 * Returns true if there is an instance. This can be used to detect whether
	 * the program running is a GUI or not.
	 * @return true if there is an instance.
	 */
	public static boolean isGUI()
	{
		return _instance != null;
	}

	/** 
	  Starts a modal error dialog with the passed message. 
	  @param msg the error message
	*/
	public void showError(String msg)
	{
		Logger.instance().log(Logger.E_FAILURE, msg);
		JOptionPane.showMessageDialog(this,
			AsciiUtil.wrapString(msg, 60), "Error!", JOptionPane.ERROR_MESSAGE);
	}
	
	public int showConfirm(String title, String msg, int option)
	{
		return JOptionPane.showConfirmDialog(this, AsciiUtil.wrapString(msg, 60),
			title, option);
	}

	/**
	  Launches the passed modal dialog at a reasonable position on the screen.
	  @param dlg the modal dialog
	*/
	public void launchDialog(JDialog dlg)
	{
		Point loc = getLocation();
		Dimension frmSize = getSize();
		Dimension dlgSize = dlg.getPreferredSize();
		int x = (frmSize.width - dlgSize.width) / 2 + loc.x;
		int y = (frmSize.height - dlgSize.height) / 2 + loc.y;
	
		dlg.setLocation(x, y);
		dlg.setVisible(true);
	}

	/**
	 * Sets this frame's position to center it in the screen.
	 */
	public void centerOnScreen()
	{
		//Center the window
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension frameSize = getSize();
		if (frameSize.height > screenSize.height)
			frameSize.height = screenSize.height;
		if (frameSize.width > screenSize.width)
			frameSize.width = screenSize.width;
		setLocation((screenSize.width - frameSize.width) / 2,
			(screenSize.height - frameSize.height) / 2);
	}
	
	public static TopFrame getDbEditFrame()
	{
		TopFrame jf = DbEditorFrame.instance();
		if (jf == null)
		{
			PlatformWizard pw = PlatformWizard.instance();
			if (pw != null && pw.getPlatwizFrame() != null)
				jf = pw.getPlatwizFrame();
			else
			jf = TopFrame.instance();
		}
		return jf;
	}

	/** @return the tabbed pane for TS Groups */
	public JTabbedPane getTsGroupsListTabbedPane() { return null; }
	
	/** @return the panel for TS Groups */
	public JPanel getTsGroupsListPanel() { return null; }
	
	/** @return the data type standard */
//	public static String getDataTypeStandard() { return null; }
	
	public void setExitOnClose(boolean tf)
	{
		exitOnClose = tf;
	}

	public boolean getExitOnClose()
	{
		return exitOnClose;
	}

	public void trackChanges(String frameTitle)
	{
		// If already tracking changes, do nothing.
		if (trackingChanges)
			return;
		// Option in OpenDCS 6.1 to NOT remember screen positions & sizes.
		if (!DecodesSettings.instance().rememberScreenPosition)
			return;
		trackingChanges = true;
		File tmpDir = new File(EnvExpander.expand("$DCSTOOL_USERDIR/tmp"));
		if (!tmpDir.isDirectory())
			if (!tmpDir.mkdirs())
			{
				Logger.instance().warning(
					"Cannot track GUI size & location changes because '"
					+ tmpDir.getPath() + "' does not exist and cannot be created.");
			}
		changeTrackFile = new File(
			EnvExpander.expand("$DCSTOOL_USERDIR/" + frameTitle));
		changeTrackFile = new File(tmpDir, frameTitle + ".loc");
		
		locSizeProps = new Properties();
		Point curLoc = getLocation();
		Dimension curSize = getSize();
		if (changeTrackFile.canRead())
		{
			FileInputStream fis;
			try
			{
				fis = new FileInputStream(changeTrackFile);
				locSizeProps.load(fis);
				fis.close();
				String s = locSizeProps.getProperty("x");
				int x = s != null ? Integer.parseInt(s) : curLoc.x;
				s = locSizeProps.getProperty("y");
				int y = s != null ? Integer.parseInt(s) : curLoc.y;
				s = locSizeProps.getProperty("h");
				int h = s != null ? Integer.parseInt(s) : curSize.height;
				s = locSizeProps.getProperty("w");
				int w = s != null ? Integer.parseInt(s) : curSize.width;
				setBounds(x,y,w,h);
			}
			catch (Exception e1)
			{
				Logger.instance().warning("Cannot read size & loc file '"
					+ changeTrackFile.getPath() + "': " + e1);
			}
		}
		else
		{
			locSizeProps.setProperty("x", ""+curLoc.x);
			locSizeProps.setProperty("y", ""+curLoc.y);
			locSizeProps.setProperty("w", ""+curSize.width);
			locSizeProps.setProperty("h", ""+curSize.height);
		}
		addComponentListener(
			new ComponentAdapter()
			{
				public void componentResized(ComponentEvent e)
				{
					sizeChanged(e.getComponent().getSize());
				}
				public void componentMoved(ComponentEvent e)
				{
					positionChanged(e.getComponent().getLocation());
				}
			});
	}
	
	private synchronized void sizeChanged(Dimension d)
	{
		if (changeTrackFile == null)
			return;
		locSizeProps.setProperty("w", ""+d.width);
		locSizeProps.setProperty("h", ""+d.height);
		saveLocSize();
	}
	private synchronized void positionChanged(Point p)
	{
		if (changeTrackFile == null)
			return;
		locSizeProps.setProperty("x", ""+p.x);
		locSizeProps.setProperty("y", ""+p.y);
		saveLocSize();
	}
	private void saveLocSize()
	{
		try
		{
			FileOutputStream fos = new FileOutputStream(changeTrackFile);
			locSizeProps.store(fos, null);
			fos.close();
		}
		catch(Exception ex)
		{
			Logger.instance().warning("Cannot write to '" + changeTrackFile.getPath()
				+ "': " + ex);
			changeTrackFile = null;
		}
	}

	
}
