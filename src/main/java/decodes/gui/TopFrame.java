/*
*  $Id$
*/
package decodes.gui;

import java.awt.Point;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;

import decodes.dbeditor.DbEditorFrame;
import decodes.platwiz.PlatformWizard;
import decodes.sql.SqlDatabaseIO;
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
	private JPanel appContentPane = null;
	private Container jframeContentPane = null;

	private JPanel statusPanel = null;
	JLabel dbNameLabel = null, dbStatusLabel = null;
	public static String profileName = null;

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
		
		appContentPane = new JPanel(new BorderLayout());

		jframeContentPane = super.getContentPane();
		jframeContentPane.setLayout(new BorderLayout());
		jframeContentPane.add(appContentPane, BorderLayout.CENTER);
		
		statusPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 1));
		statusPanel.setBorder(new BevelBorder(BevelBorder.LOWERED));
		jframeContentPane.add(statusPanel, BorderLayout.SOUTH);
//		statusPanel.setPreferredSize(new Dimension(this.getWidth(), 16));
//		statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));
		
		statusPanel.add(dbNameLabel = new JLabel());
		statusPanel.add(dbStatusLabel = new JLabel());
		setStatusBar();
    }
    
	/**
	 * Return the pane for applications to use.
	 */
	@Override
	public Container getContentPane()
	{
		return appContentPane;
	}

	public void setStatusBar()
	{
		final String dbName = 
			(profileName != null ? ("(Profile " + profileName + ") ") : "")
			+ DecodesSettings.instance().editDatabaseType + " "
			+ DecodesSettings.instance().editDatabaseLocation;
		
		String s = "(Not initialized)";
		if (DecodesSettings.instance().editDatabaseTypeCode == DecodesSettings.DB_XML)
			s = "";
		else
		{
			decodes.db.Database db = decodes.db.Database.getDb();
			if (db != null)
			{
				decodes.db.DatabaseIO dbio = db.getDbIo();
				if (dbio == null)
					s = "(Not Open)";
				if (dbio instanceof SqlDatabaseIO)
					s = ((SqlDatabaseIO)dbio).getConnection() != null ? "(Connected)" : "(Not Connected)";
			}
		}

		final String statusText = s;
		SwingUtilities.invokeLater(
			new Runnable()
			{
				@Override
				public void run()
				{
					dbNameLabel.setText(dbName);
					dbStatusLabel.setText(statusText);
				}
			});
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
		dlg.validate();
		dlg.setLocationRelativeTo(this);
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
		String name = (TopFrame.profileName == null ? "" : (TopFrame.profileName+"-"))
			+ frameTitle + ".loc";
		changeTrackFile = new File(tmpDir, name);
		
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
			int offsetX=0, offsetY=0;
		
			if (TopFrame.profileName != null)
			{
				offsetX = 15;
				offsetY = 15;
			}
			locSizeProps.setProperty("x", ""+(curLoc.x+offsetX));
			locSizeProps.setProperty("y", ""+(curLoc.y+offsetY));
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

	@Override
	public void setTitle(String title)
	{
		String profileName = DecodesSettings.instance().getProfileName();
		if (profileName != null)
			title = title + " (" + profileName + ")";
		super.setTitle(title);
	}
	
	/**
	 * Used by Launcher to determine if it's ok to close an app's frame.
	 * Each app should overload, check for open elements that have changes that need
	 * to be saved, and if any, bring this frame to the fore and issue a warning.
	 * Default implementation here always returns true. 
	 * @return false if there are unsaved edits, true if closing does no harm.
	 */
	public boolean canClose()
	{
		return true;
	}
	
}
