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
package decodes.dbeditor.sources;

import java.util.ResourceBundle;

import javax.swing.table.AbstractTableModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import decodes.db.Constants;
import decodes.db.DataSource;
import decodes.db.DataSourceList;
import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.dbeditor.SourcesListPanel;
import decodes.gui.TopFrame;

public class DataSourceListTableModel extends AbstractTableModel
{
    private static final Logger log = LoggerFactory.getLogger(DataSourceListTableModel.class);
    private static ResourceBundle generic = ResourceBundle.getBundle("decodes/resources/generic");
    private static ResourceBundle dbedit = ResourceBundle.getBundle("decodes/resources/dbedit");
    private String colNames[] =
    {
        generic.getString("name"),
        generic.getString("type"),
        dbedit.getString("SourcesListPanel.args"),
        dbedit.getString("SourcesListPanel.usedBy")
    };
    private SourcesListPanel panel;
    private DataSourceList theList;
    private final Database db;

    public DataSourceListTableModel(SourcesListPanel slp, Database db)
    {
        super();
        this.panel = slp;
        this.db = db;
        theList = db.dataSourceList;
        theList.countUsedBy();
    }


    public int getRowCount()
    {
        return theList.size();
    }

    public DataSource getObjectAt(int r)
    {
        return (DataSource)getRowObject(r);
    }

    public Object getRowObject(int r)
    {
        if (r >= 0 && r < getRowCount())
        {
            return theList.getList().get(r);
        }
        else return null;
    }

    public void addDataSource(DataSource ds)
    {
        theList.add(ds);
        fireTableDataChanged();
    }

    public void deleteObject(DataSource ds)
    {

        try
        {
            db.getDbIo().deleteDataSource(ds);
            theList.remove(ds);
            fireTableDataChanged();
        }
        catch(DatabaseException e)
        {
            if ( ds.getId() != Constants.undefinedId )
            {
                log.atError()
                   .setCause(e)
                   .log("Unable to delete DataSource {}.", ds);
                TopFrame.instance().showError(e.toString());
            }
        }
    }

    public int getColumnCount()
    {
        int r = colNames.length;
        return r;
    }

    public String getColumnName(int c)
    {
        return colNames[c];
    }

    public boolean isCellEditable(int r, int c)
    {
        return false;
    }

    public Object getValueAt(int r, int c)
    {
        DataSource ds = getObjectAt(r);
        if (ds == null)
        {
            return "";
        }
        else
        {
            return getDsColumn(ds, c);
        }
    }

    private static Object getDsColumn(DataSource ds, int c)
    {
        switch(c)
        {
            case 0: return ds.getName();
            case 1:
                return ds.dataSourceType == null ? "" : ds.dataSourceType;
            case 2:
                return ds.getDataSourceArgDisplay() == null ? "" : ds.getDataSourceArgDisplay();
            case 3:
                return ds.numUsedBy;
            default: return "";
        }
    }

    public void replace(DataSource oldOb, DataSource newOb)
    {
        theList.remove(oldOb);
        theList.add(newOb);
        fireTableDataChanged();
    }
}
