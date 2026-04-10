/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package decodes.tsdb.compedit.algotab;

import java.util.ArrayList;
import java.util.Set;

import javax.swing.table.AbstractTableModel;

import decodes.tsdb.DbCompAlgorithm;
import decodes.tsdb.DbIoException;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesDb;
import ilex.util.Pair;
import org.opendcs.utils.AlgorithmCatalogScanner;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;

public final class ExecClassTableModel extends AbstractTableModel
{
    private static final org.slf4j.Logger log = OpenDcsLoggerFactory.getLogger();
    private String colNames[] = {"Already Imported", "Name", "Exec Class", "Description"};

    private final ArrayList<Pair<Boolean,DbCompAlgorithm>> classlist = new ArrayList<>();
    private final TimeSeriesDb tsDb;


    public ExecClassTableModel(TimeSeriesDb tsDb)
    {
        this.tsDb = tsDb;
    }

    @Override
    public int getColumnCount()
    {
        int r = colNames.length;
        return r;
    }

    @Override
    public Class<?> getColumnClass(int index)
    {
        switch(index)
        {

            case 0: return Boolean.class;
            case 1: return String.class;
            case 2: return String.class;
            case 3: return String.class;
            default: return Object.class;
        }
    }

    public int indexOf(String selection)
    {
        for(int i=0; i<classlist.size(); i++)
            if (selection.equals(classlist.get(i).second.getName()))
                return i;
        return -1;
    }

    @Override
    public String getColumnName(int c)
    {
        return colNames[c];
    }

    @Override
    public int getRowCount()
    {
        return classlist.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        if (rowIndex < 0 || rowIndex >= classlist.size())
            return null;
        DbCompAlgorithm algo = classlist.get(rowIndex).second;
        switch (columnIndex)
        {
            case 0: return classlist.get(rowIndex).first;
            case 1: return algo.getName();
            case 2: return algo.getExecClass();
            case 3: return algo.getComment();
            default: return null;
        }
    }

    public DbCompAlgorithm getAlgoAt(int rowIndex)
    {
        if (rowIndex < 0 || rowIndex >= classlist.size())
        {
            return null;
        }
        else
        {
            return classlist.get(rowIndex).second;
        }
    }

    public Boolean getLoaded(int rowIndex)
    {
        if (rowIndex < 0 || rowIndex >= classlist.size())
        {
            return null;
        }
        else
        {
            return classlist.get(rowIndex).first;
        }
    }

    public void load() throws NoSuchObjectException
    {
        load(false);
    }

    public void load(boolean newOnly) throws NoSuchObjectException
    {
        try
        {
            Set<String> importedExecClasses = AlgorithmCatalogScanner.getImportedExecClasses(tsDb);
            for (DbCompAlgorithm algo : AlgorithmCatalogScanner.scanAvailableAlgorithms())
            {
                boolean alreadyImported = importedExecClasses.contains(algo.getExecClass());
                classlist.add(new Pair<>(alreadyImported, algo));
            }
            this.fireTableDataChanged();
        }
        catch (DbIoException ex)
        {
            log.atError()
               .setCause(ex)
               .log("Unable to search database for current algorithms.");
        }
    }

    public void removeAlgo(DbCompAlgorithm algoToRemove)
    {
        for (int i = 0; i < classlist.size(); i++)
        {
            Pair<Boolean,DbCompAlgorithm> pair = classlist.get(i);
            if (algoToRemove.getExecClass().equalsIgnoreCase(pair.second.getExecClass()))
            {
                classlist.remove(i);
                fireTableRowsDeleted(i, i);
                return;
            }
        }

    }
}
