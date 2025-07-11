package decodes.tsdb.comprungui;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

public class TimeSeriesTablePanel extends JPanel
{
    private final TimeSeriesTableModel tableModel;
    private final TimeSeriesTable timeSeriesTable;
    private final JTable timesTable;
    private final JScrollPane scrollPane;


    public TimeSeriesTablePanel()
    {
        setLayout(new BorderLayout());
        tableModel = new TimeSeriesTableModel(null);
        timeSeriesTable = new TimeSeriesTable(tableModel, null);
        timesTable = new JTable();
        scrollPane = new JScrollPane();
        scrollPane.setPreferredSize(new Dimension(600, 400));

        timeSeriesTable.getSelectionModel().addListSelectionListener(new SelectionUpdater(timesTable));
        timesTable.getSelectionModel().addListSelectionListener(new SelectionUpdater(timeSeriesTable));

        timeSeriesTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		//scrollPane.add(timeSeriesTable);
		scrollPane.setViewportView(timeSeriesTable);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		
		timesTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        timesTable.setAutoCreateColumnsFromModel(false);
        AbstractTableModel firstColumnModel = new AbstractTableModel()
        {

            @Override
            public int getRowCount()
            {
                return tableModel.rows;
            }

            @Override
            public int getColumnCount() 
            {
                return 1;
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex)
            {
                return tableModel.getValueAt(rowIndex, columnIndex);
            }
            
        };
		timesTable.setModel(firstColumnModel);
		timesTable.setRowSorter(timeSeriesTable.getRowSorter());
		timesTable.setPreferredScrollableViewportSize(new Dimension(120, 0));
		TableColumnModel timesModel = timesTable.getColumnModel();
        TableColumn tc = new TableColumn(0);
        tc.setMinWidth(120);
        tc.setMaxWidth(120);
        timesModel.addColumn(tc);
        
		scrollPane.setRowHeaderView(timesTable);
        System.out.println("***" +CompRunGuiFrame.dateTimeColumnLabel+"***");
		scrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, new JLabel(CompRunGuiFrame.dateTimeColumnLabel));
        add(scrollPane, BorderLayout.CENTER);
    }

    public TimeSeriesTableModel getTimeSeriesModel()
    {
        return tableModel;
    }
    

    public static class SelectionUpdater implements ListSelectionListener
    {
        public final JTable other;

        public SelectionUpdater(JTable other)
        {
            this.other = other;
        }

        @Override
        public void valueChanged(ListSelectionEvent e)
        {
            if (e.getValueIsAdjusting())
            {
                return;
            }

            ListSelectionModel lsm = (ListSelectionModel)e.getSource();
            ListSelectionModel otherLsm = other.getSelectionModel();
            int[] selection = lsm.getSelectedIndices();
            
            int[] other = otherLsm.getSelectedIndices();
            boolean reset = false;
            for (int i = 0; i < selection.length; i++)
            {
                if (i >= other.length || other[i] != selection[i])
                {
                    
                    reset = true;
                    break;
                }
            }

            if (reset)
            {
                otherLsm.clearSelection();
                for(int i: selection)
                {
                    otherLsm.addSelectionInterval(i, i);
                }
            }
        }
        
    }
}
