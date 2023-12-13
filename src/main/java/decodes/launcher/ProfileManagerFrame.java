package decodes.launcher;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.TimeZone;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.opendcs.gui.tables.DateRenderer;

import decodes.gui.SortingListTable;
import decodes.gui.SortingListTableModel;
import decodes.gui.TopFrame;
import decodes.util.DecodesSettings;
import ilex.util.EnvExpander;
import ilex.util.FileUtil;
import ilex.util.LoadResourceBundle;
import ilex.util.Logger;

/**
 * This GUI allows the user to create & delete alternative "profiles" which are
 * Decodes Configurations.
 */
@SuppressWarnings("serial")
public class ProfileManagerFrame extends TopFrame
{
    private static final Logger logger = Logger.instance();
    static ResourceBundle genericLabels = null;
    static ResourceBundle launcherLabels = null;
    private JTable profileTable = null;
    private ProfileTableModel profileModel = null;
    private static final File searchDir = new File(EnvExpander.expand("$DCSTOOL_USERDIR"));

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

        profileModel = new ProfileTableModel(Profile.getProfiles(searchDir));
        profileTable = new JTable(profileModel)
        {
            @Override
            public String getToolTipText(MouseEvent e)
            {
                String tip = null;
                java.awt.Point point = e.getPoint();
                int rowIndex = profileTable.rowAtPoint(point);
                int modelRow = profileTable.convertRowIndexToModel(rowIndex);
                Profile profile = profileModel.getProfile(modelRow);
                if (profile != null)
                {
                    tip = profile.getFile().getAbsolutePath();
                }
                return tip;
            }
        };
        profileTable.setRowSorter(new TableRowSorter<TableModel>(profileModel));
        profileTable.setDefaultRenderer(Date.class, new DateRenderer(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")));
        profileTable.setName("profileTable");

        JScrollPane scrollPane = new JScrollPane(profileTable,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridBagLayout());
        JButton copyButton = new JButton(genericLabels.getString("copy"));
        copyButton.setName("copyProfile");
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
        deleteButton.setName("deleteProfile");
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
        reloadButton.setName("reloadProfiles");
        reloadButton.addActionListener(e -> profileModel.updateProfiles(Profile.getProfiles(searchDir)));

        buttonPanel.add(reloadButton,
            new GridBagConstraints(0, 2, 1, 1, 0, .5,
                GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL, new Insets(5, 2, 2, 2), 0, 0));

        mainPanel.add(buttonPanel, BorderLayout.EAST);
    }

    protected void deletePressed()
    {
        int idx = profileTable.getSelectedRow();
        if (idx < 0)
        {
            showError("Select row, then press delete.");
            return;
        }
        int modelRow = profileTable.convertRowIndexToModel(idx);

        String profileName = (String)profileModel.getValueAt(modelRow, 0);
        if (modelRow == 0)
        {
            showError("Cannot delete the default profile!");
            return;
        }

        Profile profile = profileModel.getProfile(modelRow);
        int choice = showConfirm(genericLabels.getString("confirm"),
            LoadResourceBundle.sprintf(genericLabels.getString("confirmDelete"),
                "Profile '" + profileName + "'"),
            JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION)
        {
            return;
        }

        logger.warning("Deleting profile '" + profile.getFile().getAbsolutePath() + "'");
        if (!profileModel.removeProfile(profile))
        {
            showError("Failed to delete profile '" + profile.getFile().getAbsolutePath() + "' -- check permissions on this file.");
            return;
        }
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
        int modelRow = profileTable.convertRowIndexToModel(idx);

        final Profile profile2copy = profileModel.getProfile(modelRow);
        final File file2copy = profile2copy.getFile();

        // Get the file to copy TO.
        String newName = JOptionPane.showInputDialog(this, "Enter name for copy:");
        if (newName == null || newName.trim().length() == 0)
            return;

        if(profileModel.profileExists(newName))
        {
            showError("A profile with that name already exists!");
            return;
        }
        File newFile = new File(file2copy.getParentFile(),newName + ".profile");

        try
        {
            FileUtil.copyFile(file2copy, newFile);
            final Profile profile = Profile.getProfile(newFile);
            profileModel.addProfile(profile);
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
}

@SuppressWarnings("serial")
class ProfileTableModel extends AbstractTableModel
{
    String[] colnames = null;
    int [] widths = { 50, 50 };
    private List<Profile> profiles;
    private SimpleDateFormat lmtFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public ProfileTableModel(List<Profile> profiles)
    {
        this.profiles = profiles;
        colnames = new String[]
        {
            ProfileManagerFrame.launcherLabels.getString("ProfMgr.profile"),
            ProfileManagerFrame.genericLabels.getString("lastMod")
        };
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

    @Override
    public Class<?> getColumnClass(int c)
    {
        return c == 0 ? String.class : Date.class;
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

        Profile profile = profiles.get(row);
        return col == 0 ? profile.toString() : new Date(profile.getFile().lastModified());
    }

    public void addProfile(Profile p)
    {
        profiles.add(p);
        fireTableRowsInserted(profiles.size()-1,profiles.size()-1);
    }

    public boolean removeProfile(Profile p)
    {
        int idx = profiles.indexOf(p);
        if (profiles.remove(p))
        {
            fireTableRowsDeleted(idx,idx);
            return p.getFile().delete();
        }
        else
        {
            return false;
        }
    }

    public Profile getProfile(int modelRow)
    {
        return profiles.get(modelRow);
    }

    public void updateProfiles(List<Profile> profiles)
    {
        this.profiles = profiles;
        fireTableDataChanged();
    }

    /**
     * Case insensitive search of the current profiles.
     * @param name
     * @return
     */
    public boolean profileExists(String name)
    {
        return profiles.stream()
                       .anyMatch(p -> p.getName().equalsIgnoreCase(name));
    }
}
