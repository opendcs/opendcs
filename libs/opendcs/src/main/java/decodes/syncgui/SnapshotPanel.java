package decodes.syncgui;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import javax.swing.*;

import ilex.util.EnvExpander;
import ilex.util.AsciiUtil;
import ilex.util.FileUtil;
import ilex.gui.FileDownloadDialog;

import decodes.util.DecodesSettings;


/**
Panel shown on right side of GUI when a snapshot directory is selected.
*/
public class SnapshotPanel extends JPanel
{
	JLabel jLabel1 = new JLabel();
	JTextField districtField = new JTextField();
	JLabel jLabel2 = new JLabel();
	JTextField synchronizedOnField = new JTextField();
	JPanel jPanel1 = new JPanel();
	GridBagLayout gridBagLayout1 = new GridBagLayout();
	BorderLayout borderLayout1 = new BorderLayout();
	JPanel jPanel2 = new JPanel();
	JButton downloadCopyButton = new JButton();
	JButton restoreButton = new JButton();
	JTextArea jTextArea1 = new JTextArea();
	JTextArea jTextArea2 = new JTextArea();
	GridBagLayout gridBagLayout2 = new GridBagLayout();
	static private JFileChooser jFileChooser = new JFileChooser();
	static
	{
		jFileChooser.setCurrentDirectory(
			new File(EnvExpander.expand("$DECODES_INSTALL_DIR")));
	}

	DistrictDBSnap snapshot = null;
	JButton getZipSnapButton = new JButton();
	JTextArea jTextArea3 = new JTextArea();

	public SnapshotPanel() {
		try {
			jbInit();
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	void jbInit() throws Exception {
		synchronizedOnField.setEditable(false);
		synchronizedOnField.setText("2004-08-31 00:05");
		jLabel1.setText("District: ");
		this.setLayout(borderLayout1);
		districtField.setEditable(false);
		districtField.setText("MVM - Mississippi Valley Memphis");
		jLabel2.setText("Snapshot Synchronized On: ");
		jPanel1.setLayout(gridBagLayout1);
		jPanel2.setLayout(gridBagLayout2);
		downloadCopyButton.setToolTipText("Select a directory to hold a downloaded copy of this DB.");
		downloadCopyButton.setText("Download & Unzip");
		downloadCopyButton.addActionListener(new SnapshotPanel_downloadCopyButton_actionAdapter(this));
		restoreButton.setToolTipText("Copies this database OVER your current edit-db.");
		restoreButton.setText("Restore My Edit-DB");
		restoreButton.addActionListener(new SnapshotPanel_restoreButton_actionAdapter(this));
		jTextArea1.setBackground(SystemColor.control);
		jTextArea1.setMaximumSize(new Dimension(350, 2147483647));
		jTextArea1.setText("Download and unzip a copy of this snapshot into a separate directory " +
    "on my hard disk.");
		jTextArea1.setLineWrap(true);
		jTextArea1.setWrapStyleWord(true);
		jTextArea2.setWrapStyleWord(true);
		jTextArea2.setLineWrap(true);
		jTextArea2.setText("Restore my editable database from this snapshot. CAUTION: This will " +
		"overwrite the current contents of your edit-db!");
		jTextArea2.setBackground(SystemColor.control);
		jTextArea2.setMaximumSize(new Dimension(350, 2147483647));
		getZipSnapButton.setText("Get Zip Snapshot");
		getZipSnapButton.addActionListener(new SnapshotPanel_getZipSnapButton_actionAdapter(this));
		jTextArea3.setBackground(SystemColor.control);
		jTextArea3.setMaximumSize(new Dimension(350, 2147483647));
		jTextArea3.setEditable(false);
		jTextArea3.setText("Get the ZIP Snapshot File for this Database.");
		jTextArea3.setLineWrap(true);
		jTextArea3.setWrapStyleWord(true);
		jPanel1.add(jLabel1,	 new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
						,GridBagConstraints.SOUTHEAST, GridBagConstraints.NONE, new Insets(10, 0, 4, 2), 0, 0));
		jPanel1.add(districtField,	 new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0
						,GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, new Insets(10, 0, 4, 25), 0, 0));
		jPanel1.add(synchronizedOnField,		new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0
						,GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(4, 0, 15, 60), 0, 0));
		jPanel1.add(jLabel2,	 new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
						,GridBagConstraints.NORTHEAST, GridBagConstraints.NONE, new Insets(4, 15, 15, 2), 0, 0));
		this.add(jPanel2, BorderLayout.CENTER);
		this.add(jPanel1, BorderLayout.NORTH);
		jPanel2.add(downloadCopyButton,					new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
						,GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(15, 10, 15, 10), 0, 0));
		jPanel2.add(getZipSnapButton,			 new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
						,GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(20, 10, 15, 10), 0, 0));
		jPanel2.add(jTextArea3,		 new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
						,GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(15, 5, 15, 5), 0, 0));
		jPanel2.add(jTextArea2,		new GridBagConstraints(1, 2, 1, 2, 1.0, 1.0
						,GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(15, 5, 15, 5), 0, 0));
		jPanel2.add(jTextArea1,		 new GridBagConstraints(1, 1, 1, 1, 1.0, 0.0
						,GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(15, 5, 15, 5), 0, 0));
		jPanel2.add(restoreButton,	new GridBagConstraints(0, 2, 1, 1, 0.0, 1.0
						,GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(15, 10, 15, 10), 0, 0));
	}

	/**
	  Called when user presses the 'restore' button.
	  @param e ignored
	*/
	void restoreButton_actionPerformed(ActionEvent e)
	{
		DecodesSettings settings = DecodesSettings.instance();
		String dbPath = settings.editDatabaseLocation;

		if (settings.editDatabaseTypeCode != settings.DB_XML)
		{
			SyncGuiFrame.instance().showError(
				"Your editable database is not an XML database, so this "
				+ "operation cannot be performed.");
			return;
		}

		int res = JOptionPane.showConfirmDialog(SyncGuiFrame.instance(),
			AsciiUtil.wrapString(
			"This operation will OVERWRITE your current editable database"
			+ " at '" + dbPath
			+ "' with the contents of the selected database on the hub. "
			+ "\nARE YOU SURE you want to proceed?", 60),
			"Confirm Overwrite Edit Database", JOptionPane.YES_NO_OPTION);
		if (res != JOptionPane.OK_OPTION)
			return;


		res = JOptionPane.showConfirmDialog(SyncGuiFrame.instance(),
			AsciiUtil.wrapString(
			"First, your old edit-database will be renamed to '"
			+ dbPath + ".bak' ... Then the selected database will be "
			+ "downloaded from the hub.\nOK to proceed?", 60),
			"Confirm Overwrite Edit Database", JOptionPane.YES_NO_OPTION);
		if (res != JOptionPane.OK_OPTION)
			return;

		File dir = new File(dbPath);
		if (dir.isDirectory())
		{
			String bakname = dbPath + ".bak";
			File bakdir = new File(bakname);
			if (bakdir.exists() && !FileUtil.deleteDir(bakdir))
			{
				SyncGuiFrame.instance().showError(
					"The backup directory '" + bakname + "' cannot be "
					+ "deleted. Delete it manually and then retry this "
					+ "operation.");
				return;
			}
			dir.renameTo(bakdir);
		}

		if (!dir.mkdirs())
		{
			SyncGuiFrame.instance().showError(
				"The directory '" + dbPath + "' does not exist and "
				+ "cannot be created!");
			return;
		}

		String urlstr =
			SyncConfig.instance().getHubHome() + "/"
			+ snapshot.getDistrict().getName() + "/"
			+ snapshot.getDirName() + "/db.zip";
			
		FileDownloadDialog dlg = new FileDownloadDialog(
			SyncGuiFrame.instance(), "District Database Download", true);

		String localname = dbPath + "/db.zip";
		dlg.downloadAndUnzip(urlstr, localname, dbPath);
		File f = new File(localname);
		if (!f.delete())
			SyncGuiFrame.instance().showError(
				"Cannot delete '" + localname + "'");
	}

	/**
	  Called when user presses the 'download copy' button.
	  @param e ignored
	*/
	void downloadCopyButton_actionPerformed(ActionEvent e)
	{
		jFileChooser.setDialogTitle("Save Database Snapshot as ...");

		String decHomePath = EnvExpander.expand("$DECODES_INSTALL_DIR");
		String dbPath = decHomePath + "/" + snapshot.getDistrict().getName();
		jFileChooser.setCurrentDirectory(new File(decHomePath));
		jFileChooser.setSelectedFile(new File(dbPath));
		jFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		if (jFileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
		{
			File dir = jFileChooser.getSelectedFile();
			File decodesHome = new File(decHomePath);
			if (dir.equals(decodesHome))
			{
				SyncGuiFrame.instance().showError(
					 "You may not place this database at the DECODES home "
					+ "directory. Please select a subdirectory or some other"
					+ " location!");
				return;
			}
			String path = dir.getPath();
			if (dir.isFile())
			{
				SyncGuiFrame.instance().showError(
					 "The selected name '" + dir
					+ "' already exists as a normal file. Please enter a "
					+ "directory name in which to store the remote DECODES "
					+ "database snapshot.");
				return;
			}
			else if (dir.isDirectory())
			{
				int res = JOptionPane.showConfirmDialog(SyncGuiFrame.instance(),
					AsciiUtil.wrapString(
					"The directory '" + path + "' already exists. This "
					+ "operation will rename the existing directory with a "
					+ "'.bak' extension and then create a new directory to "
					+ "hold the DECODES "
					+ " database snapshot. Do you want to proceed?", 60),
					"Confirm Overwrite Directory", JOptionPane.YES_NO_OPTION);
				if (res != JOptionPane.OK_OPTION)
					return;
				String bakname = path + ".bak";
				File bakdir = new File(bakname);
				if (bakdir.exists() && !FileUtil.deleteDir(bakdir))
				{
					SyncGuiFrame.instance().showError(
						"The backup directory '" + bakname + "' cannot be "
						+ "deleted. Delete it manually and then retry this "
						+ "operation.");
					return;
				}
				dir.renameTo(bakdir);
			}
			if (!dir.mkdirs())
			{
				SyncGuiFrame.instance().showError(
					"The directory '" + path + "' does not exist and "
					+ "cannot be created!");
				return;
			}

			String urlstr =
				SyncConfig.instance().getHubHome() + "/"
				+ snapshot.getDistrict().getName() + "/"
				+ snapshot.getDirName() + "/db.zip";
			
			FileDownloadDialog dlg = new FileDownloadDialog(
				SyncGuiFrame.instance(), "District Database Download", true);

			String localname = path + "/db.zip";
			dlg.downloadAndUnzip(urlstr, localname, path);
			File f = new File(localname);
			if (!f.delete())
				SyncGuiFrame.instance().showError(
					"Cannot delete '" + localname + "'");
		}
	}

	/**
		Sets the snapshot shown in this panel.
		@param snap the snapshot
	*/
	public void setSnapshot(DistrictDBSnap snap)
	{
		this.snapshot = snap;
		districtField.setText(snapshot.getDistrict().getName());
		synchronizedOnField.setText(snapshot.toString());
	}

	void getZipSnapButton_actionPerformed(ActionEvent e)
	{
		jFileChooser.setDialogTitle("Save Database Snapshot Zip File as ...");
		String dbPath = EnvExpander.expand("$DECODES_INSTALL_DIR/"
				+ snapshot.getDistrict().getName() + "-db.zip");
//System.out.println("Setting chooser selection to '" + dbPath + "'");
		jFileChooser.setSelectedFile(new File(dbPath));
		jFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		if (jFileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
		{
			File f = jFileChooser.getSelectedFile();
			String path = f.getPath();
			if (f.isFile())
				f.delete();
			String urlstr =
				SyncConfig.instance().getHubHome() + "/"
				+ snapshot.getDistrict().getName() + "/"
				+ snapshot.getDirName() + "/db.zip";
			FileDownloadDialog dlg = new FileDownloadDialog(
				SyncGuiFrame.instance(), "District Database Download", true);

			dlg.downloadFile(urlstr, path);
		}
	}
}

class SnapshotPanel_restoreButton_actionAdapter implements java.awt.event.ActionListener {
	SnapshotPanel adaptee;

	SnapshotPanel_restoreButton_actionAdapter(SnapshotPanel adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.restoreButton_actionPerformed(e);
	}
}

class SnapshotPanel_downloadCopyButton_actionAdapter implements java.awt.event.ActionListener {
	SnapshotPanel adaptee;

	SnapshotPanel_downloadCopyButton_actionAdapter(SnapshotPanel adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.downloadCopyButton_actionPerformed(e);
	}
}

class SnapshotPanel_getZipSnapButton_actionAdapter implements java.awt.event.ActionListener {
	SnapshotPanel adaptee;

	SnapshotPanel_getZipSnapButton_actionAdapter(SnapshotPanel adaptee) {
		this.adaptee = adaptee;
	}
	public void actionPerformed(ActionEvent e) {
		adaptee.getZipSnapButton_actionPerformed(e);
	}
}
