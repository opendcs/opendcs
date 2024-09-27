/**
 * Copyright 2024 The OpenDCS Consortium and contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package decodes.dbeditor;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.Collections;
import java.util.ResourceBundle;

import decodes.gui.*;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.NetworkList;
import decodes.db.NetworkListEntry;
import decodes.db.NetworkListList;
import decodes.dbeditor.networklist.NetlistListTableModel;
import ilex.util.LoadResourceBundle;

/**
Dbedit panel that shows a sorting list of network lists in the database.
*/
public class NetlistListPanel extends JPanel
    implements ListOpsController
{
    private static final Logger log = LoggerFactory.getLogger(NetlistListPanel.class);
    static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
    static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();

    BorderLayout borderLayout1 = new BorderLayout();
    JLabel jLabel1 = new JLabel();
    JPanel jPanel1 = new JPanel();
    BorderLayout borderLayout2 = new BorderLayout();
    JScrollPane jScrollPane1 = new JScrollPane();
    ListOpsPanel listOpsPanel = new ListOpsPanel(this);

    JTable netlistListTable;
    NetlistListTableModel tableModel;
    DbEditorFrame parent;

    /** Constructor. */
    public NetlistListPanel()
    {
        try
        {
            tableModel = new NetlistListTableModel(Database.getDb());
            netlistListTable = new JTable(tableModel);
            netlistListTable.setAutoCreateRowSorter(true);
            netlistListTable.getSelectionModel().setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);
            netlistListTable.addMouseListener(
                new MouseAdapter()
                {
                    public void mouseClicked(MouseEvent e)
                    {
                        if (e.getClickCount() == 2)
                        {
                               openPressed();
                        }
                    }
                });
            jbInit();
        }
        catch(Exception ex)
        {
            log.atError()
               .setCause(ex)
               .log("Failed to create NetlistPanel.");
        }
    }

    /**
      Sets the parent frame object. Each list panel needs to know this.
      @param parent the DbEditorFrame
    */
    void setParent(DbEditorFrame parent)
    {
        this.parent = parent;
    }

    /** Initializes GUI components. */
    private void jbInit() throws Exception
    {
        jLabel1.setHorizontalAlignment(SwingConstants.CENTER);
        jLabel1.setText(dbeditLabels.getString("NetlistListPanel.DefinedLists"));
        this.setLayout(borderLayout1);
        jPanel1.setLayout(borderLayout2);
        this.add(jLabel1, BorderLayout.NORTH);
        this.add(jPanel1, BorderLayout.CENTER);
        jPanel1.add(jScrollPane1, BorderLayout.CENTER);
        this.add(listOpsPanel, BorderLayout.SOUTH);
        jScrollPane1.getViewport().add(netlistListTable, null);
    }

    private NetworkList getSelection()
    {
        int idx = netlistListTable.getSelectedRow();
        if (idx == -1)
        {
            return null;
        }
        int modelRow = netlistListTable.convertRowIndexToModel(idx);
        return tableModel.getObjectAt(modelRow);
    }

    /** @return type of entity that this panel edits. */
    public String getEntityType()
    {
        return dbeditLabels.getString("NetlistListPanel.NetListsText");
    }

    /** Called when the 'Open' button is pressed. */
    public void openPressed()
    {
        NetworkList nl = getSelection();
        if (nl == null)
        {
            TopFrame.instance().showError(
                    LoadResourceBundle.sprintf(
                    dbeditLabels.getString("NetlistListPanel.OpenError"),
                    getEntityType())
                );
        }
        else
        {
            doOpen(nl);
        }
    }

    /** Called when the 'New' button is pressed. */
    public void newPressed()
    {
        String newName = JOptionPane.showInputDialog(
                dbeditLabels.getString("NetlistListPanel.NewPrompt")+
                " " + getEntityType() + ":");
        if (newName == null)
        {
            return;
        }

        if (Database.getDb().networkListList.find(newName) != null)
        {
            TopFrame.instance().showError(
            LoadResourceBundle.sprintf(
            dbeditLabels.getString("NetlistListPanel.NewError")
            , getEntityType()));
            return;
        }

        NetworkList ob = new NetworkList(newName);
        Database.getDb().networkListList.add(ob);
        tableModel.fireTableDataChanged();
        doOpen(ob);
    }

    /** Called when the 'Copy' button is pressed. */
    public void copyPressed()
    {
        NetworkList nl = getSelection();
        if (nl == null)
        {
            TopFrame.instance().showError(
                    LoadResourceBundle.sprintf(
                    dbeditLabels.getString("NetlistListPanel.CopyError1"),
                    getEntityType()));
            return;
        }
        String newName = JOptionPane.showInputDialog(
            dbeditLabels.getString("NetlistListPanel.CopyPrompt"));
        if (newName == null)
        {
            return;
        }

        if (Database.getDb().networkListList.find(newName) != null)
        {
            TopFrame.instance().showError(
                    LoadResourceBundle.sprintf(
                    dbeditLabels.getString("NetlistListPanel.NewError")
                    , getEntityType()));
            return;
        }

        NetworkList ob = nl.copy();
        ob.clearId();
        ob.name = newName;
        try
        {
            ob.write();
        }
        catch(DatabaseException e)
        {
            TopFrame.instance().showError(
                dbeditLabels.getString("NetlistListPanel.CopyError2")
                + ob.name + "': " + e);
            return;
        }
        tableModel.addNetworkList(ob);
        doOpen(ob);
    }

    /** Called when the 'Delete' button is pressed. */
    public void deletePressed()
    {
        NetworkList nl = getSelection();
        if (nl == null)
        {
            TopFrame.instance().showError(
                    LoadResourceBundle.sprintf(
                    dbeditLabels.getString("NetlistListPanel.DeleteError"),
                    getEntityType()));
            return;
        }

        DbEditorTabbedPane netlistTabbedPane = parent.getNetworkListTabbedPane();
        DbEditorTab tab = netlistTabbedPane.findEditorFor(nl);
        if (tab != null)
        {
            TopFrame.instance().showError(
                LoadResourceBundle.sprintf(
                dbeditLabels.getString("NetlistListPanel.DeleteError1")
                , getEntityType()));
            return;
        }
        int r = JOptionPane.showConfirmDialog(this,
                dbeditLabels.getString("NetlistListPanel.DeletePrompt1")
                +" " + getEntityType()+ " " + nl.name +
                "?", dbeditLabels.getString("NetlistListPanel.DeletePrompt2")
                , JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION)
        {
            tableModel.deleteObject(nl);
        }
    }

    public void refreshPressed()
    {
    }

    /**
      Opens an object in an Edit Panel.
      @param nl the object to be edited.
    */
    private void doOpen(NetworkList nl)
    {
        DbEditorTabbedPane dbtp = parent.getNetworkListTabbedPane();
        DbEditorTab tab = dbtp.findEditorFor(nl);
        if (tab != null)
        {
            dbtp.setSelectedComponent(tab);
        }
        else
        {
            NetlistEditPanel newTab = new NetlistEditPanel(nl);
            newTab.setParent(parent);
            String title = nl.name;
            dbtp.add(title, newTab);
            dbtp.setSelectedComponent(newTab);
        }
    }
}
