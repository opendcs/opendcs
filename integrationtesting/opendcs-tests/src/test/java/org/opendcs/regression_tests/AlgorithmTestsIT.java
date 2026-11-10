package org.opendcs.regression_tests;

import java.io.File;
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
import decodes.tsdb.DbAlgorithmExecutive;
import decodes.tsdb.DbCompParm;
import decodes.tsdb.DbComputation;
import decodes.tsdb.DbIoException;
import decodes.tsdb.DuplicateTimeSeriesException;
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
import org.opendcs.fixtures.helpers.TestConfig;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;
import org.opendcs.fixtures.annotations.EnableIfTsDb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
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
                log.error(String.format("Invalid directory: %s", current_Directory));
            }
            return algoTests;
        }
    }

    private DynamicTest getDynamicTest(File test, File comp, TsImporter importer, TimeSeriesDAI tsDao)
	{
		ImporterHelper helper = new ImporterHelper(tsDb, configuration, environment, exit);

        return DynamicTest.dynamicTest(comp.getName() +" "+test.getName(), () -> {
            TestConfig config = TestConfig.EMPTY;
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
                    config = TestConfig.read(comp_data);
                    config.assumeEnabledForCurrentEngine();
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

            for (CTimeSeries ctsi: inputTS)
            {
                for (int idx = 0; idx < ctsi.size(); idx++)
                {
                    VarFlags.setWasAdded(ctsi.sampleAt(idx));
                }
                try
                {
                    theData.addTimeSeries(ctsi);
                }
                catch (DuplicateTimeSeriesException ex)
                {
                    log.atTrace().setCause(ex).log("Time series {}, was already saved. Check your input/output data for duplicates.", ctsi.getDisplayName());
                }
            }
            for (CTimeSeries ctso: outputTS){
                theData.addTimeSeries(ctso);
            }

            // new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss z")
            //testComp.setProperty("ValidStart", "2024/10/09-23:00:00 UTC");
            testComp.prepareForExec(tsDb);

            assertUnassignedRoles(helper, testComp, config.unassignedRoles(), "before execution");

            if (config.expectError() != null)
            {
                assertComputationFails(testComp, theData, config.expectError());
                return;
            }

            testComp.apply(theData, tsDb);

            assertUnassignedRoles(helper, testComp, config.unassignedRoles(), "after execution");

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

    /**
     * Checks each role the test's config declares unassigned, before the computation runs and
     * again after it. Before is what proves the record was written at all, since a fixture that
     * quietly writes nothing would otherwise test less than it appears to. After is what proves
     * running the computation left the record alone.
     */
    private void assertUnassignedRoles(ImporterHelper helper, DbComputation computation,
                                       List<String> roles, String stage)
    {
        if (roles.isEmpty())
        {
            return;
        }

        DbAlgorithmExecutive executive = computation.getExecutive();
        assertNotNull(executive, () -> "the computation must be prepared before role "
            + "assignment can be checked");

        for (String role : roles)
        {
            String roleOfComp = stage + ": role " + role + " of computation "
                + computation.getName();

            // Exactly one, because nothing constrains (computation_id, algo_role_name) to be
            // unique, so a fixture that is not delete-then-insert accumulates records.
            assertEquals(1, helper.countUnconfiguredRoleRecords(computation.getName(), role),
                () -> roleOfComp + " must have exactly one unconfigured cp_comp_ts_parm record");

            DbCompParm parm = helper.readCompParm(computation.getName(), role);
            assertNotNull(parm, () -> roleOfComp + " has no cp_comp_ts_parm record");
            assertTrue(parm.getSiteDataTypeId().isNull(),
                () -> roleOfComp + " should not name a site and datatype");

            // The record alone cannot tell an unassigned role from an absent one. A record
            // naming no site and datatype still yields a ParmRef, so isAssigned is the check
            // that separates the two.
            assertNotNull(executive.getParmRef(role), () -> stage + ": no ParmRef for role " + role
                + " even though the computation has a record for it");
            assertFalse(executive.isAssigned(role), () -> stage + ": role " + role
                + " has a record naming no time series and must not be assigned");

            log.info("{}: role {} of {} is declared but unassigned",
                     stage, role, computation.getName());
        }
    }

    /**
     * Runs a computation that the test's config declares must fail, and checks that it
     * failed for the reason the config names rather than through some unrelated error.
     */
    private void assertComputationFails(DbComputation computation, DataCollection theData,
                                        String expectedMessage)
    {
        Throwable failure = assertThrows(Throwable.class, () -> computation.apply(theData, tsDb),
            () -> "Expected the computation to fail with '" + expectedMessage + "'");

        for (Throwable cause = failure; cause != null; cause = cause.getCause())
        {
            if (cause.getMessage() != null && cause.getMessage().contains(expectedMessage))
            {
                log.info("Computation failed as expected: {}", cause.getMessage());
                return;
            }
        }

        fail(() -> "Expected a failure containing '" + expectedMessage + "' but got: " + failure);
    }
}
