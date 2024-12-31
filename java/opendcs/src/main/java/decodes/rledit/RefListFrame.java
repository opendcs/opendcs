/*
*  $Id$
*/
package decodes.rledit;

import java.awt.*;
import java.awt.event.*;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.*;
import javax.swing.border.*;

import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import ilex.util.*;
import opendcs.dai.EnumDAI;
import decodes.db.*;
import decodes.dbeditor.TraceDialog;
import decodes.dbeditor.routing.RSListTableModel;
import decodes.decoder.Season;
import decodes.gui.SortingListTable;
import decodes.rledit.panels.EnumerationPanel;

/**
RefListFrame is the GUI application for Reference List Editor.
This program allows you to edit DECODES enumerations, engineering
units, EU conversions and data type equivalencies.
*/
@SuppressWarnings("serial")
public class RefListFrame extends JFrame
{
    private static final Logger log = LoggerFactory.getLogger(RefListFrame.class);
    private static ResourceBundle genericLabels = RefListEditor.getGenericLabels();
    private static ResourceBundle labels = RefListEditor.getLabels();
    private JPanel contentPane;
    private JMenuBar jMenuBar1 = new JMenuBar();
    private JMenu jMenuFile = new JMenu();
    private JMenuItem jMenuFileExit = new JMenuItem();
    private JMenu jMenuHelp = new JMenu();
    private JMenuItem jMenuHelpAbout = new JMenuItem();
    private JLabel statusBar = new JLabel();
    private BorderLayout borderLayout1 = new BorderLayout();
    private JTabbedPane rlTabbedPane = new JTabbedPane();
    private final EnumerationPanel EnumTab;
    private JPanel EUTab = new JPanel();
    private JPanel EuCnvtTab = new JPanel();
    private JMenuItem mi_saveToDb = new JMenuItem();
    private BorderLayout borderLayout3 = new BorderLayout();
    private JTextArea jTextArea2 = new JTextArea();



    private JPanel jPanel3 = new JPanel();
    private JScrollPane jScrollPane2 = new JScrollPane();
    private EUTableModel euTableModel = new EUTableModel();
    private JTable euTable = new SortingListTable(euTableModel,
        new int[] { 20, 30, 25, 25 });
    private JButton addEUButton = new JButton();
    private JButton editEUButton = new JButton();
    private JButton deleteEUButton = new JButton();


    private JButton undoDeleteEUButton = new JButton();
    private GridBagLayout gridBagLayout2 = new GridBagLayout();
    private BorderLayout borderLayout4 = new BorderLayout();
    private JTextArea jTextArea3 = new JTextArea();

    private JPanel jPanel4 = new JPanel();
    private JScrollPane jScrollPane3 = new JScrollPane();
    private EUCnvTableModel ucTableModel = new EUCnvTableModel();
    private JTable ucTable = new SortingListTable(ucTableModel,
        new int[] {17, 17, 18, 8, 8, 8, 8, 8, 8 });
    private JButton addEUCnvtButton = new JButton();
    private JButton editEUCnvtButton = new JButton();
    private JButton deleteEUCnvtButton = new JButton();
    private JButton undoDelEuCnvtButton = new JButton();
    private GridBagLayout gridBagLayout3 = new GridBagLayout();
    private Border border4;

    private DTEquivTableModel dteTableModel = new DTEquivTableModel();
    private JTable dteTable = new SortingListTable(dteTableModel,null);
    private JButton addDTEButton = new JButton();
    private JButton editDTEButton = new JButton();
    private JButton deleteDTEButton = new JButton();
    private JButton undoDeleteDTEButton = new JButton();


    private Border border6;
    private Border border7;

    //================================================

    private boolean unitsChanged = false;
    private boolean convertersChanged = false;
    private boolean dtsChanged = false;
    private boolean seasonsChanged = false;

    private EngineeringUnit deletedEU = null;
    private UnitConverterDb deletedConverter = null;
    private String []deletedDte = null;

    private SeasonListTableModel seasonListTableModel = new SeasonListTableModel();
    private SortingListTable seasonsTable = new SortingListTable(seasonListTableModel,
        SeasonListTableModel.colWidths);

    private final OpenDcsDatabase database;

    /**
     * No args constructor for JBuilder.
     */
    public RefListFrame(OpenDcsDatabase database)
    {
        this.database = database;

		/**
		 * NOTE: this *should* be off the GUI thread but the performance impact for 
		 * enums is minimal and this shows a good demostration of the getLegacyDatabase.
		 * 
		 * TODO: Correct before merge.
		 */
        ArrayList<DbEnum> v = new ArrayList<>();
        for(Iterator<DbEnum> enumIt = database.getLegacyDatabase(Database.class).get().enumList.iterator();
            enumIt.hasNext(); )
        {
            decodes.db.DbEnum en = enumIt.next();
            /*if (en.enumName.equalsIgnoreCase("EquationScope")
             || en.enumName.equalsIgnoreCase("DataOrder")
             || en.enumName.equalsIgnoreCase("UnitFamily")
             || en.enumName.equalsIgnoreCase("LookupAlgorithm")
             || en.enumName.equalsIgnoreCase("RecordingMode")
             || en.enumName.equalsIgnoreCase("EquipmentType")
             || en.enumName.equalsIgnoreCase("Season"))
                continue;
            String s = TextUtil.capsExpand(en.enumName);*/
            v.add(en);
        }
        Collections.sort(v, (a,b) -> a.enumName.compareTo(b.enumName));

        this.EnumTab = new EnumerationPanel(v);
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        try
        {
            jbInit();
            initControls();
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        // Default operation is to do nothing when user hits 'X' in upper
        // right to close the window. We will catch the closing event and
        // do the same thing as if user had hit File - Exit.
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(
            new WindowAdapter()
            {
                public void windowClosing(WindowEvent e)
                {
                      jMenuFileExit_actionPerformed(null);
                }
            });
    }

    /**
     * Initializes the controls in the frames.
     * Calls the init methods in other GUI objects, table models, etc.
     */
    private void initControls()
    {

        euTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        euTable.setRowHeight(20);
        ucTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ucTable.setRowHeight(20);
        dteTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        dteTable.setRowHeight(20);
    }

    /** Initializes GUI components. */
    private void jbInit() throws Exception
    {
        contentPane = (JPanel) this.getContentPane();

        border4 = BorderFactory.createCompoundBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED,Color.white,Color.white,new Color(124, 124, 124),new Color(178, 178, 178)),BorderFactory.createEmptyBorder(6,6,6,6));

        border6 = BorderFactory.createCompoundBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED,Color.white,Color.white,new Color(124, 124, 124),new Color(178, 178, 178)),BorderFactory.createEmptyBorder(6,6,6,6));
        border7 = BorderFactory.createCompoundBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED,Color.white,Color.white,new Color(124, 124, 124),new Color(178, 178, 178)),BorderFactory.createEmptyBorder(6,6,6,6));
        contentPane.setLayout(borderLayout1);
        this.setFont(new java.awt.Font("Serif", 0, 16));
        this.setSize(new Dimension(800, 536));
        this.setTitle(labels.getString("RefListFrame.frameTitle"));
        statusBar.setText(" ");
        jMenuFile.setText(genericLabels.getString("file"));
        jMenuFileExit.setText(genericLabels.getString("exit"));
        jMenuFileExit.addActionListener(new RefListFrame_jMenuFileExit_ActionAdapter(this));
        jMenuHelp.setText(genericLabels.getString("help"));
        jMenuHelpAbout.setText(genericLabels.getString("about"));
        jMenuHelpAbout.addActionListener(new RefListFrame_jMenuHelpAbout_ActionAdapter(this));
        rlTabbedPane.setTabPlacement(JTabbedPane.TOP);
        mi_saveToDb.setText(labels.getString("RefListFrame.saveToDB"));
        mi_saveToDb.addActionListener(new RefListFrame_mi_saveToDb_actionAdapter(this));


        EUTab.setLayout(borderLayout3);
        jTextArea2.setFont(new java.awt.Font("Serif", 0, 14));
        jTextArea2.setBorder(border6);
        jTextArea2.setEditable(false);
        jTextArea2.setText(labels.getString("RefListFrame.engineeringTabDesc"));
        jTextArea2.setLineWrap(true);
        jTextArea2.setRows(3);
        jTextArea2.setWrapStyleWord(true);
        jPanel3.setLayout(gridBagLayout2);
        addEUButton.setMaximumSize(new Dimension(122, 23));
        addEUButton.setMinimumSize(new Dimension(122, 23));
        addEUButton.setPreferredSize(new Dimension(122, 23));
        addEUButton.setText(genericLabels.getString("add"));
        addEUButton.addActionListener(new RefListFrame_addEUButton_actionAdapter(this));
        editEUButton.setMaximumSize(new Dimension(122, 23));
        editEUButton.setMinimumSize(new Dimension(122, 23));
        editEUButton.setPreferredSize(new Dimension(122, 23));
        editEUButton.setText(genericLabels.getString("edit"));
        editEUButton.addActionListener(new RefListFrame_editEUButton_actionAdapter(this));
        deleteEUButton.setMaximumSize(new Dimension(122, 23));
        deleteEUButton.setMinimumSize(new Dimension(122, 23));
        deleteEUButton.setPreferredSize(new Dimension(122, 23));
        deleteEUButton.setText(genericLabels.getString("delete"));
        deleteEUButton.addActionListener(new RefListFrame_deleteEUButton_actionAdapter(this));

        undoDeleteEUButton.setEnabled(false);
//        undoDeleteEUButton.setMaximumSize(new Dimension(122, 23));
//        undoDeleteEUButton.setMinimumSize(new Dimension(122, 23));
//        undoDeleteEUButton.setPreferredSize(new Dimension(122, 23));
        undoDeleteEUButton.setText(labels.getString("RefListFrame.undoDelete"));
        undoDeleteEUButton.addActionListener(new RefListFrame_undoDeleteEUButton_actionAdapter(this));

        EuCnvtTab.setLayout(borderLayout4);
        jTextArea3.setFont(new java.awt.Font("Serif", 0, 14));
        jTextArea3.setBorder(border7);
        jTextArea3.setEditable(false);
        jTextArea3.setText(labels.getString("RefListFrame.euconversionsDesc"));
        jTextArea3.setLineWrap(true);
        jTextArea3.setRows(3);
        jTextArea3.setWrapStyleWord(true);
        jPanel4.setLayout(gridBagLayout3);
        addEUCnvtButton.setMaximumSize(new Dimension(122, 23));
        addEUCnvtButton.setMinimumSize(new Dimension(122, 23));
        addEUCnvtButton.setPreferredSize(new Dimension(122, 23));
        addEUCnvtButton.setText(genericLabels.getString("add"));
        addEUCnvtButton.addActionListener(new RefListFrame_addEUCnvtButton_actionAdapter(this));
        editEUCnvtButton.setMaximumSize(new Dimension(122, 23));
        editEUCnvtButton.setMinimumSize(new Dimension(122, 23));
        editEUCnvtButton.setPreferredSize(new Dimension(122, 23));
        editEUCnvtButton.setRequestFocusEnabled(true);
        editEUCnvtButton.setText(genericLabels.getString("edit"));
        editEUCnvtButton.addActionListener(new RefListFrame_editEUCnvtButton_actionAdapter(this));
        deleteEUCnvtButton.setMaximumSize(new Dimension(122, 23));
        deleteEUCnvtButton.setMinimumSize(new Dimension(122, 23));
        deleteEUCnvtButton.setPreferredSize(new Dimension(122, 23));
        deleteEUCnvtButton.setText(genericLabels.getString("delete"));
        deleteEUCnvtButton.addActionListener(new RefListFrame_deleteEUCnvtButton_actionAdapter(this));
//        undoDelEuCnvtButton.setMaximumSize(new Dimension(122, 23));
//        undoDelEuCnvtButton.setMinimumSize(new Dimension(122, 23));
//        undoDelEuCnvtButton.setPreferredSize(new Dimension(122, 23));
        undoDelEuCnvtButton.setText(labels.getString("RefListFrame.undoDelete"));
        undoDelEuCnvtButton.addActionListener(new RefListFrame_undoDelEuCnvtButton_actionAdapter(this));

        addDTEButton.setMaximumSize(new Dimension(122, 23));
        addDTEButton.setMinimumSize(new Dimension(122, 23));
        addDTEButton.setPreferredSize(new Dimension(122, 23));
        addDTEButton.setText(labels.getString("RefListFrame.addEquiv"));
        addDTEButton.addActionListener(new RefListFrame_addDTEButton_actionAdapter(this));
        editDTEButton.setMaximumSize(new Dimension(122, 23));
        editDTEButton.setMinimumSize(new Dimension(122, 23));
        editDTEButton.setPreferredSize(new Dimension(122, 23));
        editDTEButton.setMargin(new Insets(2, 14, 2, 14));
        editDTEButton.setText(genericLabels.getString("edit"));
        editDTEButton.addActionListener(new RefListFrame_editDTEButton_actionAdapter(this));
        deleteDTEButton.setMaximumSize(new Dimension(122, 23));
        deleteDTEButton.setMinimumSize(new Dimension(122, 23));
        deleteDTEButton.setPreferredSize(new Dimension(122, 23));
        deleteDTEButton.setText(labels.getString("RefListFrame.deleteEquiv"));
        deleteDTEButton.addActionListener(new RefListFrame_deleteDTEButton_actionAdapter(this));
        undoDeleteDTEButton.setEnabled(false);
        undoDeleteDTEButton.setText(labels.getString("RefListFrame.undoDelete"));
        undoDeleteDTEButton.addActionListener(new RefListFrame_undoDeleteDTEButton_actionAdapter(this));

        contentPane.setFont(new java.awt.Font("Dialog", 0, 14));
        EuCnvtTab.setBorder(BorderFactory.createEmptyBorder());
        jMenuFile.add(mi_saveToDb);
        jMenuFile.add(jMenuFileExit);
        jMenuHelp.add(jMenuHelpAbout);
        jMenuBar1.add(jMenuFile);
        jMenuBar1.add(jMenuHelp);
        this.setJMenuBar(jMenuBar1);
        contentPane.add(statusBar, BorderLayout.SOUTH);
        contentPane.add(rlTabbedPane, BorderLayout.CENTER);

        rlTabbedPane.add(EnumTab,     labels.getString("RefListFrame.enumTab"));


        rlTabbedPane.add(EUTab,    labels.getString("RefListFrame.EngUnitsTab"));
        EUTab.add(jTextArea2, BorderLayout.NORTH);
        EUTab.add(jPanel3, BorderLayout.CENTER);
        jPanel3.add(jScrollPane2,    new GridBagConstraints(0, 0, 1, 4, 1.0, 1.0
                        ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(18, 10, 13, 0), 18, -56));
        jScrollPane2.getViewport().add(euTable, null);

        rlTabbedPane.add(EuCnvtTab,    labels.getString("RefListFrame.euConvTab"));
        EuCnvtTab.add(jTextArea3, BorderLayout.NORTH);
        EuCnvtTab.add(jPanel4, BorderLayout.CENTER);
        jPanel4.add(jScrollPane3,    new GridBagConstraints(0, 0, 1, 4, 1.0, 1.0
                        ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(21, 11, 10, 0), 25, -90));
        jScrollPane3.getViewport().add(ucTable, null);

        JPanel dtTab = new JPanel(new BorderLayout());
        JTextArea dtTabDescArea = new JTextArea();
        dtTabDescArea.setFont(new java.awt.Font("Serif", 0, 14));
        dtTabDescArea.setBorder(border4);
        dtTabDescArea.setEditable(false);
        dtTabDescArea.setText(labels.getString("RefListFrame.dataTypeEquDesc"));
        dtTabDescArea.setLineWrap(true);
        dtTabDescArea.setRows(3);
        dtTabDescArea.setWrapStyleWord(true);
        dtTab.add(dtTabDescArea, BorderLayout.NORTH);
        JPanel dtePanelCenter = new JPanel(new GridBagLayout());
        JScrollPane dtePanelScrollPane = new JScrollPane();
        dtePanelScrollPane.getViewport().add(dteTable, null);
        dtePanelCenter.add(dtePanelScrollPane,     new GridBagConstraints(0, 0, 1, 4, 1.0, 1.0,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
            new Insets(17, 15, 10, 0), 27, -77));
        dtTab.add(dtePanelCenter, BorderLayout.CENTER);
        rlTabbedPane.add(dtTab,    labels.getString("RefListFrame.dataTypeEquivTab"));


        // =========== Seasons Tab ==============
        JPanel seasonsTab = new JPanel(new BorderLayout());
        JTextArea seasonsTabDescArea = new JTextArea();
        seasonsTabDescArea.setFont(new java.awt.Font("Serif", 0, 14));
        seasonsTabDescArea.setBorder(border4);
        seasonsTabDescArea.setEditable(false);
        seasonsTabDescArea.setText(labels.getString("SeasonsTab.desc"));
        seasonsTabDescArea.setLineWrap(true);
        seasonsTabDescArea.setRows(3);
        seasonsTabDescArea.setWrapStyleWord(true);
        seasonsTab.add(seasonsTabDescArea, BorderLayout.NORTH);
        JPanel seasonsPanelCenter = new JPanel(new GridBagLayout());
        JScrollPane seasonsPanelScrollPane = new JScrollPane();
        seasonsPanelScrollPane.getViewport().add(seasonsTable, null);
        seasonsPanelCenter.add(seasonsPanelScrollPane,
            new GridBagConstraints(0, 0, 1, 5, 1.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(17, 15, 10, 0), 27, -77));
        seasonsTab.add(seasonsPanelCenter, BorderLayout.CENTER);
        JButton addSeasonButton = new JButton(genericLabels.getString("add"));
        addSeasonButton.addActionListener(
            new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    addSeasonPressed();
                }
            });
        seasonsPanelCenter.add(addSeasonButton,
            new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                new Insets(10, 5, 5, 5), 0, 0));

        JButton editSeasonButton = new JButton(genericLabels.getString("edit"));
        editSeasonButton.addActionListener(
            new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    editSeasonPressed();
                }
            });
        seasonsPanelCenter.add(editSeasonButton,
            new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(5, 5, 5, 5), 0, 0));

        JButton deleteSeasonButton = new JButton(genericLabels.getString("delete"));
        deleteSeasonButton.addActionListener(
            new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    deleteSeasonPressed();
                }
            });
        seasonsPanelCenter.add(deleteSeasonButton,
            new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(5, 5, 5, 5), 0, 0));

        JButton seasonUpButton = new JButton(labels.getString("RefListFrame.moveUp"));
        seasonUpButton.addActionListener(
            new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    seasonUpPressed();
                }
            });
        seasonsPanelCenter.add(seasonUpButton,
            new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(5, 5, 5, 5), 0, 0));

        JButton seasonDownButton = new JButton(labels.getString("RefListFrame.moveDown"));
        seasonDownButton.addActionListener(
            new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    seasonDownPressed();
                }
            });
        seasonsPanelCenter.add(seasonDownButton,
            new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0,
                GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
                new Insets(5, 5, 5, 5), 0, 0));

        rlTabbedPane.add(seasonsTab, labels.getString("SeasonsTab.tabName"));



        jPanel3.add(addEUButton,        new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
                        ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(15, 20, 5, 20), 0, 0));
        jPanel3.add(undoDeleteEUButton,     new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
                        ,GridBagConstraints.NORTH, GridBagConstraints.NONE, new Insets(5, 20, 5, 20), 0, 0));
        jPanel3.add(deleteEUButton,     new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
                        ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 20, 5, 20), 0, 0));
        jPanel3.add(editEUButton,     new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
                        ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 20, 5, 20), 0, 0));

        jPanel4.add(addEUCnvtButton,        new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0
                        ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(15, 16, 5, 12), 0, 0));
        jPanel4.add(editEUCnvtButton,     new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
                        ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 16, 5, 12), 0, 0));
        jPanel4.add(deleteEUCnvtButton,     new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
                        ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 16, 5, 12), 0, 0));
        jPanel4.add(undoDelEuCnvtButton,     new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
                        ,GridBagConstraints.NORTH, GridBagConstraints.NONE, new Insets(5, 16, 5, 12), 0, 0));

        dtePanelCenter.add(addDTEButton,
            new GridBagConstraints(1, 0, 1, 1, 0.0, 0.5,
                GridBagConstraints.SOUTH, GridBagConstraints.NONE,
                new Insets(5, 20, 5, 20), 0, 0));
        dtePanelCenter.add(editDTEButton,
            new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(5, 20, 5, 20), 0, 0));
        dtePanelCenter.add(deleteDTEButton,
            new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.NONE,
                new Insets(5, 20, 5, 20), 0, 0));
        dtePanelCenter.add(undoDeleteDTEButton,
            new GridBagConstraints(1, 3, 1, 1, 0.0, .5,
                GridBagConstraints.NORTH, GridBagConstraints.NONE,
                new Insets(5, 20, 5, 20), 0, 0));
    }

    protected void seasonDownPressed()
    {
        int row = seasonsTable.getSelectedRow();
        if (row == -1)
        {
            showError(
                labels.getString("SeasonsTab.noSelection") + " " +
                labels.getString("RefListFrame.moveDown"));
            return;
        }
        if (seasonListTableModel.moveDown(row))
            seasonsTable.setRowSelectionInterval(row+1, row+1);
        seasonsChanged = true;
    }

    protected void seasonUpPressed()
    {
        int row = seasonsTable.getSelectedRow();
        if (row == -1)
        {
            showError(
                labels.getString("SeasonsTab.noSelection") + " " +
                labels.getString("RefListFrame.moveUp"));
            return;
        }
        if (seasonListTableModel.moveUp(row))
            seasonsTable.setRowSelectionInterval(row-1, row-1);
        seasonsChanged = true;
    }

    protected void deleteSeasonPressed()
    {
        Season season = null;
        int idx = this.seasonsTable.getSelectedRow();
        if (idx == -1
         || (season = (Season)seasonListTableModel.getRowObject(idx)) == null)
        {
            showError(
                labels.getString("SeasonsTab.noSelection") + " " + genericLabels.getString("delete"));
            return;
        }
        int r = JOptionPane.showConfirmDialog(this,
            "Confirm", labels.getString("SeasonsTab.confirmDelete") + " " + season.getAbbr(),
            JOptionPane.YES_NO_OPTION);
        if (r != JOptionPane.YES_OPTION)
            return;
        seasonListTableModel.deleteAt(idx);
        seasonsChanged = true;
    }

    protected void editSeasonPressed()
    {
        Season season = null;
        int idx = this.seasonsTable.getSelectedRow();
        if (idx == -1
         || (season = (Season)seasonListTableModel.getRowObject(idx)) == null)
        {
            showError(
                labels.getString("SeasonsTab.noSelection") + " " + genericLabels.getString("edit"));
            return;
        }
        SeasonEditDialog dlg = new SeasonEditDialog(this);
        dlg.fillValues(season);
        launchDialog(dlg);
        if (dlg.isOkPressed())
        {
            seasonListTableModel.fireTableDataChanged();
            seasonsChanged = true;
        }
    }

    protected void addSeasonPressed()
    {
        Season season = new Season();
        SeasonEditDialog dlg = new SeasonEditDialog(this);
        dlg.fillValues(season);
        launchDialog(dlg);
        if (dlg.isOkPressed())
        {
            seasonListTableModel.add(season);
            seasonsChanged = true;
        }
    }

    /**
     * File | Exit action performed.
     * @param e ignored
     */
    public void jMenuFileExit_actionPerformed(ActionEvent e)
    {
        if (EnumTab.enumsChanged() || unitsChanged || convertersChanged || dtsChanged || seasonsChanged)
        {
            int r = JOptionPane.showConfirmDialog(this,
                labels.getString("RefListFrame.unsavedChangesQues"),
                labels.getString("RefListFrame.confirmExit"),
                JOptionPane.YES_NO_OPTION);
            if (r != JOptionPane.YES_OPTION)
                return;
        }
        System.exit(0);
    }

    /**
     * Help | About action performed.
     * @param e ignored
     */
    public void jMenuHelpAbout_actionPerformed(ActionEvent e) {
        RefListFrame_AboutBox dlg = new RefListFrame_AboutBox(this);
        Dimension dlgSize = dlg.getPreferredSize();
        Dimension frmSize = getSize();
        java.awt.Point loc = getLocation();
        dlg.setLocation((frmSize.width - dlgSize.width) / 2 + loc.x, (frmSize.height - dlgSize.height) / 2 + loc.y);
        dlg.setModal(true);
        dlg.pack();
        dlg.setVisible(true);
//        dlg.show();
    }


    /**
     * Displays modal EU dialog. If OK pressed, results are added to the
     * table. Sets modified flag.
     * @param e ActionEvent
     */
    void addEUButton_actionPerformed(ActionEvent e)
    {
        EUDialog dlg = new EUDialog();
        launchDialog(dlg);
        if (dlg.wasChanged())
        {
            String abbr = dlg.getAbbr();
            if (abbr == null || abbr.length() == 0)
            {
                showError(labels.getString("RefListFrame.euAbbrErr"));
                return;
            }

            EngineeringUnit eu =
                Database.getDb().engineeringUnitList.getByAbbr(abbr);
            if (eu != null)
            {
                showError(LoadResourceBundle.sprintf(
                        labels.getString("RefListFrame.euAlreadyExistErr"),
                        abbr));
                return;
            }

            eu = EngineeringUnit.getEngineeringUnit(abbr);
            eu.setName(dlg.getEUName());
            eu.family = dlg.getFamily();
            eu.measures = dlg.getMeasures();
            unitsChanged = true;
            euTableModel.rebuild();
            euTableModel.fireTableDataChanged();
        }
    }

    /**
    * Displays model EU dialog with the currently selected EU. If OK pressed,
    * any modifications are copied back into the object in the table. Sets
    * modified flag.
    * @param e ActionEvent
    */
    void editEUButton_actionPerformed(ActionEvent e)
    {
        int row = euTable.getSelectedRow();
        if (row == -1)
        {
            showError(labels.getString("RefListFrame.engEditInfo"));
            return;
        }
        EngineeringUnit eu = (EngineeringUnit)euTableModel.getRowObject(row);
        String oldAbbr = eu.abbr;
        EUDialog dlg = new EUDialog();
        dlg.fillValues(eu);
        launchDialog(dlg);
        if (dlg.wasChanged())
        {
            EngineeringUnitList eul = Database.getDb().engineeringUnitList;
            String abbr = dlg.getAbbr();
            if (abbr == null || abbr.length() == 0)
            {
                showError(labels.getString("RefListFrame.euAbbrErr"));
                return;
            }
            if (!oldAbbr.equalsIgnoreCase(abbr))
            {
                // Abbr was changed, make sure it doesn't clash with another EU.
                EngineeringUnit otherEU = eul.getByAbbr(abbr);
                if (otherEU != null)
                {
                    showError(LoadResourceBundle.sprintf(
                            labels.getString("RefListFrame.cantChangeAbbr"),
                            abbr));
                    return;
                }
            }

            eul.remove(eu);  // Remove hash entry for old abbr & name.
            eu.abbr = abbr;
            eu.setName(dlg.getEUName());
            eu.family = dlg.getFamily();
            eu.measures = dlg.getMeasures();
            eul.add(eu);     // Re-add with correct hash entries.

            unitsChanged = true;
            euTableModel.rebuild();
            euTableModel.fireTableDataChanged();
        }
    }

    /**
     * Deletes the currently selected EU and adds it to the undo buffer.
     * Enables the undo-delete button.
     * @param e ActionEvent
     */
    void deleteEUButton_actionPerformed(ActionEvent e)
    {
        int row = euTable.getSelectedRow();
        if (row == -1)
        {
            showError(labels.getString("RefListFrame.engDeleteInfo"));
            return;
        }
        deletedEU = (EngineeringUnit)euTableModel.getRowObject(row);
        Database.getDb().engineeringUnitList.remove(deletedEU);
        unitsChanged = true;
        euTableModel.rebuild();
        euTableModel.fireTableDataChanged();
        undoDeleteEUButton.setEnabled(true);
    }

    /**
     * Re-adds the currently selected EU back into the table.
     * Disables the undo-delete button.
     * @param e ActionEvent
     */
    void undoDeleteEUButton_actionPerformed(ActionEvent e)
    {
        undoDeleteEUButton.setEnabled(false);
        if (deletedEU == null)
            return;
        Database.getDb().engineeringUnitList.add(deletedEU);
        deletedEU = null;
        unitsChanged = true;
        euTableModel.rebuild();
        euTableModel.fireTableDataChanged();
    }

    /**
     * Displays the modal EU Conversion dialog. If OK pressed, the results
     * are added to the table as a new conversion.
     * @param e ActionEvent
     */
    void addEUCnvtButton_actionPerformed(ActionEvent e)
    {
        UnitConverterDb uc = new UnitConverterDb("", "");
        uc.algorithm = "";
        EUCnvEditDialog dlg = new EUCnvEditDialog();
        dlg.fillValues(this, uc);
        launchDialog(dlg);
        if (dlg.wasChanged())
        {
            UnitConverterSet ucs = Database.getDb().unitConverterSet;
            ucs.addDbConverter(uc);
            convertersChanged = true;
            ucTableModel.rebuild();
            ucTableModel.fireTableDataChanged();
        }
    }

    /**
     * Displays the model EU Conversion dialog with the selected conversion.
     * If OK pressed, the results are added to the table.
     * @param e ActionEvent
     */
    void editEUCnvtButton_actionPerformed(ActionEvent e)
    {
        int row = ucTable.getSelectedRow();
        if (row == -1)
        {
            showError(labels.getString("RefListFrame.unitConvEditInfo"));
            return;
        }

        UnitConverterDb uc = (UnitConverterDb)ucTableModel.getRowObject(row);
        String oldFrom = uc.fromAbbr;
        String oldTo = uc.toAbbr;
        EUCnvEditDialog dlg = new EUCnvEditDialog();
        dlg.fillValues(this, uc);

        launchDialog(dlg);
        if (dlg.wasChanged())
        {
            UnitConverterSet ucs = Database.getDb().unitConverterSet;

            /*
              Converters are hashed based on from/to abbreviations. So
              if either abbr is changed, delete from set & re-add.
              Note: Dialog makes sure there is no clash if abbrs are changed.
            */
            if (!oldFrom.equals(uc.fromAbbr) || !oldTo.equals(uc.toAbbr))
            {
                ucs.removeDbConverter(oldFrom, oldTo);
                ucs.addDbConverter(uc);
            }

            convertersChanged = true;
            ucTableModel.rebuild();
            ucTableModel.fireTableDataChanged();
        }
    }

    /**
     * Deletes the selected EU conversion & adds it to the undo buffer.
     * Enables the undo button.
     * @param e ActionEvent
     */
    void deleteEUCnvtButton_actionPerformed(ActionEvent e)
    {
        int row = ucTable.getSelectedRow();
        if (row == -1)
        {
            showError(labels.getString("RefListFrame.unitConvDeleteInfo"));
            return;
        }
        deletedConverter = (UnitConverterDb)ucTableModel.getRowObject(row);
        Database.getDb().unitConverterSet.removeDbConverter(
            deletedConverter.fromAbbr, deletedConverter.toAbbr);
        convertersChanged = true;
        ucTableModel.rebuild();
        ucTableModel.fireTableDataChanged();
        undoDelEuCnvtButton.setEnabled(true);
    }

    /**
     * Re-adds the deleted EU Conversion back into the table.
     * Disables the undo button.
     * @param e ActionEvent
     */
    void undoDelEuCnvtButton_actionPerformed(ActionEvent e)
    {
        if (deletedConverter == null)
            return;
        Database.getDb().unitConverterSet.addDbConverter(deletedConverter);
        deletedConverter = null;
        convertersChanged = true;
        ucTableModel.rebuild();
        ucTableModel.fireTableDataChanged();
        undoDelEuCnvtButton.setEnabled(false);
    }

    /**
     * Displays the DataType Equivalence dialog. If OK pressed, adds a
     * new DTE to the table.
     * @param e ActionEvent
     */
    void addDTEButton_actionPerformed(ActionEvent e)
    {
        undoDeleteDTEButton.setEnabled(false);
        deletedDte = null;

        DTEDialog dlg = new DTEDialog(this);
        dlg.fillValues(dteTableModel, -1);
        launchDialog(dlg);
        if (dlg.wasChanged())
        {
            dtsChanged = true;
            dteTableModel.rebuild();
            dteTableModel.fireTableDataChanged();
        }
    }

    /**
     * Displays the modal DataType Equivalence dialog with the selected
     * element in the table.
     * @param e ActionEvent
     */
    void editDTEButton_actionPerformed(ActionEvent e)
    {
        undoDeleteDTEButton.setEnabled(false);
        deletedDte = null;

        int row = dteTable.getSelectedRow();
        if (row == -1)
        {
            showError(labels.getString("RefListFrame.selectRowEditInfo"));
            return;
        }
        DTEDialog dlg = new DTEDialog(this);
        dlg.fillValues(dteTableModel, row);
        launchDialog(dlg);
        if (dlg.wasChanged())
        {
            dtsChanged = true;
            dteTableModel.rebuild();
            dteTableModel.fireTableDataChanged();
        }
    }

    /**
     * Restores the deleted data type equivalence to the table.
     * Disables the undo button.
     * @param e ActionEvent
     */
    void undoDeleteDTEButton_actionPerformed(ActionEvent e)
    {
        if (deletedDte != null)
        {
            DataType lastDT = null;
            for(int i=0; i<deletedDte.length; i++)
            {
                String v = deletedDte[i];
                if (v != null && v.length() > 0)
                {
                    String std = dteTableModel.getColumnName(i);
                    DataType ndt = DataType.getDataType(std, v);
                    if (lastDT != null)
                        lastDT.assertEquivalence(ndt);
                    lastDT = ndt;
                }
            }
            dteTableModel.rebuild();
            dteTableModel.fireTableDataChanged();
        }
        dtsChanged = true;
        deletedDte = null;
        undoDeleteDTEButton.setEnabled(false);
    }

    /**
     * Deletes the selected data type equivalence and adds it to the undo buffer.
     * Enables the undo button.
     * @param e ActionEvent
     */
    void deleteDTEButton_actionPerformed(ActionEvent e)
    {
        int row = dteTable.getSelectedRow();
        if (row == -1)
        {
            showError(labels.getString("RefListFrame.selectRowDeleteInfo"));
            return;
        }
        deletedDte = (String[])dteTableModel.getRowObject(row);
        DataTypeSet dts = Database.getDb().dataTypeSet;
        for(int i=0; i<dteTableModel.getColumnCount(); i++)
        {
            String v = deletedDte[i];
            if (v != null && v.length() > 0)
            {
                String std = dteTableModel.getColumnName(i);
                DataType dt = dts.get(std, v);
                if (dt != null)
                    dt.deAssertEquivalence();
            }
        }
        undoDeleteDTEButton.setEnabled(true);
        dteTableModel.rebuild();
        dteTableModel.fireTableDataChanged();
        dtsChanged = true;
    }

    /**
     * Called when save to DB menu item selected.
     * @param e ActionEvent
     */
    void mi_saveToDb_actionPerformed(ActionEvent e)
    {
		final TraceDialog dlg = new TraceDialog(this, true);
		final String CLOSE_MSG = "Saving Changes.";
		dlg.setCloseText(CLOSE_MSG);
		final AtomicBoolean result = new AtomicBoolean(false);
		final Collection<DbEnum> changedEnums = EnumTab.getChanged();
		SwingWorker<Boolean,String> worker = new SwingWorker<Boolean,String>()
		{
			@Override
			protected Boolean doInBackground() throws Exception
			{
				Database db = Database.getDb();
				if (seasonsChanged)
				{
					publish("Writing Seasons");
					seasonListTableModel.storeBackToEnum();
					db.enumList.write();
					seasonsChanged = false;
				}
				if (EnumTab.enumsChanged())
				{
					publish("Writing Enumerations");
					try(DataTransaction tx = database.newTransaction();
						EnumDAI enumDao = database.getDao(EnumDAI.class).get();)
					{
						for (DbEnum curEnum: changedEnums)
						{
							publish("\t" + curEnum.enumName);
							enumDao.writeEnum(tx, curEnum);
						}
					}
				}
				if (unitsChanged || convertersChanged)
				{
					publish("Engineering Units");
					db.engineeringUnitList.write();
					unitsChanged = convertersChanged = false;
				}
				if (dtsChanged)
				{
					publish("Data Types");
					db.dataTypeSet.write();
					dtsChanged = false;
				}
				return true;
			}

			@Override
			protected void process(List<String> chunks)
			{
				for (String text: chunks)
				{
					dlg.addText(text);
				}
			}

			@Override
			protected void done()
			{
				try
				{
					if (get())
					{
						result.set(true);
						dlg.addText(CLOSE_MSG);
						EnumTab.resetChanged();
						JOptionPane.showConfirmDialog(RefListFrame.this, labels.getString("RefListFrame.changesWritten"),
					"Info", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE);
					}
				}
				catch (Exception ex)
				{
					final String msg =
						LoadResourceBundle.sprintf(genericLabels.getString("cannotSave"), "Enumeration", ex.toString());
					log.atError()
						.setCause(ex)
						.log(msg);
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					pw.println();
					pw.println(msg);
					ex.printStackTrace(pw);
					dlg.addText(sw.toString());
				}
			}
		};
		worker.execute();
		dlg.setVisible(true);
    }

    /**
      Launches the passed modal dialog at a reasonable position on the screen.
      @param dlg the dialog.
    */
    private void launchDialog(JDialog dlg)
    {
        dlg.setModal(true);
        dlg.validate();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    /**
     * Shows an error message in a JOptionPane and prints it to stderr.
     * @param msg the error message.
     */
    public void showError(String msg)
    {
        System.err.println(msg);
        JOptionPane.showMessageDialog(this,
            AsciiUtil.wrapString(msg, 60), "Error!", JOptionPane.ERROR_MESSAGE);
    }
}
class RefListFrame_jMenuFileExit_ActionAdapter implements ActionListener {
    RefListFrame adaptee;

    RefListFrame_jMenuFileExit_ActionAdapter(RefListFrame adaptee) {
        this.adaptee = adaptee;
    }
    public void actionPerformed(ActionEvent e) {
        adaptee.jMenuFileExit_actionPerformed(e);
    }
}

class RefListFrame_jMenuHelpAbout_ActionAdapter implements ActionListener {
    RefListFrame adaptee;

    RefListFrame_jMenuHelpAbout_ActionAdapter(RefListFrame adaptee) {
        this.adaptee = adaptee;
    }
    public void actionPerformed(ActionEvent e) {
        adaptee.jMenuHelpAbout_actionPerformed(e);
    }
}



class RefListFrame_addEUButton_actionAdapter implements java.awt.event.ActionListener {
    RefListFrame adaptee;

    RefListFrame_addEUButton_actionAdapter(RefListFrame adaptee) {
        this.adaptee = adaptee;
    }
    public void actionPerformed(ActionEvent e) {
        adaptee.addEUButton_actionPerformed(e);
    }
}

class RefListFrame_editEUButton_actionAdapter implements java.awt.event.ActionListener {
    RefListFrame adaptee;

    RefListFrame_editEUButton_actionAdapter(RefListFrame adaptee) {
        this.adaptee = adaptee;
    }
    public void actionPerformed(ActionEvent e) {
        adaptee.editEUButton_actionPerformed(e);
    }
}

class RefListFrame_deleteEUButton_actionAdapter implements java.awt.event.ActionListener {
    RefListFrame adaptee;

    RefListFrame_deleteEUButton_actionAdapter(RefListFrame adaptee) {
        this.adaptee = adaptee;
    }
    public void actionPerformed(ActionEvent e) {
        adaptee.deleteEUButton_actionPerformed(e);
    }
}

class RefListFrame_undoDeleteEUButton_actionAdapter implements java.awt.event.ActionListener {
    RefListFrame adaptee;

    RefListFrame_undoDeleteEUButton_actionAdapter(RefListFrame adaptee) {
        this.adaptee = adaptee;
    }
    public void actionPerformed(ActionEvent e) {
        adaptee.undoDeleteEUButton_actionPerformed(e);
    }
}

class RefListFrame_addEUCnvtButton_actionAdapter implements java.awt.event.ActionListener {
    RefListFrame adaptee;

    RefListFrame_addEUCnvtButton_actionAdapter(RefListFrame adaptee) {
        this.adaptee = adaptee;
    }
    public void actionPerformed(ActionEvent e) {
        adaptee.addEUCnvtButton_actionPerformed(e);
    }
}

class RefListFrame_editEUCnvtButton_actionAdapter implements java.awt.event.ActionListener {
    RefListFrame adaptee;

    RefListFrame_editEUCnvtButton_actionAdapter(RefListFrame adaptee) {
        this.adaptee = adaptee;
    }
    public void actionPerformed(ActionEvent e) {
        adaptee.editEUCnvtButton_actionPerformed(e);
    }
}

class RefListFrame_deleteEUCnvtButton_actionAdapter implements java.awt.event.ActionListener {
    RefListFrame adaptee;

    RefListFrame_deleteEUCnvtButton_actionAdapter(RefListFrame adaptee) {
        this.adaptee = adaptee;
    }
    public void actionPerformed(ActionEvent e) {
        adaptee.deleteEUCnvtButton_actionPerformed(e);
    }
}

class RefListFrame_undoDelEuCnvtButton_actionAdapter implements java.awt.event.ActionListener {
    RefListFrame adaptee;

    RefListFrame_undoDelEuCnvtButton_actionAdapter(RefListFrame adaptee) {
        this.adaptee = adaptee;
    }
    public void actionPerformed(ActionEvent e) {
        adaptee.undoDelEuCnvtButton_actionPerformed(e);
    }
}

class RefListFrame_addDTEButton_actionAdapter implements java.awt.event.ActionListener {
    RefListFrame adaptee;

    RefListFrame_addDTEButton_actionAdapter(RefListFrame adaptee) {
        this.adaptee = adaptee;
    }
    public void actionPerformed(ActionEvent e) {
        adaptee.addDTEButton_actionPerformed(e);
    }
}

class RefListFrame_editDTEButton_actionAdapter implements java.awt.event.ActionListener {
    RefListFrame adaptee;

    RefListFrame_editDTEButton_actionAdapter(RefListFrame adaptee) {
        this.adaptee = adaptee;
    }
    public void actionPerformed(ActionEvent e) {
        adaptee.editDTEButton_actionPerformed(e);
    }
}

class RefListFrame_undoDeleteDTEButton_actionAdapter implements java.awt.event.ActionListener {
    RefListFrame adaptee;

    RefListFrame_undoDeleteDTEButton_actionAdapter(RefListFrame adaptee) {
        this.adaptee = adaptee;
    }
    public void actionPerformed(ActionEvent e) {
        adaptee.undoDeleteDTEButton_actionPerformed(e);
    }
}

class RefListFrame_deleteDTEButton_actionAdapter implements java.awt.event.ActionListener {
    RefListFrame adaptee;

    RefListFrame_deleteDTEButton_actionAdapter(RefListFrame adaptee) {
        this.adaptee = adaptee;
    }
    public void actionPerformed(ActionEvent e) {
        adaptee.deleteDTEButton_actionPerformed(e);
    }
}

class RefListFrame_mi_saveToDb_actionAdapter implements java.awt.event.ActionListener {
    RefListFrame adaptee;

    RefListFrame_mi_saveToDb_actionAdapter(RefListFrame adaptee) {
        this.adaptee = adaptee;
    }
    public void actionPerformed(ActionEvent e) {
        adaptee.mi_saveToDb_actionPerformed(e);
    }
}
