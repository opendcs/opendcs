package decodes.dupdcpgui;

import ilex.util.EnvExpander;
import ilex.util.TextUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.table.AbstractTableModel;

import lrgs.common.DcpAddress;
import decodes.db.Constants;
import decodes.db.Database;
import decodes.db.NetworkList;
import decodes.db.NetworkListEntry;
import decodes.db.Platform;
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
@SuppressWarnings("serial")
class DuplicateDcpsTableModel 
	extends AbstractTableModel 
	implements SortingListTableModel
{
//	private String module = "DupDcpsTableModel";
	private static String[] columnNames = 
		new String[]
		{
		"GOES Address", "Duplicated In", "Control Office", "NWSHB5 Code",
		"PDT Owner", "Corps DCP Name", "Corps Description", "PDT Description", "NWSHB5 Description"
		};
		
	private DuplicateDcpsFrame parent;
	private ArrayList<DuplicateDcp> vec = new ArrayList<DuplicateDcp>();
	private int sortColumn = -1;
	private ArrayList<NetworkList> groups;
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
	public DuplicateDcpsTableModel(DuplicateDcpsFrame parent,
		ArrayList<NetworkList> groups,
		HashMap<DcpAddress,ControllingDistrict> controlDistList)
	{
		super();
		this.parent = parent;
		this.groups = groups;//Network List names
		this.controlDistList = controlDistList;//controlling districts list 
//		module = parent.module + ":" + "DuplicateDcpsListTableModel";
		buildDupDcpList();
	}

	private void buildDupDcpList()
	{
		initializePdt(parent.getMainObj().getPdtFilePath()); 

		// NWSHB5 is used to fill out the NWShb5 Code and NWSHB5 Description
		initializeHads(parent.getMainObj().getNwsFilePath());

		// Working Hash Map of dcpAddress and DuplicateDcp obj
		HashMap<DcpAddress, DuplicateDcp> dupDcpsHash = 
			new HashMap<DcpAddress, DuplicateDcp>();
		
		for (NetworkList nl : groups)
		{
			String distName = this.parseDistrictName(nl.name);

			for(Iterator<NetworkListEntry> it = nl.iterator(); it.hasNext(); )
			{
				NetworkListEntry nle = it.next();
				DcpAddress dcpAddress = new DcpAddress(nle.transportId);
				
				// Clear working variables, then fill in prioritized order from various sources.
				corpsDcpName = "";
				corpsDesc = "";
				pdtOwner = "";
				pdtDesc = "";
				nwsCode = "";
				nwsDesc = "";
				ControllingDistrict cd = controlDistList.get(dcpAddress);
				String controlOffice = cd != null ? cd.getDistrict() : UNRESOLVED;

				// NWSHB5 code and NWS Desc
				findNWSInfo(dcpAddress);
				
				// PDT onwer "agency" and PDT description
				findPDTInfo(dcpAddress);
				
				// Corps Dcp Name - platform name and Corps Description
				findCorpsInfo(controlOffice, nle);
				
				// Have I already seen this DCP?
				DuplicateDcp dup = (DuplicateDcp)dupDcpsHash.get(dcpAddress);
				if (dup != null)
				{
					// duplicatedIn is a comma-separate list of districts.
					String duplicatedIn = dup.getDuplicatedIn();
					if (duplicatedIn.length() > 0)
						duplicatedIn = duplicatedIn + ", ";
					duplicatedIn = duplicatedIn + distName;
					dup.setDuplicatedIn(duplicatedIn);
					
					// controlOffset set above
					dup.setControllingDist(controlOffice);
					
					//Set it to duplicate
					dup.setDuplicatedDcp(true);
				}
				else // First time we've seen this DCP.
				{
					//Add it to the dupDcpsHash but NOT as a duplicate
					dupDcpsHash.put(dcpAddress,
						new DuplicateDcp(corpsDcpName, dcpAddress, corpsDesc, 
						distName, distName, nwsCode, pdtOwner, pdtDesc, nwsDesc, false));
				}
			}
		}

		vec.clear();
		vec.addAll(dupDcpsHash.values());
		sortByColumn(2);//Sort by controlling office - unresolved first
	}

	/**
	 * @param pdtFilePath
	 */
	private void initializePdt(String pdtFilePath)
	{
		pdt = Pdt.instance();
		File pdtFile = new File(pdtFilePath);
		pdt.load(pdtFile);
	}

	/**
	 * @param hadsFilePath
	 */
	private void initializeHads(String hadsFilePath)
	{
		hads = Hads.instance();
		hads.startMaintenanceThread(
			"http://www.weather.gov/ohd/hads/compressed_defs/all_dcp_defs.txt", 
			EnvExpander.expand(hadsFilePath));
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
	
	private void findCorpsInfo(String controlOffice, NetworkListEntry nle)
	{
		Database db = Database.getDb();

		Platform p = db.platformList.findPlatform(
			Constants.medium_Goes, nle.getTransportId(), new Date()); 

		if (p != null)
		{
			//Get description from Platform
			corpsDesc = p.description;
			if (corpsDesc == null || corpsDesc.equals(""))
			{
				// No description in Platform - get it from site.
				if (p.getSite() != null)
					corpsDesc = p.getSite().getDescription();		
			}
		}
		
		//Find Corps Dcp Name from the network list that this dcp address
		//belongs to.
		//Find out if there is a controlling office for this DCP address
		if (controlOffice != null && !controlOffice.equalsIgnoreCase(UNRESOLVED))
		{
			String ctlNlName = controlOffice + DuplicateDcpsGUI.GROUP_DELIMITER_NAME;
			NetworkList ctlNl = db.networkListList.find(ctlNlName);
			if (ctlNl != null)
			{
				NetworkListEntry ctlNle = ctlNl.getEntry(nle.transportId);
				if (ctlNle != null)
					nle = ctlNle;
			}
		}
			
		if (corpsDcpName.length() == 0 
		 && nle.getPlatformName() != null && nle.getPlatformName().length() > 0)
			corpsDcpName = nle.getPlatformName();
		if (corpsDesc.length() == 0
		 && nle.getDescription() != null && nle.getDescription().length() > 0)
			corpsDesc = nle.getDescription();
	}
	
	/**
	 * Return a vector with all rows in the model
	 * @return
	 */
	public ArrayList<DuplicateDcp> getList()
	{
		return vec;
	}
	
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
						DuplicateDcpsFrame.setInfoSaved(false);
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
		return vec.get(r);
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
		if (dupDcp == null)
			return "";
		
		switch (c)
		{
		case 0: // Goes Address
			return dupDcp.getDcpAddress().toString();
		case 1: // Duplicated In
			return dupDcp.getDuplicatedIn();
		case 2: // Controlling Dist
			return dupDcp.getControllingDist();
		case 3: // NWS Code
			return dupDcp.getNwshb5Code();
		case 4: // PDT Owner
			return dupDcp.getPdtOwner();
		case 5: // Corps Dcp name
			return dupDcp.getDcpName();
		case 6: //corpsDescriptionColumn
			return TextUtil.getFirstLine(dupDcp.getDescription());
		case 7: //pdtDescColumn
			return TextUtil.getFirstLine(dupDcp.getPdtDescription());
		case 8: //nwsDescColumn
			return TextUtil.getFirstLine(dupDcp.getNwsDescription());
		default:
			return "";
		}
	}
}

/**
 * Compare and sort columns.
 */
class DuplicateDcpsColumnComparator implements Comparator<DuplicateDcp>
{
	int col;

	DuplicateDcpsColumnComparator(int col)
	{
		this.col = col;
	}

	@Override
	public int compare(DuplicateDcp dupDcp1, DuplicateDcp dupDcp2)
	{
		if (dupDcp1 == dupDcp2)
			return 0;

		if (col == 2)
		{	//Sort the District column so that Unresolved shows first
			if (dupDcp1.getControllingDist().equalsIgnoreCase("Unresolved")
				&& dupDcp2.getControllingDist().equalsIgnoreCase("Unresolved"))
			{
				return 0;//two rows the same
			}
			else if (dupDcp1.getControllingDist().equalsIgnoreCase("Unresolved")
					&& !dupDcp2.getControllingDist().equalsIgnoreCase("Unresolved"))
			{
				return -1;//first row Unresolved
			}
			else if (!dupDcp1.getControllingDist().equalsIgnoreCase("Unresolved")
					&& dupDcp2.getControllingDist().equalsIgnoreCase("Unresolved"))
			{
				return 1;//second row unresolved
			}
		}
		return DuplicateDcpsColumnizer.getColumn(dupDcp1, col).compareToIgnoreCase(
				DuplicateDcpsColumnizer.getColumn(dupDcp2, col));
	}

}
