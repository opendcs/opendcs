package decodes.launcher;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.TimeZone;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.AbstractTableModel;

import decodes.gui.SortingListTable;
import decodes.gui.SortingListTableModel;
import decodes.gui.TopFrame;
import decodes.util.DecodesSettings;
import ilex.util.EnvExpander;
import ilex.util.FileUtil;
import ilex.util.LoadResourceBundle;
import ilex.util.Logger;
import ilex.util.StringPair;
import ilex.util.TextUtil;

/**
 * This GUI allows the user to create & delete alternative "profiles" which are
 * Decodes Configurations.
 */
@SuppressWarnings("serial")
public class ProfileManagerFrame 
	extends TopFrame
{
	static ResourceBundle genericLabels = null;
	static ResourceBundle launcherLabels = null;
	private SortingListTable profileTable = null;
	private ProfileTableModel profileModel = null;


	public ProfileManagerFrame()
	{
		DecodesSettings settings = DecodesSettings.instance();
		genericLabels = LoadResourceBundle.getLabelDescriptions(
			"decodes/resources/generic", settings.language);
		launcherLabels = LoadResourceBundle.getLabelDescriptions(
			"decodes/resources/launcherframe", settings.language);
		
		guiInit();
		pack();
		this.trackChanges("ProfileManager");

	}
	
	private void guiInit()
	{
		this.setTitle(launcherLabels.getString("ProfMgr.title"));
		JPanel mainPanel = (JPanel) this.getContentPane();
		mainPanel.setLayout(new BorderLayout());
		
		profileModel = new ProfileTableModel(this);
		profileTable = new SortingListTable(profileModel, profileModel.widths);
		
		JScrollPane scrollPane = new JScrollPane(profileTable, 
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		mainPanel.add(scrollPane, BorderLayout.CENTER);
		
		JPanel buttonPanel = new JPanel(new GridBagLayout());
		JButton copyButton = new JButton(genericLabels.getString("copy"));
		copyButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					copyPressed();
				}
			});
		buttonPanel.add(copyButton,
			new GridBagConstraints(0, 0, 1, 1, 0, 0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 2, 2, 2), 0, 0));

		JButton deleteButton = new JButton(genericLabels.getString("delete"));
		deleteButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					deletePressed();
				}
			});
		buttonPanel.add(deleteButton,
			new GridBagConstraints(0, 1, 1, 1, 0, .5,
				GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 5, 2), 0, 0));

		JButton reloadButton = new JButton(genericLabels.getString("reload"));
		reloadButton.addActionListener(
			new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					reloadPressed();
				}
			});
		buttonPanel.add(reloadButton,
			new GridBagConstraints(0, 2, 1, 1, 0, .5,
				GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL, new Insets(5, 2, 2, 2), 0, 0));

		mainPanel.add(buttonPanel, BorderLayout.EAST);
	}
	
	protected void reloadPressed()
	{
		load();
	}

	protected void deletePressed()
	{
		int idx = profileTable.getSelectedRow();
		if (idx < 0)
		{
			showError("Select row, then press delete.");
			return;
		}
		
		String profileName = (String)profileModel.getValueAt(idx, 0);
		if (profileName.equalsIgnoreCase("(default)"))
		{
			showError("Cannot delete the default profile!");
			return;
		}
	
		int choice = showConfirm(genericLabels.getString("confirm"),
			LoadResourceBundle.sprintf(genericLabels.getString("confirmDelete"), 
				"Profile '" + profileName + "'"), 
			JOptionPane.YES_NO_OPTION);
		if (choice != JOptionPane.YES_OPTION)
			return;

		File f = new File(EnvExpander.expand("$DCSTOOL_USERDIR/" + profileName + ".profile"));
		if (!f.delete())
		{
			showError("Failed to delete profile '" + f.getPath() + "' -- check permissions on this file.");
			return;
		}
		else
			load();
	}

	protected void copyPressed()
	{
		// Get the file to copy FROM.
		int idx = profileTable.getSelectedRow();
		if (idx < 0)
		{
			showError("Select row, then press delete.");
			return;
		}
		String profile2copy = (String)profileModel.getValueAt(idx, 0);
		File file2copy = profile2copy.equalsIgnoreCase("(default)") 
			? DecodesSettings.instance().getSourceFile()
			: new File(EnvExpander.expand("$DCSTOOL_USERDIR/" + profile2copy + ".profile"));
		
		// Get the file to copy TO.
		String newName = JOptionPane.showInputDialog(this, "Enter name for copy:");
		if (newName == null || newName.trim().length() == 0)
			return;
		for(int row = 0; row < profileTable.getRowCount(); row++)
			if (newName.equalsIgnoreCase((String)profileModel.getValueAt(row, 0)))
			{
				showError("A profile with that name already exists!");
				return;
			}
		File newFile = new File(EnvExpander.expand("$DCSTOOL_USERDIR/" + newName + ".profile"));

		try
		{
			FileUtil.copyFile(file2copy, newFile);
			load();
		}
		catch (IOException ex)
		{
			String msg = "Cannot copy '" + file2copy.getPath() + "' to '" + newFile.getPath()
				+ "': " + ex;
			Logger.instance().warning(msg);
			showError(msg);
			newFile.delete();
		}
	}

	public void load()
	{
		profileModel.load();
	}
}

@SuppressWarnings("serial")
class ProfileTableModel extends AbstractTableModel
	implements SortingListTableModel
{
	String[] colnames = null;
	int [] widths = { 50, 50 };
	private int sortColumn = 0;
	private ArrayList<StringPair> profiles = new ArrayList<StringPair>();
	private ProfileManagerFrame frame = null;
	private SimpleDateFormat lmtFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	
	public ProfileTableModel(ProfileManagerFrame frame)
	{
		this.frame = frame;
		colnames = new String[] { 
			ProfileManagerFrame.launcherLabels.getString("ProfMgr.profile"), 
			ProfileManagerFrame.genericLabels.getString("lastMod") };
		lmtFormat.setTimeZone(TimeZone.getTimeZone(DecodesSettings.instance().guiTimeZone));
	}
	
	@Override
	public int getColumnCount()
	{
		return colnames.length;
	}
	
	public String getColumnName(int col)
	{
		return colnames[col];
	}

	public boolean isCellEditable(int row, int col)
	{
		return false;
	}
	
	@Override
	public int getRowCount()
	{
		return profiles.size();
	}

	@Override
	public synchronized Object getValueAt(int row, int col)
	{
		if (row < 0 || row >= profiles.size()
		 || col < 0 || col >= 2)
			return "";
		
		StringPair sp = profiles.get(row);
		return col == 0 ? sp.first : sp.second;
	}

	@Override
	public synchronized void sortByColumn(int col)
	{
		if (col < 0 || col >= 2)
			return;
		
		this.sortColumn = col;
		
		Collections.sort(profiles, 
			new Comparator<StringPair>()
			{
				@Override
				public int compare(StringPair o1, StringPair o2)
				{
					if (sortColumn == 0)
					{
						if (TextUtil.strEqualIgnoreCase(o1.first, "(default)"))
							return -1;
						else if (TextUtil.strEqualIgnoreCase(o2.first, "(default)"))
							return 1;
						int r = TextUtil.strCompareIgnoreCase(o1.first, o2.first);
						if (r != 0)
							return r;
						return TextUtil.strCompareIgnoreCase(o1.second, o2.second);
					}
					else
					{
						int r = TextUtil.strCompareIgnoreCase(o1.second, o2.second);
						if (r != 0)
							return r;
						return TextUtil.strCompareIgnoreCase(o1.first, o2.first);
					}
				}
			
			});
		fireTableDataChanged();
	}

	@Override
	public Object getRowObject(int row)
	{
		return profiles.get(row);
	}
	
	public void load()
	{
		File userDir = new File(EnvExpander.expand("$DCSTOOL_USERDIR"));
		File[] files = userDir.listFiles(
			new FilenameFilter()
			{
				@Override
				public boolean accept(File dir, String name)
				{
					return name.toLowerCase().endsWith(".profile");
				}
			});
		profiles.clear();
		for(File file : files)
		{
			String name = file.getName();
			int idx = name.lastIndexOf('.');
			if (idx > 0)
				name = name.substring(0, idx);
			profiles.add(new StringPair(name, lmtFormat.format(new Date(file.lastModified()))));
		}
		profiles.add(new StringPair("(default)", 
			lmtFormat.format(DecodesSettings.instance().getLastModified())));
		sortByColumn(sortColumn);
	}
}