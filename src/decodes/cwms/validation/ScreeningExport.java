package decodes.cwms.validation;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.StringTokenizer;

import opendcs.dai.TimeSeriesDAI;
import ilex.cmdline.BooleanToken;
import ilex.cmdline.StringToken;
import ilex.cmdline.TokenOptions;
import ilex.util.Logger;
import lrgs.gui.DecodesInterface;
import decodes.cwms.CwmsTimeSeriesDb;
import decodes.cwms.validation.dao.ScreeningDAI;
import decodes.cwms.validation.dao.TsidScreeningAssignment;
import decodes.db.Constants;
import decodes.db.DataPresentation;
import decodes.db.DataType;
import decodes.db.Database;
import decodes.db.EngineeringUnit;
import decodes.db.InvalidDatabaseException;
import decodes.db.PresentationGroup;
import decodes.db.UnitConverter;
import decodes.sql.DbKey;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsdbAppTemplate;
import decodes.util.CmdLineArgs;
import decodes.util.DecodesException;

/**
 * Export CWMS Screening to DATCHK file format
 */
public class ScreeningExport extends TsdbAppTemplate
{
	public static final String module = "ScreeningExport";
	
	private StringToken screeningIdArg = new StringToken("n", "screening ID",
		"", TokenOptions.optSwitch | TokenOptions.optMultiple, "");
	private BooleanToken nameListOnStdin = new BooleanToken("q", "(Accept list of IDs from stdin.)",
		"", TokenOptions.optSwitch, false);
	private StringToken presentationArg = new StringToken("G", "PresentationGroup to define screening units", "", 
		TokenOptions.optSwitch, "");
	private BooleanToken allArg = new BooleanToken("A", "(Export all screenings)",
		"", TokenOptions.optSwitch, false);
	private BooleanToken englishUnitsArg = new BooleanToken("E", "(Export in English units)",
		"", TokenOptions.optSwitch, false);
	private StringToken tsIdArg = new StringToken("T", "Time Series ID -- output screening for referenced TSID.",
		"", TokenOptions.optSwitch | TokenOptions.optMultiple, "");


	
	private ArrayList<String> screeningIDs = new ArrayList<String>();
	private NumberFormat nf = NumberFormat.getNumberInstance();
	private NumberFormat intf = NumberFormat.getIntegerInstance();
	private PresentationGroup presGroup = null;
	private DataPresentation dataPres = null;
	private UnitConverter unitConverter = null;
	

	public ScreeningExport()
	{
		super("screening.log");
		nf.setMaximumFractionDigits(3);
		nf.setGroupingUsed(false);
		intf.setGroupingUsed(false);
	}

	public static void main(String[] args)
		throws Exception
	{
		ScreeningExport me = new ScreeningExport();
		me.execute(args);
	}

	@Override
	public void addCustomArgs(CmdLineArgs cmdLineArgs)
	{
		cmdLineArgs.addToken(screeningIdArg);
		cmdLineArgs.addToken(nameListOnStdin);
		cmdLineArgs.addToken(presentationArg);
		cmdLineArgs.addToken(allArg);
		cmdLineArgs.addToken(englishUnitsArg);
		cmdLineArgs.addToken(tsIdArg);
		DecodesInterface.silent = true;
	}

	@Override
	protected void runApp() throws Exception
	{
		if (!theDb.isCwms())
		{
			System.err.println("This utility exports CWMS screenings. You are not connected to a CWMS database.");
			System.exit(1);
		}
		CwmsTimeSeriesDb cwmsDb = (CwmsTimeSeriesDb)theDb;
		
		String pgName = presentationArg.getValue();
		if (pgName != null && pgName.length() > 0)
		{
			presGroup = Database.getDb().presentationGroupList.find(pgName);
			try
			{
				if (presGroup != null)
					presGroup.prepareForExec();
			}
			catch (InvalidDatabaseException ex)
			{
				warning("Cannot initialize presentation group '" + pgName + ": " + ex);
				presGroup = null;
			}
		}
		ScreeningDAI screeningDAO = cwmsDb.makeScreeningDAO();
		TimeSeriesDAI timeSeriesDAO = cwmsDb.makeTimeSeriesDAO();

		if (allArg.getValue())
		{
			ArrayList<Screening> allScreenings = screeningDAO.getAllScreenings();
			for(Screening s : allScreenings)
				screeningIDs.add(s.getScreeningName());
		}
		else
		{
			for(int idx = 0; idx < screeningIdArg.NumberOfValues(); idx++)
			{
				String id = screeningIdArg.getValue(idx);
				if (id == null || id.trim().length() == 0)
					continue;
				screeningIDs.add(id);
			}
			for(int idx = 0; idx < tsIdArg.NumberOfValues(); idx++)
			{
				String tsidStr = tsIdArg.getValue(idx);
				try
				{
					TimeSeriesIdentifier tsid = timeSeriesDAO.getTimeSeriesIdentifier(tsidStr);
					TsidScreeningAssignment tsa = screeningDAO.getScreeningForTS(tsid);
					if (tsa == null)
						warning("No screening assigned to '" + tsidStr + "'");
					else screeningIDs.add(tsa.getScreening().getScreeningName());
				}
				catch(NoSuchObjectException ex)
				{
					warning("No such time series '" + tsidStr + "': " + ex);
				}
			}
			
			if (nameListOnStdin.getValue())
			{
				String line;
				while((line = System.console().readLine()) != null)
				{
					screeningIDs.add(line);
				}
			}
		}
		
		if (screeningIDs.size() == 0)
		{
			System.err.println("No Screening IDs provided. Nothing to do.");
			System.exit(1);
		}
		
		ArrayList<TsidScreeningAssignment> tsidAssignments = screeningDAO.getTsidScreeningAssignments(false);
		for(String screeningID : screeningIDs)
		{
			DbKey key = screeningDAO.getKeyForId(screeningID);
			if (DbKey.isNull(key))
			{
				System.err.println("Screening ID '" + screeningID + "' not found.");
				continue;
			}
				
			Screening screening = screeningDAO.getByKey(key);
			output(screening);
			
			System.out.println();
			// Output assignments for this screening, if any
			for(TsidScreeningAssignment tsa : tsidAssignments)
				if (tsa.getScreening().getKey().equals(key))
				{
					System.out.println("ASSIGN " + tsa.getTsid().getUniqueString() + " : "
						+ screening.getScreeningName());
				}
			System.out.println();	
		}
		screeningDAO.close();
		timeSeriesDAO.close();
	}
	
	private void output(Screening screening)
	{
		String unitsAbbr = ((CwmsTimeSeriesDb)theDb).getBaseParam().getStoreUnits4Param(screening.getParamId());

		if (englishUnitsArg.getValue())
		{
			// Convert all the limits in this screening to the english units
			String storUnits = screening.getCheckUnitsAbbr();
			String engUnits = ((CwmsTimeSeriesDb)theDb).getBaseParam().getEnglishUnits4Param(screening.getParamId());
Logger.instance().info("English units requested, looking for converter for param '" + screening.getParamId()
	+ "' storUnits='" + storUnits + "' engunits='" + engUnits + "'");
			unitConverter = Database.getDb().unitConverterSet.get(
				EngineeringUnit.getEngineeringUnit(storUnits), 
				EngineeringUnit.getEngineeringUnit(engUnits));
			unitsAbbr = engUnits;
		}
		System.out.println("*===========================================");
		System.out.println("SCREENING " + screening.getScreeningName());
		if (screening.getScreeningDesc() != null)
		{
			String desc = screening.getScreeningDesc();
			StringTokenizer st = new StringTokenizer(desc, "\r\n");
			while(st.hasMoreTokens())
				System.out.println("DESC " + st.nextToken());
		}
		
		System.out.println("PARAM " + screening.getParamId());
		
		if (screening.getParamTypeId() != null)
			System.out.println("PARAMTYPE " + screening.getParamTypeId());
		if (screening.getDurationId() != null)
			System.out.println("DURATION " + screening.getDurationId());
		
		dataPres = null;
		if (presGroup != null)
		{
			DataType dt = DataType.getDataType(Constants.datatype_CWMS, screening.getParamId());
			dataPres = presGroup.findDataPresentation(dt);
			if (dataPres == null)
			{
				// try with just the base param
				int idx = screening.getParamId().indexOf('-');
				if (idx > 0)
				{
					dt = DataType.getDataType(Constants.datatype_CWMS, screening.getParamId().substring(0, idx));
					dataPres = presGroup.findDataPresentation(dt);
				}
			}
		}
		if (dataPres != null && dataPres.getUnitsAbbr() != null && !unitsAbbr.equalsIgnoreCase(dataPres.getUnitsAbbr()))
		{
			unitConverter = decodes.db.Database.getDb().unitConverterSet.get(
				EngineeringUnit.getEngineeringUnit(unitsAbbr), EngineeringUnit.getEngineeringUnit(dataPres.getUnitsAbbr()));
			if (unitConverter == null)
			{
				System.err.println("Cannot convert param " + screening.getParamId() + " units from "
					+ unitsAbbr + " to " + dataPres.getUnitsAbbr() + " -- will output limits in " + unitsAbbr);
			}
			else
				unitsAbbr = dataPres.getUnitsAbbr();
		}
		System.out.println("UNITS " + unitsAbbr);

		System.out.println("RANGE_ACTIVE " + screening.isRangeActive());
		System.out.println("ROC_ACTIVE " + screening.isRocActive());
		System.out.println("CONST_ACTIVE " + screening.isConstActive());
		System.out.println("DURMAG_ACTIVE " + screening.isDurMagActive());
		
		for(ScreeningCriteria crit : screening.criteriaSeasons)
			outputCrit(crit);
		
		System.out.println("SCREENING_END");
	}

	private void outputCrit(ScreeningCriteria crit)
	{
		System.out.println();
		
		System.out.println("CRITERIA_SET");

		Calendar cal = crit.getSeasonStart();
		if (cal != null)
			System.out.println("SEASON " + (cal.get(Calendar.MONTH)+1) + "/" + cal.get(Calendar.DAY_OF_MONTH));
		
		String est = crit.getEstimateExpression();
		if (est != null && est.trim().length() > 0)
			System.out.println("ESTIMATE " + est);
		
		AbsCheck abs = crit.getAbsCheckFor('R');
		if (abs != null)
			System.out.println("CRITERIA ABS R " + nf.format(cnvt(abs.getLow())) 
				+ " " + nf.format(cnvt(abs.getHigh())));
		
		abs = crit.getAbsCheckFor('Q');
		if (abs != null)
			System.out.println("CRITERIA ABS Q " + nf.format(cnvt(abs.getLow())) 
				+ " " + nf.format(cnvt(abs.getHigh())));
		
		RocPerHourCheck roc = crit.getRocCheckFor('R');
		if (roc != null)
			System.out.println("CRITERIA RATE R " + nf.format(cnvt(roc.getFall())) 
				+ " " + nf.format(cnvt(roc.getRise())));
		
		roc = crit.getRocCheckFor('Q');
		if (roc != null)
			System.out.println("CRITERIA RATE Q " + nf.format(cnvt(roc.getFall())) 
				+ " " + nf.format(cnvt(roc.getRise())));
		
		ConstCheck con = crit.getConstCheckFor('R');
		if (con != null)
			System.out.println("CRITERIA CONST R " + cwmsDur2Datchk(con.getDuration()) + " "
				+ nf.format(cnvt(con.getMinToCheck())) + " " + nf.format(cnvt(con.getTolerance())) 
				+ " " + intf.format(con.getAllowedMissing()));
		
		con = crit.getConstCheckFor('Q');
		if (con != null)
			System.out.println("CRITERIA CONST Q " + cwmsDur2Datchk(con.getDuration()) + " "
				+ nf.format(cnvt(con.getMinToCheck())) + " " + nf.format(cnvt(con.getTolerance())) 
				+ " " + intf.format(con.getAllowedMissing()));
		
		for(DurCheckPeriod dcp : crit.durCheckPeriods)
			System.out.println("CRITERIA DUR " + dcp.getFlag() + " " + nf.format(cnvt(dcp.getLow()))
				+ " " + nf.format(cnvt(dcp.getHigh())) + " " + cwmsDur2Datchk(dcp.getDuration()));
		
		System.out.println("CRITERIA_SET_END");
	}
	
	public double cnvt(double stor)
	{
		if (unitConverter != null)
			try
			{
				return unitConverter.convert(stor);
			}
			catch (DecodesException ex)
			{
				System.err.println("Cannot convert limit " + stor + ": " + ex);
			}
		return stor;
	}
	
	private String cwmsDur2Datchk(String dur)
	{
		StringBuilder sb = new StringBuilder();
		for(int idx = 0; idx < dur.length(); idx++)
			if (Character.isDigit(dur.charAt(idx)))
				sb.append(dur.charAt(idx));
			else
			{
				sb.append(dur.charAt(idx));
				break;
			}
		return sb.toString();
	}
}
