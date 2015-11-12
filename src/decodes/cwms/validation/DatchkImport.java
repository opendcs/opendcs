/**
 * $Id$
 * 
 * Copyright 2015 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
 * 
 * $Log$
 */
package decodes.cwms.validation;

import java.io.ByteArrayOutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;

import opendcs.dai.TimeSeriesDAI;
import ilex.cmdline.BooleanToken;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.Base64;
import decodes.cwms.CwmsTimeSeriesDb;
import decodes.cwms.CwmsTsId;
import decodes.cwms.validation.dao.ScreeningDAI;
import decodes.cwms.validation.dao.TsidScreeningAssignment;
import decodes.sql.DbKey;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsdbAppTemplate;
import decodes.util.CmdLineArgs;

public class DatchkImport extends TsdbAppTemplate
{
	public final static String module = "DatchkImport";
	
	private StringToken configFile = new StringToken("", "DATCHK Config File",
		"", TokenOptions.optArgument | TokenOptions.optRequired, "");
	private BooleanToken confirmEach = new BooleanToken("C", "(confirm each screening before write",
		"", TokenOptions.optSwitch, false);
	
	
	public DatchkImport()
	{
		super("screening.log");
	}

	@Override
	protected void runApp() 
		throws Exception
	{
		DatchkReader reader = new DatchkReader(configFile.getValue());
		
		System.out.println("Loading screenings specified in " + configFile.getValue());
		reader.reloadAll();
		System.out.println("" + reader.getScreenedTsids().size() + " screenings read.");
		
		ArrayList<TsidScreeningAssignment> importAssignments = new ArrayList<TsidScreeningAssignment>();
		
		MessageDigest md = MessageDigest.getInstance("SHA");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DigestOutputStream dos = new DigestOutputStream(baos, md);
		Base64 base64 = new Base64();

		
		for(String tsidStr : reader.getScreenedTsids())
		{
			Screening screening = reader.findScreening(tsidStr);
			if (screening == null)
			{
				warning("After loading, cannot find screening for '" + tsidStr + "'");
				continue;
			}
			
			// DATCHK Reader will create a separate screening for every time series with the name
			// of the screening being the TSID.
			// CWMS requires a unique name with max of 16 chars. 
			// So the name will be LLLLLL-PPP-nnnnn, where ...
			// LLLLLL is the location ID, truncated to 6 chars
			// PPP is the param ID truncated to 6 chars
			// nnnnn is the first 5 chars of a base-64 representation of the SHA hash of the entire tsid.
			CwmsTsId cwmsTsid = new CwmsTsId();
			cwmsTsid.setUniqueString(tsidStr);
			String loc = cwmsTsid.getPart("location");
			if (loc.length() > 6)
				loc = loc.substring(0, 6);
			String param = cwmsTsid.getPart("param");
			if (param.length() > 4)
				param = param.substring(0, 4);
			dos.write(tsidStr.getBytes());
			byte []sha = md.digest();
			String hash = new String(base64.encode(sha)).substring(0, 4);

			screening.setScreeningName(loc + "." + param + "." + hash);
			screening.setParamId(cwmsTsid.getPart("param"));
			screening.setParamTypeId(cwmsTsid.getPart("paramtype"));
			screening.setDurationId(cwmsTsid.getPart("duration"));

			importAssignments.add(new TsidScreeningAssignment(cwmsTsid, screening, true));
		}
		
		System.out.println("Will import the following screenings:");
		for (TsidScreeningAssignment tsa : importAssignments)
			System.out.println("\tScreening " + tsa.getScreening().getScreeningName()
				+ " for tsid " + tsa.getTsid().getUniqueString());

		System.out.println();
		System.out.print("Enter y to proceed, or n to cancel: ");
		System.out.flush();
		String answer = System.console().readLine();
		if (answer == null || answer.trim().length() == 0 || !answer.trim().toUpperCase().startsWith("Y"))
			System.exit(0);
		
		CwmsTimeSeriesDb cwmsDb = (CwmsTimeSeriesDb)theDb;
		TimeSeriesDAI timeSeriesDAO = theDb.makeTimeSeriesDAO();
		ScreeningDAI screeningDAO = cwmsDb.makeScreeningDAO();
		for (TsidScreeningAssignment tsa : importAssignments)
		{
			if (confirmEach.getValue())
			{
				System.out.print("Import screening " + tsa.getScreening().getScreeningName() + "? (y/n) ");
				answer = System.console().readLine();
				if (answer == null)
					break;
				else if (answer.trim().length() == 0 || !answer.trim().toUpperCase().startsWith("Y"))
					continue;
			}
			try
			{
				TimeSeriesIdentifier dbTsid = timeSeriesDAO.getTimeSeriesIdentifier(tsa.getTsid().getUniqueString());
				System.out.println("Time series " + tsa.getTsid().getUniqueString() + " already exists with ts_code=" 
					+ dbTsid.getKey());
				tsa.setTsid(dbTsid);
			}
			catch(NoSuchObjectException ex)
			{
				// Make sure the time series for this screening exists. If it is a
				// never before seen paramId, the screening cannot be created.
				System.out.println("Creating time series " + tsa.getTsid().getUniqueString());
				tsa.getTsid().setKey(timeSeriesDAO.createTimeSeries(tsa.getTsid()));
			}
			System.out.println("Importing Screening " + tsa.getScreening().getScreeningName() + ":");
			//DatchkReader.printScreening(tsa.getScreening());
			screeningDAO.writeScreening(tsa.getScreening());
			System.out.println("Importing assignment to " + tsa.getTsid().getUniqueString());
			screeningDAO.assignScreening(tsa.getScreening(), tsa.getTsid(), true);
		}
	}
		
	@Override
	public void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		cmdLineArgs.addToken(confirmEach);
		cmdLineArgs.addToken(configFile);
	}
	
	public static void main(String[] args)
		throws Exception
	{
		DatchkImport me = new DatchkImport();
		me.execute(args);
	}


}
