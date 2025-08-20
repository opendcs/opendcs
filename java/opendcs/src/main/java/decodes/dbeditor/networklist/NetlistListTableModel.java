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
package decodes.dbeditor.networklist;

import java.util.ResourceBundle;

import javax.swing.table.AbstractTableModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import decodes.db.Database;
import decodes.db.DatabaseException;
import decodes.db.NetworkList;
import decodes.db.NetworkListList;
import decodes.gui.TopFrame;

public class NetlistListTableModel extends AbstractTableModel
{
    private static final Logger log = LoggerFactory.getLogger(NetlistListTableModel.class);
    private static final ResourceBundle generic = ResourceBundle.getBundle("decodes/resources/generic");
    private static final ResourceBundle dbedit = ResourceBundle.getBundle("decodes/resources/dbedit");

    static String columnNames[] =
    {
        dbedit.getString("NetlistListPanel.TableColumn1"),
        dbedit.getString("NetlistListPanel.TableColumn2"),
        dbedit.getString("NetlistListPanel.TableColumn3")
    };

    private NetworkListList theList;
    private final Database db;

    public NetlistListTableModel(Database db)
    {
        super();
        this.db = db;
        theList = db.networkListList;
        refill();
    }

    void refill()
    {
    }

    public int getRowCount()
    {
        return theList.size();
    }

    public int getColumnCount()
    {
        return columnNames.length;
    }

    public String getColumnName(int col)
    {
        return columnNames[col];
    }

    public NetworkList getObjectAt(int r)
    {
        return (NetworkList)getRowObject(r);
    }

    public Object getRowObject(int r)
    {
        if (r >= 0 && r < getRowCount())
        {
            return theList.getList().elementAt(r);
        }
        else
        {
            return null;
        }
    }

    public void addNetworkList(NetworkList ob)
    {
        theList.add(ob);
        fireTableDataChanged();
    }

    public void deleteObject(NetworkList ob)
    {

        try
        {
            db.getDbIo().deleteNetworkList(ob);
            theList.remove(ob);
            fireTableDataChanged();
        }
        catch(DatabaseException e)
        {
            log.atError()
               .setCause(e)
               .log("Unable to delete network list.");
            TopFrame.instance().showError(
                "Error trying to delete network list: " + e.toString());
        }

    }

    public Object getValueAt(int r, int c)
    {
        NetworkList ob = getObjectAt(r);
        if (ob == null)
        {
            return "";
        }
        else
        {
            return getNlColumn(ob, c);
        }
    }

    public static Object getNlColumn(NetworkList ob, int c)
    {
        switch(c)
        {
            case 0: return ob.name;
            case 1: return ob.transportMediumType;
            case 2: return ob.networkListEntries.size();
            default: return "";
        }
    }

    public void replace(NetworkList oldOb, NetworkList newOb)
    {
        theList.remove(oldOb);
        theList.add(newOb);
        fireTableDataChanged();
    }
}
