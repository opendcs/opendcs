/*
*  $Id$
*
*  This is open-source software written by ILEX Engineering, Inc., under
*  contract to the federal government. You are free to copy and use this
*  source code for your own purposes, except that no part of the information
*  contained in this file may be claimed to be proprietary.
*
*  Except for specific contractual terms between ILEX and the federal 
*  government, this source code is provided completely without warranty.
*  For more information contact: info@ilexeng.com
*  
*  $Log$
*  Revision 1.7  2013/03/21 18:27:39  mmaloney
*  DbKey Implementation
*
*/
package decodes.tsdb.compedit;

import ilex.util.Logger;
import ilex.util.TextUtil;

import java.util.*;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;

import opendcs.dai.AlgorithmDAI;

import decodes.gui.SortingListTableModel;

import decodes.sql.DbKey;
import decodes.tsdb.*;
import decodes.util.DecodesSettings;

public class AlgoListTableModel 
	extends AbstractTableModel 
	implements SortingListTableModel 
{
	
	private String idLabel;
	private String nameLabel;
	private String execLabel;
	private String numCompsLabel;
	private String commentLabel;
	private String errMsg1;
	private String errMsg2;
	private String errMsg3;
	
//	Vector<DbCompAlgorithm> myvector = new Vector<DbCompAlgorithm>();
	ArrayList<AlgorithmInList> myList = new ArrayList<AlgorithmInList>();
	private String columnNames[];
	static int columnWidths[] = { 5, 20, 25, 8, 42};
	static char sortType[] = { 'i', 'S', 'S', 'i', 'S' };

	int sortedBy = -1;
	int minAlgoId = -1;
	
	private void fillLabels()
	{
		idLabel=CAPEdit.instance().compeditDescriptions.getString("AlgoListTableModel.ID");
		nameLabel = CAPEdit.instance().compeditDescriptions.getString("AlgoListTableModel.Name");
		execLabel= CAPEdit.instance().compeditDescriptions.getString("AlgoListTableModel.Exec");
		numCompsLabel= CAPEdit.instance().compeditDescriptions.getString("AlgoListTableModel.Comps");
		commentLabel= CAPEdit.instance().compeditDescriptions.getString("AlgoListTableModel.Comment");
		errMsg1=CAPEdit.instance().compeditDescriptions.getString("AlgoListTableModel.ErrMsg1");
		errMsg2=CAPEdit.instance().compeditDescriptions.getString("AlgoListTableModel.ErrMsg2");
		errMsg2=CAPEdit.instance().compeditDescriptions.getString("AlgoListTableModel.ErrMsg3");
	}
	
	public AlgoListTableModel()
	{
		super();
		fillLabels();
		columnNames = new String[]
			{ idLabel, nameLabel, execLabel, numCompsLabel, commentLabel };

		//Get the DbComp config - tsdb.conf file. This file will contain
		//the minAlgoId property which is used to determine the algorithms
		//that will be displayed on the list
		minAlgoId = DecodesSettings.instance().minAlgoId;
	}
	
	public void sortByColumn(int c)
	{
		Collections.sort(myList, new AlgorithmsListComparator(c, sortType[c]));
		sortedBy = c;
		fireTableDataChanged();
	}
	
	public void fill()
	{

		TimeSeriesDb tsdb = CAPEdit.instance().getTimeSeriesDb();
		AlgorithmDAI algoDao = tsdb.makeAlgorithmDAO();
		try
		{
			myList = algoDao.listAlgorithmsForGui();
		}
		catch(DbIoException ex)
		{
			CAPEdit.instance().getFrame().showError(
				"Cannot list algorithms: " + ex);
			myList = new ArrayList<AlgorithmInList>();
		}
		finally
		{
			algoDao.close();
		}
		if (sortedBy != -1)
			sortByColumn(sortedBy);
		for(Iterator<AlgorithmInList> ait = myList.iterator(); ait.hasNext();)
		{
			AlgorithmInList ai = ait.next();
			if (ai.getAlgorithmId().getValue() < minAlgoId)
				ait.remove();
		}
	}

	public Object getRowObject(int idx) 
	{
		return myList.get(idx);
	}
	
	public DbCompAlgorithm getRowAlgorithm(int idx)
	{
		TimeSeriesDb tsdb = CAPEdit.instance().getTimeSeriesDb();
		AlgorithmDAI algorithmDAO = tsdb.makeAlgorithmDAO();

		try
		{
			AlgorithmInList ail = myList.get(idx);
			return algorithmDAO.getAlgorithmById(ail.getAlgorithmId());
		}
		catch(Exception ex)
		{
			CAPEdit.instance().getFrame().showError(
				"Cannot list algorithms: " + ex);
			myList = new ArrayList<AlgorithmInList>();
			return null;
		}
		finally
		{
			algorithmDAO.close();
		}
	}

	public int getRowCount() 
	{
		return myList.size();
	}

	public int getColumnCount() {
		return columnNames.length;
	}
	
	public String getColumnName(int col)
	{
		return columnNames[col];
	}

	public Object getValueAt(int rowIndex, int columnIndex) 
	{
		AlgorithmInList ail = myList.get(rowIndex);
		return getNlColumn(ail, columnIndex);
	}
	
	public static String getNlColumn(AlgorithmInList obj, int columnIndex) 
	{
		switch(columnIndex)
		{
		case 0: return "" + obj.getAlgorithmId();
		case 1: return obj.getAlgorithmName();
		case 2: return obj.getExecClass();
		case 3: return "" + obj.getNumCompsUsing();
		case 4: return obj.getDescription();
		default: return "";
		}
	}

	public boolean existsInList(String name)
	{
		for(AlgorithmInList dca : myList)
		{
			if (name.equalsIgnoreCase(dca.getAlgorithmName()))
				return true;
		}
		return false;
	}
	
	public String getNameById(DbKey id)
	{
		for(AlgorithmInList dca : myList)
		{
			if (dca.getAlgorithmId().equals(id))
				return dca.getAlgorithmName();
		}
		return "";
	}
}
class AlgorithmsListComparator implements Comparator<AlgorithmInList>
{
	int column;
	char sortType;

	public AlgorithmsListComparator(int column, char sortType)
	{
		this.column = column;
		this.sortType = sortType;
	}

	/**
	 * Compare the eqMod names of the specified type.
	 */
	public int compare(AlgorithmInList ds1, AlgorithmInList ds2)
	{
		if (ds1 == ds2)
			return 0;
		String s1 = AlgoListTableModel.getNlColumn(ds1, column);
		String s2 = AlgoListTableModel.getNlColumn(ds2, column);
		if (sortType == 'i')
		{
			try
			{
				int i1 = Integer.parseInt(s1.trim());
				int i2 = Integer.parseInt(s2.trim());
				return i1 - i2;
			}
			catch(Exception ex) {}
		}

		return s1.compareToIgnoreCase(s2);
	}

	public boolean equals(Object ob)
	{
		return false;
	}
}
