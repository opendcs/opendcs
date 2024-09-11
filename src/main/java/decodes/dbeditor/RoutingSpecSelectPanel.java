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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ResourceBundle;

import decodes.db.Database;

import decodes.dbeditor.routing.RSListTableModel;
import decodes.db.RoutingSpec;


@SuppressWarnings("serial")
public class RoutingSpecSelectPanel extends JPanel 
{
    private static final Logger log = LoggerFactory.getLogger(RoutingSpecSelectPanel.class);
    static ResourceBundle genericLabels = DbEditorFrame.getGenericLabels();
    static ResourceBundle dbeditLabels = DbEditorFrame.getDbeditLabels();
    private JTable rslTable;
    private RSListTableModel tableModel;
    private RoutingSpecListPanel parentPanel = null;
    private RoutingSpecSelectDialog parentDialog = null;

    /** Constructor. */
    public RoutingSpecSelectPanel()
    {
        try
        {
            tableModel = new RSListTableModel(Database.getDb());
            rslTable = new JTable(tableModel);
            rslTable.setAutoCreateRowSorter(true);
            rslTable.getSelectionModel().setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);

            rslTable.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseClicked(MouseEvent e)
                {
                    if (e.getClickCount() == 2)
                    {
                        if (parentDialog != null)
                        {
                            parentDialog.openPressed();
                        }
                        else if (parentPanel != null)
                        {
                            parentPanel.openPressed();
                        }
                    }
                }
            });
            jbInit();
        }
        catch (Exception ex)
        {
            log.atError()
               .setCause(ex)
               .log("Failed to setup ROutingSpecSelectPanel.");
        }
    }

    void setParentPanel(RoutingSpecListPanel parentPanel)
    {
        this.parentPanel = parentPanel;
    }
    void setParentDialog(RoutingSpecSelectDialog parentDialog)
    {
        this.parentDialog = parentDialog;
    }

    /** Initializes GUI components. */
    private void jbInit() throws Exception
    {
        this.setLayout(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane();
        this.add(scrollPane, BorderLayout.CENTER);
        scrollPane.getViewport().add(rslTable, null);
    }

    /** @return the currently selected RoutingSpec. */
    public RoutingSpec getSelection()
    {
        int idx = rslTable.getSelectedRow();
        if (idx == -1)
        {
            return null;
        }
        int modelRow = rslTable.convertRowIndexToModel(idx);
        return tableModel.getObjectAt(modelRow);
    }
    
    public void clearSelection()
    {
        rslTable.clearSelection();
    }

    public void refill()
    {
        tableModel.refill();
    }

    public void addRoutingSpec(RoutingSpec rs)
    {
        tableModel.addObject(rs);
    }

    public RSListTableModel getModel()
    {
        return this.tableModel;
    }

    public void deleteSelection()
    {
        int r = rslTable.getSelectedRow();
        if (r == -1)
        {
            return;
        }
        int modelRow = rslTable.convertRowIndexToModel(r);
        tableModel.deleteObjectAt(modelRow);
    }

    public void setSelection(String rsName)
    {
        for(int row=0; row < tableModel.getRowCount(); row++)
        {
            int modelRow = rslTable.convertRowIndexToModel(row);
            if (rsName.equals(tableModel.getObjectAt(modelRow).getName()))
            {
                rslTable.clearSelection();
                rslTable.setRowSelectionInterval(row, row);
                return;
            }
        }
    }
}
