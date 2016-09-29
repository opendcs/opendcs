/*
* $Id$
*
* $Log$
* Revision 1.1.1.1  2014/05/19 15:28:59  mmaloney
* OPENDCS 6.0 Initial Checkin
*
* Revision 1.2  2013/02/28 16:47:04  mmaloney
* Deprecated this entire class.
*
* Revision 1.1  2012/10/16 21:40:17  gchen
* CMWS rating utility to import/export rating XML file to/from CWMS DB; convert the rating rdb file into the XML, and view ratings.
*
 * This software was written by Cove Software, LLC ("COVE") under contract 
 * to the United States Government. 
 * 
 * No warranty is provided or implied other than specific contractual terms
 * between COVE and the U.S. Government
 * 
 * Copyright 2016 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * All rights reserved.
*/
package decodes.cwms;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.commons.lang.xwork.StringUtils;

import ilex.cmdline.*;
import ilex.util.EnvExpander;
import ilex.util.Logger;

import decodes.tsdb.DbIoException;
import decodes.tsdb.TsdbAppTemplate;
import decodes.util.CmdLineArgs;

import hec.data.RatingException;
import hec.data.cwmsRating.RatingSet;

/**
CwmsDbUtils provides user the utilities to import/export ratings and 
to load the rating data files into CWMS ratings.

This is a stand-alone application with command-line.

Main Command Line Args include:

	-Z Time Zone (defaults to tz specified for database)
	-X import/export/convert/view operation for cwms ratings 
	-F Rating XML file name to be imported from
	-R Rating XML file directory to be imported/exported
	-O CWMS office ID
    Following the options are any number of location IDs for export.

@deprecated use import/export utilities in decodes.cwms.rating
*/
public class CwmsDbUtils
	extends TsdbAppTemplate
{
	private StringToken tzArg = null;
	private StringToken utilNameArg = null;
	private StringToken fileNameArg = null;
	private StringToken dirNameArg = null;
	private StringToken officeIdArg = null;
	private StringToken locationIdsArg = null;
	
	private static TimeZone tz = null;
	private Properties props = new Properties();
	
	private Connection conn = null;

		
	public CwmsDbUtils()
	{
		super("util.log");
		setSilent(true);
	}

	/** For cmdline version, adds argument specifications. */
	protected void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		utilNameArg = new StringToken("X", "import, export, convert, view", 
			"operations of cwms ratings", TokenOptions.optSwitch, "view");
		cmdLineArgs.addToken(utilNameArg);
		tzArg  = new StringToken("Z", "Time Zone", "", 
			TokenOptions.optSwitch, "UTC");
		cmdLineArgs.addToken(tzArg);
		officeIdArg = new StringToken("O", "CWMS office ID", "", 
				TokenOptions.optSwitch, "");
		cmdLineArgs.addToken(officeIdArg);
		fileNameArg = new StringToken("F", "Rating XML file name to be imported", "", 
				TokenOptions.optSwitch, ".xml");
		cmdLineArgs.addToken(fileNameArg);
		dirNameArg = new StringToken("R", "Rating XML file directory to be imported/exported", "", 
				TokenOptions.optSwitch, "");
		cmdLineArgs.addToken(dirNameArg);
		locationIdsArg = new StringToken("", "Location IDs", "", 
			TokenOptions.optArgument|TokenOptions.optRequired
			|TokenOptions.optMultiple, "");
		cmdLineArgs.addToken(locationIdsArg);
		
	}

	protected void runApp()
		throws Exception
	{
		conn = theDb.getConnection();
		
		this.props = cmdLineArgs.getCmdLineProps();	
		setTimeZone(TimeZone.getTimeZone(tzArg.getValue()));

		String officeId = officeIdArg.getValue().trim();
		String fileName = fileNameArg.getValue().trim();
		String dirName  = dirNameArg.getValue().trim();
		String utilName = utilNameArg.getValue().trim();	
		try
		{
			if (utilName.equalsIgnoreCase("export")) 
			{
				// Export the ratings into an XML file
				if (! new File(dirName).exists())
					dirName = EnvExpander.expand("$DCSTOOL_HOME");
				doExport2XML(dirName, officeId, locationIdsArg);
			} 
			else if (utilName.equalsIgnoreCase("import")) 
			{
				// Import the rating data XML file into CWMS DB
				if (new File(fileName).exists())
				{
					doImport2XML(fileName);
				}
				
				// Import all the rating data XML files in the directory into CWMS DB
				if (new File(dirName).exists())
				{
					for (File tmpFile : (new File(dirName)).listFiles()) {
						if (tmpFile.getName().toLowerCase().endsWith(".xml"))
							doImport2XML(tmpFile.toString());
					}
				}
			} 
			else if (utilName.equalsIgnoreCase("convert")) 
			{
				// Convert the rating rdb file into the XML file
				if (! new File(fileName).exists())
				{
					Logger.instance().warning("No rating rdb file (" + fileName + ") is defined or existed !");
					return;
				}
				for (int cnt=locationIdsArg.NumberOfValues(), i=0; i<cnt; i++)
				{
					doConvertRating2XML(fileName, officeId, locationIdsArg.getValue(i));
				}
			}
			else if (utilName.equalsIgnoreCase("view")) 
			{
				String tmpRatingHeader = String.format("%-10s ", "OfficeID") +
					String.format("%-10s ", "LocationID") + String.format("%-40s ", "RatingID") +
					String.format("%-30s ", "TemplateID") + String.format("%-40s ", "Description");
				System.out.println(tmpRatingHeader);
				Logger.instance().info(tmpRatingHeader);
				tmpRatingHeader = String.format("%-10s ", "----------") + String.format("%-10s ", "----------") +
					String.format("%-40s ", "----------------------------------------") + 
					String.format("%-30s ", "------------------------------") + 
					String.format("%-40s ", "----------------------------------------");
				System.out.println(tmpRatingHeader);
				Logger.instance().info(tmpRatingHeader);
				
				for (int cnt=locationIdsArg.NumberOfValues(), i=0; i<cnt; i++)
				{
					doViewRatingSpecs(officeId, locationIdsArg.getValue(i));
				}
			}
			else 
			{ 
				Logger.instance().warning("No rating operation is defined !"); 
			}
		} 
		catch(Exception ex)
		{
			Logger.instance().failure("Failed to "+utilName+" ratings. " + ex.toString());
		}
	}

	
	void doExport2XML(String dirName, String officeId, StringToken locationIdsArg)
		throws Exception
	{
		String         fn; 
		RatingSet      crSet;
		BufferedWriter rxOut;
		
		for (int cnt=locationIdsArg.NumberOfValues(), i=0; i<cnt; i++)
		{
			for (String ratingId : getRatingIds(officeId, locationIdsArg.getValue(i)))
			{
				fn = EnvExpander.expand(dirName+"/"+ratingId.trim()+".xml");
				rxOut = new BufferedWriter(new FileWriter(fn));
				try
				{
					Logger.instance().info("Export the ratings to '"+fn+"' for Rating ID: '"+ratingId+"'");
					crSet = RatingSet.fromDatabase(theDb.getConnection(), officeId, ratingId);
					rxOut.write(crSet.toXmlString("\t")); 
				}
				catch(RatingException ex1)
				{
					Logger.instance().warning("No such ratings for '" + ratingId + "': " + ex1);
				} 
				catch(Exception ex)
				{
					Logger.instance().warning(ex.toString()); 
				} 
				finally
				{
					rxOut.close(); 
				}
			}
		}
	}

	
	void doImport2XML(String fileName)
		throws Exception
	{
		String            fText = ""; 
		String 		      fLine = null;
		BufferedReader    rxIn;
		RatingSet         crSet;
		
		rxIn = new BufferedReader(new FileReader(fileName));
		while ((fLine = rxIn.readLine()) != null)
		{
		    fText = fText + fLine.trim();
		}
		rxIn.close();  
		try
		{
			Logger.instance().info("Start to import the rating XML file '" + fileName + "'");
			crSet = RatingSet.fromXml(fText);
			crSet.storeToDatabase(conn, true);
			Logger.instance().info("End to import the rating XML file '" + fileName + "'");
		}
		catch(RatingException ex1)
		{
			Logger.instance().failure("Failed to import the rating xml file '" + fileName + "': " + ex1.toString());
		}
		catch(Exception ex)
		{
			Logger.instance().warning(ex.toString());
		}
	}

	
	void doConvertRating2XML(String fileName, String officeId, String locationId)
		throws Exception
	{
		String 		    fLine = null;
		String 			regexp = "[\\s,;\\n\\t]+";
		String			sFrmt;
		BufferedReader  rxIn;
		BufferedWriter  rxOut;
		
		String          tmpStr;
		boolean			valueFlag;
		String[] 		ratingCols;
		ArrayList<String[]>	ratingRows = new ArrayList<String[]>();
		
		// The parameters for rating data
		String			station_name = "";
		String			active = "true";
		String			effective_date = "";
		String			param_id = "";
		String[]		param_name = {"Stage", "Flow"};
		String			units_id = "";
		String[]		units_name = {"ft", "cfs"};
		String			spec_id = "";
		String			spec_ver = "Production";
		String			spec_desc = "";
		String			temp_id = "";
		String			temp_ver = "USGS-EXSA";
		String			temp_desc = "USGS-Style Expanded, Shift-Adjusted Stage-Discharge Rating";
		String			ind_rnd_name, ind_param_name;
		String			ind_rnd_val = null, ind_param_val;
		String			dep_rnd_name, dep_param_name;
		String			dep_rnd_val = null, dep_param_val;

		// Read the rating rdb file
		rxIn = new BufferedReader(new FileReader(fileName));
		while ((fLine = rxIn.readLine()) != null)
		{
		    // Header Section
			units_id = String.format("%s;%s", units_name[0], units_name[1]);
			param_id = String.format("%s;%s", param_name[0], param_name[1]);
			temp_id  = String.format("%s.%s", param_id, temp_ver);
			spec_id  = String.format("%s.%s.%s", locationId, temp_id, spec_ver);
			if (fLine.startsWith("# //"))
			{
				if (fLine.contains("RATING_INDEP"))
				{
					fLine = fLine.substring(fLine.indexOf("ROUNDING="));
					tmpStr = fLine.substring(0, fLine.indexOf("PARAMETER="));
					ind_rnd_name = "ROUNDING";
					ind_rnd_val  = StringUtils.replace(tmpStr.substring(tmpStr.indexOf("\"")), "\"", "").trim();
//					System.out.println(tmpStr+" "+ind_rnd_name+" "+ind_rnd_val);
					tmpStr = fLine.substring(fLine.indexOf("PARAMETER="));
					ind_param_name = "PARAMETER";
					ind_param_val  = tmpStr.substring(tmpStr.indexOf("\""));
//					System.out.println(tmpStr+" "+ind_param_name+" "+ind_param_val);
				}
				if (fLine.contains("RATING_DEP"))
				{
					fLine = fLine.substring(fLine.indexOf("ROUNDING="));
					tmpStr = fLine.substring(0, fLine.indexOf("PARAMETER="));
					dep_rnd_name = "ROUNDING";
					dep_rnd_val  = StringUtils.replace(tmpStr.substring(tmpStr.indexOf("\"")), "\"", "").trim();
//					System.out.println(tmpStr+" "+dep_rnd_name+" "+dep_rnd_val);
					tmpStr = fLine.substring(fLine.indexOf("PARAMETER="));
					dep_param_name = "PARAMETER";
					dep_param_val  = tmpStr.substring(tmpStr.indexOf("\""));
//					System.out.println(tmpStr+" "+dep_param_name+" "+dep_param_val);
				}
				if (fLine.contains("STATION NAME"))
				{
					tmpStr = fLine.substring(fLine.indexOf("NAME="));
					station_name = StringUtils.replace(tmpStr.substring(tmpStr.indexOf("\"")), "\"", "").trim();
					spec_desc = String.format("%s (%s) %s %s Rating", station_name, locationId, temp_ver, spec_ver);
				}
				if (fLine.contains("RATING_DATETIME BEGIN="))
				{
					fLine = fLine.substring(fLine.indexOf("BEGIN="));
					tmpStr = fLine.substring(fLine.indexOf("=")+1, fLine.indexOf(" ")).trim();
					Date tmpDT = new Date();
					tmpDT = (new SimpleDateFormat("yyyyMMddhhmmss")).parse(tmpStr);
					effective_date = (new SimpleDateFormat("yyyy-MM-dd hh:mm:ss")).format(tmpDT);
					effective_date = StringUtils.replace(effective_date, " ", "T");
//					System.out.println(effective_date);
				}
				if (fLine.contains("SHIFT_PREV BEGIN="))
				{
					fLine = fLine.substring(fLine.indexOf("BEGIN="));
					tmpStr = fLine.substring(fLine.indexOf("=\"")+2, fLine.indexOf("\" ")).trim();
					Date tmpDT = new Date();
					tmpDT = (new SimpleDateFormat("yyyyMMddhhmmss")).parse(tmpStr);
					effective_date = (new SimpleDateFormat("yyyy-MM-dd hh:mm:ss")).format(tmpDT);
					effective_date = StringUtils.replace(effective_date, " ", "T");
//					System.out.println(effective_date);
				}
			}
			// Data Section
			else
			{
				valueFlag = true;
				ratingCols = new String[3];
				ratingCols = fLine.split(regexp);
				for(int i=0; i<3; i++)
				{
					try
					{
						Double.parseDouble(ratingCols[i]);
					}
					catch (Exception ex)
					{
						valueFlag = false;
					}
				}
				if (valueFlag)
				{
					ratingRows.add(ratingCols);
				}
			}
		}
		rxIn.close();

		// Write the ratings into XML file
		fileName = fileName.substring(0, fileName.lastIndexOf(".")) + "_" + locationId + ".xml";
		rxOut = new BufferedWriter(new FileWriter(fileName));
		
		rxOut.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
		rxOut.write("<ratings xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " + 
			"xsi:noNamespaceSchemaLocation=\"http://www.hec.usace.army.mil/xmlSchema/CWMS/Ratings.xsd\">\n");
		// Write rating template
		rxOut.write(String.format("\t<rating-template office-id=\"%s\">\n", officeId));
		rxOut.write(String.format("\t\t<parameters-id>%s</parameters-id>\n", param_id));
		rxOut.write(String.format("\t\t<version>%s</version>\n", temp_ver));
		rxOut.write("\t\t<ind-parameter-specs>\n");
		rxOut.write("\t\t\t<ind-parameter-spec position=\"1\">\n");
		rxOut.write(String.format("\t\t\t\t<parameter>%s</parameter>\n", param_name[0]));
		rxOut.write(String.format("\t\t\t\t<in-range-method>%s</in-range-method>\n", "LINEAR"));
		rxOut.write(String.format("\t\t\t\t<out-range-low-method>%s</out-range-low-method>\n", "ERROR"));
		rxOut.write(String.format("\t\t\t\t<out-range-high-method>%s</out-range-high-method>\n", "ERROR"));
		rxOut.write("\t\t\t</ind-parameter-spec>\n");
		rxOut.write("\t\t</ind-parameter-specs>\n");
		rxOut.write(String.format("\t\t<dep-parameter>%s</dep-parameter>\n", param_name[1]));
		rxOut.write(String.format("\t\t<description>%s</description>\n", temp_desc));
		rxOut.write("\t</rating-template>\n");
		// Write the rating spec
		rxOut.write(String.format("\t<rating-spec office-id=\"%s\">\n", officeId));
		rxOut.write(String.format("\t\t<rating-spec-id>%s</rating-spec-id>\n", spec_id));
		rxOut.write(String.format("\t\t<template-id>%s</template-id>\n", temp_id));
		rxOut.write(String.format("\t\t<location-id>%s</location-id>\n", locationId));
		rxOut.write(String.format("\t\t<version>%s</version>\n", spec_ver));
		rxOut.write(String.format("\t\t<in-range-method>%s</in-range-method>\n", "LINEAR"));
		rxOut.write(String.format("\t\t<out-range-low-method>%s</out-range-low-method>\n", "NEAREST"));
		rxOut.write(String.format("\t\t<out-range-high-method>%s</out-range-high-method>\n", "NEAREST"));
		rxOut.write(String.format("\t\t<active>%s</active>\n", active));
		rxOut.write(String.format("\t\t<auto-update>%s</auto-update>\n", "false"));
		rxOut.write(String.format("\t\t<auto-activate>%s</auto-activate>\n", "false"));
		rxOut.write(String.format("\t\t<auto-migrate-extension>%s</auto-migrate-extension>\n", "false"));
		rxOut.write("\t\t<ind-rounding-specs>\n");
		rxOut.write(String.format("\t\t\t<ind-rounding-spec position=\"1\">%s</ind-rounding-spec>\n", ind_rnd_val));
		rxOut.write("\t\t</ind-rounding-specs>\n");
		rxOut.write(String.format("\t\t<dep-rounding-spec>%s</dep-rounding-spec>\n", dep_rnd_val));
		rxOut.write(String.format("\t\t<description>%s</description>\n", spec_desc));
		rxOut.write("\t</rating-spec>\n");
		// Write the rating
		rxOut.write(String.format("\t<rating office-id=\"%s\">\n", officeId));
		rxOut.write(String.format("\t\t<rating-spec-id>%s</rating-spec-id>\n", spec_id));
		rxOut.write(String.format("\t\t<units-id>%s</units-id>\n", units_id));
		rxOut.write(String.format("\t\t<effective-date>%s</effective-date>\n", effective_date));
		rxOut.write(String.format("\t\t<active>%s</active>\n", active));
		// Write the rating data section
		rxOut.write("\t\t<rating-points>\n");
		sFrmt = "\t\t\t<point><ind>%s</ind><dep>%s</dep></point>\n";
		for (String[] rowVals : ratingRows)
		{
			rxOut.write(String.format(sFrmt, rowVals[0], rowVals[2])); 
		}
		rxOut.write("\t\t</rating-points>\n");
		rxOut.write("\t</rating>\n");
		rxOut.write("</ratings>\n");
		rxOut.close(); 
	}

	
	void doViewRatingSpecs(String officeId, String locationId)
		throws Exception
	{
		if ((officeId.isEmpty()) || (officeId == null))
		{
			System.out.println("Ths office ID is either empty or null");
			Logger.instance().warning("Ths office ID is either empty or null");
			return;
		}
		for (String[] ratingSpec : getRatingSpecs(officeId, locationId))
		{
			String tmpRatingSpec = "";
			for (int i=0; i<ratingSpec.length; i++)
			{
				if ((i==0) | (i==1))
				{
					tmpRatingSpec = tmpRatingSpec + String.format("%-10s ", ratingSpec[i]);
				}
				else if (i==3)
				{
					tmpRatingSpec = tmpRatingSpec + String.format("%-30s ", ratingSpec[i]);
				}
				else
				{
					tmpRatingSpec = tmpRatingSpec + String.format("%-40s ", ratingSpec[i]);
				}
			}
			System.out.println(tmpRatingSpec);
			Logger.instance().info(tmpRatingSpec);
		}
	}

	
	/*
	 * Execute the DB query with the SQL statement	
	 */
	ResultSet doExecuteQuery(Connection conn, String qryString)
		throws DbIoException
	{
		Statement	queryStmt = null;
		String		msgString;
		
		try
		{
			if (conn == null)
			{
				msgString = "doDBQuery() Invalid connection object, conn = " + conn;
				Logger.instance().warning(msgString);
				throw new DbIoException(msgString);
			}
			
			msgString = "SQL query '" + qryString + "'";
			queryStmt = conn.createStatement();
			Logger.instance().debug3(msgString);
			
			return queryStmt.executeQuery(qryString);
		}
		catch(SQLException ex)
		{
			msgString = "SQL Error in query '" + qryString + "': " + ex;
			Logger.instance().warning(msgString);
			throw new DbIoException(msgString);
		}
	}

	
	/*
	 * Retrieve the rating IDs from CWMS DB
	 */
	ArrayList<String[]> getRatingSpecs(String officeId, String locationId)
	  throws DbIoException
	{
		ArrayList<String[]> retString = null;
		String 		qryString;
		ResultSet 	qryResult;
		
		qryString = "select OFFICE_ID,LOCATION_ID,RATING_ID,TEMPLATE_ID,DESCRIPTION " +
					"from cwms_v_rating_spec where " +
					"OFFICE_ID in('" + officeId.trim() + "') and " +
					"LOCATION_ID in('" + locationId.trim() + "') " +
					"order by OFFICE_ID,LOCATION_ID,RATING_ID";
		
		try
		{
			qryResult =doExecuteQuery(conn, qryString);
			retString = new ArrayList<String[]>();
			while(qryResult.next())
			{
				Logger.instance().debug3("Query result: " + 
					" Office_ID = " + qryResult.getString("OFFICE_ID") +
					" Location_ID = " + qryResult.getString("LOCATION_ID") +
					" Rating_ID = " + qryResult.getString("RATING_ID") +
					" Template_ID = " + qryResult.getString("TEMPLATE_ID") +
					" Description = " + qryResult.getString("DESCRIPTION"));
				String[] rowVals = new String[5];
				for (int i=0; i<rowVals.length; i++)
				{
					rowVals[i] = qryResult.getString(i+1);
				}
				retString.add(rowVals);
			}
		}
		catch(SQLException ex)
		{
			Logger.instance().warning(
				"Error reading rating specs for Location Code="
				+ locationId + ": " + qryString + " Exception: " + ex.toString());
		}
		
		return retString;
	}

	
	/*
	 * Retrieve the rating IDs from CWMS DB
	 */
	ArrayList<String> getRatingIds(String officeId, String locationId)
	  throws DbIoException
	{
		ArrayList<String> retString = null;
		String 		qryString;
		ResultSet 	qryResult;
		
		qryString = "select RATING_ID from cwms_v_rating_spec where " +
					"OFFICE_ID in('" + officeId.trim() + "') and " +
					"LOCATION_ID in('" + locationId.trim() + "')";
		try
		{
			qryResult =doExecuteQuery(conn, qryString);
			retString = new ArrayList<String>();
			while(qryResult.next())
			{
				Logger.instance().debug3("Query result: " + qryResult.getString("RATING_ID"));
				retString.add(qryResult.getString("RATING_ID"));
			}
		}
		catch(SQLException ex)
		{
			Logger.instance().warning(
				"Error reading rating spec IDs for Location Code="
				+ locationId + ": " + qryString + " Exception: " + ex.toString());
		}
		
		return retString;
	}

	
	static void setTimeZone(TimeZone _tz)
	{
		tz = _tz;
	}

	
	void shutdown()
	{
		closeDb();
	}

	
	public void finalize()
	{
		shutdown();
	}

	
	public static void main(String args[])
		throws Exception
	{
		CwmsDbUtils cdu = new CwmsDbUtils();
		cdu.execute(args);
	}
}
