/*
 * Copyright 2025 OpenDCS Consortium and its Contributors
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package decodes.tsdb.groupedit;

import decodes.tsdb.DbIoException;
import decodes.tsdb.TsGroup;
import decodes.util.DecodesSettings;
import ilex.util.LoadResourceBundle;
import ilex.util.Logger;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dai.TsGroupDAI;
import org.slf4j.LoggerFactory;

import javax.swing.table.AbstractTableModel;
import java.util.*;

class TsGroupsSelectTableModel extends AbstractTableModel
{
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(TsGroupsSelectTableModel.class);
    private static String[] columnNames;
    private TsGroupListPanel panel;
    private ArrayList<TsGroup> theGroupList = new ArrayList<TsGroup>();
    private Map<TsGroup, Integer> compCount = new HashMap<TsGroup, Integer>();
    private String module;

    public TsGroupsSelectTableModel(TsGroupListPanel panel)
    {
        super();
        this.panel = panel;
        module = panel.module + ":" + "TSGroupsSelectTableModel";

        ResourceBundle groupResources = LoadResourceBundle.getLabelDescriptions(
                "decodes/resources/groupedit",
                DecodesSettings.instance().language);

        columnNames = new String[6];

        columnNames[0] = groupResources.getString("TsGroupsListSelectPanel.groupIdColumnLabel");
        columnNames[1] = groupResources.getString("TsGroupsListSelectPanel.nameColumnLabel");
        columnNames[2] = groupResources.getString("TsGroupsListSelectPanel.typeColumnLabel");
        columnNames[3] = groupResources.getString("TsGroupsListSelectPanel.descriptionColumnLabel");
        columnNames[4] = groupResources.getString("TsGroupsListSelectPanel.tsCountColumnLabel");
        columnNames[5] = groupResources.getString("TsGroupsListSelectPanel.compsUsedColumnLabel");
    }

    public void setTsGroupListFromDb()
    {
        theGroupList = getTsGroupListFromDb();
    }

//TODO Is the following method necessary?
    /**
     * This method is used from the TsGroupDefinition so that
     * we add the included sub group members from the TsGroup obj.
     * @param groupList
     */
    public void addSubgroups(ArrayList<TsGroup> groupList)
    {
        for (TsGroup g: groupList)
        {
            if (g != null)
                addTsGroup(g);
        }
    }

    /**
     * This method is used from the TsGroupDefinitionPanel when
     * adding new sub groups.
     *
     * @param tsGroupToAdd
     */
    public void addTsGroup(TsGroup tsGroupToAdd)
    {
        for(Iterator<TsGroup> it = theGroupList.iterator(); it.hasNext(); )
        {
            TsGroup group = it.next();
            if (tsGroupToAdd.getGroupId() == group.getGroupId())
            {
                it.remove();
                break;
            }
        }
        theGroupList.add(tsGroupToAdd);
        fireTableDataChanged();
    }

    public ArrayList<TsGroup> getTsGroupListFromDb()
    {
        ArrayList<TsGroup> tsGroups = new ArrayList<TsGroup>();
        if (panel.theTsDb != null)
        {
            try(TimeSeriesDAI tsDao = panel.theTsDb.makeTimeSeriesDAO();
                TsGroupDAI groupDAO = panel.theTsDb.makeTsGroupDAO())
            {
                tsDao.reloadTsIdCache();
                tsGroups = groupDAO.getTsGroupList(null);
                if (tsGroups == null)
                {
                    Logger.instance().warning(
                            module + " The Ts Group List is null.");
                    panel.showError("The Ts Group List is empty.");
                }
                for(TsGroup tsGroup : tsGroups)
                {
                    int count = groupDAO.countCompsUsingGroup(tsGroup.getGroupId());
                    this.compCount.put(tsGroup, count);
                }
            }
            catch (DbIoException ex)
            {
                String msg = module + " Can not get the Ts Group List "
                        + ex.getMessage();
                log.atInfo().log(msg,ex);
                panel.showError(msg);
            }
        }
        else
        {
            log.atError().log(module + " The TsDb obj is null.");
        }
        return tsGroups;
    }

    /**
     * Find out if we have a Ts Group with the given name on the list.
     */
    public boolean tsGroupExistsInList(String groupNameIn)
    {
        if (groupNameIn != null)
        {
            String groupName = groupNameIn.trim();
            for (TsGroup group : theGroupList)
            {
                String groupNameonList = group.getGroupName();
                if (groupNameonList != null)
                    groupNameonList = groupNameonList.trim();
                if (groupName.equalsIgnoreCase(groupNameonList))
                    return true;
            }
        }
        return false;
    }

    void modifyTsGroupList(TsGroup oldG, TsGroup newG)
    {
        int gIndex = theGroupList.indexOf(oldG);
        if (gIndex != -1)
        {
            theGroupList.set(gIndex, newG);
        } else
        {
            theGroupList.add(newG);
        }
        fireTableDataChanged();
    }

    //TODO the model shouldn't be doing database IO. The client should do this.
    public void refresh()
    {
        theGroupList.clear();
        theGroupList.addAll(getTsGroupListFromDb());
        fireTableDataChanged();
    }

    //TODO Where is this used from, and why?
    public ArrayList<TsGroup> getAllTsGroupsInList()
    {
        ArrayList<TsGroup> ret;
        ret = new ArrayList<TsGroup>();
        ret.addAll(theGroupList);
        return ret;
    }

    void deleteTsGroupAt(int index)
    {
        if (index >= 0 && index < theGroupList.size())
        {
            theGroupList.remove(index);
            fireTableDataChanged();
        }
    }

    //TODO Why does the client need the following. It should always delete by index.
    void deleteTsGroup(TsGroup tsGObj)
    {
        int ddIndex = theGroupList.indexOf(tsGObj);
        if (ddIndex != -1)
            theGroupList.remove(ddIndex);
        fireTableDataChanged();
    }

    public int getColumnCount()
    {
        return columnNames.length;
    }

    public String getColumnName(int col)
    {
        return columnNames[col];
    }

    public boolean isCellEditable(int r, int c)
    {
        return false;
    }

    public int getRowCount()
    {
        return theGroupList.size();
    }

    public TsGroup getTsGroupAt(int r)
    {
        return (TsGroup) getRowObject(r);
    }

    @Override
    public Object getValueAt(int row, int col)
    {
        TsGroup g = theGroupList.get(row);
        switch(col)
        {
            case 0:
                return g.getGroupId();

            case 1:
                return g.getGroupName();

            case 2:
                return g.getGroupType();

            case 3:
                return firstLine(g.getDescription());

            case 4:
                return g.getTsMemberList().size();

            case 5:
                return compCount.getOrDefault(g, 0);

            default:
                return "";
        }
    }

    /**
     * Trim the text down to the first CR, LF, period or 60 chars, whichever comes first.
     */
    private static String firstLine(String txt)
    {
        if (txt == null)
            return "";

        // split on CR, LF or period; limit=2 so we only do one split
        String first = txt.split("[\\r\\n\\.]", 2)[0];
        return first.length() > 60
                ? first.substring(0, 60)
                : first;
    }

    public Object getRowObject(int r)
    {
        return theGroupList.get(r);
    }

    public void replaceTsGroup(TsGroup modifiedGroup)
    {
        for(Iterator<TsGroup> it = theGroupList.iterator(); it.hasNext(); )
        {
            TsGroup group = it.next();
            if (modifiedGroup.getGroupId().equals(group.getGroupId()))
            {
                it.remove();
                continue;
            }

            TsGroup subgroup = null;
            for(TsGroup sg : group.getIncludedSubGroups())
                if (sg.getGroupId().equals(modifiedGroup.getGroupId()))
                {
                    subgroup = sg;
                    break;
                }
            if (subgroup != null)
            {
                group.getIncludedSubGroups().remove(subgroup);
                group.addSubGroup(modifiedGroup, 'A');
            }

            subgroup = null;
            for(TsGroup sg : group.getExcludedSubGroups())
                if (sg.getGroupId().equals(modifiedGroup.getGroupId()))
                {
                    subgroup = sg;
                    break;
                }
            if (subgroup != null)
            {
                group.getExcludedSubGroups().remove(subgroup);
                group.addSubGroup(modifiedGroup, 'S');
            }

            subgroup = null;
            for(TsGroup sg : group.getIntersectedGroups())
                if (sg.getGroupId().equals(modifiedGroup.getGroupId()))
                {
                    subgroup = sg;
                    break;
                }
            if (subgroup != null)
            {
                group.getIntersectedGroups().remove(subgroup);
                group.addSubGroup(modifiedGroup, 'I');
            }
        }
        theGroupList.add(modifiedGroup);
        fireTableDataChanged();
    }

    @Override
    public Class<?> getColumnClass(int col)
    {
        switch(col)
        {
            case 0: return Long.class;    // Group ID
            case 4:                     // TS Count
            case 5: return Integer.class;// Components used
            default:
                return String.class;     // name, type, description
        }
    }


}
