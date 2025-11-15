package org.opendcs.regression_tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.opendcs.fixtures.assertions.TimeSeries.assertEquals;
import static org.opendcs.fixtures.helpers.TestResources.getResource;

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.CallableStatement;
import java.util.Collection;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;
import org.opendcs.fixtures.annotations.EnableIfTsDb;
import org.opendcs.fixtures.Programs;
import org.opendcs.fixtures.helpers.TestResources;
import org.opendcs.utils.FailableResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import decodes.db.Constants;
import decodes.db.DatabaseException;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.db.UnitConverter;
import decodes.tsdb.BadTimeSeriesException;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DbIoException;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsImporter;
import decodes.tsdb.ImportComp;
import decodes.tsdb.TsdbException;
import decodes.tsdb.VarFlags;
import decodes.util.DecodesSettings;
import decodes.util.TSUtil;
import decodes.tsdb.DataCollection;
import decodes.tsdb.DbCompParm;
import decodes.tsdb.DbComputation;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dao.ComputationDAO;
import opendcs.dai.ComputationDAI;
import opendcs.dai.SiteDAI;
import decodes.cwms.CwmsFlags;
import decodes.cwms.CwmsTimeSeriesDb;
import decodes.cwms.rating.CwmsRatingDao;
import ilex.var.TimedVariable;
import ilex.util.FileUtil;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDatabase;

import hec.data.RatingException;

@DecodesConfigurationRequired({
    "shared/test-sites.xml"})
public class AlgorithmTestsIT extends AppTestBase
{
    private Logger log = LoggerFactory.getLogger(AlgorithmTestsIT.class);

    @ConfiguredField
    private TimeSeriesDb tsDb;
    
    @ConfiguredField
    private OpenDcsDatabase db;

    /**
     * This just tests that timeseries can be saved to the database, retrieved, and totally deleted.
     * 
     * @throws Exception
     */
    @TestFactory
    @EnableIfTsDb
    public Collection<DynamicTest> test_algorithm_operations() throws Exception
    {
        DecodesSettings settings = DecodesSettings.instance();
        try (TimeSeriesDAI tsDao = tsDb.makeTimeSeriesDAO();
             SiteDAI siteDAO = tsDb.makeSiteDAO();)
        {
            TsImporter importer = new TsImporter(TimeZone.getTimeZone("UTC"), settings.siteNameTypePreference, (tsIdStr) -> 
            {
                try
                {
                    return tsDao.getTimeSeriesIdentifier(tsIdStr);
                }
                catch (Exception ex)
                {
                    log.warn("No existing time series. Will attempt to create.");

                    try
                    {
                        TimeSeriesIdentifier tsId = tsDb.makeEmptyTsId();
                        tsId.setUniqueString(tsIdStr);
                        Site site = tsDb.getSiteById(siteDAO.lookupSiteID(tsId.getSiteName()));
                        if (site == null)
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
                        throw new DbIoException(String.format("No such time series and cannot create for '%s'", tsIdStr), ex);
                    }
                }
            });
            String workingDirectoryPath = Paths.get("").toAbsolutePath().toString();
            String current_Directory = buildFilePath(workingDirectoryPath, "src", "test", "resources", "data", "Comps");
            File directory = new File(current_Directory);

            Collection<DynamicTest> algoTests = new ArrayList<DynamicTest>();

            final String filterTest = System.getProperty("opendcs.test.algorithm.filter", "").trim();
            if (directory.exists() && directory.isDirectory()) {
                File[] comps = directory.listFiles();
                if (comps != null) {
                    for (File comp : comps) {
                        if (comp.isDirectory()) {
                            algoTests.addAll(Arrays.stream(comp.listFiles(test -> test.isDirectory()))
                                .filter(test -> filterTest == "" || filterTest.equals(comp.getName()+test.getName()))
                                .map(test -> getDynamicTest(test, comp, importer, tsDao))
                                .collect(Collectors.toList()));  
                        }
                    }
                }
            } 
            else 
            {
                log.error("Invalid directory: " + current_Directory);
            }
            return algoTests;
        }
    }

    private DynamicTest getDynamicTest(File test, File comp, TsImporter importer, TimeSeriesDAI tsDao){
        return DynamicTest.dynamicTest(comp.getName() +" "+test.getName(), () -> {
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
                }
                else if (name.endsWith(".sql"))
                {
                    log.info("Found SQL file: " + comp_data.getAbsolutePath());
                    executeSqlFile(comp_data);
                }
            }
            
            List<CTimeSeries> inputTS = loadTSimport(buildFilePath(test.getAbsolutePath(),"timeseries","inputs"), importer);
            Collection<CTimeSeries> outputTS = loadTSimport(buildFilePath(test.getAbsolutePath(),"timeseries","outputs"), importer);
            Collection<CTimeSeries> expectedOutputTS = loadTSimport(buildFilePath(test.getAbsolutePath(),"timeseries","expectedOutputs"), importer);

            loadRatingimport(buildFilePath(test.getAbsolutePath(),"rating"));
            loadScreenings(buildFilePath(test.getAbsolutePath(), "screenings"));


            DbComputation testComp = null;
            try (ComputationDAI compdao = tsDb.makeComputationDAO())
            {
               testComp = compdao.getComputationByName(test.getName()+comp.getName());
            }

            DataCollection theData = new DataCollection();

            for (CTimeSeries ctsi: inputTS){
                for (int idx = 0; idx < ctsi.size(); idx++){
                    VarFlags.setWasAdded(ctsi.sampleAt(idx));
                }
                theData.addTimeSeries(ctsi);
            }
            for (CTimeSeries ctso: outputTS){
                theData.addTimeSeries(ctso);
            }

            // new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss z")
            //testComp.setProperty("ValidStart", "2024/10/09-23:00:00 UTC");
            testComp.prepareForExec(tsDb);
            testComp.apply(theData, tsDb);

            Iterator<CTimeSeries> iterExpect = expectedOutputTS.iterator();
            
            while (iterExpect.hasNext())
            {
                CTimeSeries currExpect = iterExpect.next();
                String tsName = currExpect.getNameString();
                TimeSeriesIdentifier outputID = tsDao.getTimeSeriesIdentifier(tsName);

                log.info(currExpect.getNameString());

                CTimeSeries algoOutput = theData.getTimeSeriesByTsidKey(outputID);
                log.info("expected units: " + currExpect.getUnitsAbbr());
                TSUtil.convertUnits(algoOutput, currExpect.getUnitsAbbr());

                
                if (log.isInfoEnabled()) 
                {
                    for (int i = 0; i<algoOutput.size(); i++)
                    {
                        TimedVariable TVOutput = algoOutput.sampleAt(i);
                        TimedVariable TVExpected = currExpect.findWithin(TVOutput.getTime(), 0);
                        log.info("output time: "+TVOutput.getTime());
                        log.info("output value  : "+TVOutput.getDoubleValue());
                        log.info("expected value: {}", TVExpected != null
                                                                          ? TVExpected.getDoubleValue()
                                                                          : " intentionally missing in output file");
                    }
                }

                assertEquals(currExpect, algoOutput, "expected true", testComp.getValidStart(), testComp.getValidEnd());
            }
        });
    }

    private void loadScreenings(String screeningsFile) throws Exception
    {
        File folderTS = new File(screeningsFile);
        if (!folderTS.exists())
        {
            return;
        }
        File log = new File(configuration.getUserDir().getParentFile(), "screenings-import-"+ folderTS.getName()+".log");
        Programs.ImportScreenings(log, configuration.getPropertiesFile(), environment, exit, screeningsFile);
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

    private ArrayList<CTimeSeries> loadTSimport(String folderTSstr, TsImporter importer)
    throws Exception
    {
        File folderTS = new File(folderTSstr);
        ArrayList<CTimeSeries> fullTs = new ArrayList<CTimeSeries>();
        if (!folderTS.exists()){
            return fullTs;
        }
        for (File tsfiles : folderTS.listFiles())
        {
            String relativePath = "Comps"+tsfiles.getAbsolutePath().split("Comps")[1];

            try(TimeSeriesDAI tsDao = tsDb.makeTimeSeriesDAO();
                InputStream inputStream = TestResources.getResourceAsStream(configuration, relativePath);)
            {
                Collection<CTimeSeries> allTs = importer.readTimeSeriesFile(inputStream);
                for (CTimeSeries tsIn: allTs)
                {
                    log.info("load: " + tsIn.getDisplayName());
                    log.info("Saving {}", tsIn.getTimeSeriesIdentifier());

                    tsDao.saveTimeSeries(tsIn);
                    FailableResult<TimeSeriesIdentifier,TsdbException> tsIdSavedResult = tsDao.findTimeSeriesIdentifier(tsIn.getTimeSeriesIdentifier().getUniqueString());
                    assertTrue(tsIdSavedResult.isSuccess(), "Time series was not correctly saved.");
                }
                fullTs.addAll(allTs);
            }
        }
        return fullTs;
    }

    private void loadRatingimport(String folderRatingStr) throws Exception
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
                }
            }
        }
    }
    
    /**
     * Execute SQL file in the database for test setup
     * @param sqlFile The SQL file to execute
     */
    private void executeSqlFile(File sqlFile)
    {
        if (db == null)
        {
            log.warn("OpenDcsDatabase not available, skipping SQL file execution: " + sqlFile.getName());
            return;
        }
        
        try
        {
            // Read the SQL file content
            String sqlContent = new String(Files.readAllBytes(sqlFile.toPath()), StandardCharsets.UTF_8);
            
            // Replace office ID placeholder if this is a CWMS database
            if (tsDb instanceof CwmsTimeSeriesDb)
            {
                CwmsTimeSeriesDb cwmsDb = (CwmsTimeSeriesDb) tsDb;
                String officeId = cwmsDb.getDbOfficeId();
                sqlContent = sqlContent.replace("DEFAULT_OFFICE", officeId);
            }
            
            // Execute the SQL using a transaction
            try (DataTransaction tx = db.newTransaction())
            {
                Connection conn = tx.connection(Connection.class)
                    .orElseThrow(() -> new RuntimeException("JDBC Connection not available in this transaction."));
                
                // Use CallableStatement to execute the SQL (handles PL/SQL blocks properly)
                try (CallableStatement stmt = conn.prepareCall(sqlContent))
                {
                    log.info("Executing SQL from file: " + sqlFile.getName());
                    log.debug("SQL Content: " + sqlContent.substring(0, Math.min(200, sqlContent.length())) + "...");
                    stmt.execute();
                }
                log.info("Successfully executed SQL file: " + sqlFile.getName());
            }
        }
        catch (Exception ex)
        {
            log.error("Failed to execute SQL file " + sqlFile.getName() + ": " + ex.getMessage(), ex);
            // Don't fail the test, just log the error
        }
    }
}
