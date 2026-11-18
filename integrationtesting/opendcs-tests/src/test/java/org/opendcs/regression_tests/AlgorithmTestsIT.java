package org.opendcs.regression_tests;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import decodes.db.Constants;
import decodes.db.Site;
import decodes.db.SiteName;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.ComputationExecution;
import decodes.tsdb.DataCollection;
import decodes.tsdb.DbAlgorithmExecutive;
import decodes.tsdb.DbCompParm;
import decodes.tsdb.DbCompResolver;
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
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import jakarta.annotation.Nonnull;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.InboundSseEvent;
import jakarta.ws.rs.sse.SseEventSource;
import opendcs.dai.ComputationDAI;
import opendcs.dai.SiteDAI;
import opendcs.dai.TimeSeriesDAI;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.fixtures.helpers.ImporterHelper;
import org.opendcs.fixtures.helpers.TestConfig;
import org.opendcs.odcsapi.filters.LoggingFilter;
import org.opendcs.odcsapi.filters.W3CTraceFilter;
import org.opendcs.odcsapi.res.it.BaseApiIT;
import org.opendcs.odcsapi.util.ApiConstants;
import org.opendcs.util.functional.ThrowingSupplier;
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
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.opendcs.fixtures.helpers.ImporterHelper.buildFilePath;
import static org.opendcs.fixtures.assertions.TimeSeries.assertEquals;

@DecodesConfigurationRequired({
    "shared/test-sites.xml"})
/**
 * Note on Suppres Null everyone... at the time of addplication, non of the elements could be null
 * and I was sick of the IDE flagging them. Be cautious.
 * AlgorithmTestsIT
 */
public class AlgorithmTestsIT extends BaseApiIT // we also want to run these through the API
{
    private Logger log = LoggerFactory.getLogger(AlgorithmTestsIT.class);


    final DateTimeFormatter ISO_FMT = DateTimeFormatter.ISO_INSTANT;

    @ConfiguredField
    private TimeSeriesDb tsDb;
    
    @ConfiguredField
    private OpenDcsDatabase db;

    private static final ExecutorService compExecService = Executors.newFixedThreadPool(2); // allow parallel tests

    /**
     * This just tests that timeseries can be saved to the database, retrieved, and totally deleted.
     * 
     * @throws Exception
     */
    @SuppressWarnings("null")
    @TestFactory 
    @Execution(value = ExecutionMode.SAME_THREAD, reason = "Comp system needs rework before this could reasonably be parallel.")
    @EnableIfTsDb
    public Collection<DynamicNode> test_algorithm_operations() throws Exception
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

            var algoTests = new ArrayList<DynamicNode>();

            final String filterTest = System.getProperty("opendcs.test.algorithm.filter", "").trim();
            if (directory.exists() && directory.isDirectory()) {
                File[] comps = directory.listFiles();
                if (comps != null) {
                    for (File comp : comps) {
                        if (comp.isDirectory()) {
                            algoTests.add(
                                dynamicContainer(
                                    comp.getName(), 
                                    Arrays.stream(comp.listFiles(test -> test.isDirectory()))
                                         .filter(test -> filterTest == "" || filterTest.equals(comp.getName()+test.getName()))
                                         .map(test -> getDynamicTest(test, comp, importer, tsDao))
                                         .collect(Collectors.toList())
                                )
                            );  
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

    @SuppressWarnings("null")
    private DynamicNode getDynamicTest(File test, File comp, TsImporter importer, TimeSeriesDAI tsDao)
	{
		ImporterHelper helper = new ImporterHelper(tsDb, configuration, environment, exit);
        var context = new ExecutionContext(test, helper, comp, importer, tsDao);

        return dynamicContainer(test.getName(), Stream.of(
            initializeComputation(context),
            createComputationAppTest(context),
            createComputationApiTest(context)
        )); 
            
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

    @Nonnull
    private DynamicTest initializeComputation(ExecutionContext ctx)
    {
        final var testName = "Initalize Data";
        return DynamicTest.dynamicTest(testName, () -> {
            
            for (File comp_data : ctx.test.listFiles()) 
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
                    ctx.config = TestConfig.read(comp_data);
                    ctx.config.assumeEnabledForCurrentEngine();
                }
                else if (name.endsWith(".sql"))
                {
                    log.info("Found SQL file: " + comp_data.getAbsolutePath());
					ctx.helper.executeSqlFile(db, comp_data);
                }
            }
            
            ctx.inputTS = ctx.helper.loadTSimport(buildFilePath(ctx.test.getAbsolutePath(),"timeseries","inputs"), ctx.importer);
            ctx.outputTS = ctx.helper.loadTSimport(buildFilePath(ctx.test.getAbsolutePath(),"timeseries","outputs"), ctx.importer);
            ctx.expectedOutputTS = ctx.helper.loadTSimport(buildFilePath(ctx.test.getAbsolutePath(),"timeseries","expectedOutputs"), ctx.importer);

            ctx.helper.loadRatingimport(buildFilePath(ctx.test.getAbsolutePath(),"rating"));
            ctx.helper.loadScreenings(buildFilePath(ctx.test.getAbsolutePath(), "screenings"));

            ctx.comp = ((ThrowingSupplier<DbComputation,Exception>)() ->
            {
                try (ComputationDAI compdao = tsDb.makeComputationDAO())
                {
                return compdao.getComputationByName(ctx.test.getName()+ctx.compFile.getName());
                }
            }).get();

            ctx.dataIn = new DataCollection();

            for (CTimeSeries ctsi: ctx.inputTS)
            {
                for (int idx = 0; idx < ctsi.size(); idx++)
                {
                    VarFlags.setWasAdded(ctsi.sampleAt(idx));
                }
                ctsi.addDependentCompId(ctx.comp.getId());
                ctx.dataIn.addTimeSeries(ctsi);
            }
            for (CTimeSeries ctso: ctx.outputTS)
            {
                ctx.dataIn.addTimeSeries(ctso);
            }
            if (ctx.comp.hasGroupInput())
            {
                // Somewhat arbitrary, but we need one for the app execution.
                // Some algorithms do alter behavior depending on 
                // which input triggers, so while this is a good initial default,
                // this should be moved into the TestConfig in the future.
                ctx.comp.triggeringTsid = ctx.inputTS.getFirst().getTimeSeriesIdentifier();
            }
            // we prepare now as we will need to use isAssigned later.
            ctx.comp.prepareForExec(tsDb);
        });
    }

    @Nonnull
    private DynamicTest createComputationAppTest(ExecutionContext ctx)
    {
        final var testName = "ComputationApp";

        return DynamicTest.dynamicTest(testName, () -> {
            assumeTrue(ctx.comp != null);
            if (ctx.config.expectError() != null)
            {
                assertComputationFails(ctx.comp, ctx.dataIn, ctx.config.expectError());
                return;
            }
            // While we already have the computation, we want to be able to test group comps
            // and make sure we're doing the same thing the other portions of computation execution
            // is doing.
            var resolver = new DbCompResolver(tsDb);
            var comps = List.of(resolver.resolve(ctx.dataIn))
                            .stream()
                            .peek(c -> log.info("Seeing if we should run {}", c.getName()))
                            .filter(c -> c.getName().equals(ctx.comp.getName()))
                            .toList();
            assertFalse(comps.isEmpty());
            log.info("Will execute {} computations for {}", comps.size(), testName);
            ctx.dataOut = new DataCollection();
            // This is here because @BeforeEach doesn't execute before Dynamic computations.
            // Might be worth moving the generation to a parameterized test with
            // a Argument provider
            try (ComputationExecution compExecutor = new ComputationExecution(db, compExecService))
            {
                var results = compExecutor.execute(comps, ctx.dataIn, dcOut ->
                {
                    for (var ts: dcOut.getAllTimeSeries())
                    {
                        try 
                        {
                            ctx.dataOut.addTimeSeries(ts);
                        }
                        catch (DuplicateTimeSeriesException ex)
                        {
                            fail("This should not happen.", ex);
                        }
                    }
                    return dcOut;
                });
                assertEquals(comps.size(), results.computesTried(), () -> "Incorrect number of computations run for " + testName);
                assertEquals(0, results.numErrors());
            }
            for (var concreteComp: comps)
            {
                assertUnassignedRoles(ctx.helper, concreteComp, ctx.config.unassignedRoles(), "after execution");
            }

            Iterator<CTimeSeries> iterExpect = ctx.expectedOutputTS.iterator();
            
            while (iterExpect.hasNext())
            {
                CTimeSeries currExpect = iterExpect.next();
                String tsName = currExpect.getNameString();
                TimeSeriesIdentifier outputID = ctx.tsDao.getTimeSeriesIdentifier(tsName);

                log.info(currExpect.getNameString());

                CTimeSeries algoOutput = ctx.dataOut.getTimeSeriesByTsidKey(outputID);
                assertNotNull(algoOutput, "Expected time series was not created.");
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

                assertEquals(currExpect, algoOutput, "expected true", ctx.comp.getValidStart(),
                                                                              ctx.comp.getValidEnd());
            }
        });
    }

    /**
     * Drive a computation using the REST API /runcomputation endpoint.
     * @param ctx Contains required data to perform operations.
     * @return
     */
    private DynamicTest createComputationApiTest(ExecutionContext ctx)
    {
        final var testName = "RestAPI";

        return DynamicTest.dynamicTest(testName, () -> 
        {
            assumeTrue(ctx.comp != null);
            assumeTrue(ctx.config.expectError() == null); // will work out later

            setUpCreds();
		    authenticate();
            
            String organization = getOrganization();		

            final var compTraceId = UUID.randomUUID().toString();
            final var traceParent = "00-06df66e0fcc70c06ef71cf9ed9753bf8-12b7001e81b533ab-01";
            ClientRequestFilter auth = crCtx ->
            {
                var hdrs = crCtx.getHeaders();
                hdrs.putSingle(ApiConstants.ORGANIZATION_HEADER, organization);
                hdrs.putSingle("Cookie", getCookie());
                hdrs.putSingle(LoggingFilter.HEADER_TRACE_ID, compTraceId);
                hdrs.putSingle(W3CTraceFilter.TRACE_PARENT.toString(), traceParent);
            };

            URI baseURI = URI.create(String.format("%s:%d/%s",
                                         RestAssured.baseURI, RestAssured.port, RestAssured.basePath));
            String from = ISO_FMT.format(ctx.start());
            String to = ISO_FMT.format(ctx.end());

            WebTarget target;
            try (Client client = ClientBuilder.newBuilder()
                    .register(SseEventSource.class)
                    .register(auth)
                    .build())
            {
                target = client.target(baseURI)
                        .path("runcomputation")
                        .queryParam("computationid", ctx.comp.getId())
                        .queryParam("start", from)
                        .queryParam("end", to);

                if (ctx.comp.hasGroupInput())
                {
                    target = target.queryParam("tsid", ctx.comp.triggeringTsid.getKey().getValue());
                }

                List<InboundSseEvent> events = new CopyOnWriteArrayList<>();
                CountDownLatch done = new CountDownLatch(1);
                CountDownLatch firstEvent = new CountDownLatch(1);

                try(SseEventSource source = SseEventSource.target(target).build())
                {
                    source.register(
                        (InboundSseEvent event) ->
                        {
                            events.add(event);
                            firstEvent.countDown();

                            if (event.getName().equalsIgnoreCase("Results"))
                            {
                                done.countDown();
                            }
                            else
                            {
                                @SuppressWarnings("null")
                                String data = event.readData(String.class, MediaType.TEXT_PLAIN_TYPE);
                                String name = event.getName();
                                log.atInfo().log(String.format("Received SSE event: %s, %s", name, data));
                            }
                        },
                        (Throwable error) -> {
                            throw new AssertionError("SSE Error: " + error.getMessage());
                        }
                    );
                    source.open();
                    boolean received = firstEvent.await(10, TimeUnit.SECONDS);
                    boolean completed = done.await(100, TimeUnit.SECONDS);
                    assertTrue(received, "Timed out waiting for SSE events");
                    assertTrue(completed, "Timed out waiting for SSE completion");

                    assertFalse(events.isEmpty(), "SSE did not receive any events");
                    InboundSseEvent event = events.getFirst();

                    @SuppressWarnings("null")
                    String data = event.readData(String.class, MediaType.TEXT_PLAIN_TYPE);
                    assertNotNull(data);
                    assertFalse(data.isBlank());

                    // Whether the run actually computed anything. See the key assertion below for why
                    // this is not simply assumed.
                    boolean ranClean = events.stream()
                            .filter(e -> !e.getName().equalsIgnoreCase("Results"))
                            .map(e -> e.readData(String.class, MediaType.TEXT_PLAIN_TYPE))
                            .anyMatch(d -> d != null && d.contains("executed with 0 errors"));
                    assertTrue(ranClean, "computation failed");
                    int foundCount = 0;

                    int expectedTsCount = 0;
                    for (var parm: ctx.comp.getParmList())
                    {
                        // If you create new tests and this isn't behaving quite right
                        // it will likely be easiest to add a "(un)assigned output" in addition to the
                        // list of assigned roles. Mixing the input and output list is likely going
                        // to cause confusion.
                        if (parm.isOutput())
                        {
                            expectedTsCount++;
                        }
                    }

                    for (InboundSseEvent sseEvent : events)
                    {
                        if (sseEvent.getName().equalsIgnoreCase("Results"))
                        {
                            foundCount++;
                            JsonPath jsonPath = JsonPath.from(
                                sseEvent.readData(String.class, MediaType.TEXT_PLAIN_TYPE));

                            assertEquals(expectedTsCount, jsonPath.getList("tsIds").size(),
                                         () -> "Expected " + ctx.expectedOutputTS.size() + " results" + jsonPath.prettyPrint());
                            // A manual run writes nothing, so its computed values have to travel inline
                            // on this event -- there is nothing for the caller to read back afterwards.
                            //
                            // Guarded on the run having succeeded because of a SEPARATE, PRE-EXISTING
                            // problem this assertion uncovered: on the OpenDCS/OpenTSDB schema this
                            // fixture's non-group computation has no site_datatype_id on its input parm,
                            // so OpenTsdb.expandSDI returns null, the input is never filled, and the
                            // computation fails before producing anything. That reproduces identically
                            // on an unmodified tree -- the suite simply never noticed, because the tsId
                            // count came from the parm-derived list and the data assertion below reads
                            // values the fixture itself imported. Un-guard this once inputs resolve.
                            if (ranClean)
                            {
                                assertFalse(jsonPath.getList("data").isEmpty(),
                                        "A successful run must return its computed values inline");
                                List<Number> keys = jsonPath.getList("tsIds.key");
                                keys.forEach(key -> assertTrue(key != null && key.longValue() > 0,
                                        "Result tsId has no database key: " + key));
                            }
                        }
                    }
                    assertEquals(1, foundCount,
                                 String.format("Expected %d SSE Result events, found %d", 1, foundCount));

                    for (CTimeSeries ts : ctx.expectedOutputTS)
                    {
                        CTimeSeries cts = fillTimeSeries(ts, Instant.parse(from), Instant.parse(to));
                        assertNotNull(cts);
                        assertNotNull(cts.getTimeSeriesIdentifier());
                        assertTrue(cts.size() > 0, "No data found for time series " + cts.getTimeSeriesIdentifier());
                    }
                }
            }

        });
    }

    private static final class ExecutionContext
    {
        File test;
        File compFile;
        TsImporter importer;
        TimeSeriesDAI tsDao;
        TestConfig config = TestConfig.EMPTY;
        ImporterHelper helper;
        List<CTimeSeries> inputTS;
        Collection<CTimeSeries> outputTS;
        Collection<CTimeSeries> expectedOutputTS;
        DbComputation comp;
        DataCollection dataIn;
        DataCollection dataOut;
        
        public ExecutionContext(File test, ImporterHelper helper, File compFile, TsImporter importer, TimeSeriesDAI tsDao)
        {
            this.test = test;
            this.helper = helper;
            this.compFile = compFile;
            this.importer = importer;
            this.tsDao = tsDao;
        }

        public ZonedDateTime start()
        {
            Date earliest = inputTS.getFirst().sampleAt(0).getTime();
            for (var ts: inputTS)
            {
                var nextCheck = ts.sampleAt(0).getTime();
                if (nextCheck.before(earliest))
                {
                    earliest = nextCheck;
                }
            }

            return earliest.toInstant().atZone(ZoneId.of("UTC"));
        }

        public ZonedDateTime end()
        {
            Date latest = inputTS.getFirst().sampleAt(inputTS.getFirst().size()-1).getTime();
            for (var ts: inputTS)
            {
                var nextCheck = ts.sampleAt(ts.size()-1).getTime();
                if (nextCheck.after(latest))
                {
                    latest = nextCheck;
                }
            }

            return latest.toInstant().atZone(ZoneId.of("UTC"));
        }
    }
}
