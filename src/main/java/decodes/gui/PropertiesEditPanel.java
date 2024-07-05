package decodes.gui;
import java.awt.*;

import javax.swing.*;
import javax.swing.table.*;

import org.cobraparser.html.domimpl.HTMLElementBuilder.P;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.TreeSet;
import java.util.Enumeration;

import javax.swing.border.*;

import decodes.gui.properties.PropertiesEditPanelController;
import decodes.gui.properties.PropertiesTableModel;
import decodes.util.DynamicPropertiesOwner;
import decodes.util.PropertiesOwner;
import decodes.util.PropertySpec;

import java.awt.event.*;

import ilex.util.Logger;
import ilex.util.StringPair;
import ilex.util.TextUtil;

/**
 * A panel that allows you to edit a group of Properties.
*
* @see PropertiesEditDialog
*/
@SuppressWarnings("serial")
public class PropertiesEditPanel extends JPanel
{
    private static ResourceBundle genericLabels = null;
    private JScrollPane jScrollPane1 = new JScrollPane();
    private JTable propertiesTable;
    private TitledBorder titledBorder1;
    private JButton editButton = new JButton();
    private JButton addButton = new JButton();
    private GridBagLayout gridBagLayout1 = new GridBagLayout();
    private PropertiesEditController controller = null;


    /** Will be true after any changes were made. */
    public boolean changesMade;

    /**
     * Constructs a PropertiesEditPanel for the passed Properties set.
    *
    * @param properties The properties set to edit.
    * @param canAddAndDelete Is the user allowed to manipulate what properties on in the list.
    */
    private PropertiesEditPanel(TableModel model, boolean canAddAndDelete, PropertiesEditController controller)
    {
        this.controller = controller;
        try
        {
            genericLabels = PropertiesEditDialog.getGenericLabels();
            //model, new int[]{30, 70})
            propertiesTable = new JTable(model);

            propertiesTable.setAutoCreateRowSorter(true);
            propertiesTable.getTableHeader().setReorderingAllowed(false);
            // Set column renderer so that tooltip is property description
            TableColumn col = propertiesTable.getColumnModel().getColumn(0);
            col.setCellRenderer(new ttCellRenderer());
            col.setPreferredWidth(30);
            col = propertiesTable.getColumnModel().getColumn(1);
            col.setCellRenderer(new ttCellRenderer());
            col.setPreferredWidth(70);
            jbInit(canAddAndDelete);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        changesMade = false;
    }

    public void setTitle(String title)
    {
        titledBorder1.setTitle(title);
    }

    /*
    * This internal class is used to add tooltip for known properties in the
    * table
    */
    class ttCellRenderer extends DefaultTableCellRenderer
    {
        public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int col)
        {
            JLabel cr = (JLabel) super.getTableCellRendererComponent(table, value, isSelected,
                hasFocus, row, col);
            cr.setOpaque(false);
            PropertiesTableModel model = (PropertiesTableModel)table.getModel();
            HashMap<String, PropertySpec> propHash = model.getPropHash();
            if (propHash != null)
            {
                // property name is in column 0
                int modelRow = table.convertRowIndexToModel(row);
                String pn = ((String) model.getValueAt(modelRow, 0)).toUpperCase();
                PropertySpec ps = propHash.get(pn);
                cr.setToolTipText(ps != null ? ps.getDescription() : "");
            }
            if (value instanceof Color)
            {
                Color c = (Color)value;
                cr.setOpaque(true);
                cr.setBackground(c);
                cr.setText("0x" + Integer.toHexString(c.getRGB()).substring(2));
            }
            return cr;
        }
    }

    /** Initializes GUI components. */
    private void jbInit(boolean canAddAndDelete) throws Exception
    {
        titledBorder1 = new TitledBorder(
            BorderFactory.createLineBorder(new Color(153, 153, 153), 2),
            genericLabels.getString("properties"));
        this.setLayout(gridBagLayout1);
        this.setBorder(titledBorder1);
        JButton deleteButton = new JButton(genericLabels.getString("delete"));
        deleteButton.addActionListener(e ->
        {
            int r = propertiesTable.getSelectedRow();
            if (r != -1)
            {
                final int modelRow = propertiesTable.convertRowIndexToModel(r);
                new SwingWorker<Void,Void>()
                {

                    @Override
                    protected Void doInBackground() throws Exception
                    {
                        controller.deleteButton_actionPerformed(modelRow);
                        return null;
                    }

                }.execute();

            }

        });
        addButton.setText(genericLabels.getString("add"));
        addButton.addActionListener(controller::addButton_actionPerformed);
        editButton.setText(genericLabels.getString("edit"));
        editButton.addActionListener(e -> fireEditRow());
        this.add(jScrollPane1,
            new GridBagConstraints(0, 0, 1, 3, 1.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(2, 4, 2, 4), 0, 0));
        if (canAddAndDelete)
            this.add(addButton,
                new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                    new Insets(2, 4, 2, 4), 20, 0));
        this.add(editButton,
            new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(2, 4, 2, 4), 20, 0));
        if (canAddAndDelete)
            this.add(deleteButton,
                new GridBagConstraints(1, 2, 1, 1, 0.0, 1.0,
                    GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
                    new Insets(2, 4, 2, 4), 20, 0));
        jScrollPane1.setViewportView(propertiesTable);

        propertiesTable.addMouseListener(new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                if (e.getClickCount() == 2)
                {
                    fireEditRow();
                }
            }
        } );

    }


    /**
     * Sets the owner dialog (if there is one).
    *
    * @param dlg
    *            the owner dialog
    */
    public void setOwnerDialog(JDialog dlg)
    {
        this.controller.setOwnerDialog(dlg);

    }

    /**
     * Sets the owner frame.
    *
    * @param frm
    *            the owner frame
    */
    public void setOwnerFrame(JFrame frm)
    {
        this.controller.setOwnerFrame(frm);
    }

    public PropertiesTableModel getModel()
    {
        return controller.getModel();
    }

    private void fireEditRow()
    {
        int tableRow = propertiesTable.getSelectedRow();
        if (tableRow == -1)
            return ;
        //Get the correct row from the table model
        final int modelRow = propertiesTable.convertRowIndexToModel(tableRow);
        new SwingWorker<Void,Void>()
        {

            @Override
            protected Void doInBackground() throws Exception
            {
                controller.editPressed(modelRow);
                return null;
            }

        }.execute();
    }

    public static PropertiesEditPanel from(Properties properties)
    {
        return from(properties, false);
    }


    public static PropertiesEditPanel from(Properties properties, boolean canAddDelete)
    {
        PropertiesTableModel model = new PropertiesTableModel(properties);
        return from(model, canAddDelete);
    }

    public static PropertiesEditPanel from(PropertiesTableModel model, boolean canAddDelete )
    {
        PropertiesEditPanelController controller = new PropertiesEditPanelController(model, null);
        PropertiesEditPanel ret = new PropertiesEditPanel(model, canAddDelete, controller);
        controller.setView(ret);
        return ret;
    }


    public static interface PropertiesEditController
    {
        void editPressed(int modelRow);
        void deleteButton_actionPerformed(int modelRow);
        void addButton_actionPerformed(ActionEvent e);
        void setOwnerDialog(JDialog dlg);
        /**
         * Sets the owner frame.
        *
        * @param frm
        *            the owner frame
        */
        void setOwnerFrame(JFrame frm);
        PropertiesTableModel getModel();
    }
}
