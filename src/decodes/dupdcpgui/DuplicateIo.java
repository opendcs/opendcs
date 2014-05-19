package decodes.dupdcpgui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Collection;

import ilex.util.EnvExpander;
import ilex.util.Logger;

import lrgs.common.DcpAddress;


/**
 * This class is used by the DuplicateDcpsList GUI and
 * generateToImportNlFile utility.
 * This class creates the controlling district file and
 * <district>-TOIMPORT.nl.
 */
public class DuplicateIo
{
	private static String module = "DuplicateIo";

	private String mergeDirName = "$DECODES_INSTALL_DIR/dcptoimport";
	private File mergeDir;
	private String distFileName = "controlling-districts.txt";
	private File distFile;

	/** Maps a DCP Address to its controlling district, if one is defined. */
	private HashMap<DcpAddress,ControllingDistrict> controlDistList = 
		new HashMap<DcpAddress,ControllingDistrict>();
	
	/**
	 * Construct the IO with dir name & district file name.
	 * @param mergeDirName The merg directory
	 * @param distFileName the district filename within the merge directory
	 */
	public DuplicateIo(String mergeDirName, String distFileName)
	{
		init(mergeDirName, distFileName);
	}	
	
	/**
	 * Initialize or Re-initialize the IO with dir name & district file name.
	 * @param mergeDirName The merge directory
	 * @param distFileName the district filename within the merge directory
	 */
	public void init(String mergeDirName, String distFileName)
	{
		if (mergeDirName != null)
			this.mergeDirName = mergeDirName;
		mergeDir = new File(EnvExpander.expand(this.mergeDirName));
		
		if (distFileName != null)
			this.distFileName = distFileName;
		distFile = new File(mergeDir, this.distFileName);
	}
	
	/**
	 * Read all Controlling Districts from the controlling
	 * District file. The ControllingDistrict file is composed
	 * of dcpAddress and Distrcit Name. The format is:
	 * 		dcpAddress:District
	 * 
	 * @return true if file read successfully, false if not.
	 */
	public boolean readControllingDist()
	{
		controlDistList.clear();

		//Controlling District is found on the DECODES INSTALL DIR/dcptoimport
		FileReader reader = null;
		BufferedReader tempBuf = null;
		try
		{
			reader = new FileReader(distFile);
			tempBuf = new BufferedReader(reader);
			while (tempBuf != null && tempBuf.ready())
			{
				String line = tempBuf.readLine();
				String[] strArray = line.split(":");
				if (strArray != null)
				{
					DcpAddress addr = new DcpAddress(strArray[0]);
					ControllingDistrict cd = new ControllingDistrict(addr,
					    strArray[1]);
					controlDistList.put(addr, cd);
				}
			}
			return true;
		}
		catch (FileNotFoundException e)
		{
			Logger.instance().warning(module 
				+ " No controlling District File '" + distFile.getPath()+"'");
		}
		catch (IOException e)
		{
			Logger.instance().warning(
			    module + " Can not read '" + distFile.getPath()+
			    "': " + e.getMessage());
		}
		return false;
	}
	
	/**
	 * Write the Controlling District List from the Duplicate DCPs GUI
	 * into the controlling-districts.txt file located on 
	 * DECODES INSTALL DIR/dcptoimport directory.
	 * 
	 * @param cd
	 */
	public boolean writeControllingDist()
	{
		FileWriter fw;
		boolean result = true;
		mergeDir.mkdirs();
		try
		{
			Logger.instance().info("Writing control dist list to '"
				+ distFile.getPath() + "'");
			fw = new FileWriter(distFile);
			fw.write(toFileString(controlDistList.values()));
			fw.flush();
			fw.close();
		}
		catch (IOException ex)
		{
			Logger.instance().failure(
			    module + " Can not create " + "Controlling District File '"
			        + distFile.getPath() + "': " + ex.getMessage());
			result = false;
		}
		return result;
	}

    /**
     * Return the entire controlling district list in the format 
     * that it would be stored in a text file.
     */
	private String toFileString(Collection<ControllingDistrict> cd)
	{
		StringBuilder ret = new StringBuilder("");
		for (ControllingDistrict cld : cd)
		{			
			ret.append(cld.getDcpAddress().toString()
				+":" + cld.getDistrict()+ '\n');
		}
		return new String(ret);
	}
	
	/**
	 * Create a <distrcit>-TOIMPORT.nl file under the Merge Directory.
	 * 
	 * @param buffer
	 * @param districtName
	 * @return
	 */
	public boolean writeToImportNlFile(String strBuffer, 
		String districtName)
	{
		FileWriter fw;
		File f = new File(mergeDir, districtName + "-TOIMPORT.nl");
		boolean result = true;
		mergeDir.mkdirs();
		try
		{
			fw = new FileWriter(f);
			fw.write(strBuffer);
			fw.flush();
			fw.close();
		}
		catch (IOException ex)
		{
			Logger.instance().failure(
			    module + " Can not create '" + f.getPath() + "': "
			        + ex.getMessage());
			result = false;
		}
		return result;
	}
	
	/**
	 * Extract out District name from network list name.
	 * 
	 * @param networkListName
	 * @return
	 */
	public static String parseDistrictName(String networkListName)
	{
		String tempN = networkListName;
		int indx = networkListName.indexOf(
			DuplicateDcpsList.GROUP_DELIMITER_NAME);
		if (indx != -1)
		{
			tempN = networkListName.substring(0,indx);	
		}
		return tempN;
	}
	
	/**
	 * This method will return the District that the given Dcp Address
	 * belongs to.
	 * 
	 * @param dcpAddress
	 * @return Controlling District Name
	 */
	public String getDistrictName(DcpAddress dcpAddress)
	{
		ControllingDistrict cd = controlDistList.get(dcpAddress);
		return cd == null ? (String)null : cd.getDistrict();
	}
	
	public ControllingDistrict getControllingDistrict(DcpAddress dcpAddress)
	{
		return controlDistList.get(dcpAddress);
	}
	
	public HashMap<DcpAddress,ControllingDistrict> getHashMap()
	{
		return controlDistList;
	}
	
	/**
	 * Return the controlling district file path
	 * 
	 * @return
	 */
	public File getDistFile()
	{
		return distFile;
	}

	/**
     * @return the controlDistList
     */
    public HashMap<DcpAddress, ControllingDistrict> getControlDistList()
    {
    	return controlDistList;
    }
}
