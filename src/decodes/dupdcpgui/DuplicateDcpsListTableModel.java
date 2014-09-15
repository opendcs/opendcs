package decodes.dupdcpgui;

import ilex.util.EnvExpander;
import ilex.util.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import lrgs.common.DcpAddress;
import decodes.db.Constants;
import decodes.db.Database;
import decodes.db.NetworkList;
import decodes.db.NetworkListEntry;
import decodes.db.Platform;
import decodes.db.Site;
import decodes.dcpmon_old.DcpGroup;
import decodes.dcpmon_old.DcpGroupList;
import decodes.gui.SortingListTableModel;
import decodes.util.Pdt;
import decodes.util.PdtEntry;
import decodes.util.hads.Hads;
import decodes.util.hads.HadsEntry;

/**
 * This class model fills out the Duplicate DCPs GUI.
 * It will find out all the duplicate DCPs that are in
 * Multiple District.
 * The list will show all DCPs that are in all the Network lists
 * used by the DCP Monitor.
 * The Duplicated once will show two or more districts in the 
 * "Duplicated In" column. The DCPs that are not duplicated will show
 * one district in the "Duplicated In" column.
 */
class DuplicateDcpsListTableModel 
	extends AbstractTableModel 
	implements SortingListTableModel
{
	private String module;
	private static String[] columnNames;
	private DuplicateDcpsListFrame parent;
	private Vector<DuplicateDcp> vec;
	private int sortColumn = -1;
	private ArrayList<String> groupNames;
	private HashMap<DcpAddress,ControllingDistrict> controlDistList;
	private Pdt pdt = null;
	private Hads hads = null;
	
	private String corpsDcpName;
	private String corpsDesc;
	private String pdtOwner;
	private String pdtDesc;
	private String nwsCode;
	private String nwsDesc;
	
	private final String UNRESOLVED =  "Unresolved";
		
	/** Constructor */
	public DuplicateDcpsListTableModel(DuplicateDcpsListFrame parent,
		ArrayList<String> groupNames,
		HashMap<DcpAddress,ControllingDistrict> controlDistList)
	{
		super();
		this.parent = parent;
		this.groupNames = groupNames;//Network List names
		this.controlDistList = controlDistList;//controlling districts list 
		module = parent.module + ":" + "DuplicateDcpsListTableModel";
		columnNames = new String[9];
		columnNames[0] = parent.goesAddrColumn;
		columnNames[1] = parent.duplicatedInColumn;
		columnNames[2] = parent.controllingDistColumn;
		columnNames[3] = parent.nwsCodeColumn;
		columnNames[4] = parent.pdtOwnerColumn;
		columnNames[5] = parent.corpsDcpNameColumn;
		columnNames[6] = parent.corpsDescriptionColumn;
		columnNames[7] = parent.pdtDescColumn;
		columnNames[8] = parent.nwsDescColumn;
		getDupDcpsList();
	}

	private void getDupDcpsList()
	{
		vec = null;
		Database db = Database.getDb();
		//Load PDT if its file path was giving as argument - it is used to 
		//fill out the PDT Owner and PDT Description
		String pdtFilePath = DuplicateDcpsList.getPdtFilePath(); 
		if (pdtFilePath != null && !pdtFilePath.equals(""))
		{
			initializePdt(pdtFilePath);
		}
		//Load NWSHB5 if its file path was giving as argument - it is used to 
		//fill out the NWShb5 Code and NWSHB5 Description
		String hadsFilePath = DuplicateDcpsList.getNwsFile();
		if (hadsFilePath != null && !hadsFilePath.equals(""))
		{
			initializeHads(hadsFilePath);
		}
		//Hash Map of dcpAddress and DuplicateDcp obj
		HashMap<DcpAddress, DuplicateDcp> dupDcpsHash = 
			new HashMap<DcpAddress, DuplicateDcp>();
		
		//Loop through the groupNames (network list names) and find
		//a network list for each name, there must be a network list
		//for each group name on the database
		for (String name : groupNames)//these are network list names
		{
			NetworkList nl = db.networkListList.getNetworkList(name);
			if(nl == null)
				continue;
			//Now create a list with all the duplicate dcp addresses
			//as well as the non duplicate 
			//that are found on these network lists
			for(Iterator it = nl.iterator(); it.hasNext(); )
			{	//Loop through all platforms in nl.iterator() 
				//Each NetworkListEntry contains a DCP Address which
				//belongs to a platform
				NetworkListEntry nle = (NetworkListEntry)it.next();
				if (nle != null)
				{	//Get the dcp address
					DcpAddress dcpAddress = new DcpAddress(nle.transportId);
					corpsDcpName = "";
					corpsDesc = "";
					pdtOwner = "";
					pdtDesc = "";
					nwsCode = "";
					nwsDesc = "";
					//Duplicated In
					//	Calculated later
					//Controlling Office
					String controlOffice = findControllingOffice(dcpAddress);
					//NWSHB5 code and NWS Desc
					findNWSInfo(dcpAddress);
					//PDT onwer "agency" and PDT description
					findPDTInfo(dcpAddress);
					//Corps Dcp Name - platform name and Corps Description
					findCorpsInfo(dcpAddress,
							controlOffice, nle.getDescription(), nle.getPlatformName());
					//Figure out if this is a duplicate dcp address
					DuplicateDcp dup = 
						(DuplicateDcp)dupDcpsHash.get(dcpAddress);
					if (dup != null)
					{	//verify if we have this duplicate or not
						//dupDcpsHash contains a copy of every duplicate dcp
						//address as well as all the duplicates
						//OK this is a duplicate dcp address
						String duplicatedIn = dup.getDuplicatedIn();
						if (duplicatedIn.length() > 0)
							duplicatedIn = duplicatedIn + ", ";
						duplicatedIn = duplicatedIn + 
											parseDistrictName(name);
						dup.setDuplicatedIn(duplicatedIn);
						//find if there is a controlling district or not
						dup.setControllingDist(controlOffice);
						//Set it to duplicate
						dup.setDuplicatedDcp(true);
						//if not a controlling district - append the 
						//dcp names from the network list
						//This is not possible- WE ARE READING from
						//the SQL data base so we have only one
						//dcp name
//							if (controlOffice != null && 
//								controlOffice.equalsIgnoreCase(UNRESOLVED))
//							{
//								String dcpName = dup.getDcpName();
//								if (dcpName.length() > 0)
//									dcpName = dcpName + ", ";
//								dcpName = dcpName + nle.platformName;
//								dup.setDcpName(dcpName);
//							}
//						}
					}
					else
					{
						//Add it to the dupDcpsHash but NOT as a duplicate
						dup = new DuplicateDcp(corpsDcpName,
								dcpAddress, corpsDesc, parseDistrictName(name),
								parseDistrictName(name), nwsCode, pdtOwner, 
								pdtDesc, nwsDesc, false);
						dupDcpsHash.put(dcpAddress, dup);
					}
				}
			}
		}
		//debugLists(dupDcpAddressList, dcpAddressList);
		vec = new Vector<DuplicateDcp>(dupDcpsHash.values());
		sortByColumn(2);//Sort by controlling office - unresolved first
		System.out.println("DONE");
	}

	/**
	 * @param pdtFilePath
	 */
	private void initializePdt(String pdtFilePath)
	{
		pdt = Pdt.instance();
		File pdtFile = new File(EnvExpander.expand(pdtFilePath));
		pdt.load(pdtFile);
	}

	/**
	 * @param hadsFilePath
	 */
	private void initializeHads(String hadsFilePath)
	{
		hads = Hads.instance();
		File hadsFile = new File(EnvExpander.expand(hadsFilePath));
		hads.load(hadsFile);
	}
	
	/**
	 * This method uses the PDT file to get a description
	 * and pdt owner for the given dcp address.
	 * 
	 * @param dcpAddress
	 *
	 */
	private void findPDTInfo(DcpAddress dcpAddress)
	{
		if (pdt != null)
		{
			PdtEntry pdtEntry = pdt.find(dcpAddress);
			if (pdtEntry != null)
			{
				pdtDesc = pdtEntry.description;
				pdtOwner = pdtEntry.agency;
			}
		}
	}
	
	/**
	 * This uses the NWS file to get the nwshb5 code.
	 * 
	 * @param dcpAddress
	 */
	private void findNWSInfo(DcpAddress dcpAddress)
	{
		if (hads != null)
		{
			HadsEntry hadsEntry = hads.find(dcpAddress);
			if (hadsEntry != null)
			{
				nwsCode = hadsEntry.dcpName;
				nwsDesc = hadsEntry.description;
			}
		}
	}
	
	private String findControllingOffice(DcpAddress dcpAddress)
	{
		String district = UNRESOLVED;
		if (controlDistList != null)
		{
			ControllingDistrict cd;
			cd = (ControllingDistrict)controlDistList.get(dcpAddress);
			if (cd != null)
			{
				district = cd.getDistrict();
			}	
		}
		return district;
	}
	
	private void findCorpsInfo(DcpAddress dcpAddress,
			String controlOffice, String nleDescription, 
			String nlePlatformName)
	{
		boolean dcpInPlat = false;
		Database db = Database.getDb();
		Platform p = 
			db.platformList.findPlatform(
				Constants.medium_Goes,// nl.transportMediumType 
				dcpAddress.toString(), new Date());
		if (p != null)
		{	//Get description from Platform
			dcpInPlat = true;
			corpsDesc = p.description;
			if (corpsDesc == null || corpsDesc.equals(""))
			{	//No description in Platform - get it from 
				//Site, need to read first the complete platform
//				try
//				{
//					p.read();//this really slow down the GUI
					Site pSite = p.site;
					if (pSite != null)
						corpsDesc = pSite.getDescription();		
//				} catch (DatabaseException e)
//				{
//					Logger.instance().warning( module +
//							"Can not read Platform" +
//							"from DB for dcpAddress = " + dcpAddress +
//							", " + e.getMessage());
//				}
			}
		}
		
		//Find Corps Dcp Name from the network list that this dcp address
		//belongs to.
		//Find out if there is a controlling office for this DCP address
		if (controlOffice != null && 
				!controlOffice.equalsIgnoreCase(UNRESOLVED))
		{	// 
			//if the dcp address is on the SQL database - needs to append
			//RIVERGAGES-DAS in order to find this group in the DcpGroupList
			String groupN = controlOffice;
			if (dcpInPlat == true)
			{
				groupN = controlOffice + 
								DuplicateDcpsList.GROUP_DELIMITER_NAME;
			}
			DcpGroup grp1 = 
				DcpGroupList.instance().getGroup(groupN);
			if (grp1 != null)
			{
				//if this dcp address is not in the SQL database get the 
				//description from the network list entry that this belongs to
				if ((corpsDesc == null || corpsDesc.equals("")) && 
						dcpInPlat == false)
				{
					corpsDesc = grp1.getDcpDescription(dcpAddress);
				}
				corpsDcpName = grp1.getDcpName(dcpAddress);
			}
		}
		//If no control office - get description from current network list 
		//entry, if this dcp address does not belong to an SQL Platform record.
		if ((corpsDesc == null || corpsDesc.equals("")) && 
				dcpInPlat == false)
		{
			corpsDesc = nleDescription;
		}
		//If no control office get Dcp Name from current network list entry
		if (corpsDcpName == null || corpsDcpName.equals(""))
		{
			corpsDcpName = nlePlatformName;
		}
	}
	
	/**
	 * Return a vector with all rows in the model
	 * @return
	 */
	public Vector<DuplicateDcp> getList()
	{
		return vec;
	}
	
	/**
	 * @param dupDcpAddressList
	 * @param dcpAddressList
	 */
//	private void debugLists(ArrayList<String> dupDcpAddressList, 
//		HashMap<String, String> dcpAddressList)
//	{
//		Logger.instance().info("Entire DCP List");
//		for (String dcp: dcpAddressList.values())
//		{
//			Logger.instance().info(dcp);
//		}
//		Logger.instance().info("Duplicate DCP List");
//		for (String dcpDup: dupDcpAddressList)
//		{
//			Logger.instance().info(dcpDup);
//		}
//	}

	/**
	 * From the Network List name verify if it contains
	 * RIVERGAGES
	 * @param networkListName
	 * @return
	 */
	private String parseDistrictName(String networkListName)
	{
		return DuplicateIo.parseDistrictName(networkListName);
	}
	
	public DuplicateDcp getDuplicateDcpAt(int r)
	{
		return (DuplicateDcp) getRowObject(r);
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
		if (c == 2)
			return true;
		return false;
	}

	public int getRowCount()
	{
		return vec.size();
	}

	public void setValueAt(Object aValue, int r, int c)
	{
		if (c == 2)
		{
			String s = (String)aValue;
			DuplicateDcp dd = getDuplicateDcpAt(r);
			if (dd == null)
				return;
			//If the selected controlling district is not
			//in the "Duplicated In" list - can not select 
			//this district for this dcp
			//Error This DCP does not belong to this district
			String dupIn = dd.getDuplicatedIn();
			if (s != null)
			{
				if (s.equalsIgnoreCase(UNRESOLVED))
				{
					dd.setControllingDist(s);
				}
				else
				{
					if (dupIn.indexOf(s) != -1)
					{
						dd.setControllingDist(s);
						//Here we can reset the Corps DCP Name
						//and Corps Description based on the control office
						//selected
						//dd.getGoesAddr();
						
						//Flag it as changed so when user preses "QUIT"
						//we alert him needs to save
						DuplicateDcpsListFrame.setInfoSaved(false);
					}
					else
					{
						parent.showError("This Dcp " +
							dd.getDcpAddress() + " does not belong to this" +
							" District " + s);
					}	
				}
			}
		}
	}
	
	public Object getValueAt(int r, int c)
	{
		return DuplicateDcpsColumnizer.getColumn(getDuplicateDcpAt(r), c);
	}

	public Object getRowObject(int r)
	{
		return vec.elementAt(r);
	}

	public void sortByColumn(int c)
	{
		sortColumn = c;
		Collections.sort(vec, new DuplicateDcpsColumnComparator(c));
	}

	public void reSort()
	{
		if (sortColumn >= 0)
			sortByColumn(sortColumn);
	}
}

/**
 * Helper class to retrieve Dup DCPs fields by column number. 
 * Used for displaying values in the table and for sorting.
 */
class DuplicateDcpsColumnizer
{
	static String getColumn(DuplicateDcp dupDcp, int c)
	{
		switch (c)
		{
		case 0: // Goes Address
		{
			if (dupDcp != null)
				return dupDcp.getDcpAddress().toString();
			else
				return "";
		}
		case 1: // Duplicated In
			if (dupDcp != null)
				return dupDcp.getDuplicatedIn();
			else
				return "";
		case 2: // Controlling Dist
			if (dupDcp != null)
				return dupDcp.getControllingDist();
			else
				return "";
		case 3: // NWS Code
			if (dupDcp != null)
				return dupDcp.getNwshb5Code();
			else
				return "";
		case 4: // PDT Owner
			if (dupDcp != null)
				return dupDcp.getPdtOwner();
			else
				return "";
		case 5: // Corps Dcp name
			if (dupDcp != null)
				return dupDcp.getDcpName();
			else
				return "";
		case 6: //corpsDescriptionColumn
			if (dupDcp != null)
				return dupDcp.getDescription() == null ? 
					"" : getFirstLine(dupDcp.getDescription());
			else
				return "";
		case 7: //pdtDescColumn
			if (dupDcp != null)
				return dupDcp.getPdtDescription() == null ? 
						"" : getFirstLine(dupDcp.getPdtDescription());
			else
				return "";
		case 8: //nwsDescColumn
			if (dupDcp != null)
				return dupDcp.getNwsDescription() == null ? 
						"" : getFirstLine(dupDcp.getNwsDescription());
			else
				return "";
		default:
			return "";
		}
	}

	private static String getFirstLine(String tmp)
	{
		if (tmp == null)
			return "";
		int len = tmp.length();
		int ci = len;
		if (ci > 60)
			ci = 60;
		int i = tmp.indexOf('\r');
		if (i > 0 && i < ci)
			ci = i;
		i = tmp.indexOf('\n');
		if (i > 0 && i < ci)
			ci = i;
		i = tmp.indexOf('.');
		if (i > 0 && i < ci)
			ci = i;

		if (ci < len)
			return tmp.substring(0,ci);
		else
			return tmp;
	}
}

/**
 * Compare and sort columns.
 */
class DuplicateDcpsColumnComparator implements Comparator
{
	int col;

	DuplicateDcpsColumnComparator(int col)
	{
		this.col = col;
	}

	public int compare(Object dupDcp1, Object dupDcp2)
	{
		if (dupDcp1 == dupDcp2)
			return 0;
		DuplicateDcp d1 = (DuplicateDcp) dupDcp1;
		DuplicateDcp d2 = (DuplicateDcp) dupDcp2;
		
		if (col == 2)
		{	//Sort the District column so that Unresolved shows first
			if (d1.getControllingDist().equalsIgnoreCase("Unresolved")
				&& d2.getControllingDist().equalsIgnoreCase("Unresolved"))
			{
				return 0;//two rows the same
			}
			else if (d1.getControllingDist().equalsIgnoreCase("Unresolved")
					&& !d2.getControllingDist().equalsIgnoreCase("Unresolved"))
			{
				return -1;//first row Unresolved
			}
			else if (!d1.getControllingDist().equalsIgnoreCase("Unresolved")
					&& d2.getControllingDist().equalsIgnoreCase("Unresolved"))
			{
				return 1;//second row unresolved
			}
		}
		return DuplicateDcpsColumnizer.getColumn(d1, col).compareToIgnoreCase(
				DuplicateDcpsColumnizer.getColumn(d2, col));
	}
}
