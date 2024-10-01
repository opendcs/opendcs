package ilex.gui;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;

import ilex.util.AsciiUtil;
import ilex.util.FileUtil;
import ilex.util.ZipMonitor;

/**
This class opens a model dialog and starts a thread to download an URL
to a local file.
*/
public class FileDownloadDialog extends JDialog
	implements ZipMonitor
{
	private JPanel panel1 = new JPanel();
	private BorderLayout borderLayout1 = new BorderLayout();
	private JPanel jPanel1 = new JPanel();
	private JButton cancelButton = new JButton();
	private JPanel jPanel2 = new JPanel();
	private JLabel jLabel1 = new JLabel();
	private JLabel jLabel2 = new JLabel();
	private JLabel jLabel3 = new JLabel();
	private JLabel jLabel4 = new JLabel();
	JProgressBar progressBar = new JProgressBar();
	private JTextField fromField = new JTextField();
	private JTextField toField = new JTextField();
	JTextField sizeField = new JTextField();
	JTextField completedField = new JTextField();
	private GridBagLayout gridBagLayout1 = new GridBagLayout();
	private DownloadThread downloadThread;
    JLabel jLabel5 = new JLabel();
    JTextField unzipStatusField = new JTextField();
    JLabel doingWhatLabel = new JLabel();

	private boolean cancelledFlag;
	boolean doUnzip;
	private boolean closeOnComplete;
	private boolean downloadResult;

	/**
	* Constructor is public, you may instantiate directly or call the
	* static 'downloadFile' method.
	*/
	public FileDownloadDialog(Frame frame, String title, boolean modal)
	{
		super(frame, title, modal);
		try {
			jbInit();
			pack();
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
		cancelledFlag = false;
		downloadThread = null;
		progressBar.setMinimum(0);
		closeOnComplete = true;
	}

	/** no-args constructor for JBuilder */
	public FileDownloadDialog()
	{
		this(null, "File Download", true);
	}

	/**
	* Set to true to cause dialog to close when download is complete.
	* @param tf true or false.
	*/
	public void setCloseOnComplete(boolean tf)
	{
		closeOnComplete = tf;
	}

	public void setUnzipFlag(boolean tf)
	{
		doUnzip = tf;
    	doingWhatLabel.setVisible(tf);
    	unzipStatusField.setVisible(tf);
	}

	/**
	 * Download a URL to a local file.
	 * Call this method from the Swing GUI thread only.
	 * If errors occur, they will be displayed in a message dialog.
	 * @param urlstr the URL
	 * @param localname the local file name.
	 * @return true if download successful, false if error.
	 */
	public boolean downloadFile(String urlstr, String localname)
	{
    	doingWhatLabel.setText("Downloading");
		cancelledFlag = false;
		downloadThread = new DownloadThread(this, urlstr, localname);

		toField.setText(localname);
		fromField.setText(urlstr);
		sizeField.setText("Opening URL ...");

		Window owner = getOwner();
		if (owner != null)
		{
			Dimension dlgSize = getPreferredSize();
			Point loc = owner.getLocation();
			Dimension frmSize = owner.getSize();
			int x = (frmSize.width - dlgSize.width) / 2 + loc.x;
			if (x < 0) x = 0;
			int y = (frmSize.height - dlgSize.height) / 2 + loc.y;
			if (y < 0) y = 0;
			setLocation(x, y);
		}

		downloadResult = true;
		downloadThread.start();
		setVisible(true);

		// When setVisible returns, the dialog has been closed, either
		// due to cancel, error, or completion
		return downloadResult;
	}

	/**
	  Downloads and unzips the file (assuming it is a zip file) into the
	  specified target.
	*/
	public boolean downloadAndUnzip(String urlstr, final String localname, 
		final String targetDir)
	{
    	doingWhatLabel.setText("Downloading");
		cancelledFlag = false;
		downloadThread = new DownloadThread(this, urlstr, localname);
		downloadThread.targetDir = targetDir;

		toField.setText(localname);
		fromField.setText(urlstr);
		sizeField.setText("Opening URL ...");

		Window owner = getOwner();
		if (owner != null)
		{
			Dimension dlgSize = getPreferredSize();
			Point loc = owner.getLocation();
			Dimension frmSize = owner.getSize();
			int x = (frmSize.width - dlgSize.width) / 2 + loc.x;
			if (x < 0) x = 0;
			int y = (frmSize.height - dlgSize.height) / 2 + loc.y;
			if (y < 0) y = 0;
			setLocation(x, y);
		}

		downloadResult = true;
		setUnzipFlag(true);
		setCloseOnComplete(false);
		downloadThread.start();
		setVisible(true);

		return downloadResult;
	}

	/** JBuilder component initialization */
	private void jbInit() throws Exception
	{
		panel1.setLayout(borderLayout1);
		cancelButton.setText("Cancel");
		cancelButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					cancelButton_actionPerformed(e);
				}
			});
		jPanel2.setLayout(gridBagLayout1);
		jLabel1.setText("Downloading From:");
		jLabel2.setText("Downloading To:");
		jLabel3.setText("Total File Size:");
		jLabel4.setText("Bytes Downloaded:");
		progressBar.setStringPainted(true);
		fromField.setEditable(false);
		fromField.setText("");
		toField.setEditable(false);
		toField.setText("");
		sizeField.setEditable(false);
		sizeField.setText("");
		completedField.setEditable(false);
		completedField.setText("");
		panel1.setPreferredSize(new Dimension(560, 300));
        jLabel5.setEnabled(true);
        jLabel5.setText("Files Unzipped:");
        unzipStatusField.setEditable(false);
        unzipStatusField.setText("");
        doingWhatLabel.setText("Downloading");
        getContentPane().add(panel1);
		panel1.add(jPanel1, BorderLayout.SOUTH);
		jPanel1.add(cancelButton, null);
		panel1.add(jPanel2, BorderLayout.CENTER);
		jPanel2.add(completedField,
			    new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(4, 0, 4, 150), 120, 0));
		jPanel2.add(jLabel2,   new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(4, 10, 4, 2), 0, 0));
		jPanel2.add(jLabel1,   new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHEAST, GridBagConstraints.NONE, new Insets(20, 10, 4, 2), 0, 0));
		jPanel2.add(fromField,   new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(20, 0, 4, 22), 0, 0));
		jPanel2.add(toField,   new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(4, 0, 4, 22), 0, 0));
		jPanel2.add(sizeField,   new GridBagConstraints(1, 2, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(4, 0, 4, 150), 120, 0));
		jPanel2.add(jLabel3,   new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(4, 10, 4, 2), 0, 0));
		jPanel2.add(jLabel4,    new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(4, 10, 4, 2), 0, 0));
		jPanel2.add(progressBar,    new GridBagConstraints(0, 5, 2, 1, 1.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(15, 22, 0, 22), 0, 0));
        jPanel2.add(jLabel5,    new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(4, 10, 4, 2), 0, 0));
        jPanel2.add(unzipStatusField,     new GridBagConstraints(1, 4, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(4, 0, 4, 150), 120, 0));
        jPanel2.add(doingWhatLabel,   new GridBagConstraints(0, 6, 2, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 0, 10, 0), 0, 0));
	}

	/**
	 * Update the number of bytes already downloaded.
	 * @param nbytes integer number of bytes downloaded so far.
	 */
	public void setProgress(final int nbytes)
	{
		// Most likely invoked from a thread other than the GUI thread.

		SwingUtilities.invokeLater(
			new Runnable()
			{
				public void run()
				{
					progressBar.setValue(nbytes);
					completedField.setText("" + nbytes);
				}
			});
	}

	/**
	* Called from the inner thread doing the download when the transfer is
	* complete. If completion due to an error, errmsg will explain why.
	* Else errmsg will be null.
	* @param errmsg null if no error, explanation if completion is due to error.
	*/
	public void downloadComplete(final String errmsg)
	{
		if (errmsg == null && closeOnComplete)
			try { Thread.sleep(1000L); } catch(InterruptedException ex) {}

		SwingUtilities.invokeLater(
			new Runnable()
			{
				public void run()
				{
					if (errmsg != null && !cancelledFlag)
					{
						System.err.println(errmsg);
						JOptionPane.showMessageDialog(null,
							AsciiUtil.wrapString(errmsg, 60), "Error!",
							JOptionPane.ERROR_MESSAGE);
						closeDlg();
					}
					else if (closeOnComplete)
						closeDlg();
					else
						cancelButton.setText("Ok");
				}
			});
		downloadResult = (errmsg == null);
	}

	/**
	 * Cancel the download.
	 * @param e ActionEvent
	 */
	void cancelButton_actionPerformed(ActionEvent e)
	{
		if (cancelButton.getText().equals("Ok"))
		{
			closeDlg();
		}
		else
		{
			// Set flag so downloadComplete will not display error.
			cancelledFlag = true;
			downloadThread.cancel();
			downloadResult = false;
		}
	}

	/** Closes the dialog */
	void closeDlg()
	{
		setVisible(false);
		dispose();
	}

	/**
	  From ZipMonitor, Sets the status.
	  @param status the status
	*/
	public void setZipStatus(final String status)
	{
		SwingUtilities.invokeLater(
			new Runnable()
			{
				public void run()
				{
    				unzipStatusField.setText(status);
				}
			});
	}

	/**
	  From ZipMonitor,
	  Sets the number of entries in the zip file. Can be used to set the
	  dimension of a progress bar, etc.
	  @param num the number of entries in the zip file
	*/
	public void setNumZipEntries(final int num)
	{
		SwingUtilities.invokeLater(
			new Runnable()
			{
				public void run()
				{
					progressBar.setMaximum(num);
				}
			});
	}

	/**
	  From ZipMonitor, 
	  Sets the current progress, that is, the number of entries thus far
	  unzipped.
	  @param num current progress.
	*/
	public void setZipProgress(final int num)
		throws IOException
	{
		if (cancelledFlag)
			throw new IOException("Cancelled.");

		SwingUtilities.invokeLater(
			new Runnable()
			{
				public void run()
				{
					progressBar.setValue(num);
				}
			});
	}

	/**
	  From ZipMonitor, 
	  Called if the zipping or unzipping was aborted due to some error.
	  @param ex the Exception which caused the abort.
	*/
	public void zipFailed(final Exception ex)
	{
		downloadComplete("Unzip failed: " + ex);
	}

	/** Called when the transfer is completed successfully. */
	public void zipComplete()
	{
		SwingUtilities.invokeLater(
			new Runnable()
			{
				public void run()
				{
    				unzipStatusField.setText("Complete.");
				}
			});
		downloadComplete(null);
	}


	public static void main(String args[])
	{
		String url = args[0];
		String local = "downloaded";
		FileDownloadDialog dlg = new FileDownloadDialog();
		dlg.setCloseOnComplete(true);
		dlg.downloadFile(url, local);
	}
}

/**
This Thread class does the IO operations and calls the parent to update
the GUI status fields.
*/
class DownloadThread extends Thread
{
	private FileDownloadDialog parent = null;
	private String urlstr = null;
	private String localname = null;
	private static int BUF_LEN = 4096;
	boolean cancelled;
	BufferedInputStream istrm = null;
	BufferedOutputStream ostrm = null;
	String targetDir = "";

	/**
	  Constructor
	  @param parent the FileDownloadDialog that spawned this thread.
	  @param urlstr The String containing the URL to download from
	  @param localname the name of the local file to download to
	*/
	DownloadThread(FileDownloadDialog parent, String urlstr, String localname)
	{
		this.parent = parent;
		this.urlstr = urlstr;
		this.localname = localname;
	}

	public void run()
	{
		cancelled = false;
		try { sleep(1000L); } catch(InterruptedException ex){}

		try
		{
			// Open the input stream from the passed URL.
			URL url = new URL(urlstr);
			URLConnection urlcon = url.openConnection();
			int len = urlcon.getContentLength();
			parent.sizeField.setText("" + len);
			parent.completedField.setText("0");
			parent.progressBar.setMaximum(len);

			istrm = new BufferedInputStream(urlcon.getInputStream(), BUF_LEN);

			// Open an output stream from the passed localname
			ostrm = new BufferedOutputStream(
				new FileOutputStream(localname), BUF_LEN);

			byte buf[] = new byte[BUF_LEN];
			int done = 0;
			int buflen;

			while(!cancelled && (buflen = istrm.read(buf)) > 0)
			{
				ostrm.write(buf, 0, buflen);
				done += buflen;
				parent.setProgress(done);
			}
			if (!cancelled)
			{
				istrm.close();
				ostrm.close();
			}

			if (parent.doUnzip)
			{
    			parent.doingWhatLabel.setText("Unzipping");
				FileUtil.unzip(localname, targetDir, parent);
			}
			else
				parent.downloadComplete(null);
		}
		catch(IOException ex)
		{
			parent.downloadComplete("Download failed: "+ex);
			File lf = new File(localname);
			lf.delete();
		}
	}

	void cancel()
	{
		// Kill the background thread by closing the streams.
		cancelled = true;

		if (istrm != null)
			try { istrm.close(); } catch(IOException ex) {}
		if (ostrm != null)
			try { ostrm.close(); } catch(IOException ex) {}
	}
}
