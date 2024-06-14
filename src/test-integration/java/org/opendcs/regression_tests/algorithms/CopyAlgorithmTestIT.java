package org.opendcs.regression_tests.algorithms;


import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.jupiter.api.Test;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.assertions.TimeSeries;
import org.python.icu.util.TimeZone;

import decodes.cwms.CwmsTsId;
import decodes.db.DataType;
import decodes.sql.DbKey;
import decodes.tsdb.CTimeSeries;
import decodes.tsdb.DataCollection;
import decodes.tsdb.DbAlgoParm;
import decodes.tsdb.DbCompAlgorithm;
import decodes.tsdb.DbCompParm;
import decodes.tsdb.DbComputation;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsImporter;
import decodes.tsdb.VarFlags;
import ilex.var.TimedVariable;

public class CopyAlgorithmTestIT extends AppTestBase
{
    @ConfiguredField
    TimeSeriesDb tsDb;
    
    @Test
    void test_copy_algorithm() throws Exception
    {
        DbComputation comp = new DbComputation(DbKey.NullKey, "test");
        DbCompAlgorithm algo = new DbCompAlgorithm("copy");
        algo.setExecClass(decodes.tsdb.algo.CopyAlgorithm.class.getName());
        algo.addParm(new DbAlgoParm("input","i"));
        algo.addParm(new DbAlgoParm("output","o"));
        TimeSeriesIdentifier inputTsId = tsDb.makeTsId("Test.Stage.Inst.15Minutes.0.test");
        TimeSeriesIdentifier outputTsId = tsDb.makeTsId("Test.Stage.Inst.15Minutes.0.test-copy");
        CTimeSeries inputTs = tsDb.makeTimeSeries(inputTsId);
        CTimeSeries outputTs = new CTimeSeries(outputTsId);
        CTimeSeries outputTsExpected = new CTimeSeries(outputTsId);
        GregorianCalendar cal = new GregorianCalendar();
        cal.set(2024, 6, 14, 0, 0, 0);
        Date theTime = cal.getTime();
        inputTs.addSample(new TimedVariable(theTime, 1.0, VarFlags.DB_ADDED));
        outputTsExpected.addSample(new TimedVariable(theTime, 1.0, 0));

        DbCompParm inputCompParm = new DbCompParm("input", inputTs.getSDI(), inputTs.getInterval(), inputTs.getTableSelector(), 0);
        DbCompParm outputCompParm = new DbCompParm("output", outputTs.getSDI(), outputTs.getInterval(), outputTs.getTableSelector(), 0);
        inputCompParm.setAlgoParmType("i");
        outputCompParm.setAlgoParmType("o");
        comp.addParm(inputCompParm);
        comp.addParm(outputCompParm);
        comp.setAlgorithm(algo);
        comp.prepareForExec(tsDb);
        DataCollection dc = new DataCollection();
        dc.addTimeSeries(inputTs);
        dc.addTimeSeries(outputTs);
        comp.apply(dc, tsDb);
        TimeSeries.assertEquals(outputTsExpected, outputTs, "Timeseries was not copied correctly.");
    }
}
