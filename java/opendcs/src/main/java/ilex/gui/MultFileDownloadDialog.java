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
public class MultFileDownloadDialog extends JDialog
{
	private JPanel panel1 = new JPanel();
	private BorderLayout borderLayout1 = new BorderLayout();
	private JPanel jPanel1 = new JPanel();
	private JButton cancelButton = new JButton();
	private JPanel jPanel2 = new JPanel();
	private JLabel jLabel1 = new JLabel();
	private JLabel jLabel2 = new JLabel();
	private JLabel jLabel3 = new JLabel();
	JProgressBar thisFileProgressBar = new JProgressBar();
	private JTextField fromField = new JTextField();
	private JTextField toField = new JTextField();
	JTextField numFilesField = new JTextField();
	private GridBagLayout gridBagLayout1 = new GridBagLayout();
	private mDownloadThread downloadThread;
    JLabel aLabel = new JLabel();

	private boolean cancelledFlag;
	private boolean closeOnComplete;
	private boolean downloadResult;
	JLabel jLabel4 = new JLabel();
	JProgressBar totalProgressBar = new JProgressBar();
    JLabel jLabel5 = new JLabel();
    JTextField currentFileField = new JTextField();

	int numDone;
	int numFiles;

	/**
	* Constructor is public, you may instantiate directly or call the
	* static 'downloadFile' method.
	*/
	public MultFileDownloadDialog(Frame frame, String title, boolean modal)
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
		thisFileProgressBar.setMinimum(0);
	}

	/** no-args constructor for JBuilder */
	public MultFileDownloadDialog()
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

	/**
	 * Download a set of files.
	 *
	 * @param urldir the URL representing the directory containing the files
	 * @param files an array of file names
	 * @param localdir the local directory
	 * @return true if download successful, false if error.
	 */
	public boolean downloadFiles(String urldir, String files[], String localdir)
	{
		cancelledFlag = false;
		downloadThread = new mDownloadThread(this, urldir, files, localdir);
		numDone = 0;
		numFiles = files.length;
		totalProgressBar.setMaximum(files.length);

		fromField.setText(urldir);
		toField.setText(localdir);
		numFilesField.setText("0/" + files.length);

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
		if (cancelledFlag)
			return false;
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
		jLabel3.setText("Files Transferred:");
		thisFileProgressBar.setStringPainted(true);
		fromField.setEditable(false);
		fromField.setText("");
		toField.setEditable(false);
		toField.setText("");
		numFilesField.setEditable(false);
		numFilesField.setText("");
		panel1.setPreferredSize(new Dimension(560, 300));
        aLabel.setText("Total Progress:");
        jLabel4.setText("This File:");
    totalProgressBar.setStringPainted(true);
    jLabel5.setText("Current File:");
        currentFileField.setEditable(false);
        currentFileField.setText("");
        getContentPane().add(panel1);
		panel1.add(jPanel1, BorderLayout.SOUTH);
		jPanel1.add(cancelButton, null);
		panel1.add(jPanel2, BorderLayout.CENTER);
		jPanel2.add(jLabel2,     new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(4, 10, 4, 2), 0, 0));
		jPanel2.add(jLabel1,     new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.NORTHEAST, GridBagConstraints.NONE, new Insets(20, 10, 4, 2), 0, 0));
		jPanel2.add(fromField,     new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(20, 0, 4, 22), 0, 0));
		jPanel2.add(toField,     new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(4, 0, 4, 22), 0, 0));
		jPanel2.add(numFilesField,     new GridBagConstraints(1, 3, 1, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(4, 0, 4, 150), 120, 0));
		jPanel2.add(jLabel3,     new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(4, 10, 4, 2), 0, 0));
		jPanel2.add(thisFileProgressBar,         new GridBagConstraints(1, 5, 1, 1, 1.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(15, 0, 5, 22), 0, 0));
        jPanel2.add(aLabel,       new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 0, 10, 0), 0, 0));
    jPanel2.add(jLabel4,    new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(15, 15, 5, 2), 0, 0));
    jPanel2.add(totalProgressBar,   new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 10, 22), 0, 0));
        jPanel2.add(jLabel5,   new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(4, 10, 4, 2), 0, 0));
        jPanel2.add(currentFileField,   new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(4, 0, 4, 22), 0, 0));
	}

	/**
	 * Update the number of bytes already downloaded.
	 * @param num integer number of bytes downloaded so far.
	 */
	public void setProgress(final int num)
	{
		// Most likely invoked from a thread other than the GUI thread.

		SwingUtilities.invokeLater(
			new Runnable()
			{
				public void run()
				{
					thisFileProgressBar.setValue(num);
				}
			});
	}

	/**
	 * Called from the download thread when a new file is started.
	*/
	public void setCurrentFile(final String name, final int length)
	{
		numDone++;

		SwingUtilities.invokeLater(
			new Runnable()
			{
				public void run()
				{
					totalProgressBar.setValue(numDone);
					thisFileProgressBar.setMaximum(length);
					thisFileProgressBar.setValue(0);
					numFilesField.setText("" + numDone + "/" + numFiles);
    				currentFileField.setText(name);
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

	//public static void main(String args[])
	//{
		//String url = args[0];
		//String local = "downloaded";
		//MultFileDownloadDialog dlg = new MultFileDownloadDialog();
		//dlg.setCloseOnComplete(true);
		//dlg.downloadFile(url, local);
	//}
}

/**
This Thread class does the IO operations and calls the parent to update
the GUI status fields.
*/
class mDownloadThread extends Thread
{
	private MultFileDownloadDialog parent = null;
	private String urldir = null;
	private String localdir = null;
	private static int BUF_LEN = 4096;
	boolean cancelled;
	BufferedInputStream istrm = null;
	BufferedOutputStream ostrm = null;
	String targetDir = "";
	String files[];

	/**
	  Constructor.
	  @param parent the MultFileDownloadDialog that spawned this thread.
	  @param urldir The String containing the URL to download from
	  @param files an array of filenames
	  @param localdir the name of the local directory to download to
	*/
	mDownloadThread(MultFileDownloadDialog parent, String urldir, 
		String files[], String localdir)
	{
		this.parent = parent;
		this.urldir = urldir;
		this.files = files;
		this.localdir = localdir;
	}

	public void run()
	{
		cancelled = false;
		try { sleep(1000L); } catch(InterruptedException ex){}

		try
		{
			for(int i=0; !cancelled && i<files.length; i++)
			{
				String filename = files[i];

				String urlstr = urldir + "/" + filename;
				URL url = new URL(urlstr);
//System.out.println("Opening '" + urlstr + "'");
				URLConnection urlcon = url.openConnection();
				int len = urlcon.getContentLength();
				parent.setCurrentFile(filename, len);

				istrm = new BufferedInputStream(urlcon.getInputStream(),
					BUF_LEN);

//System.out.println("writing '" + localdir + File.separator + filename + "'");
				ostrm = new BufferedOutputStream(
					new FileOutputStream(localdir + File.separator + filename),
					BUF_LEN);

				byte buf[] = new byte[BUF_LEN];
				int done = 0;
				int buflen;

				while(!cancelled && (buflen = istrm.read(buf)) > 0)
				{
					ostrm.write(buf, 0, buflen);
					done += buflen;
					parent.setProgress(done);
				}
				istrm.close();
				ostrm.close();
			}
			if (!cancelled)
			{
				istrm.close();
				ostrm.close();
			}

			parent.downloadComplete(null);
		}
		catch(IOException ex)
		{
			parent.downloadComplete("Download failed: "+ex);
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
