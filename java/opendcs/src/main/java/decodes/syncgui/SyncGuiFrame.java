package decodes.syncgui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import ilex.util.Logger;
import ilex.util.AsciiUtil;
import decodes.gui.TopFrame;

public class SyncGuiFrame extends TopFrame
{
	JPanel contentPane;
	JMenuBar jMenuBar1 = new JMenuBar();
	JMenu jMenuFile = new JMenu();
	JMenuItem jMenuFileExit = new JMenuItem();
	JMenu jMenuHelp = new JMenu();
	JMenuItem jMenuHelpAbout = new JMenuItem();
	BorderLayout borderLayout1 = new BorderLayout();
	JSplitPane jSplitPane1 = new JSplitPane();
	TreePanel treePanel = new TreePanel();
	JPanel rightPanel = new JPanel();
	BorderLayout borderLayout2 = new BorderLayout();
	JPanel jPanel1 = new JPanel();
	GridBagLayout gridBagLayout1 = new GridBagLayout();
	JPanel leftStatus = new JPanel();
	JPanel rightStatus = new JPanel();
	JLabel leftStatusLabel = new JLabel();
	JLabel rightStatusLabel = new JLabel();
	FlowLayout flowLayout1 = new FlowLayout();
	FlowLayout flowLayout2 = new FlowLayout();
	JScrollPane scrollPane = new JScrollPane();
	DistrictPanel districtPanel = new DistrictPanel();
	FileListPanel fileListPanel = new FileListPanel();
	PlatListPanel platListPanel = new PlatListPanel();
	SnapshotPanel snapshotPanel = new SnapshotPanel();
	TopPanel topPanel = new TopPanel();

	/** The singleton instance */
	private static SyncGuiFrame _instance = null;

	/** Thread to download files in the background */
	private DownloadThread downloadThread;

	/** @return the singleton instance. */
	public static SyncGuiFrame instance()
	{
		if (_instance == null)
			_instance = new SyncGuiFrame();
		return _instance;
	}


	//Construct the frame
	private SyncGuiFrame()
	{
		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		try {
			jbInit();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		downloadThread = new DownloadThread();
		downloadThread.start();
		showTopPanel();
//		SwingUtilities.invokeLater(
//			new Runnable()
//			{
//				public void run()
//				{
//					treePanel.tree.expandRow(0);
//				}
//			});
	}

	//Component initialization
	private void jbInit() throws Exception	{
		setTitle("DECODES Database Synchronization GUI");
		contentPane = (JPanel) this.getContentPane();
		contentPane.setLayout(borderLayout1);
		this.setSize(new Dimension(940, 559));
		this.setTitle("DECODES Database Synchronization");
		jMenuFile.setText("File");
		jMenuFileExit.setText("Exit");
		jMenuFileExit.addActionListener(new SyncGuiFrame_jMenuFileExit_ActionAdapter(this));
		jMenuHelp.setText("Help");
		jMenuHelpAbout.setText("About");
		jMenuHelpAbout.addActionListener(new SyncGuiFrame_jMenuHelpAbout_ActionAdapter(this));
		rightPanel.setLayout(borderLayout2);
		jPanel1.setLayout(gridBagLayout1);
		jPanel1.setMinimumSize(new Dimension(10, 25));
		jPanel1.setPreferredSize(new Dimension(10, 25));
		leftStatus.setBorder(BorderFactory.createLoweredBevelBorder());
		leftStatus.setMinimumSize(new Dimension(18, 45));
		leftStatus.setPreferredSize(new Dimension(18, 45));
		leftStatus.setLayout(flowLayout1);
		rightStatus.setBorder(BorderFactory.createLoweredBevelBorder());
		rightStatus.setMinimumSize(new Dimension(55, 45));
		rightStatus.setPreferredSize(new Dimension(55, 45));
		rightStatus.setLayout(flowLayout2);
		leftStatusLabel.setText(" ");
		rightStatusLabel.setText("");
		flowLayout1.setAlignment(FlowLayout.LEFT);
		flowLayout1.setHgap(2);
		flowLayout1.setVgap(1);
		flowLayout2.setAlignment(FlowLayout.LEFT);
		flowLayout2.setHgap(2);
		flowLayout2.setVgap(1);
		treePanel.setFont(new java.awt.Font("Dialog", 0, 16));
    jMenuFile.add(jMenuFileExit);
		jMenuHelp.add(jMenuHelpAbout);
		jMenuBar1.add(jMenuFile);
		jMenuBar1.add(jMenuHelp);
		this.setJMenuBar(jMenuBar1);
		contentPane.add(jSplitPane1, BorderLayout.CENTER);
		jSplitPane1.add(scrollPane, JSplitPane.LEFT);
		scrollPane.getViewport().add(treePanel, null);
		jSplitPane1.add(rightPanel, JSplitPane.RIGHT);
		contentPane.add(jPanel1,  BorderLayout.SOUTH);
		jPanel1.add(leftStatus,    new GridBagConstraints(0, 0, 1, 1, 0.8, 1.0
		        ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
		jPanel1.add(rightStatus,  new GridBagConstraints(1, 0, 1, 1, 0.2, 1.0
		        ,GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 75, 0));
		leftStatus.add(leftStatusLabel, null);
		rightStatus.add(rightStatusLabel, null);
    	jSplitPane1.setDividerLocation(360);
	}

	//File | Exit action performed
	public void jMenuFileExit_actionPerformed(ActionEvent e) {
		System.exit(0);
	}

	//Help | About action performed
	public void jMenuHelpAbout_actionPerformed(ActionEvent e) {
		SyncGuiFrame_AboutBox dlg = new SyncGuiFrame_AboutBox(this);
		Dimension dlgSize = dlg.getPreferredSize();
		Dimension frmSize = getSize();
		Point loc = getLocation();
		dlg.setLocation((frmSize.width - dlgSize.width) / 2 + loc.x, (frmSize.height - dlgSize.height) / 2 + loc.y);
		dlg.setModal(true);
		dlg.pack();
		dlg.show();
	}

	//Overridden so we can exit when window is closed
	protected void processWindowEvent(WindowEvent e) {
		super.processWindowEvent(e);
		if (e.getID() == WindowEvent.WINDOW_CLOSING) {
			jMenuFileExit_actionPerformed(null);
		}
	}

//	/**
//	 * Opens a stream for an URL.
//	 * @param relurl the path of the URL relative to HUB_HOME
//	 * @return the InputStream or null if error.
//	 */
//	public InputStream openURL(String relurl)
//		throws MalformedURLException,
//	{
//		String hh = SyncConfig.instance().getHubHome();
//		String urlstr = hh + "/" + relurl;
//		try
//		{
//			URL url = new URL(urlstr);
//			return url.openStream();
//		}
//		catch(MalformedURLException ex)
//		{
//			String msg = "Error Malformed URL: " + urlstr;
//			Logger.instance().failure(msg);
//			return null;
//		}
//		catch(IOException ex)
//		{
//			String msg = "Error: " + urlstr + ": " + ex.getMessage();
//			Logger.instance().failure(msg);
//			return null;
//		}
//	}
//
//	/**
//	  Closes the passed stream and clears the status bar.
//	  @param strm the stream to close
//	*/
//	public void closeURL(InputStream strm, Exception ex)
//	{
//		try { strm.close(); }
//		catch(IOException ex2) {}
//		if (ex != null)
//			Logger.instance().warning(ex.toString());
//	}

	/**
	  Displays a string in the left part of the status bar.
	  @param str the string to display.
	*/
	public void showLeftStatus(String str)
	{
		leftStatusLabel.setText(str);
	}

	/**
	  Displays a string in the right part of the status bar.
	  @param str the string to display.
	*/
	public void showRightStatus(String str)
	{
		rightStatusLabel.setText(str);
	}

	/** Downloads a file in the background. */
	public void downloadBackground(String relurl, DownloadReader reader)
	{
		downloadThread.enqueue(relurl, reader);
	}

	public void showTopPanel()
	{
		rightPanel.setVisible(false);
		rightPanel.removeAll();
		rightPanel.add(topPanel, BorderLayout.CENTER);
		rightPanel.setVisible(true);
	}

	public void showDistrictPanel(District dist)
	{
		rightPanel.setVisible(false);
		rightPanel.removeAll();
		rightPanel.add(districtPanel, BorderLayout.CENTER);
		districtPanel.setDistrict(dist);
		rightPanel.setVisible(true);
	}

	public void showSnapshotPanel(DistrictDBSnap snap)
	{
		rightPanel.setVisible(false);
		rightPanel.removeAll();
		rightPanel.add(snapshotPanel, BorderLayout.CENTER);
		snapshotPanel.setSnapshot(snap);
		rightPanel.setVisible(true);
	}

	public void showPlatListPanel(PlatList platList)
	{
		rightPanel.setVisible(false);
		rightPanel.removeAll();
		rightPanel.add(platListPanel, BorderLayout.CENTER);
		platListPanel.setPlatList(platList);
		rightPanel.setVisible(true);
	}

	public void showFileListPanel(FileList fileList)
	{
		rightPanel.setVisible(false);
		rightPanel.removeAll();
		fileListPanel.setFileList(fileList);
		rightPanel.add(fileListPanel, BorderLayout.CENTER);
		rightPanel.setVisible(true);
	}

	/**
	  Starts a modal error dialog with the passed message.
	  @param msg the error message
	*/
	public void showError(String msg)
	{
		Logger.instance().failure(msg);
		JOptionPane.showMessageDialog(this,
			AsciiUtil.wrapString(msg, 60), "Error!", JOptionPane.ERROR_MESSAGE);
	}


}

class SyncGuiFrame_jMenuFileExit_ActionAdapter implements ActionListener {
	SyncGuiFrame adaptee;

	SyncGuiFrame_jMenuFileExit_ActionAdapter(SyncGuiFrame adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.jMenuFileExit_actionPerformed(e);
	}
}

class SyncGuiFrame_jMenuHelpAbout_ActionAdapter implements ActionListener {
	SyncGuiFrame adaptee;

	SyncGuiFrame_jMenuHelpAbout_ActionAdapter(SyncGuiFrame adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.jMenuHelpAbout_actionPerformed(e);
	}
}
