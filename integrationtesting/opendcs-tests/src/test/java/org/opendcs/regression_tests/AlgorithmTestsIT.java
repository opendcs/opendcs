package org.opendcs.regression_tests;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

import decodes.db.Constants;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DataCollection;
import decodes.tsdb.DbComputation;
import decodes.tsdb.DbIoException;
import decodes.tsdb.ImportComp;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsImporter;
import decodes.tsdb.VarFlags;
import decodes.util.DecodesSettings;
import decodes.util.TSUtil;
import ilex.var.TimedVariable;
import opendcs.dai.ComputationDAI;
import opendcs.dai.SiteDAI;
import opendcs.dai.TimeSeriesDAI;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.helpers.ImporterHelper;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;
import org.opendcs.fixtures.annotations.EnableIfTsDb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.opendcs.fixtures.helpers.ImporterHelper.buildFilePath;
import static org.opendcs.fixtures.assertions.TimeSeries.assertEquals;

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

    private DynamicTest getDynamicTest(File test, File comp, TsImporter importer, TimeSeriesDAI tsDao)
	{
		ImporterHelper helper = new ImporterHelper(tsDb, configuration, environment, exit, ImporterHelper.CONTEXT.TOOLKIT);

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
					helper.executeSqlFile(db, comp_data);
                }
            }
            
            List<CTimeSeries> inputTS = helper.loadTSimport(buildFilePath(test.getAbsolutePath(),"timeseries","inputs"), importer);
            Collection<CTimeSeries> outputTS = helper.loadTSimport(buildFilePath(test.getAbsolutePath(),"timeseries","outputs"), importer);
            Collection<CTimeSeries> expectedOutputTS = helper.loadTSimport(buildFilePath(test.getAbsolutePath(),"timeseries","expectedOutputs"), importer);

            helper.loadRatingimport(buildFilePath(test.getAbsolutePath(),"rating"));
            helper.loadScreenings(buildFilePath(test.getAbsolutePath(), "screenings"));


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
}
