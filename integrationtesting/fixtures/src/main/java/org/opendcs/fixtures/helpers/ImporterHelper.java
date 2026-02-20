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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.TimeZone;

import decodes.cwms.CwmsTimeSeriesDb;
import decodes.cwms.rating.CwmsRatingDao;
import decodes.db.Constants;
import decodes.db.Site;
import decodes.db.SiteName;
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
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.fixtures.Programs;
import org.opendcs.fixtures.spi.Configuration;
import org.opendcs.utils.FailableResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.security.SystemExit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class ImporterHelper
{
	private static final Logger log = LoggerFactory.getLogger(ImporterHelper.class);
	private final TimeSeriesDb tsDb;
	private final EnvironmentVariables environment;
	private final SystemExit exit;
	private final CONTEXT context;
	private final Configuration configuration;

	public ImporterHelper(TimeSeriesDb tsDb, Configuration configuration, EnvironmentVariables environment, SystemExit exit, CONTEXT context)
	{
		this.tsDb = tsDb;
		this.environment = environment;
		this.exit = exit;
		this.context = context;
		this.configuration = configuration;
	}

	public enum CONTEXT
	{
		REST_API,
		TOOLKIT
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
				log.info("Comps: " + comp_data.getAbsolutePath());
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
					log.atError().setCause(ex).log("Error getting Computation: " + test.getName()+comp.getName());
					throw new RuntimeException(ex);
				}
			}
			else if (name.contains(".config"))
			{
				log.info("Has config: " + comp_data.getAbsolutePath());
				File configFile = new File(comp_data.getAbsolutePath());
				try (InputStream configStream = new FileInputStream(configFile)) {
					String firstLine = new BufferedReader(new InputStreamReader(configStream)).readLine();
					String keyword = "EnableOn:";
					if (firstLine != null && firstLine.contains(keyword)) {
						String substring = firstLine.substring(firstLine.indexOf(keyword) + keyword.length()).trim();
						final String testEngine = System.getProperty("opendcs.test.engine", "").trim();
						assumeFalse(!substring.equals(testEngine), "Test is disabled by config file for: " + substring);
					}
				}
				catch (IOException ex)
				{
					log.atError().setCause(ex).log("Error reading config file: " + comp_data.getAbsolutePath());
					throw new RuntimeException(ex);
				}
			}
			else if (name.endsWith(".sql"))
			{
				log.info("Found SQL file: " + comp_data.getAbsolutePath());
				try
				{
					executeSqlFile(configuration.getOpenDcsDatabase(), comp_data);
				}
				catch (Throwable ex)
				{
					log.atError().setCause(ex).log("Error executing SQL file: " + comp_data.getAbsolutePath());
					throw new RuntimeException(ex);
				}
			}
		}

		try
		{
			results.withInputTsList(loadTSimport(buildFilePath(test.getAbsolutePath(), "timeseries", "inputs"), importer));
			results.withOutputTsList(loadTSimport(buildFilePath(test.getAbsolutePath(), "timeseries", "outputs"), importer));
			results.withImportedTsList(loadTSimport(buildFilePath(test.getAbsolutePath(), "timeseries", "expectedOutputs"), importer));

			loadRatingimport(buildFilePath(test.getAbsolutePath(), "rating"));
			loadScreenings(buildFilePath(test.getAbsolutePath(), "screenings"));
			return results.build();
		}
		catch(Exception ex)
		{
			log.atError().setCause(ex).log("Error loading TS/Rating/Screenings");
			throw new RuntimeException(ex);
		}
	}

	public TsImporter buildTsImporter(TimeSeriesDAI tsDao, SiteDAI siteDAO)
	{
		DecodesSettings settings = DecodesSettings.instance();
		return new TsImporter(TimeZone.getTimeZone("UTC"), settings.siteNameTypePreference, (tsIdStr) ->
		{
			try
			{
				return tsDao.getTimeSeriesIdentifier(tsIdStr);
			}
			catch(Exception ex)
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
				catch(Exception ex2)
				{
					ex2.addSuppressed(ex);
					throw new DbIoException(String.format("No such time series and cannot create for '%s'", tsIdStr), ex2);
				}
			}
		});
	}

	public static String buildFilePath(String... parts) {
		// Start with the first part
		Path path = Paths.get(parts[0]);

		// Append all the other parts using resolve() so it's platform independent
		for (int i = 1; i < parts.length; i++) {
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
		File log = new File(configuration.getUserDir().getParentFile(), "screenings-import-"+ folderTS.getName()+".log");
		Programs.ImportScreenings(log, configuration.getPropertiesFile(), environment, exit, screeningsFile);
	}

	public ArrayList<CTimeSeries> loadTSimport(String folderTSstr, TsImporter importer)
			throws Exception
	{
		File folderTS = new File(folderTSstr);
		ArrayList<CTimeSeries> fullTs = new ArrayList<>();
		if (!folderTS.exists()){
			return fullTs;
		}
		for (File tsfiles : folderTS.listFiles())
		{
			String relativePath = "Comps"+tsfiles.getAbsolutePath().split("Comps")[1];
			try(TimeSeriesDAI tsDao = tsDb.makeTimeSeriesDAO();
				InputStream inputStream = TestResources.getResourceAsStream(configuration, relativePath))
			{
				Collection<CTimeSeries> allTs = importer.readTimeSeriesFile(inputStream);
				for (CTimeSeries tsIn: allTs)
				{
					log.info("load: " + tsIn.getDisplayName());
					log.info("Saving {}", tsIn.getTimeSeriesIdentifier());

					tsDao.saveTimeSeries(tsIn);
					FailableResult<TimeSeriesIdentifier, TsdbException> tsIdSavedResult = tsDao.findTimeSeriesIdentifier(tsIn.getTimeSeriesIdentifier().getUniqueString());
					assertTrue(tsIdSavedResult.isSuccess(), "Time series was not correctly saved.");
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
		try (CwmsRatingDao crd = new CwmsRatingDao((CwmsTimeSeriesDb)tsDb)){
			crd.setUseReference(false);
			for (File tsfiles : folderTS.listFiles())
			{
				try
				{
					String xml = FileUtil.getFileContents(tsfiles);
					crd.importXmlToDatabase(xml);
				}
				catch(Exception ex)
				{
					log.error(ex.getMessage(), ex);
					throw new RuntimeException("Error importing rating file: " + tsfiles.getAbsolutePath(), ex);
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
			log.warn("OpenDcsDatabase not available, skipping SQL file execution: " + sqlFile.getName());
			return;
		}

		try
		{
			// Read the SQL file content
			String sqlContent = new String(Files.readAllBytes(sqlFile.toPath()), StandardCharsets.UTF_8);

			// Replace office ID placeholder if this is a CWMS database
			if(tsDb instanceof CwmsTimeSeriesDb)
			{
				CwmsTimeSeriesDb cwmsDb = (CwmsTimeSeriesDb) tsDb;
				String officeId = cwmsDb.getDbOfficeId();
				sqlContent = sqlContent.replace("DEFAULT_OFFICE", officeId);
			}

			// Execute the SQL using a transaction
			try(DataTransaction tx = db.newTransaction())
			{
				Connection conn = tx.connection(Connection.class)
						.orElseThrow(() -> new RuntimeException("JDBC Connection not available in this transaction."));

				// Use CallableStatement to execute the SQL (handles PL/SQL blocks properly)
				try(CallableStatement stmt = conn.prepareCall(sqlContent))
				{
					log.info("Executing SQL from file: " + sqlFile.getName());
					log.debug("SQL Content: " + sqlContent.substring(0, Math.min(200, sqlContent.length())) + "...");
					stmt.execute();
				}
				log.info("Successfully executed SQL file: " + sqlFile.getName());
			}
		}
		catch(Exception ex)
		{
			log.error("Failed to execute SQL file " + sqlFile.getName() + ": " + ex.getMessage(), ex);
			// Don't fail the test, just log the error
		}
	}

	public static final class ImportResults
	{
		private final List<CTimeSeries> importedTsList;
		private final List<Long> tsCompIds;
		private final List<CTimeSeries> outputTsList;
		private final List<CTimeSeries> inputTsList;

		private ImportResults(Builder builder)
		{
			importedTsList = builder.importedTsList;
			tsCompIds = builder.tsCompIds;
			inputTsList = builder.inputTsList;
			outputTsList = builder.outputTsList;
		}

		public List<CTimeSeries> getImportedTsList()
		{
			return importedTsList;
		}

		public List<CTimeSeries> getOutputTsList()
		{
			return outputTsList;
		}

		public List<CTimeSeries> getInputTsList()
		{
			return inputTsList;
		}

		public List<Long> getTsCompIds()
		{
			return tsCompIds;
		}

		public static class Builder
		{
			private List<CTimeSeries> importedTsList;
			private List<Long> tsCompIds = new ArrayList<>();
			private List<CTimeSeries> inputTsList;
			private List<CTimeSeries> outputTsList;

			public Builder withImportedTsList(List<CTimeSeries> expectedTsList)
			{
				this.importedTsList = expectedTsList;
				return this;
			}

			public Builder withInputTsList(List<CTimeSeries> inputTsList)
			{
				this.inputTsList = inputTsList;
				return this;
			}

			public Builder withOutputTsList(List<CTimeSeries> outputTsList)
			{
				this.outputTsList = outputTsList;
				return this;
			}

			public Builder withTsCompIds(long tsCompId)
			{
				this.tsCompIds.add(tsCompId);
				return this;
			}

			public ImportResults build()
			{
				return new ImportResults(this);
			}
		}
	}
}
