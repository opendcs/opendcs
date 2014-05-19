package decodes.excel;

import java.util.HashMap;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;

/**
 * The ExcelConsumer creates one of this object for every site 
 * found on the Routing Spec network list.
 * This class will contain the excel workbook to generate the 
 * xls file and the list of sensors that will go in it.
 */
public class ExcelWorkBook
{
	private String nameOfSite;
	private String platDesc;//==> APART
	private String siteDesc;//==> EPART
	private HSSFWorkbook wb;
	private HashMap<Integer, ExcelColumn> excelColumnHash;
	
	/** Initialize the ExcelWorkBook */
	public ExcelWorkBook(String nameOfSite, String platDesc, String siteDesc)
	{
		wb = createWorkBook(nameOfSite);
		this.nameOfSite = nameOfSite;
		this.platDesc = getFirstLine(platDesc);
		this.siteDesc = getFirstLine(siteDesc);
		excelColumnHash = new HashMap<Integer, ExcelColumn>();
	}
	
	/**
	 * Creates a Work Book for the given Site Name
	 * 
	 * @param siteName
	 * @return
	 */
	private HSSFWorkbook createWorkBook(String siteName)
	{
		HSSFWorkbook wb = new HSSFWorkbook();
		wb.createSheet(siteName);
		return wb;
	}

	/** Parse the descriptions - gets only the first line */
	private String getFirstLine(String tmp)
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
	
	/** Return the excelColumnHash */
	public HashMap<Integer, ExcelColumn> getExcelColumnHash()
	{
		return excelColumnHash;
	}

	/** Set the excelColumnHash */
	public void setExcelColumnHash(
							HashMap<Integer, ExcelColumn> excelColumnHash)
	{
		this.excelColumnHash = excelColumnHash;
	}

	/** Return the name of Site for this workbook */
	public String getNameOfSite()
	{
		return nameOfSite;
	}

	/** Set the name of Site for this workbook */
	public void setNameOfSite(String nameOfSite)
	{
		this.nameOfSite = nameOfSite;
	}

	/** Return the workbook */
	public HSSFWorkbook getWb()
	{
		return wb;
	}

	/** Set the workbook */
	public void setWb(HSSFWorkbook wb)
	{
		this.wb = wb;
	}

	public String getPlatDesc()
	{
		return platDesc;
	}

	public void setPlatDesc(String platDesc)
	{
		this.platDesc = platDesc;
	}

	public String getSiteDesc()
	{
		return siteDesc;
	}

	public void setSiteDesc(String siteDesc)
	{
		this.siteDesc = siteDesc;
	}
}