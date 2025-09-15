package decodes.tsdb.groupedit;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Dialog for selecting which time series to import from a file.
 */
public class TsImportSelectionDialog extends JDialog
{
    private JTable tsTable;
    private DefaultTableModel tableModel;
    private JButton selectAllButton;
    private JButton selectNoneButton;
    private JButton importButton;
    private JButton cancelButton;
    private boolean cancelled = true;
    private List<String> selectedTsIds = new ArrayList<>();

    public TsImportSelectionDialog(JFrame parent, Collection<String> tsIds)
    {
        super(parent, "Select Time Series to Import", true);
        initComponents(tsIds);
        setLocationRelativeTo(parent);
    }

    private void initComponents(Collection<String> tsIds)
    {
        setLayout(new BorderLayout());

        // Create table model with checkbox column and TSID column
        String[] columnNames = {"Import", "Time Series ID"};
        Object[][] data = new Object[tsIds.size()][2];
        int i = 0;
        for (String tsId : tsIds)
        {
            data[i][0] = Boolean.TRUE; // Default to selected
            data[i][1] = tsId;
            i++;
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
        tsTable.getColumnModel().getColumn(1).setPreferredWidth(400);

        JScrollPane scrollPane = new JScrollPane(tsTable);
        scrollPane.setPreferredSize(new Dimension(600, 400));
        add(scrollPane, BorderLayout.CENTER);

        // Create top panel with selection buttons
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Found " + tsIds.size() + " time series in file:"));
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
        selectedTsIds.clear();
        for (int i = 0; i < tableModel.getRowCount(); i++)
        {
            Boolean selected = (Boolean) tableModel.getValueAt(i, 0);
            if (selected != null && selected)
            {
                String tsId = (String) tableModel.getValueAt(i, 1);
                selectedTsIds.add(tsId);
            }
        }

        if (selectedTsIds.isEmpty())
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

    public List<String> getSelectedTsIds()
    {
        return selectedTsIds;
    }
}