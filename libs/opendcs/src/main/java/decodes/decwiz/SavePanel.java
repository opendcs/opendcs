/*
* $Id$
*/
package decodes.decwiz;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Calendar;
import java.util.Properties;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.io.IOException;
import javax.swing.*;

import ilex.util.EnvExpander;
import decodes.gui.TopFrame;
import decodes.util.DecodesSettings;
import decodes.db.TransportMedium;


/**
This class is the 3rd panel in the decoding wizard.
*/
public class SavePanel 
	extends DecWizPanel
{
	private JLabel rawLabel = new JLabel();
	private JTextField rawField = new JTextField();
	private JButton browseRawButton = new JButton();
	private String append_overwrite[] = { "Append", "Overwrite" };
	private JComboBox rawCombo = new JComboBox(append_overwrite);
	private JLabel saveLabel = new JLabel();
	private JTextField saveDecodedField = new JTextField();
	private JButton browseDecodedButton = new JButton();
	private JComboBox decodedCombo = new JComboBox(append_overwrite);
	private JLabel saveSummaryLabel = new JLabel();
	private JTextField saveSummaryField = new JTextField();
	private JButton browseSummaryButton = new JButton();
	private JComboBox summaryCombo = new JComboBox(append_overwrite);
	private GridBagLayout gridBagLayout1 = new GridBagLayout();
	private JButton doSaveButton = new JButton("Save Files");


	private JFileChooser rawDataFileChooser = new JFileChooser(
		EnvExpander.expand(DecodesSettings.instance().decwizRawDataDir));
	private JFileChooser decodedDataFileChooser = new JFileChooser(
		EnvExpander.expand(DecodesSettings.instance().decwizDecodedDataDir));
	private JFileChooser summaryFileChooser = new JFileChooser(
		EnvExpander.expand(DecodesSettings.instance().decwizSummaryLog));

	/** Constructor. */
	public SavePanel()
	{
		super();

//	Setup default directory  for raw data file chooser 

		String path = EnvExpander.expand(
				DecodesSettings.instance().decwizRawDataDir);
		rawDataFileChooser = new JFileChooser(path);

//	Setup default directory for decoded data file chooser 

		path = EnvExpander.expand(
			DecodesSettings.instance().decwizDecodedDataDir);
		decodedDataFileChooser = new JFileChooser(path);

//	Setup default directory for summary data file chooser 
		path = EnvExpander.expand(DecodesSettings.instance().decwizSummaryLog);
		summaryFileChooser = new JFileChooser(path);

		try
		{
			jbInit();
		}
		catch (Exception exception)
		{
			exception.printStackTrace();
		}

//	Set up the default file name for the summary file

		String fn = EnvExpander.expand(DecodesSettings.instance().decwizSummaryLog.trim());
		File f = new File(fn);
		summaryFileChooser.setSelectedFile(f);
		saveSummaryField.setText(f.getPath());
	}

	private void jbInit() throws Exception
	{
		this.setLayout(gridBagLayout1);
		rawLabel.setText("Move Raw Data To:");
		rawField.setPreferredSize(new Dimension(180, 23));
		rawField.setText("");
		browseRawButton.setPreferredSize(new Dimension(100, 27));
		browseRawButton.setText("Browse");
		saveLabel.setText("Save Decoded Data To:");
		saveDecodedField.setPreferredSize(new Dimension(180, 23));
		saveDecodedField.setText("");
		browseDecodedButton.setPreferredSize(new Dimension(100, 27));
		browseDecodedButton.setText("Browse");
		saveSummaryLabel.setText("Save Summary To:");
		saveSummaryField.setPreferredSize(new Dimension(180, 23));
		saveSummaryField.setText("");
		browseSummaryButton.setPreferredSize(new Dimension(100, 27));
		browseSummaryButton.setText("Browse");
		doSaveButton.setPreferredSize(new Dimension(150, 27));

		this.add(rawLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.5
			, GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE,
			new Insets(20, 20, 5, 2), 0, 0));
		this.add(rawField, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.5
			, GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
			new Insets(20, 0, 5, 0), 0, 0));
		this.add(browseRawButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.5
			, GridBagConstraints.SOUTH, GridBagConstraints.NONE,
			new Insets(20, 10, 5, 5), 0, 0));
		this.add(rawCombo, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.5
			, GridBagConstraints.SOUTHWEST, GridBagConstraints.NONE,
			new Insets(20, 5, 5, 10), 0, 0));
		
		this.add(saveLabel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
			, GridBagConstraints.EAST, GridBagConstraints.NONE,
			new Insets(5, 10, 5, 2), 0, 0));
		this.add(saveDecodedField, new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0
			, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
			new Insets(5, 0, 5, 0), 0, 0));
		this.add(browseDecodedButton,
			new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(5, 10, 5, 5), 0, 0));
		this.add(decodedCombo,
			new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0, 
				GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(5, 5, 5, 10), 0, 0));

		this.add(saveSummaryLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
			, GridBagConstraints.EAST, GridBagConstraints.NONE,
			new Insets(5, 20, 20, 2), 0, 0));
		this.add(saveSummaryField, new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0
			, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
			new Insets(5, 0, 20, 0), 0, 0));
		this.add(browseSummaryButton, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0
			, GridBagConstraints.WEST, GridBagConstraints.NONE,
			new Insets(5, 10, 20, 5), 0, 0));
		this.add(summaryCombo,
			new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0, 
				GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
				new Insets(5, 5, 5, 10), 0, 0));

		this.add(doSaveButton,
			new GridBagConstraints(0, 3, 4, 1, 0.0, 0.5, 
				GridBagConstraints.NORTH, GridBagConstraints.NONE,
				new Insets(20, 15, 20, 15), 0, 0));

		browseRawButton.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					browseRawButtonPressed();
				}
			});
		browseDecodedButton.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					browseDecodedButtonPressed();
				}
			});
		browseSummaryButton.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					browseSummaryButtonPressed();
				}
			});
		doSaveButton.addActionListener(
			new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					doSaveButtonPressed();
				}
			});
		rawCombo.setSelectedIndex(1);
		decodedCombo.setSelectedIndex(0);
		summaryCombo.setSelectedIndex(0);
		rawDataFileChooser.setDialogTitle("Save Raw Data File");
		decodedDataFileChooser.setDialogTitle("Save Decoded Data");
		summaryFileChooser.setDialogTitle("Save Summary Log");
	}

	public String getTitle()
	{
		return "Save Raw & Decoded Data";
	}

	public void activate()
	{
		setChoosers();
	}

	public void setChoosers()
	{
		Date lastTime = getDecodePanel().getLastTimeOfDecodedData();
		if ( lastTime == null ) 
			lastTime = new Date();
		String tzname = "UTC";
		TransportMedium tm = getFileIdPanel().getTransportMedium();
		if ( tm == null ) {
			tzname = "UTC";
		}
		String siteNo = getFileIdPanel().getSiteDisplayName();
		if( siteNo  == null )
			siteNo = "uknownsite";
		Properties p = new Properties(System.getProperties());
		p.setProperty("SITENAME",siteNo);
		p.setProperty("AGENCY",DecodesSettings.instance().agency);
		p.setProperty("LOCATION",DecodesSettings.instance().location);
		String archiveDir = DecodesSettings.instance().archiveDataDir;
		String df = null;

		String dir = archiveDir;
		if ( dir == null || dir.isEmpty() ) 
			dir = DecodesSettings.instance().decwizRawDataDir;
		dir = EnvExpander.expand(dir,p,lastTime,tzname);
		File f = new File(dir);
		if (!f.isDirectory())
			f.mkdirs();
		String archiveFile = DecodesSettings.instance().archiveDataFileName;
		if ( archiveFile == null || archiveFile.isEmpty() )
			archiveFile = getFileIdPanel().getFileName();
		File output = new File( EnvExpander.expand(dir+
					"/"+archiveFile,p,lastTime, tzname));
		rawDataFileChooser.setSelectedFile(output);
		rawField.setText(output.getPath());

		dir = archiveDir;
		if ( dir == null || dir.isEmpty() ) 
			dir = DecodesSettings.instance().decwizSummaryLog;
		dir = EnvExpander.expand(dir,p,lastTime,tzname);
		f = new File(dir);
		if (!f.isDirectory())
			f.mkdirs();
		archiveFile = DecodesSettings.instance().archiveSummaryFileName;
		if ( archiveFile == null || archiveFile.isEmpty() ) {
			archiveFile = DecodesSettings.instance().decwizSummaryLog+".${DATE(yyyyMMdd)";
			output = new File( EnvExpander.expand(archiveFile,p,lastTime,tzname));
		} else
			output = new File( EnvExpander.expand(dir+"/"+archiveFile,p,lastTime,tzname));
		summaryFileChooser.setSelectedFile(output);
		saveSummaryField.setText(output.getPath());

		dir = DecodesSettings.instance().decwizDecodedDataDir;
		if ( dir == null || dir.isEmpty() ) 
			dir = "${HOME}";
		else {
			dir = EnvExpander.expand(dir,p,lastTime,tzname);
			f = new File(dir);
			if (!f.isDirectory())
				f.mkdirs();
		}
		archiveFile = DecodesSettings.instance().decodedDataFileName;
		if ( archiveFile == null || archiveFile.isEmpty() )
			archiveFile = getFileIdPanel().getSiteDisplayName();
		output = new File(EnvExpander.expand(dir+"/"+archiveFile,p));
		decodedDataFileChooser.setSelectedFile(output);
		saveDecodedField.setText(output.getPath());
	}

	public boolean deactivate()
	{
		return true;
	}

	private void browseRawButtonPressed()
	{
		if (rawDataFileChooser.showSaveDialog(this) 
			== JFileChooser.APPROVE_OPTION)
		{
			File f = rawDataFileChooser.getSelectedFile();
			rawField.setText(f.getPath());
		}
	}

	private void browseDecodedButtonPressed()
	{
		if (decodedDataFileChooser.showSaveDialog(this) 
			== JFileChooser.APPROVE_OPTION)
		{
			File f = decodedDataFileChooser.getSelectedFile();
			saveDecodedField.setText(f.getPath());
		}
	}

	private void browseSummaryButtonPressed()
	{
		if (summaryFileChooser.showSaveDialog(this) 
			== JFileChooser.APPROVE_OPTION)
		{
			File f = summaryFileChooser.getSelectedFile();
			saveSummaryField.setText(f.getPath());
		}
	}


	private void doSaveButtonPressed()
	{
		FileOutputStream fos = null;
		FileLock flock = null;
		String fn = rawField.getText().trim();

		boolean append = (rawCombo.getSelectedIndex() == 0);
		int filesSaved = 0;
		if (fn.length() > 0)
		{
			try
			{
				fos = new FileOutputStream(fn, append);
				FileChannel chan = fos.getChannel();
				flock = chan.tryLock();
				String s = getFileIdPanel().getRawData();
				String out = s.replaceAll("\u00AE","\r");
				fos.write(out.getBytes());
				filesSaved++;
			}
			catch(IOException ex)
			{
				showError("Cannot save raw data to '" + fn + "': " + ex);
			}
			finally
			{
				if (flock != null)
					try { flock.release(); } catch(Exception ex) {}
				if (fos != null)
					try { fos.close(); } catch(Exception ex) {}
			}
			File f = new File(getFileIdPanel().getFilePath());
			f.delete();
		}

		fos = null;
		flock = null;
		fn = saveSummaryField.getText().trim();
		append = (summaryCombo.getSelectedIndex() == 0);
		if (fn.length() > 0)
		{
			try
			{
				fos = new FileOutputStream(fn, append);
				FileChannel chan = fos.getChannel();
				flock = chan.tryLock();
				String s = getDecodePanel().getSummaryData();
				fos.write(s.getBytes());
				filesSaved++;
			}
			catch(IOException ex)
			{
				showError("Cannot save summary data to '" + fn + "': " + ex);
			}
			finally
			{
				if (flock != null)
					try { flock.release(); } catch(Exception ex) {}
				if (fos != null)
					try { fos.close(); } catch(Exception ex) {}
			}
		}

		fos = null;
		flock = null;
		fn = saveDecodedField.getText().trim();
		append = (decodedCombo.getSelectedIndex() == 0);
		if (fn.length() > 0)
		{
			try
			{
				fos = new FileOutputStream(fn, append);
				FileChannel chan = fos.getChannel();
				flock = chan.tryLock();
				String s = getDecodePanel().getDecodedData();
				fos.write(s.getBytes());
				filesSaved++;
			}
			catch(IOException ex)
			{
				showError("Cannot save decoded data to '" + fn + "': " + ex);
			}
			finally
			{
				if (flock != null)
					try { flock.release(); } catch(Exception ex) {}
				if (fos != null)
					try { fos.close(); } catch(Exception ex) {}
			}
		}
		
		if (filesSaved > 0)
			JOptionPane.showMessageDialog(TopFrame.instance(), 
			"" + filesSaved + " Files saved.", "Files Saved", 
			JOptionPane.INFORMATION_MESSAGE);
	}
}
