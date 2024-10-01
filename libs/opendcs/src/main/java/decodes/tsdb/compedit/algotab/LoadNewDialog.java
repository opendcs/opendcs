package decodes.tsdb.compedit.algotab;

import javax.swing.JDialog;

import decodes.tsdb.DbCompAlgorithm;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesDb;
import ilex.util.Pair;
import opendcs.dai.AlgorithmDAI;

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
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;

import javax.swing.BoxLayout;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.JSplitPane;
import javax.swing.ScrollPaneConstants;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class LoadNewDialog extends JDialog
{
    private static final Logger log = LoggerFactory.getLogger(LoadNewDialog.class);
    final TimeSeriesDb tsDb;
    final ExecClassTableModel execClasses;
    private JTable table;
    private boolean dataImported = false;


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

        JPanel panel_1 = new JPanel();
        springLayout.putConstraint(SpringLayout.EAST, panel, 0, SpringLayout.WEST, panel_1);
        springLayout.putConstraint(SpringLayout.EAST, panel_1, 0, SpringLayout.EAST, getContentPane());
        springLayout.putConstraint(SpringLayout.NORTH, panel_1, 0, SpringLayout.NORTH, getContentPane());
        springLayout.putConstraint(SpringLayout.WEST, panel_1, -134, SpringLayout.EAST, getContentPane());
        springLayout.putConstraint(SpringLayout.SOUTH, panel_1, 0, SpringLayout.SOUTH, getContentPane());
        getContentPane().add(panel_1);
        GridBagLayout gbl_panel_1 = new GridBagLayout();
        gbl_panel_1.columnWidths = new int[]{112, 0};
        gbl_panel_1.rowHeights = new int[]{25, 25, 0};
        gbl_panel_1.columnWeights = new double[]{1.0, Double.MIN_VALUE};
        gbl_panel_1.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
        panel_1.setLayout(gbl_panel_1);

        JButton loadSelectedButton = new JButton(ResourceBundle.getBundle("decodes/resources/algoedit").getString("CheckNewAlgoDialog.loadSelected"));
        loadSelectedButton.addActionListener(e -> importSelected() );
        GridBagConstraints gbc_loadSelectedButton = new GridBagConstraints();
        gbc_loadSelectedButton.fill = GridBagConstraints.HORIZONTAL;
        gbc_loadSelectedButton.anchor = GridBagConstraints.NORTH;
        gbc_loadSelectedButton.insets = new Insets(0, 5, 5, 5);
        gbc_loadSelectedButton.gridx = 0;
        gbc_loadSelectedButton.gridy = 0;
        panel_1.add(loadSelectedButton, gbc_loadSelectedButton);

        JButton cancelButton = new JButton(ResourceBundle.getBundle("decodes/resources/generic").getString("close"));
        cancelButton.addActionListener(e -> cancel());

        GridBagConstraints gbc_cancelButton = new GridBagConstraints();
        gbc_cancelButton.insets = new Insets(0, 5, 0, 5);
        gbc_cancelButton.fill = GridBagConstraints.HORIZONTAL;
        gbc_cancelButton.anchor = GridBagConstraints.NORTH;
        gbc_cancelButton.gridx = 0;
        gbc_cancelButton.gridy = 1;
        panel_1.add(cancelButton, gbc_cancelButton);
        cancelButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
        this.setTitle(ResourceBundle.getBundle("decodes/resources/algoedit").getString("CheckNewAlgoDialog.title"));
    }

    void importSelected()
    {
        new SwingWorker<List<DbCompAlgorithm>,DbCompAlgorithm>()
        {

            @Override
            protected List<DbCompAlgorithm> doInBackground() throws Exception
            {
                int algos[] = table.getSelectedRows();
                List<DbCompAlgorithm> imported = new ArrayList<>();
                try (AlgorithmDAI dao = tsDb.makeAlgorithmDAO())
                {
                    for (int i = 0; i < algos.length; i++)
                    {
                        int modelRow = table.convertRowIndexToModel(algos[i]);
                        final DbCompAlgorithm toImport = execClasses.getAlgoAt(modelRow);
                        try
                        {
                            dao.writeAlgorithm(toImport);
                            imported.add(toImport);
                        }
                        catch (DbIoException ex)
                        {
                            log.atWarn()
                               .setCause(ex)
                               .log("Unable to import algorithm '{}'', exec class {}", toImport.getName(), toImport.getExecClass());
                        }
                    }
                }
                return imported;
            }
            @Override
            public void done()
            {
                try
                {
                    List<DbCompAlgorithm> imported = this.get();
                    for (DbCompAlgorithm algo: imported)
                    {
                        execClasses.removeAlgo(algo);
                        LoadNewDialog.this.dataImported = true;
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    log.atWarn()
                       .setCause(ex)
                       .log("failed to get list of imported comps after task is done.");
                }
            }
        }.execute();

    }

    void cancel()
    {
        this.setVisible(false);
    }

    public boolean importNew()
    {
        this.dataImported = false;
        this.setVisible(true);
        log.info("returning");
        return this.dataImported;
    }


}
