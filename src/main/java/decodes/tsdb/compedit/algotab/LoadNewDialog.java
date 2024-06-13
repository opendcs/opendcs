package decodes.tsdb.compedit.algotab;

import javax.swing.JDialog;

import decodes.tsdb.DbCompAlgorithm;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesDb;
import ilex.util.Pair;

import javax.swing.JButton;
import java.awt.BorderLayout;
import java.awt.Frame;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;

import org.python.icu.impl.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SpringLayout;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.util.ResourceBundle;

import javax.swing.BoxLayout;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.JSplitPane;
import javax.swing.ScrollPaneConstants;

public class LoadNewDialog extends JDialog
{
    private static final Logger log = LoggerFactory.getLogger(LoadNewDialog.class);
    final TimeSeriesDb tsDb;
    final ExecClassTableModel execClasses;
    private JTable table;
   

    public LoadNewDialog(Frame parent, TimeSeriesDb tsDb)
    {
        super(parent);
        setAlwaysOnTop(true);
        setModal(true);
        this.setMinimumSize(new Dimension(800,480));
        this.tsDb = tsDb;
        execClasses = new ExecClassTableModel(tsDb);
        new SwingWorker<Void,Void>()
        {

			@Override
			protected Void doInBackground() throws Exception {
				try
		        {
		            execClasses.load();
		        }
		        catch (NoSuchObjectException ex)
		        {
		            log.atError()
		               .setCause(ex)
		               .log("Unable load available algorithms.");
		        }
				return null;
			}
        	
        }.execute();
        
        pack();
        SpringLayout springLayout = new SpringLayout();
        getContentPane().setLayout(springLayout);
        
        JPanel panel = new JPanel();
        springLayout.putConstraint(SpringLayout.NORTH, panel, 0, SpringLayout.NORTH, getContentPane());
        springLayout.putConstraint(SpringLayout.WEST, panel, 0, SpringLayout.WEST, getContentPane());
        springLayout.putConstraint(SpringLayout.SOUTH, panel, 0, SpringLayout.SOUTH, getContentPane());
        getContentPane().add(panel);
        
        JPanel panel_1 = new JPanel();
        springLayout.putConstraint(SpringLayout.EAST, panel, 0, SpringLayout.WEST, panel_1);
        panel.setLayout(new BorderLayout(0, 0));
        
        table = new JTable(execClasses);
        final TableRowSorter<ExecClassTableModel> tsr = new TableRowSorter<>(execClasses);
        final RowFilter<ExecClassTableModel,Integer> filter = new RowFilter<ExecClassTableModel,Integer>()
        {
            @Override
            public boolean include(Entry<? extends ExecClassTableModel, ? extends Integer> entry)
            {
                return entry.getModel().getLoaded(entry.getIdentifier()) != true;
            }
        };
        tsr.setRowFilter(filter);
        table.setRowSorter(tsr);
        TableColumnModel tcm = table.getColumnModel();
        tcm.removeColumn(tcm.getColumn(0));
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setViewportView(table);
        panel.add(scrollPane, BorderLayout.CENTER);
        springLayout.putConstraint(SpringLayout.NORTH, panel_1, 0, SpringLayout.NORTH, getContentPane());
        springLayout.putConstraint(SpringLayout.SOUTH, panel_1, 0, SpringLayout.SOUTH, getContentPane());
        springLayout.putConstraint(SpringLayout.WEST, panel_1, -134, SpringLayout.EAST, getContentPane());
        springLayout.putConstraint(SpringLayout.EAST, panel_1, 5, SpringLayout.EAST, getContentPane());
        getContentPane().add(panel_1);
        panel_1.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        
        JButton loadSelectedButton = new JButton(ResourceBundle.getBundle("decodes/resources/algoedit").getString("CheckNewAlgoDialog.loadSelected"));
        panel_1.add(loadSelectedButton);
        
        JButton cancelButton = new JButton(ResourceBundle.getBundle("decodes/resources/generic").getString("cancel"));
        panel_1.add(cancelButton);
        cancelButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
        this.setTitle(ResourceBundle.getBundle("decodes/resources/algoedit").getString("CheckNewAlgoDialog.title"));
    }
}
