package org.opendcs.regression_tests;

import java.util.List;
import java.util.function.Predicate;

import decodes.db.Database;
import decodes.sql.DbKey;
import decodes.tsdb.DbCompParm;
import decodes.tsdb.DbComputation;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import opendcs.dai.ComputationDAI;
import opendcs.dai.TimeSeriesDAI;
import org.junit.jupiter.api.Test;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ComputationConfigurationRequired;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DecodesConfigurationRequired({
		"shared/test-sites.xml",
		"shared/presgrp-regtest.xml"
})
@ComputationConfigurationRequired({"shared/loading-apps.xml"})
final class ComputationDaoTestIT extends AppTestBase
{
	@ConfiguredField
	private TimeSeriesDb db;

	@ConfiguredField
	private Database decodesDb;

	@Test
	void testComputationFilter() throws Exception
	{
		try (TimeSeriesDAI tsDAI = db.makeTimeSeriesDAO();
			 ComputationDAI compDAI = db.makeComputationDAO())
		{
			TimeSeriesIdentifier tsIdInput = db.makeEmptyTsId();
			TimeSeriesIdentifier tsIdOutput = db.makeEmptyTsId();
			tsIdInput.setUniqueString("TESTSITE1.Stage.Inst.15Minutes.0.raw");
			tsIdOutput.setUniqueString("TESTSITE1.Flow.Inst.15Minutes.0.raw");
			DbKey inputKey = tsDAI.createTimeSeries(tsIdInput);
			DbKey outputKey = tsDAI.createTimeSeries(tsIdOutput);

			DbComputation comp = new DbComputation(DbKey.NullKey, "stage_flow_tmp");
			DbCompParm input = new DbCompParm("indep", inputKey, tsIdInput.getInterval(), tsIdInput.getTableSelector(), 0);
			DbCompParm output = new DbCompParm("dep", outputKey, tsIdOutput.getInterval(), tsIdOutput.getTableSelector(), 0);
			comp.addParm(input);
			comp.addParm(output);
			comp.setAlgorithmName("TabRating");
			comp.setApplicationName("compproc_regtest");
			comp.setEnabled(true);

			compDAI.writeComputation(comp);
			final DbComputation compInDb = compDAI.getComputationByName("stage_flow_tmp");

			// retrieve with default filter
			List<DbComputation> comps = compDAI.listComps(item -> true);
			assertTrue(comps.stream().anyMatch(c -> c.getKey().equals(compInDb.getKey())));

			// retrieve with filter
			Predicate<DbComputation> filter = item -> {
				for (DbCompParm parm : item.getParmList())
				{
					if (parm.getRoleName().equalsIgnoreCase("indep"))
					{
						return true;
					}
				}
				return false;
			};

			comps = compDAI.listComps(filter);
			assertTrue(comps.stream().anyMatch(c -> c.getKey().equals(compInDb.getKey())));

			// retrieve with filter that should not match
			filter = item -> {
				for (DbCompParm parm : item.getParmList())
				{
					if (parm.getRoleName().equalsIgnoreCase("foo"))
					{
						return true;
					}
				}
				return false;
			};
			comps = compDAI.listComps(filter);
			assertTrue(comps.stream().noneMatch(c -> c.getKey().equals(compInDb.getKey())));
		}
	}
}
