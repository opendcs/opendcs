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
import javax.swing.*;
import java.util.ResourceBundle;

import ilex.util.LoadResourceBundle;
import ilex.util.Logger;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.RoutingSpec;
import decodes.dbeditor.routing.RSListTableModel;

/**
 * DBEDIT panel that shows a list of routing specs in the database.
 */
@SuppressWarnings("serial")
public class RoutingSpecListPanel extends JPanel implements ListOpsController
{
    static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
    static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();
    private ListOpsPanel listOpsPanel = new ListOpsPanel(this);
    private DbEditorFrame parent = null;
    private RoutingSpecSelectPanel rsSelectPanel = new RoutingSpecSelectPanel();

    /** Constructor. */
    public RoutingSpecListPanel()
    {
        rsSelectPanel.setParentPanel(this);

        try
        {
            jbInit();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    /**
     * Sets the parent frame object. Each list panel needs to know this.
     *
     * @param parent
     *            the DbEditorFrame
     */
    void setParent(DbEditorFrame parent)
    {
        this.parent = parent;
    }

    public RSListTableModel getModel()
    {
        return this.rsSelectPanel.getModel();
    }

    /** Initializes GUI components. */
    private void jbInit() throws Exception
    {
        JLabel titleLabel = new JLabel(dbeditLabels.getString("RoutingSpecListPanel.title"));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        this.setLayout(new BorderLayout());
        this.add(titleLabel, BorderLayout.NORTH);
        this.add(rsSelectPanel, BorderLayout.CENTER);
        this.add(listOpsPanel, BorderLayout.SOUTH);
    }

    /** @return type of entity that this panel edits. */
    public String getEntityType()
    {
        return dbeditLabels.getString("ListPanel.routingSpecEntity");
    }

    /** Called when the 'Open' button is pressed. */
    public void openPressed()
    {
        RoutingSpec ob = rsSelectPanel.getSelection();
        if (ob == null)
        {
            DbEditorFrame.instance().showError(
                LoadResourceBundle.sprintf(
                    dbeditLabels.getString("ListPanel.selectOpen"),
                    getEntityType()));
        }
        else
        {
            doOpen(ob);
        }
    }

    /** Called when the 'New' button is pressed. */
    public void newPressed()
    {
        String newName =
            JOptionPane.showInputDialog(
                LoadResourceBundle.sprintf(
                    dbeditLabels.getString("ListPanel.enterNewName"),
                    getEntityType()));
        if (newName == null)
        {
            return;
        }

        if (Database.getDb().routingSpecList.find(newName) != null)
        {
            DbEditorFrame.instance().showError(
                LoadResourceBundle.sprintf(
                    dbeditLabels.getString("ListPanel.alreadyExists"),
                    getEntityType()));
            return;
        }

        RoutingSpec ob = new RoutingSpec(newName);
        ob.untilTime = "now";
        doOpen(ob);
    }

    /** Called when the 'Copy' button is pressed. */
    public void copyPressed()
    {
        RoutingSpec ob = rsSelectPanel.getSelection();
        if (ob == null)
        {
            DbEditorFrame.instance().showError(
                LoadResourceBundle.sprintf(
                    dbeditLabels.getString("ListPanel.selectCopy"),
                    getEntityType()));
            return;
        }
        String newName = JOptionPane.showInputDialog(dbeditLabels
            .getString("ListPanel.enterCopyName"));
        if (newName == null)
        {
            return;
        }

        if (Database.getDb().routingSpecList.find(newName) != null)
        {
            DbEditorFrame.instance().showError(
                LoadResourceBundle.sprintf(
                    dbeditLabels.getString("ListPanel.alreadyExists"),
                    getEntityType()));
            return;
        }

        RoutingSpec newOb = ob.copy();
        newOb.setName(newName);
        newOb.clearId();
        try
        {
            newOb.write();
        }
        catch (DatabaseException e)
        {
            DbEditorFrame.instance().showError(
                LoadResourceBundle.sprintf(
                    genericLabels.getString("cannotSave"), getEntityType()
                        + " '" + newOb.getName() + "'", e.toString()));
            return;
        }
        rsSelectPanel.addRoutingSpec(newOb);
        doOpen(newOb);
    }

    /** Called when the 'Delete' button is pressed. */
    public void deletePressed()
    {
        RoutingSpec ob = rsSelectPanel.getSelection();
        if (ob == null)
        {
            DbEditorFrame.instance().showError(
                LoadResourceBundle.sprintf(
                    dbeditLabels.getString("ListPanel.selectDelete"),
                    getEntityType()));
            return;
        }

        DbEditorTabbedPane routingTabbedPane = parent
            .getRoutingSpecTabbedPane();
        DbEditorTab tab = routingTabbedPane.findEditorFor(ob);
        if (tab != null)
        {
            DbEditorFrame.instance().showError(
                LoadResourceBundle.sprintf(
                    dbeditLabels.getString("ListPanel.beingEdited"),
                    getEntityType()));
            return;
        }
        int r = JOptionPane.showConfirmDialog(this, LoadResourceBundle.sprintf(
            dbeditLabels.getString("ListPanel.confirmDeleteMsg"),
            getEntityType()), dbeditLabels
            .getString("ListPanel.confirmDeleteTitle"),
            JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION)
        {
            if (parent.getScheduleListPanel() != null)
            {
                parent.getScheduleListPanel().deleteForRs(ob);
            }
            try
            {
                Database.getDb().getDbIo().deleteRoutingSpec(ob);
            }
            catch(DatabaseException e)
            {
                DbEditorFrame.instance().showError(
                    RoutingSpecListPanel.dbeditLabels.getString("RoutingSpecListPanel.errorDelete")
                    + e.toString());
            }
            rsSelectPanel.deleteSelection();
        }
    }

    /** Called when the 'Refresh' button is pressed. */
    public void refreshPressed()
    {
        rsSelectPanel.refill();
    }

    /**
     * Opens an object in an Edit Panel.
     *
     * @param rs
     *            the object to be edited.
     */
    private void doOpen(RoutingSpec rs)
    {
        Logger.instance().debug3("RoutingSpecListPanel.doOpen(" + rs.getName() + ")");
        DbEditorTabbedPane dbtp = parent.getRoutingSpecTabbedPane();
        DbEditorTab tab = dbtp.findEditorFor(rs);
        if (tab != null)
        {
            Logger.instance().debug3("RoutingSpecListPanel.doOpen "
                + " already open!");
            dbtp.setSelectedComponent(tab);
        }
        else
        {
            RoutingSpecEditPanel newTab = new RoutingSpecEditPanel(rs);
            Logger.instance().debug3("RoutingSpecListPanel.doOpen calling setParent("
                + (parent == null ? "NULL" : "") + ")");

            newTab.setParent(parent);
            String title = rs.getName();
            dbtp.add(title, newTab);
            dbtp.setSelectedComponent(newTab);
        }
    }
}
