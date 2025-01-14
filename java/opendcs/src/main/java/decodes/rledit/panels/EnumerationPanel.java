package decodes.rledit.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;

import org.opendcs.database.api.OpenDcsDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import decodes.db.Database;
import decodes.db.DbEnum;
import decodes.db.EnumValue;
import decodes.gui.SortingListTable;
import decodes.rledit.EnumTableModel;
import decodes.rledit.EnumValueDialog;
import decodes.rledit.RefListEditor;
import decodes.rledit.RefListFrame;
import ilex.util.AsciiUtil;
import ilex.util.TextUtil;

public class EnumerationPanel extends JPanel
{
    private static final Logger log = LoggerFactory.getLogger(EnumerationPanel.class);
    private static ResourceBundle genericLabels = RefListEditor.getGenericLabels();
    private static ResourceBundle labels = RefListEditor.getLabels();

    private JTextArea jTextArea1 = new JTextArea();
    private BorderLayout borderLayout2 = new BorderLayout();
    private GridBagLayout gridBagLayout1 = new GridBagLayout();
    private JPanel jPanel1 = new JPanel();
    private JScrollPane jScrollPane1 = new JScrollPane();
    private EnumTableModel enumTableModel = new EnumTableModel();
    private JTable enumTable = new SortingListTable(enumTableModel,
        new int[] { 9, 20, 41, 30 });
    private JButton addEnumValButton = new JButton();
    private JButton editEnumValButton = new JButton();
    private JButton deleteEnumValButton = new JButton();
    private JButton selectEnumValDefaultButton = new JButton();
    private JButton upEnumValButton = new JButton();
    private JButton downEnumValButton = new JButton();
    private JButton undoDeleteEnumValButton = new JButton();
    private JPanel jPanel2 = new JPanel();
    private FlowLayout flowLayout1 = new FlowLayout();
    private JLabel jLabel1 = new JLabel();
    private final JComboBox<DbEnum> enumComboBox;

    private Border border5;

    private boolean enumsChanged = false;
    private EnumValue deletedEnumValue = null;

    public EnumerationPanel(Collection<DbEnum> dbEnums)
    {
        enumComboBox = new JComboBox<>(new Vector<>(dbEnums));
        try
        {
            jbInit();
        }
        catch (Exception ex)
        {

            log.atError()
				.setCause(ex)
				.log("Error creating EnumerationPanel");
        }
    }

    public boolean enumsChanged()
    {
        return enumsChanged;
    }

    private void jbInit() throws Exception
    {
        jTextArea1.setBackground(Color.white);
        jTextArea1.setFont(new java.awt.Font("Serif", 0, 14));
        jTextArea1.setBorder(border5);
        jTextArea1.setEditable(false);
        jTextArea1.setText(labels.getString("RefListFrame.enumerationsTab"));
        jTextArea1.setLineWrap(true);
        jTextArea1.setRows(3);
        jTextArea1.setWrapStyleWord(true);
        this.setLayout(borderLayout2);
        jPanel1.setLayout(gridBagLayout1);
        addEnumValButton.setMaximumSize(new Dimension(122, 23));
        addEnumValButton.setMinimumSize(new Dimension(122, 23));
        addEnumValButton.setPreferredSize(new Dimension(122, 23));
        addEnumValButton.setText(genericLabels.getString("add"));
        addEnumValButton.addActionListener(e -> this.addEnumValButton_actionPerformed(e));
        editEnumValButton.setMaximumSize(new Dimension(122, 23));
        editEnumValButton.setMinimumSize(new Dimension(122, 23));
        editEnumValButton.setPreferredSize(new Dimension(122, 23));
        editEnumValButton.setText(genericLabels.getString("edit"));
        editEnumValButton.addActionListener(e -> this.editEnumValButton_actionPerformed(e));
        deleteEnumValButton.setMaximumSize(new Dimension(122, 23));
        deleteEnumValButton.setMinimumSize(new Dimension(122, 23));
        deleteEnumValButton.setPreferredSize(new Dimension(122, 23));
        deleteEnumValButton.setText(genericLabels.getString("delete"));
        deleteEnumValButton.addActionListener(e -> this.deleteEnumValButton_actionPerformed(e));
        selectEnumValDefaultButton.setText(labels.getString("RefListFrame.setDefault"));
        selectEnumValDefaultButton.addActionListener(e -> this.selectEnumValDefaultButton_actionPerformed(e));
        upEnumValButton.setMaximumSize(new Dimension(122, 23));
        upEnumValButton.setMinimumSize(new Dimension(122, 23));
        upEnumValButton.setPreferredSize(new Dimension(122, 23));
        upEnumValButton.setText(labels.getString("RefListFrame.moveUp"));
        upEnumValButton.addActionListener(e -> this.upEnumValButton_actionPerformed(e));
        downEnumValButton.setMaximumSize(new Dimension(122, 23));
        downEnumValButton.setMinimumSize(new Dimension(122, 23));
        downEnumValButton.setPreferredSize(new Dimension(122, 23));
        downEnumValButton.setText(labels.getString("RefListFrame.moveDown"));
        downEnumValButton.addActionListener(e -> this.downEnumValButton_actionPerformed(e));
        undoDeleteEnumValButton.setEnabled(false);
        undoDeleteEnumValButton.setText(labels.getString("RefListFrame.undoDelete"));
        undoDeleteEnumValButton.addActionListener(e -> this.undoDeleteEnumValButton_actionPerformed(e));
        jPanel2.setLayout(flowLayout1);
        jLabel1.setText(labels.getString("RefListFrame.enumeration"));
        enumComboBox.setMinimumSize(new Dimension(160, 19));
        enumComboBox.setPreferredSize(new Dimension(160, 19));
        enumComboBox.addActionListener(new RefListFrame_enumComboBox_actionAdapter(this));
        if (enumComboBox.getModel().getSize() > 0)
        {
            enumComboBox.setSelectedIndex(0);
        }
        border5 = BorderFactory.createCompoundBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED,Color.white,Color.white,new Color(124, 124, 124),new Color(178, 178, 178)),BorderFactory.createEmptyBorder(6,6,6,6));

        enumTable.setRowHeight(20);
        enumTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);


        this.add(jTextArea1, BorderLayout.NORTH);
        this.add(jPanel1, BorderLayout.CENTER);
        jPanel1.add(jScrollPane1,     new GridBagConstraints(0, 1, 1, 7, 1.0, 1.0
                        ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 8, 12, 0), 10, -98));
        jScrollPane1.getViewport().add(enumTable, null);

        //====================================
        jPanel1.add(editEnumValButton,        new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0
        ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 20, 5, 20), 0, 0));
        jPanel1.add(deleteEnumValButton,     new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0
                ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 20, 5, 20), 0, 0));
        jPanel1.add(jPanel2,     new GridBagConstraints(0, 0, 2, 1, 1.0, 0.1
                ,GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(14, 18, 14, 12), 0, 7));

        jPanel2.add(jLabel1, null);
        jPanel2.add(enumComboBox, null);

        jPanel1.add(addEnumValButton,        new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0
                ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 20, 5, 20), 0, 0));
        jPanel1.add(downEnumValButton,     new GridBagConstraints(1, 7, 1, 1, 0.0, 0.0
                ,GridBagConstraints.NORTH, GridBagConstraints.NONE, new Insets(5, 20, 5, 20), 0, 0));
        jPanel1.add(selectEnumValDefaultButton,     new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0
                ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 20, 5, 20), 0, 0));
        jPanel1.add(upEnumValButton,     new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0
                ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 20, 5, 20), 0, 0));
        jPanel1.add(undoDeleteEnumValButton,     new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0
                ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 20, 5, 20), 0, 0));

    }


        /**
     * Selects an Enumeration to be displayed in the table.
     * The table is repopulated and the 'Undo' button is disabled.
     * @param e ActionEvent
     */
    void enumComboBox_actionPerformed(ActionEvent e)
    {
        // todo: Populate table from selected enum.
        DbEnum dbEnum = (DbEnum)enumComboBox.getSelectedItem();
        enumTableModel.setEnum(dbEnum);
        deletedEnumValue = null;
        undoDeleteEnumValButton.setEnabled(false);
    }


    /**
     * Displays modal enum value dialog. If OK pressed, results are added
     * to table.
     * Sets modified flag.
     * @param e ActionEvent
     */
    void addEnumValButton_actionPerformed(ActionEvent e)
    {
        decodes.db.DbEnum en = (DbEnum)enumComboBox.getSelectedItem();

        EnumValueDialog evd = new EnumValueDialog();
        EnumValue ev = new EnumValue(en, "", "", "", "");
        evd.fillValues(ev);
        launchDialog(evd);
        if (evd.wasChanged())
        {
            en.replaceValue(ev.getValue(), ev.getDescription(), ev.getExecClassName(), "");
            enumTableModel.fireTableDataChanged();
            enumsChanged = true;
        }
    }


    /**
    * Displays modal enum-value dialog with selected EV. If OK pressed
    * the dialog contents are added to the table.
    * The 'modified' flag is set.
    * @param e ActionEvent
    */
    void editEnumValButton_actionPerformed(ActionEvent e)
    {
        int row = enumTable.getSelectedRow();
        if (row == -1)
        {
            showError(labels.getString("RefListFrame.enumSelectInfo"));
            return;
        }

        decodes.db.DbEnum en = (DbEnum)enumComboBox.getSelectedItem();
        EnumValue ev = enumTableModel.getEnumValueAt(row);
        EnumValueDialog evd = new EnumValueDialog();
        evd.fillValues(ev);
        launchDialog(evd);
        if (evd.wasChanged())
        {
            en.replaceValue(ev.getValue(), ev.getDescription(), ev.getExecClassName(), ev.getEditClassName());
            enumTableModel.fireTableDataChanged();
            enumsChanged = true;
        }
    }

    /**
     * Deletes the selected enum-value from the table and places it in the
     * undo buffer. Enables the Undo Delete button.
     * @param e ActionEvent
     */
    void deleteEnumValButton_actionPerformed(ActionEvent e)
    {
        int row = enumTable.getSelectedRow();
        if (row == -1)
        {
            showError(labels.getString("RefListFrame.enumDeleteInfo"));
            return;
        }

        decodes.db.DbEnum en = (DbEnum)enumComboBox.getSelectedItem();
        deletedEnumValue = enumTableModel.getEnumValueAt(row);
        en.removeValue(deletedEnumValue.getValue());
        enumTableModel.fireTableDataChanged();
        undoDeleteEnumValButton.setEnabled(true);
        enumsChanged = true;
    }

    /**
     * Adds the deleted enum value back into the table.
     * Disables the undo button.
     * @param e ActionEvent
     */
    void undoDeleteEnumValButton_actionPerformed(ActionEvent e)
    {
        if (deletedEnumValue != null)
        {
            decodes.db.DbEnum en = (DbEnum)enumComboBox.getSelectedItem();
            en.replaceValue(deletedEnumValue.getValue(),
                deletedEnumValue.getDescription(),
                deletedEnumValue.getExecClassName(), "");
            deletedEnumValue = null;
            enumTableModel.fireTableDataChanged();
        }
        undoDeleteEnumValButton.setEnabled(false);
        enumsChanged = true;
    }

    /**
     * Marks the currently selected enumeration value as the 'default' value.
     * @param e ActionEvent
     */
    void selectEnumValDefaultButton_actionPerformed(ActionEvent e)
    {
        int row = enumTable.getSelectedRow();
        if (row == -1)
        {
            showError(labels.getString("RefListFrame.enumDefaultInfo"));
            return;
        }

        decodes.db.DbEnum en = (DbEnum)enumComboBox.getSelectedItem();
        EnumValue ev = enumTableModel.getEnumValueAt(row);
        enumTableModel.fireTableDataChanged();
        en.setDefault(ev.getValue());
        enumsChanged = true;
    }

    /**
     * Moves the currently selected enumeration value up in the table.
     * When written to the database, the sort order will be set to the
     * currently displayed order. This and the down button allow you to set
     * the desired order.
     * @param e ActionEvent
     */
    void upEnumValButton_actionPerformed(ActionEvent e)
    {
        int row = enumTable.getSelectedRow();
        if (row == -1)
        {
            showError(labels.getString("RefListFrame.enumMovUpInfo"));
            return;
        }
        if (enumTableModel.moveUp(row))
        {
            enumTable.setRowSelectionInterval(row-1, row-1);
        }
        enumsChanged = true;
    }

    /**
     * Moves the currently selected enumeration value down in the table.
     * When written to the database, the sort order will be set to the
     * currently displayed order. This and the up button allow you to set
     * the desired order.
     * @param e ActionEvent
     */
    void downEnumValButton_actionPerformed(ActionEvent e)
    {
        int row = enumTable.getSelectedRow();
        if (row == -1)
        {
            showError(labels.getString("RefListFrame.enumMovDnInfo"));
            return;
        }
        if (enumTableModel.moveDown(row))
        {
            enumTable.setRowSelectionInterval(row+1, row+1);
        }
        enumsChanged = true;
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
     * Shows an error message in a JOptionPane and prints it to the error log
     * @param msg the error message.
     */
    public void showError(String msg)
    {
        log.error(msg);
        JOptionPane.showMessageDialog(this,
            AsciiUtil.wrapString(msg, 60), "Error!", JOptionPane.ERROR_MESSAGE);
    }


    private static class RefListFrame_enumComboBox_actionAdapter implements java.awt.event.ActionListener
    {
        EnumerationPanel adaptee;

        RefListFrame_enumComboBox_actionAdapter(EnumerationPanel adaptee) {
            this.adaptee = adaptee;
        }
        public void actionPerformed(ActionEvent e) {
            adaptee.enumComboBox_actionPerformed(e);
        }
}


    public Collection<DbEnum> getChanged()
    {
        ArrayList<DbEnum> enums = new ArrayList<>();
        for (int i = 0; i < enumComboBox.getItemCount(); i++)
        {
            enums.add(enumComboBox.getItemAt(i));
        }
        return enums;
    }

    public void resetChanged()
    {
        this.enumsChanged = false;
    }
}
