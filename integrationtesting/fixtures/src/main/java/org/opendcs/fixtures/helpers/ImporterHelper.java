package org.opendcs.fixtures.helpers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Pattern;

import decodes.cwms.CwmsTimeSeriesDb;
import decodes.cwms.rating.CwmsRatingDao;
import decodes.db.Constants;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DbComputation;
import decodes.tsdb.DbIoException;
import decodes.tsdb.ImportComp;
import decodes.tsdb.NoSuchObjectException;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsImporter;
import decodes.tsdb.TsdbException;
import decodes.util.DecodesSettings;
import ilex.util.FileUtil;
import opendcs.dai.ComputationDAI;
import opendcs.dai.SiteDAI;
import opendcs.dai.TimeSeriesDAI;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.fixtures.Programs;
import org.opendcs.fixtures.spi.Configuration;
import org.opendcs.utils.FailableResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.security.SystemExit;

import hec.data.RatingException;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class ImporterHelper
{
	private static final Logger log = LoggerFactory.getLogger(ImporterHelper.class);
	static final Pattern comps = Pattern.compile("Comps");
	private final TimeSeriesDb tsDb;
	private final EnvironmentVariables environment;
	private final SystemExit exit;
	private final Configuration configuration;

	public ImporterHelper(TimeSeriesDb tsDb, Configuration configuration, EnvironmentVariables environment, SystemExit exit)
	{
		this.tsDb = tsDb;
		this.environment = environment;
		this.exit = exit;
		this.configuration = configuration;
	}

	public ImportResults doImport(File test, File comp, TsImporter importer)
	{
		ImportResults.Builder results = new ImportResults.Builder();
		for (File comp_data : test.listFiles())
		{
			// Process comp xml
			String name = comp_data.getName();
			if (name.contains("Comp.xml"))
			{
				log.info("Comps: {}", comp_data.getAbsolutePath());
				String compstr = comp_data.getAbsolutePath();
				List<String> compxml =  Arrays.asList(compstr);
				ImportComp ic = new ImportComp(tsDb, true, false, compxml);
				ic.runApp();

				DbComputation testComp = null;
				try (ComputationDAI compdao = tsDb.makeComputationDAO())
				{
					testComp = compdao.getComputationByName(test.getName()+comp.getName());
					results.withTsCompIds(testComp.getId().getValue());
				}
				catch (NoSuchObjectException | DbIoException ex)
				{
					String msg = String.format("Error getting Computation: %s%s", test.getName(), comp.getName());
					log.atError().setCause(ex).log(msg);
					throw new ImportException(msg, ex);
				}
			}
			else if (name.contains(".config"))
			{
				handleConfig(comp_data);
			}
			else if (name.endsWith(".sql"))
			{
				handleSql(comp_data);
			}
		}

		try
		{
			final String timeseries = "timeseries";
			results.withInputTsList(loadTSimport(buildFilePath(test.getAbsolutePath(), timeseries, "inputs"), importer));
			results.withOutputTsList(loadTSimport(buildFilePath(test.getAbsolutePath(), timeseries, "outputs"), importer));
			results.withImportedTsList(loadTSimport(buildFilePath(test.getAbsolutePath(), timeseries, "expectedOutputs"), importer));

			loadRatingimport(buildFilePath(test.getAbsolutePath(), "rating"));
			loadScreenings(buildFilePath(test.getAbsolutePath(), "screenings"));
			return results.build();
		}
		catch(Exception ex)
		{
			String msg = "Error loading TS/Rating/Screenings";
			log.atError().setCause(ex).log(msg);
			throw new ImportException(msg, ex);
		}
	}

	private void handleConfig(File compData)
	{
		log.info("Has config: {}", compData.getAbsolutePath());
		File configFile = new File(compData.getAbsolutePath());
		try (InputStream configStream = new FileInputStream(configFile))
		{
			String firstLine = new BufferedReader(new InputStreamReader(configStream)).readLine();
			String keyword = "EnableOn:";
			if (firstLine != null && firstLine.contains(keyword))
			{
				String substring = firstLine.substring(firstLine.indexOf(keyword) + keyword.length()).trim();
				final String testEngine = System.getProperty("opendcs.test.engine", "").trim();
				assumeFalse(!substring.equals(testEngine), "Test is disabled by config file for: " + substring);
			}
		}
		catch (IOException ex)
		{
			String msg = String.format("Error reading config file: %s", compData.getAbsolutePath());
			log.atError().setCause(ex).log(msg);
			throw new ImportException(msg, ex);
		}
	}

	private void handleSql(File compData)
	{
		log.info("Found SQL file: {}", compData.getAbsolutePath());
		try
		{
			executeSqlFile(configuration.getOpenDcsDatabase(), compData);
		}
		catch (Throwable ex)
		{
			String msg = String.format("Error executing SQL file: %s", compData.getAbsolutePath());
			log.atError().setCause(ex).log(msg);
			throw new ImportException(msg, ex);
		}
	}

	public TsImporter buildTsImporter(TimeSeriesDAI tsDao, SiteDAI siteDAO)
	{
		DecodesSettings settings = DecodesSettings.instance();
		return new TsImporter(TimeZone.getTimeZone("UTC"), settings.siteNameTypePreference, tsIdStr ->
		{
			try
			{
				return tsDao.getTimeSeriesIdentifier(tsIdStr);
			}
			catch(DbIoException | NoSuchObjectException ex)
			{
				log.warn("No existing time series. Will attempt to create.");

				try
				{
					TimeSeriesIdentifier tsId = tsDb.makeEmptyTsId();
					tsId.setUniqueString(tsIdStr);
					Site site = tsDb.getSiteById(siteDAO.lookupSiteID(tsId.getSiteName()));
					if(site == null)
					{
						site = new Site();
						site.addName(new SiteName(site, Constants.snt_CWMS, tsId.getSiteName()));
						siteDAO.writeSite(site);
					}
					tsId.setSite(site);

					log.info("Calling createTimeSeries");
					tsDao.createTimeSeries(tsId);
					log.info("After createTimeSeries, ts key = {}", tsId.getKey());
					return tsId;
				}
				catch(DbIoException | NoSuchObjectException | BadTimeSeriesException ex2)
				{
					ex2.addSuppressed(ex);
					throw new DbIoException(String.format("No such time series and cannot create for '%s'", tsIdStr), ex2);
				}
			}
		});
	}

	public static String buildFilePath(String... parts)
	{
		// Start with the first part
		Path path = Paths.get(parts[0]);

		// Append all the other parts using resolve() so it's platform independent
		for (int i = 1; i < parts.length; i++)
		{
			path = path.resolve(parts[i]);
		}

		// Return the platform-specific path as a string
		return path.toString();
	}

	public void loadScreenings(String screeningsFile) throws Exception
	{
		File folderTS = new File(screeningsFile);
		if (!folderTS.exists())
		{
			return;
		}
		File logFile = new File(configuration.getUserDir().getParentFile(), "screenings-import-"+ folderTS.getName()+".log");
		Programs.ImportScreenings(logFile, configuration.getPropertiesFile(), environment, exit, screeningsFile);
	}

	public List<CTimeSeries> loadTSimport(String folderTSstr, TsImporter importer)
			throws Exception
	{
		File folderTS = new File(folderTSstr);
		ArrayList<CTimeSeries> fullTs = new ArrayList<>();
		if (!folderTS.exists())
		{
			return fullTs;
		}
		for (File tsfiles : folderTS.listFiles())
		{
			String relativePath = "Comps"+tsfiles.getAbsolutePath().split(comps.pattern())[1];
			try(TimeSeriesDAI tsDao = tsDb.makeTimeSeriesDAO();
				InputStream inputStream = TestResources.getResourceAsStream(configuration, relativePath))
			{
				Collection<CTimeSeries> allTs = importer.readTimeSeriesFile(inputStream);
				for (CTimeSeries tsIn: allTs)
				{
					log.info("load: {}", tsIn.getDisplayName());
					log.info("Saving {}", tsIn.getTimeSeriesIdentifier());

					tsDao.saveTimeSeries(tsIn);
					FailableResult<TimeSeriesIdentifier, TsdbException> tsIdSavedResult = tsDao.findTimeSeriesIdentifier(tsIn.getTimeSeriesIdentifier().getUniqueString());
					if (!tsIdSavedResult.isSuccess())
					{
						TsdbException ex = tsIdSavedResult.getFailure();
							log.error("Failed to save time series: {}", tsIn.getTimeSeriesIdentifier().getUniqueString(), ex);
						throw ex;
					}
				}
				fullTs.addAll(allTs);
			}
		}
		return fullTs;
	}

	public void loadRatingimport(String folderRatingStr)
	{
		File folderTS = new File(folderRatingStr);
		if (!folderTS.exists())
			return;
		try (CwmsRatingDao crd = new CwmsRatingDao((CwmsTimeSeriesDb) tsDb))
		{
			crd.setUseReference(false);
			for (File tsfiles : folderTS.listFiles())
			{
				try
				{
					String xml = FileUtil.getFileContents(tsfiles);
					crd.importXmlToDatabase(xml);
				}
				catch(IOException | RatingException ex)
				{
					String msg = String.format("Error importing rating file: %s", tsfiles.getAbsolutePath());
					throw new ImportException(msg, ex);
				}
			}
		}
	}

	/**
	 * Execute SQL file in the database for test setup
	 * @param sqlFile The SQL file to execute
	 */
	public void executeSqlFile(OpenDcsDatabase db, File sqlFile)
	{
		if(db == null)
		{
			log.warn("OpenDcsDatabase not available, skipping SQL file execution: {}", sqlFile.getName());
			return;
		}

		try
		{
			// Read the SQL file content
			String sqlContent = new String(Files.readAllBytes(sqlFile.toPath()), StandardCharsets.UTF_8);

			// Replace office ID placeholder if this is a CWMS database
			if(tsDb instanceof CwmsTimeSeriesDb cwmsDb)
			{
				String officeId = cwmsDb.getDbOfficeId();
				sqlContent = sqlContent.replace("DEFAULT_OFFICE", officeId);
			}

			// Execute the SQL using a transaction
			try(DataTransaction tx = db.newTransaction();
				Connection conn = tx.connection(Connection.class)
					.orElseThrow(() -> new RuntimeException("JDBC Connection not available in this transaction.")))
			{
				// Use CallableStatement to execute the SQL (handles PL/SQL blocks properly)
				try(CallableStatement stmt = conn.prepareCall(sqlContent))
				{
					log.info("Executing SQL from file: {}", sqlFile.getName());
					String logString = sqlContent.substring(0, Math.min(200, sqlContent.length()));
					log.debug("SQL Content: {}...", logString);
					stmt.execute();
				}
				log.info("Successfully executed SQL file: {}", sqlFile.getName());
			}
		}
		catch(SQLException | OpenDcsDataException | IOException ex)
		{
			log.error("Failed to execute SQL file " + sqlFile.getName() + ": " + ex.getMessage(), ex);
			// Don't fail the test, just log the error
		}
	}

	public static class ImportException extends RuntimeException
	{
		public ImportException(String message)
		{
			super(message);
		}

		public ImportException(String message, Throwable cause)
		{
			super(message, cause);
		}
	}
}
