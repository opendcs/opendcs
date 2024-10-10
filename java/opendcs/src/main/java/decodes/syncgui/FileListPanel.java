package decodes.syncgui;

import java.io.File;
import java.io.IOException;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

import ilex.gui.MultFileDownloadDialog;
import ilex.util.EnvExpander;

/**
Panel for displaying a list of database XML files.
*/
public class FileListPanel extends JPanel
{
	BorderLayout borderLayout1 = new BorderLayout();
	JPanel jPanel1 = new JPanel();
	JLabel jLabel1 = new JLabel();
	JLabel dbLabel = new JLabel();
	JLabel fileTypeLabel = new JLabel();
	JLabel jLabel2 = new JLabel();
	JPanel jPanel2 = new JPanel();
	JButton importButton = new JButton();
	JButton downloadButton = new JButton();
	JScrollPane jScrollPane1 = new JScrollPane();
	FileListModel fileListModel = new FileListModel();
	JList jFileList = new JList(fileListModel);
	static private JFileChooser jFileChooser = new JFileChooser();
	static
	{
		jFileChooser.setCurrentDirectory(
			new File(EnvExpander.expand("$DECODES_INSTALL_DIR")));
	}


	public FileListPanel()
	{
		try {
			jbInit();
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	void jbInit() throws Exception
	{
		this.setLayout(borderLayout1);
		jLabel1.setVerifyInputWhenFocusTarget(true);
		jLabel1.setText("Database ");
		dbLabel.setText("MVM 2004-08-23 00:04");
		fileTypeLabel.setText("Site");
		jLabel2.setText("files");
		importButton.setPreferredSize(new Dimension(100, 27));
		importButton.setToolTipText("Imports selected files to your Edit Database.");
		importButton.setText("Import");
		importButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					importButton_actionPerformed(e);
				}
			});
		downloadButton.setPreferredSize(new Dimension(100, 27));
		downloadButton.setToolTipText("Download selected XML files to import later.");
		downloadButton.setText("Download");
		downloadButton.addActionListener(
			new java.awt.event.ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					downloadButton_actionPerformed(e);
				}
			});
		this.add(jPanel1, BorderLayout.NORTH);
		jPanel1.add(jLabel1, null);
		jPanel1.add(dbLabel, null);
		jPanel1.add(fileTypeLabel, null);
		jPanel1.add(jLabel2, null);
		this.add(jPanel2, BorderLayout.SOUTH);
		jPanel2.add(importButton, null);
		jPanel2.add(downloadButton, null);
		this.add(jScrollPane1, BorderLayout.CENTER);
		jScrollPane1.getViewport().add(jFileList, null);
	}

	/** Sets the FileList object being displayed by this panel. */
	public void setFileList(FileList fl)
	{
		fileListModel.setFileList(fl);
		dbLabel.setText(fl.getSnap().getFullName());
		fileTypeLabel.setText("'" + fl.toString() + "'");
	}

	void importButton_actionPerformed(ActionEvent e)
	{
		int idx = jFileList.getSelectedIndex();
		if (idx == -1)
			return;
		Object objs[] = jFileList.getSelectedValues();
		String files[] = new String[objs.length];
		for(int i=0; i<objs.length; i++)
			files[i] = (String)objs[i];

		String decHomePath = EnvExpander.expand("$DECODES_INSTALL_DIR");
		DistrictDBSnap snapshot = fileListModel.fileList.myDB;
		String tmpPath = decHomePath + "/tmp";
		File tmpDir = new File(tmpPath);
		if (!tmpDir.isDirectory() && !tmpDir.mkdirs())
		{
			SyncGuiFrame.instance().showError("The directory '"
				+ tmpPath + " does not exist and cannot be created!");
			return;
		}

		String urldir =
			SyncConfig.instance().getHubHome() + "/"
			+ snapshot.getDistrict().getName() + "/"
			+ snapshot.getDirName() + "/" + fileListModel.fileList.fileType;
		MultFileDownloadDialog dlg = new MultFileDownloadDialog(
			SyncGuiFrame.instance(), "Downloading XML Files", true);
		if (!dlg.downloadFiles(urldir, files, tmpPath))
			return;

		String cmdarray[] = new String[5 + files.length];
		cmdarray[0] = "java";
		cmdarray[1] = "-cp";
		cmdarray[2] = System.getProperty("java.class.path");
		cmdarray[3] = "-DDECODES_INSTALL_DIR=" + decHomePath;
		cmdarray[4] = "decodes.dbimport.DbImport";
		for(int i=0; i<files.length; i++)
			cmdarray[5+i] = files[i];

		try
		{
			Process importProc 
				= Runtime.getRuntime().exec(cmdarray,null,tmpDir);
			int result = importProc.waitFor();
			if (result != 0)
				SyncGuiFrame.instance().showError(
					"Import process failed! Check the file '"
					+ decHomePath + "/tmp/util.log for details.");
			else
			{
				JOptionPane.showMessageDialog(SyncGuiFrame.instance(),
				"" + files.length + " files imported to your edit database.");
				for(int i=0; i<files.length; i++)
				{
					File f = new File(decHomePath + "/tmp/" + files[i]);
					f.delete();
				}
			}
		}
		catch(IOException ex)
		{
			SyncGuiFrame.instance().showError(
				"Could not start import process!");
		}
		catch(InterruptedException ex)
		{
			SyncGuiFrame.instance().showError("Import process interrupted!");
		}

	}

	void downloadButton_actionPerformed(ActionEvent e)
	{
		int idx = jFileList.getSelectedIndex();
		if (idx == -1)
			return;
		Object objs[] = jFileList.getSelectedValues();
		String names[] = new String[objs.length];
		for(int i=0; i<objs.length; i++)
			names[i] = (String)objs[i];

		String decHomePath = EnvExpander.expand("$DECODES_INSTALL_DIR");
		DistrictDBSnap snapshot = fileListModel.fileList.myDB;
		String dbPath = decHomePath + "/" + snapshot.getDistrict().getName();

		jFileChooser.setDialogTitle("Save Database files to ...");
		jFileChooser.setSelectedFile(new File(dbPath));
		jFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		if (jFileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
		{
			File dir = jFileChooser.getSelectedFile();
			String localdir = dir.getPath();
			String urldir =
				SyncConfig.instance().getHubHome() + "/"
				+ snapshot.getDistrict().getName() + "/"
				+ snapshot.getDirName() + "/" + fileListModel.fileList.fileType;
			MultFileDownloadDialog dlg = new MultFileDownloadDialog(
				SyncGuiFrame.instance(), "Downloading XML Files", true);
			dlg.downloadFiles(urldir, names, localdir);
		}
	}
}

class FileListModel extends AbstractListModel
{
	/** The FileList object being displayed */
	FileList fileList;

	FileListModel()
	{
		super();
		fileList = null;
	}

	void setFileList(FileList fl)
	{
		this.fileList = fl;

//System.out.println(fl.getSnap().getFullName() + " setFileList - size=" + fileList.getFileNames().size());
		fireContentsChanged(this, 0, fileList.getFileNames().size() - 1);
	}

	public Object getElementAt(int index)
	{
		if (fileList == null)
			return "";
		if (index >= getSize())
			return "";
		return fileList.getFileNames().elementAt(index);
	}

	public int getSize()
	{
		if (fileList == null)
			return 0;
		return fileList.size();
	}
}
