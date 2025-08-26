/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
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

import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import ilex.util.AsciiUtil;
import ilex.util.EnvExpander;
import decodes.dbeditor.DbEditorFrame;
import decodes.platwiz.PlatformWizard;
import decodes.util.DecodesSettings;
import decodes.util.ResourceFactory;

@SuppressWarnings("serial")
public class GuiDialog extends JDialog
{
	private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
	 * Does NOT log to the standard logger.
	 * @param msg the error message.
	 */
	public void showError(String msg)
	{
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
		dlg.validate();
		dlg.setLocationRelativeTo(dlg);
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
				log.warn("Cannot track GUI size & location changes because '{}' " +
						 "does not exist and cannot be created.", tmpDir.getPath());
			}
		changeTrackFile = new File(
			EnvExpander.expand("$DCSTOOL_USERDIR/" + frameTitle));
		changeTrackFile = new File(tmpDir, frameTitle + ".loc");

		locSizeProps = new Properties();
		Point curLoc = getLocation();
		Dimension curSize = getSize();
		if (changeTrackFile.canRead())
		{
			try (FileInputStream fis = new FileInputStream(changeTrackFile))
			{
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
			catch (Exception ex)
			{
				log.atWarn().setCause(ex).log("Cannot read size & loc file '{}'", changeTrackFile.getPath());
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
		try (FileOutputStream fos = new FileOutputStream(changeTrackFile);)
		{
			locSizeProps.store(fos, null);
		}
		catch(Exception ex)
		{
			log.atWarn().setCause(ex).log("Cannot write to '{}'", changeTrackFile.getPath());
			changeTrackFile = null;
		}
	}
}
