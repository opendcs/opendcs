package decodes.tsdb.groupedit;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * Dialog for selecting which time series to import from multiple files.
 * Shows the source file for each TSID to help differentiate duplicates.
 */
public class TsImportMultiFileSelectionDialog extends JDialog
{
    private JTable tsTable;
    private DefaultTableModel tableModel;
    private JButton selectAllButton;
    private JButton selectNoneButton;
    private JButton importButton;
    private JButton cancelButton;
    private boolean cancelled = true;

    // Map of filepath to list of selected TSIDs
    private Map<String, List<String>> selectedTsIdsByFile = new HashMap<>();

    public TsImportMultiFileSelectionDialog(JFrame parent, Map<String, List<String>> tsIdsByFile)
    {
        super(parent, "Select Time Series to Import", true);
        initComponents(tsIdsByFile);
        setLocationRelativeTo(parent);
    }

    private void initComponents(Map<String, List<String>> tsIdsByFile)
    {
        setLayout(new BorderLayout());

        // Count total entries
        int totalEntries = 0;
        for (List<String> entries : tsIdsByFile.values())
        {
            totalEntries += entries.size();
        }

        // Create table model with checkbox, TSID, and File columns
        String[] columnNames = {"Import", "Time Series ID", "Source File"};
        Object[][] data = new Object[totalEntries][3];

        int row = 0;
        for (Map.Entry<String, List<String>> fileEntry : tsIdsByFile.entrySet())
        {
            String filePath = fileEntry.getKey();
            String fileName = new File(filePath).getName();
            for (String tsId : fileEntry.getValue())
            {
                data[row][0] = Boolean.TRUE; // Default to selected
                data[row][1] = tsId;
                data[row][2] = fileName;
                row++;
            }
        }

        tableModel = new DefaultTableModel(data, columnNames)
        {
            @Override
            public Class<?> getColumnClass(int columnIndex)
            {
                if (columnIndex == 0)
                    return Boolean.class;
                return String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column)
            {
                return column == 0; // Only checkbox column is editable
            }
        };

        tsTable = new JTable(tableModel);
        tsTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        tsTable.getColumnModel().getColumn(0).setMaxWidth(60);
        tsTable.getColumnModel().getColumn(1).setPreferredWidth(350);
        tsTable.getColumnModel().getColumn(2).setPreferredWidth(200);

        JScrollPane scrollPane = new JScrollPane(tsTable);
        scrollPane.setPreferredSize(new Dimension(700, 400));
        add(scrollPane, BorderLayout.CENTER);

        // Create top panel with selection buttons
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        int fileCount = tsIdsByFile.size();
        topPanel.add(new JLabel("Found " + totalEntries + " time series in " + fileCount + " file(s):"));
        selectAllButton = new JButton("Select All");
        selectNoneButton = new JButton("Select None");

        selectAllButton.addActionListener(e -> selectAll(true));
        selectNoneButton.addActionListener(e -> selectAll(false));

        topPanel.add(selectAllButton);
        topPanel.add(selectNoneButton);
        add(topPanel, BorderLayout.NORTH);

        // Create bottom panel with action buttons
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        importButton = new JButton("Import Selected");
        cancelButton = new JButton("Cancel");

        importButton.addActionListener(this::importPressed);
        cancelButton.addActionListener(e -> {
            cancelled = true;
            setVisible(false);
        });

        bottomPanel.add(importButton);
        bottomPanel.add(cancelButton);
        add(bottomPanel, BorderLayout.SOUTH);

        pack();
    }

    private void selectAll(boolean select)
    {
        for (int i = 0; i < tableModel.getRowCount(); i++)
        {
            tableModel.setValueAt(select, i, 0);
        }
    }

    private void importPressed(ActionEvent e)
    {
        selectedTsIdsByFile.clear();

        // Need to track both filePath and fileName to properly map back
        Map<String, String> fileNameToPath = new HashMap<>();

        // First build a mapping of fileName to filePath
        for (int i = 0; i < tableModel.getRowCount(); i++)
        {
            String fileName = (String) tableModel.getValueAt(i, 2);
            // Find the full path for this fileName from our original data
            // This is a bit inefficient but necessary since we only stored the fileName in the table
            fileNameToPath.put(fileName, fileName); // Will be replaced in TsListPanel
        }

        // Build map of filePath -> list of TSIDs
        for (int i = 0; i < tableModel.getRowCount(); i++)
        {
            Boolean selected = (Boolean) tableModel.getValueAt(i, 0);
            if (selected != null && selected)
            {
                String tsId = (String) tableModel.getValueAt(i, 1);
                String fileName = (String) tableModel.getValueAt(i, 2);

                // For now, we'll use fileName as the key
                // The calling code will need to map this back to full paths
                selectedTsIdsByFile.computeIfAbsent(fileName, k -> new ArrayList<>()).add(tsId);
            }
        }

        if (selectedTsIdsByFile.isEmpty())
        {
            JOptionPane.showMessageDialog(this,
                "Please select at least one time series to import.",
                "No Selection",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        cancelled = false;
        setVisible(false);
    }

    public boolean isCancelled()
    {
        return cancelled;
    }

    /**
     * Returns a map of fileName -> list of selected TSIDs from that file
     * Note: Returns fileName as key, not full path. Caller must map back to full paths.
     */
    public Map<String, List<String>> getSelectedTsIdsByFile()
    {
        return selectedTsIdsByFile;
    }
}