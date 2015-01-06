/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*
*  $Log$
*  Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
*  OPENDCS 6.0 Initial Checkin
*
*  Revision 1.3  2013/01/30 20:40:54  mmaloney
*  added showConfirm method.
*
*  Revision 1.2  2008/06/26 15:01:20  cvs
*  Updates for HDB, and misc other improvements.
*
*  Revision 1.1  2008/04/04 18:21:03  cvs
*  Added legacy code to repository
*
*  Revision 1.2  2007/02/16 22:25:56  mmaloney
*  Additions to support LRGS Config Dialog
*
*  Revision 1.1  2006/02/17 19:49:27  mmaloney
*  Created.
*
*/
package decodes.gui;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.ParseException;
import java.util.Properties;

import javax.swing.*;

import ilex.util.AsciiUtil;
import ilex.util.EnvExpander;
import ilex.util.Logger;
import decodes.dbeditor.DbEditorFrame;
import decodes.platwiz.PlatformWizard;
import decodes.util.DecodesSettings;
import decodes.util.ResourceFactory;

@SuppressWarnings("serial")
public class GuiDialog extends JDialog
{
	private File changeTrackFile = null;
	private boolean trackingChanges = false;
	private Properties locSizeProps = null;

	public GuiDialog(Frame parent, String title, boolean isModal)
	{
		super(parent, title, isModal);
		ImageIcon tkIcon = new ImageIcon(
			ResourceFactory.instance().getIconPath());
		setIconImage(tkIcon.getImage());
	}

	public GuiDialog(JDialog parent, String title, boolean isModal)
	{
		super(parent, title, isModal);
		ImageIcon tkIcon = new ImageIcon(
			ResourceFactory.instance().getIconPath());
		setIconImage(tkIcon.getImage());
	}

	/**
	 * Shows an error in a modal dialog on top of this dialog.
	 * @param msg the error message.
	 */
	public void showError(String msg)
	{
		Logger.instance().log(Logger.E_FAILURE, msg);
		JOptionPane.showMessageDialog(this,
			AsciiUtil.wrapString(msg, 60), "Error!", JOptionPane.ERROR_MESSAGE);
	}
	
	/**
	 * Shows a confirm dialog and returns the result.
	 * @param msg
	 * @return
	 */
	public int showConfirm(String msg)
	{
		return JOptionPane.showConfirmDialog(this, AsciiUtil.wrapString(msg, 60));
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
	 * Gets and trims a String field value.
	 * @return the value in the field or the specified default if empty.
	 */
	public String getStringFieldValue(JTextField tf, String defaultValue)
	{
		String s = tf.getText().trim();
		return s.length() > 0 ? s : defaultValue;
	}

	/**
	 * Gets an integer field value.
	 * @return the value in the field or the specified default if empty.
	 */
	public int getIntFieldValue(JTextField tf, int defaultValue)
		throws ParseException
	{
		String s = tf.getText().trim();
		if (s.length() == 0) return defaultValue;
		return Integer.parseInt(s);
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
