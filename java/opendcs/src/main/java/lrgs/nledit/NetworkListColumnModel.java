/*
*  $Id$
*
*  $Source$
*
*  $State$
*
*  $Log$
*  Revision 1.1  2008/04/04 18:21:15  cvs
*  Added legacy code to repository
*
*  Revision 1.1  2001/02/21 13:19:29  mike
*  Created nleditor
*
*/
package lrgs.nledit;

import javax.swing.*;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.event.*;

public class NetworkListColumnModel extends DefaultTableColumnModel
	implements ListSelectionListener
{
	ListSelectionModel selectionModel = null;

    public NetworkListColumnModel()
	{
		super();
		selectionModel = getSelectionModel();
		setColumnSelectionAllowed(true);
		selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		selectionModel.addListSelectionListener(this);
    }

	public void setListSelectionModel(ListSelectionModel lsm)
	{
		selectionModel = lsm;
	}

	public void valueChanged(ListSelectionEvent lse)
	{
		if (selectionModel != null && !lse.getValueIsAdjusting())
		{
System.out.println("new selection = " + selectionModel.getMinSelectionIndex());
		}
	}
}

//class ColumnSelector implements TableColumnModelListener
//{