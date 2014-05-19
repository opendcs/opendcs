package decodes.syncgui;

import java.awt.*;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.event.*;
import java.util.Collections;
import java.util.Comparator;
import java.io.*;
import java.net.URL;

import ilex.gui.FileDownloadDialog;
import ilex.gui.MultFileDownloadDialog;
import ilex.gui.ShowFileDialog;
import ilex.util.EnvExpander;
import ilex.util.FileUtil;
import decodes.gui.SortingListTable;
import decodes.gui.SortingListTableModel;


/**
 * This panel displays this list of platforms.
 */
public class PlatListPanel extends JPanel 
{
	BorderLayout borderLayout1 = new BorderLayout();
	JPanel jPanel1 = new JPanel();
	JLabel jLabel1 = new JLabel();
	JTextField dbNameField = new JTextField();
	FlowLayout flowLayout1 = new FlowLayout();
	JPanel buttonPanel = new JPanel();
	JButton importButton = new JButton();
	JButton downloadButton = new JButton();
	JButton viewButton = new JButton();
	FlowLayout flowLayout2 = new FlowLayout();
	JScrollPane jScrollPane1 = new JScrollPane();
	JTable platListTable;
	private ShowFileDialog showFileDialog = null;
	
	static private JFileChooser jFileChooser = new JFileChooser();
	static
	{
		jFileChooser.setCurrentDirectory(
			new File(EnvExpander.expand("$DECODES_INSTALL_DIR")));
	}


	/** The plat list being shown */
	PlatList platList;
	PlatListTableModel model;

	/** Constructor -- must be no-args for JBuilder */
	public PlatListPanel() 
	{
		platList = null;
		model = new PlatListTableModel(this);
		platListTable = new SortingListTable(model, 
			new int[] { 15, 10, 13, 18, 15, 40 });
		
		try {
			jbInit();
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	void jbInit() throws Exception {
		this.setLayout(borderLayout1);
		jLabel1.setText("Platforms in database: ");
		dbNameField.setPreferredSize(new Dimension(180, 23));
		dbNameField.setEditable(false);
		dbNameField.setText("MVM 2004-08-20 00:05");
		jPanel1.setLayout(flowLayout1);
		importButton.setPreferredSize(new Dimension(100, 27));
		importButton.setToolTipText("Import selected platforms to your Edit Database");
		importButton.setText("Import");
		importButton.addActionListener(
			new java.awt.event.ActionListener() 
			{
	            public void actionPerformed(ActionEvent e) 
	            {
	                importPressed();
	            }
	        });
		downloadButton.setPreferredSize(new Dimension(100, 27));
		downloadButton.setToolTipText("Download selected platforms to XML files to be imported later.");
		downloadButton.setText("Download");
		downloadButton.addActionListener(
			new java.awt.event.ActionListener() 
			{
	            public void actionPerformed(ActionEvent e) 
	            {
	                downloadPressed();
	            }
	        });
		viewButton.setText("View");
		viewButton.setPreferredSize(new Dimension(100, 27));
		viewButton.setToolTipText("View selected platform XML.");
		viewButton.addActionListener(
			new java.awt.event.ActionListener() 
			{
	            public void actionPerformed(ActionEvent e) 
	            {
	                viewPressed();
	            }
	        });
		buttonPanel.setLayout(flowLayout2);
		flowLayout2.setHgap(25);
		flowLayout2.setVgap(10);
		this.add(jPanel1, BorderLayout.NORTH);
		jPanel1.add(jLabel1, null);
		jPanel1.add(dbNameField, null);
		this.add(buttonPanel, BorderLayout.SOUTH);
		buttonPanel.add(importButton, null);
		buttonPanel.add(downloadButton, null);
		buttonPanel.add(viewButton, null);
		this.add(jScrollPane1, BorderLayout.CENTER);
		jScrollPane1.getViewport().add(platListTable, null);
	}

	/**
	  Sets the platform list being shown.
	  @param platList the platform list.
	*/
	public void setPlatList(PlatList platList)
	{
		this.platList = platList;
//System.out.println("Editing PlatList with " + this.platList.getEntries().size()
//+ " entries.");
		model.fireTableDataChanged();
	}

	void importPressed() 
	{
		int n = platListTable.getSelectedRowCount();
		if (n == 0)
			return;

		String decHomePath = EnvExpander.expand("$DECODES_INSTALL_DIR");
		DistrictDBSnap snapshot = platList.myDB;
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
			+ snapshot.getDirName() + "/platform";
		int rows[] = platListTable.getSelectedRows();
		String files[] = new String[n];
		for(int i=0; i<n; i++)
		{
			PlatListEntry ple = model.getPlatListEntryAt(rows[i]);
			files[i] = ple.makeFileName();
		}
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

	void downloadPressed() 
	{
		int n = platListTable.getSelectedRowCount();
		if (n == 0)
			return;

		String decHomePath = EnvExpander.expand("$DECODES_INSTALL_DIR");
		DistrictDBSnap snapshot = platList.myDB;
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
				+ snapshot.getDirName() + "/platform";
			int rows[] = platListTable.getSelectedRows();
			String files[] = new String[n];
			for(int i=0; i<n; i++)
			{
				PlatListEntry ple = model.getPlatListEntryAt(rows[i]);
				files[i] = ple.makeFileName();
			}
			MultFileDownloadDialog dlg = new MultFileDownloadDialog(
				SyncGuiFrame.instance(), "Downloading XML Files", true);
			dlg.downloadFiles(urldir, files, localdir);
		}
	}
	
	private void viewPressed()
	{
		int row = platListTable.getSelectedRow();
		if (row == -1)
			return;

		String tmpDirPath = 
			EnvExpander.expand("$DECODES_INSTALL_DIR/tmp");
		File tmpDir = new File(tmpDirPath);
		if (!tmpDir.isDirectory())
			if (!tmpDir.mkdirs())
			{
				SyncGuiFrame.instance().showError(
					"Cannot make temp directory '" + tmpDirPath	+ "'");
				return;
			}
		
		PlatListEntry ple = model.getPlatListEntryAt(row);
		String fn = ple.makeFileName();
		DistrictDBSnap snapshot = platList.myDB;
		String urlstr = SyncConfig.instance().getHubHome() + "/"
			+ snapshot.getDistrict().getName() + "/"
			+ snapshot.getDirName() + "/platform/" + fn;
		File localFile = new File(tmpDir, fn);
		
		try
		{
			URL url = new URL(urlstr);
			FileUtil.copyStream(url.openStream(),
				new FileOutputStream(localFile));
		}
		catch(IOException ex)
		{
			SyncGuiFrame.instance().showError(
				"Cannot download '" + urlstr	+ "': " + ex);
			return;
		}

		if (showFileDialog == null)
			showFileDialog = new ShowFileDialog(
				SyncGuiFrame.instance(), "Platform at " + ple.siteNameValue, true);
		else
			showFileDialog.setTitle("Platform at " + ple.siteNameValue);
		showFileDialog.setFile(localFile);
		showFileDialog.setVisible(true);
	}
}


class PlatListTableModel extends AbstractTableModel
	implements SortingListTableModel
{
	private static String columnNames[] =
	{ "Site", "Agency", "Transport-ID", "Config", "Expiration", "Description" };
	private PlatListPanel panel;
	private int sortColumn = -1;

	public PlatListTableModel(PlatListPanel panel)
	{
		super();
		this.panel = panel;
	}

	/** @return constant # of columns */
	public int getColumnCount() 
	{
		return columnNames.length; 
	}

	/** @return constant column name */
	public String getColumnName(int col)
	{
		return columnNames[col];
	}

	/** @return false -- none of the cells are editable */
	public boolean isCellEditable(int r, int c) { return false; }

	/** @return number of platforms in this table */
	public int getRowCount()
	{
		return panel.platList == null ? 0 : panel.platList.getEntries().size();
	}

	/** @return the entry at the given row. */
	PlatListEntry getPlatListEntryAt(int r)
	{
		return (PlatListEntry)getRowObject(r);
	}

	/** @return object at given row. */
	public Object getRowObject(int r)
	{
		return panel.platList.getEntries().elementAt(r);
	}

	/** 
	  Return column object at given row/column.
	  @param r the row
	  @param c the column
	  @return column object at given row/column 
	*/
	public Object getValueAt(int r, int c)
	{
		return PlatListEntryColumnizer.getColumn(getPlatListEntryAt(r), c);
	}

	/**
	  Sorts the table by the given column.
	  @param c the column to sort by
	*/
	public void sortByColumn(int c)
	{
		if (panel.platList == null)
			return;
		sortColumn = c;
		Collections.sort(panel.platList.getEntries(), new ColumnComparator(c));
	}
}

/**
 Helper class to retrieve platform fields by column number. Used for
 displaying values in the table and for sorting.
*/
class PlatListEntryColumnizer
{
	static String getColumn(PlatListEntry p, int c)
	{
		switch(c)
		{
		case 0: // Site
			return p.siteNameValue;
		case 1: // Agency
			return p.agency;
		case 2: // Transport-ID
			return p.getDefaultMedium();
		case 3: // Config
			return p.configName;
		case 4: // Expiration
			return p.expiration;
		case 5: // Description
			return p.desc;
		default:
			return "";
		}
	}
}

class ColumnComparator implements Comparator
{
	int col;

	ColumnComparator(int col)
	{
		this.col = col;
	}

	public int compare(Object ob1, Object ob2)
	{
		if (ob1 == ob2)
			return 0;
		PlatListEntry p1 = (PlatListEntry)ob1;
		PlatListEntry p2 = (PlatListEntry)ob2;
		return PlatListEntryColumnizer.getColumn(p1, col).compareToIgnoreCase(
			PlatListEntryColumnizer.getColumn(p2, col));
	}
}



