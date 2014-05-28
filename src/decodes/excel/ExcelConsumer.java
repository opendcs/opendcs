package decodes.excel;

import ilex.util.EnvExpander;
import ilex.util.Logger;
import ilex.util.PropertiesUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import decodes.consumer.DataConsumer;
import decodes.consumer.DataConsumerException;
import decodes.datasource.RawMessage;
import decodes.datasource.UnknownPlatformException;
import decodes.db.Platform;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.db.TransportMedium;
import decodes.decoder.DecodedMessage;
import decodes.decoder.TimeSeries;

/**
 * The ExcelConsumer will generate an excel file "xls" for every DCP
 * in the Routing Spec Network List. The user will supply the directory
 * where the xls files will be created. This is going to be the
 * consumer argument. If no consumer arg is supplied the xls files will be
 * created on the DCSTOOL install dir. 
 * 
 * Note: if the routing spec is going to be ran on real time the following
 * property needs to be set on the routing spec properties section:
 * 	msgPerXlsFile with value of true
 *
 * When setting the Routing Spec with this Consumer need to use the 
 * Null Formatter
 * 
 */
public class ExcelConsumer extends DataConsumer
{
	/** The Directory where the xls files will be stored */
	private String xlsFilesDirectory;
	/** The directory as a File object. */
	private File directory;
	/** Local copy of properties */
	private Properties props;
	private String module = "ExcelConsumer";
	private HashMap<String, ExcelWorkBook> excelWorkBookList;
	private boolean msgPerXlsFile;
	private ArrayList<ExcelColumn> columns;
	private final String SITE_NAME_HEADER_COLUMN = "siteNames";
	private final String SENSOR_NAME_HEADER_COLUMN = "sensorNames";
	private final String FPART_VALUES_HEADER_COLUMN = "fpartValues";
	private final String UNITS_ABBR_HEADER_COLUMN = "unitsAbbr";
	private final String TYPE_VALUES_HEADER_COLUMN = "typevalues";
	private final String dateFormatString = "MM/dd/yy HH:mm";
	private SimpleDateFormat dateFormat;
	private TimeZone timeZoneObj;
	
	public ExcelConsumer()
	{
		super();
		xlsFilesDirectory = "";
		directory = null;
		msgPerXlsFile = false;
		excelWorkBookList = null;
		timeZoneObj = null;
	}
	
	/**
	  Opens and initializes the Excel consumer.
	  @param consumerArg directory where the xls files will be placed.
	  @param props routing spec properties.
	  @throws DataConsumerException if the consumer could not be initialized.
	*/
	public void open(String consumerArg, Properties props)
		throws DataConsumerException
	{
		this.props = props;
		//Set the directory for the excel files
		String xlsDir = consumerArg;
		if (xlsDir == null || xlsDir.equals(""))
			xlsDir = "$DCSTOOL_USERDIR";
		xlsFilesDirectory = EnvExpander.expand(xlsDir);
		directory = new File(xlsFilesDirectory);
		if (!directory.isDirectory())
			directory.mkdirs();

		//Get Routing Spec properties
		//Get the msgPerXlsFile property - used when running on real time
		String s = 
			PropertiesUtil.getIgnoreCase(props,"msgperxlsfile");
		if (s != null && s.equalsIgnoreCase("true"))
		{
			msgPerXlsFile = true;//means create an xls file per message
		}
		excelWorkBookList = new HashMap<String, ExcelWorkBook>();
		//Time Zone comes from routing spec time zone
		String tz = "UTC";
		dateFormat = new SimpleDateFormat(dateFormatString);
		timeZoneObj = this.tz;
		if (timeZoneObj == null)
			timeZoneObj = TimeZone.getTimeZone(tz);
		dateFormat.setTimeZone(timeZoneObj);
	}

	/**
	 * Closes the data consumer.
	 * This method is called by the routing specification when the data
	 * consumer is no longer needed.
	 * This method will write out an excel "xls" file for every site found
	 * on the network list. Notice that if the msgperxlsfile property is set
	 * to true on the routing spec properties this method does not do 
	 * anything. The workbook was written on the startMessage method
	 */
	public void close()
	{
		//If the msgperxlsfile is true - do nothing - this means that the 
		//routing spec is running on real time and we want to create an
		//excel file per msg - this was done on the endMessage method.
		if (msgPerXlsFile == false)
		{
			generateWorkBook();
		}
	}

	/**
	 * Use when running routing spec in real time with the 
	 * msgperxlsfile property set
	 */
	public void endMessage()
	{
		//If the Routing Spec is running on real time this property
		//needs to be set, which means create an xls file for every message.
		if (msgPerXlsFile == true)
		{
			generateWorkBook();
			excelWorkBookList.clear();
		}
	}

	/**
	 * Do Nothing
	 */
	public void printLine(String line)
	{
	}

	/**
	  This method is called at the beginning of each decoded message. We do all
	  the IO work here: the println method does nothing.
	  Use a NullFormatter when using ExcelConsumer.
	  This method will construct an excel file for every DCP on the 
	  Routing Spec Network List.

	  @param msg The message to be written.
	  @throws DataConsumerException if an error occurs.
	*/
	public void startMessage(DecodedMessage msg) throws DataConsumerException
	{
		// Process the time series data and create the Excel (.xls) file
		// for each DCP in the network list
		Platform platform;
		TransportMedium tm;
		RawMessage rawmsg;
		try
		{
			rawmsg = msg.getRawMessage();
			tm = rawmsg.getTransportMedium();
			platform = rawmsg.getPlatform();
		}
		catch(UnknownPlatformException ex)
		{
			Logger.instance().warning(module + 
					" Skipping Excel ingest for data from "
					+ "unknown platform: " + ex);
			return;
		}
		Site platformSite = platform.getSite();
		if (platformSite == null)
		{
			Logger.instance().warning(module + 
					" Skipping Excel ingest for data from "
					+ "unknown site, DCP Address = " + tm.getMediumId());
			return;
		}
		//Get the site name used to identify the workbooks
		SiteName siteName = platformSite.getPreferredName();
		String nameOfSite = siteName.getDisplayName();
		if (nameOfSite == null || nameOfSite.equals(""))
		{	//Just in case - we should never get in here
			Logger.instance().warning(module + 
					" Skipping Excel ingest for data from "
					+ "unknown site, DCP Address = " + tm.getMediumId());
			return;
		}
		//We need to create an ExcelWorkBook obj for every site that is
		//on the Routing Spec network list. This ExcelWorkBook obj will be
		//added to a hash map where the key is the site name. So that we
		//keep track of the sites and put each site on an individual xls file.
		//The ExcelWorkBook contains a Hash Map of ExcelColumn to keep track of
		//the sensors for this site
		ExcelWorkBook ewb = (ExcelWorkBook)excelWorkBookList.get(nameOfSite);
		if (ewb == null)
		{
			ewb = new ExcelWorkBook(nameOfSite, platform.description,
											platformSite.getDescription());
			excelWorkBookList.put(nameOfSite,ewb);
		}
		//Here - verify if we have an ExcelColumn already created with 
		//this sensor, if we do just append the timeseries samples to it.
		//if we do not have this sensor create a new ExcelColumn and
		//add it to the excelColumnHash map list of the ExcelWorkBook
		HashMap<Integer, ExcelColumn> excelColumnHash = 
												ewb.getExcelColumnHash();
		for(Iterator it = msg.getAllTimeSeries(); it.hasNext(); )
		{
			TimeSeries ts = (TimeSeries)it.next();
			if (ts.size() == 0)
				continue;
			int sensorNum = ts.getSensor().getNumber();
			//get the ExcelColumn from workbook that correspond to this
			//sensor
			//if it is found - append time series to it, otherwise create
			//a new ExcelColumn and add it to the workbookobj hash map
			ExcelColumn excelColumn = 
								(ExcelColumn)excelColumnHash.get(sensorNum);
			if (excelColumn == null)
			{
				excelColumn = new ExcelColumn(ts, nameOfSite);
				excelColumnHash.put(sensorNum, excelColumn);
			}
			else
			{
				excelColumn.appendTimeSeries(ts);
			}
		}
	}

	/**
	 * Create the xls header.
	 * 
	 * @param wb
	 * @param sheet
	 */
	private void createHeader(HSSFWorkbook wb, HSSFSheet sheet,
			String aPart, String ePart)
	{	//Create a row for the APart, Rows are 0 based.
		// Create a cell for the APART = A. position: row 0, column "cell" 0
		createStringCell(wb, sheet, (short)0, (short)0, 
									HSSFCellStyle.ALIGN_RIGHT, "A");
		//Create a cell for the BPART = B. position: row 1, column "cell" 0
		createStringCell(wb, sheet, (short)1, (short)0, 
				HSSFCellStyle.ALIGN_RIGHT, "B");
		//Create a cell for the CPART = C. position: row 2, column "cell" 0
		createStringCell(wb, sheet, (short)2, (short)0, 
				HSSFCellStyle.ALIGN_RIGHT, "C");
		//Create a cell for the EPART = E. position: row 3, column "cell" 0
		createStringCell(wb, sheet, (short)3, (short)0, 
				HSSFCellStyle.ALIGN_RIGHT, "E");
		//Create a cell for the FPART = F. position: row 4, column "cell" 0
		createStringCell(wb, sheet, (short)4, (short)0, 
				HSSFCellStyle.ALIGN_RIGHT, "F");
		//Create a cell for the Units. position: row 5, column "cell" 0
		createStringCell(wb, sheet, (short)5, (short)0, 
				HSSFCellStyle.ALIGN_RIGHT, "Units");
		//Create a cell for the Type. position: row 6, column "cell" 0
		createStringCell(wb, sheet, (short)6, (short)0, 
				HSSFCellStyle.ALIGN_RIGHT, "Type");
		
		//SET the timezone
		//TimeZone = As defined in DECODES for this Routing Spec
		//position row - 2, column "cell" - 1
		createStringCell(wb, sheet, (short)2, (short)1, 
				HSSFCellStyle.ALIGN_RIGHT, timeZoneObj.getID());
		//.getDisplayName());
		
		//TODO value for the APART ??? FOR NOW is the first line of 
		//the Platform description 
	    //Row 0 cell 1 
		//APART - User Defined for each DECODES Site 
	    //(Can be left blank)
		createStringCell(wb, sheet, (short)0, (short)1, 
							HSSFCellStyle.ALIGN_LEFT, aPart);
		
		//TODO value for the EPART ??? FOR NOW is the first line of the
		//Site description
		//Row 3 cell 1
		//EPART = User Defined for each DECODES Site (Can be left blank)
		createStringCell(wb, sheet, (short)3, (short)1, 
				HSSFCellStyle.ALIGN_LEFT, ePart);
	}
	
	/**
	 * Create the rest of the header
	 * @param rowNum
	 * @param columnNumIn
	 * @param columnType
	 * @param wb
	 * @param sheet
	 */
	private void createSensorHeaderRow(short rowNum, 
			short columnNumIn, String columnType,
			HSSFWorkbook wb, HSSFSheet sheet)
	{
		short columnNum = columnNumIn;
		for(ExcelColumn col : columns)
		{
			if (columnType.equalsIgnoreCase(SITE_NAME_HEADER_COLUMN))
			{
				createStringCell(wb, sheet, rowNum, columnNum, 
					HSSFCellStyle.ALIGN_RIGHT, col.siteNameCell);
			}
			else if (columnType.equalsIgnoreCase(SENSOR_NAME_HEADER_COLUMN))
			{
				createStringCell(wb, sheet, rowNum, columnNum, 
					HSSFCellStyle.ALIGN_RIGHT, col.sensorName);
			}
			else if (columnType.equalsIgnoreCase(FPART_VALUES_HEADER_COLUMN))
			{
				createStringCell(wb, sheet, rowNum, columnNum, 
					HSSFCellStyle.ALIGN_RIGHT, col.fPart);
			}
			else if (columnType.equalsIgnoreCase(UNITS_ABBR_HEADER_COLUMN))
			{
				createStringCell(wb, sheet, rowNum, columnNum, 
						HSSFCellStyle.ALIGN_RIGHT, col.euAbbr);
			}
			else if (columnType.equalsIgnoreCase(TYPE_VALUES_HEADER_COLUMN))
			{
				createStringCell(wb, sheet, rowNum, columnNum, 
						HSSFCellStyle.ALIGN_RIGHT, col.type);
			}
			columnNum++;
		}		
	}
	
	/**
     * Creates a cell to hold an String value. Also aligns it in a certain way.
     * It also sets the value. 
     *
     * @param wb        the workbook
     * @param sheet     the workbook sheet 
     * @param rowNum    the row number to create the cell in
     * @param column    the column number to create the cell in
     * @param align     the alignment for the cell.
     * @param value		the value to set the cell to
     */
	private void createStringCell(HSSFWorkbook wb, HSSFSheet sheet, 
			short rowNum, short column, short align, String value)
	{
		if (value != null)
    		value = value.trim();
		HSSFRow row = sheet.createRow(rowNum);
		HSSFCell cell = row.createCell(column);
		cell.setCellValue(new HSSFRichTextString(value));
		HSSFCellStyle cellStyle = wb.createCellStyle();
		cellStyle.setAlignment(align);
		cell.setCellStyle(cellStyle);
	}
    
	private void createDateCell(HSSFWorkbook wb, HSSFSheet sheet, 
			short rowNum, short column, short align, Date value)
	{
		GregorianCalendar cal;
		cal = new GregorianCalendar(timeZoneObj);
		cal.setTime(value);
		//Calendar cal = new Calendar();
		//cal.setTimeZone(timeZoneObj);
		
		//It is important to create a new cell style from the 
		//workbook otherwise you can end up
	    //modifying the built in style and effecting not only this cell but 
		//other cells.
		HSSFRow row = sheet.createRow(rowNum);
		HSSFCell cell = row.createCell(column);
		//cell.setCellValue(value);
		cell.setCellType(HSSFCell.CELL_TYPE_NUMERIC);
		cell.setCellValue(cal);
		HSSFDataFormat format = wb.createDataFormat();
		//cell.setCellValue(new Date());
	    HSSFCellStyle cellStyle = wb.createCellStyle();
//	    cellStyle.setDataFormat(
//	    		HSSFDataFormat.getBuiltinFormat("m/d/yy h:mm"));
	    cellStyle.setDataFormat(format.getFormat("MM/dd/yy HH:mm"));
	    cellStyle.setAlignment(align);
	    cell.setCellStyle(cellStyle);
	}
	
	private void createNumberCell(HSSFWorkbook wb, HSSFSheet sheet, 
			short rowNum, short column, short align, double value)
	{
		HSSFRow row = sheet.createRow(rowNum);
		HSSFCell cell = row.createCell(column);
		cell.setCellValue(value);
		//CELL_TYPE_NUMERIC
	    HSSFCellStyle cellStyle = wb.createCellStyle();
	    HSSFDataFormat format = wb.createDataFormat();
	    cellStyle.setDataFormat(format.getFormat("0.00"));
	    cellStyle.setAlignment(align);
	    cell.setCellStyle(cellStyle);
	}
	
	private void createIndexCell(HSSFWorkbook wb, HSSFSheet sheet, 
			short rowNum, short column, short align, int value)
	{
		HSSFRow row = sheet.createRow(rowNum);
		HSSFCell cell = row.createCell(column);
		cell.setCellValue(value);
	    HSSFCellStyle cellStyle = wb.createCellStyle();
	    HSSFDataFormat format = wb.createDataFormat();
	    cellStyle.setDataFormat(format.getFormat("0"));
	    cellStyle.setAlignment(align);
	    cell.setCellStyle(cellStyle);
	}
	
	/**
	 * This method writes out the given work book. It will use the 
	 * directory given as a consumer arg with the site name and
	 * current date/time for the xls file name.
	 * 
	 * @param wb
	 */
	private void writeWorkBook(HSSFWorkbook wb) 
	{
		if (wb != null)
		{
			//The sheet name will be the site name for this xls.
			String siteName = wb.getSheetName(0);
			Date currentDate = new Date();
			DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSS");
			String xlsFileName = siteName + "-" + 
						dateFormat.format(currentDate) + ".xls";
			FileOutputStream fileOut;
			try
			{
				fileOut = new FileOutputStream(xlsFilesDirectory
											+ File.separator + xlsFileName);
				wb.write(fileOut);
			    fileOut.close();
			} catch (FileNotFoundException ex)
			{
				String errMsg = module + " Can not create xls file for " +
						" site " + siteName + " " + ex.toString();
				Logger.instance().warning(errMsg);
			} catch (IOException ex)
			{
				String errMsg = module + " Can not create xls file for " +
				" site " + siteName + " " + ex.toString();
				Logger.instance().warning(errMsg);
			}
		}
		else
			Logger.instance().warning(module + " Workbook is null");
	}
	
	/**
	 * Fill out a workbook for every site on the Routing Spec Network list.
	 * The excelWorkBookList will contain a workbook obj for every site and
	 * a hash map with all the columns needed to fill out this workbook.
	 * 
	 */
	private void generateWorkBook()
	{
		Collection<ExcelWorkBook> xlsWorkBookList = 
					(Collection<ExcelWorkBook>)excelWorkBookList.values();
		//Loop through the ExcelWorkBook List
		for (ExcelWorkBook ewb : xlsWorkBookList)
		{
			if (ewb != null)
			{
				//Get the workbook out of this ExcelWorkBook
				HSSFWorkbook wb = ewb.getWb();
				//Get the sheet - which is always zero
				HSSFSheet sheet = wb.getSheetAt(0);
				//Get the list of ExcelColumn objects (contains all columns)
				Collection<ExcelColumn> tcolumns =
				//columns = 
					(Collection<ExcelColumn>)ewb.getExcelColumnHash().values();
				columns = new ArrayList<ExcelColumn>(tcolumns);
				Collections.sort(columns, new SensorColumnComparator());
				//Fill out workbook
				fillOutWorkBook(wb, sheet, ewb.getPlatDesc(), 
														ewb.getSiteDesc());
				//Write the xls file to the file system
				writeWorkBook(wb);
			}
		}
	}

	/**
	 * Fill out the workbook with the data from the columns collection.
	 * @param wb
	 * @param sheet
	 */
	private void fillOutWorkBook(HSSFWorkbook wb, HSSFSheet sheet,
			String aPart, String ePart)
	{
		// Create part of the header
		createHeader(wb, sheet, aPart, ePart);

		// Row 1 BPART - Site Name the same for all columns
		// SITE_NAME_HEADER_COLUMN
		short siteNamesRow = 1;
		short siteNameColumn = 2;
		createSensorHeaderRow(siteNamesRow, siteNameColumn,
				SITE_NAME_HEADER_COLUMN, wb, sheet);

		// Row 2 CPART - Sensor names - start at row 2, column 2
		// SENSOR_NAME_HEADER_COLUMN
		short sensorNamesRow = 2;
		short sensorNameColumn = 2;
		createSensorHeaderRow(sensorNamesRow, sensorNameColumn,
				SENSOR_NAME_HEADER_COLUMN, wb, sheet);

		// Row 4 FPART - rev or raw
		// FPART_VALUES_HEADER_COLUMN
		short fPartRow = 4;
		short fPartColumn = 2;
		createSensorHeaderRow(fPartRow, fPartColumn,
				FPART_VALUES_HEADER_COLUMN, wb, sheet);

		// Row 5 Units - EU abbreviation - start at row 5, column 2
		// UNITS_ABBR_HEADER_COLUMN
		short unitsAbbrRow = 5;
		short unitsAbbrColumn = 2;
		createSensorHeaderRow(unitsAbbrRow, unitsAbbrColumn,
				UNITS_ABBR_HEADER_COLUMN, wb, sheet);

		// Row 6 - Type - INST-VAL or PER-CUM for PC
		// TYPE_VALUES_HEADER_COLUMN
		short typeRow = 6;
		short typeColumn = 2;
		createSensorHeaderRow(typeRow, typeColumn, TYPE_VALUES_HEADER_COLUMN,
				wb, sheet);

		// Row 7 - index timestamp values ...
		int rowIndex = 1;
		short sampleRowNum = 7;
		short sampleColumnNum = 0;

		Date d;
		while ((d = findNextDate()) != null)
		{ // Index under type
			sampleColumnNum = 0;
//			createStringCell(wb, sheet, sampleRowNum, sampleColumnNum,
//					HSSFCellStyle.ALIGN_RIGHT, "" + rowIndex);
			createIndexCell(wb, sheet, sampleRowNum, sampleColumnNum,
					HSSFCellStyle.ALIGN_RIGHT, rowIndex);
			
			sampleColumnNum++;

			createStringCell(wb, sheet, sampleRowNum, sampleColumnNum,
					HSSFCellStyle.ALIGN_RIGHT, dateFormat.format(d));
//			createDateCell(wb, sheet, sampleRowNum, sampleColumnNum,
//										HSSFCellStyle.ALIGN_RIGHT, d);
			//dateFormat.
			sampleColumnNum++;
			for (ExcelColumn col : columns)
			{
				String s;
				if (d.equals(col.nextSampleTime()))
					s = col.nextSample();
				else
					s = col.getBlankSample();
				
				try {
					double val = new Double(s).doubleValue();
					createNumberCell(wb, sheet, sampleRowNum, sampleColumnNum,
							HSSFCellStyle.ALIGN_RIGHT, val);
				}
				catch (NumberFormatException ex)
				{
					createStringCell(wb, sheet, sampleRowNum, sampleColumnNum,
						HSSFCellStyle.ALIGN_RIGHT, "");
				}

				//sheet.autoSizeColumn((short) sampleColumnNum);
				sampleColumnNum++;
			}
			sampleRowNum++;
			rowIndex++;
		}
		// Ajust the width of the second columns
		//sheet.autoSizeColumn((short) 0);
		//sheet.autoSizeColumn((short) 1);
		//System.out.println("column 1 = " + sheet.getColumnWidth((short)1));
		//sheet.setColumnWidth((short)1,(short)12);
	
	}
	/** @return next date in any time series */
	private Date findNextDate()
	{
		Date ret = null;

		for(ExcelColumn col : columns)
		{
			Date d = col.nextSampleTime();
			if (d != null
			 && (ret == null || d.compareTo(ret) < 0))
				ret = d;
		}
		return ret;
	}
}
