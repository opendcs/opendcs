package org.opendcs.gui.tables;

import java.util.ArrayList;

import javax.swing.ListSelectionModel;

public final class Selection
{
    private Selection()
    {

    }   
    
    
    public static int[] getSelected(ListSelectionModel lsm)
    {
        ArrayList<Integer> selected = new ArrayList<>();
        
        for(int idx = 0, i = lsm.getMinSelectionIndex(); i <= lsm.getMaxSelectionIndex(); i++)
        {
            if (lsm.isSelectedIndex(i))
            {
                selected.add(i);
            }
        }
        int[] tmp = new int[selected.size()];
        for (int i = 0; i < tmp.length; i++)
        {
            tmp[i] = selected.get(i);
        }
        return tmp;
    }
}
