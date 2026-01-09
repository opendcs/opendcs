package org.opendcs.odcsapi.res;

import java.sql.Date;
import java.time.Instant;
import java.util.ArrayList;
import java.util.stream.Collectors;

import decodes.db.DatabaseException;
import decodes.sql.DbKey;
import decodes.tsdb.DbCompAlgorithm;
import decodes.tsdb.DbComputation;
import decodes.tsdb.TsGroup;
import org.junit.jupiter.api.Test;
import org.opendcs.odcsapi.beans.ApiComputation;
import org.opendcs.odcsapi.beans.ApiComputationRef;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opendcs.odcsapi.res.ComputationResources.map;

final class ComputationResourcesTest
{
	@Test
	void testApiComputeMap()
	{
		DbKey dbKey = DbKey.createDbKey(16704L);
		String name = "Computation";
		DbComputation dbComp = new DbComputation(dbKey, name);
		dbComp.setAlgorithmName("Computation Algorithm");
		dbComp.setComment("Computation to find the average of a set of values");
		dbComp.setAppId(DbKey.createDbKey(18596L));
		dbComp.setLastModified(Date.from(Instant.parse("2023-08-01T10:30:00Z")));
		dbComp.setAlgorithm(new DbCompAlgorithm(dbComp.getAlgorithmName()));
		dbComp.setEnabled(true);
		dbComp.setApplicationName("Average Application");
		dbComp.setValidStart(Date.from(Instant.parse("2023-07-01T00:30:00Z")));
		TsGroup group = new TsGroup();
		group.setGroupName("Computation group");
		group.setGroupId(DbKey.createDbKey(16753096L));
		dbComp.setGroup(group);
		dbComp.setEnabled(true);
		dbComp.setAlgorithmId(DbKey.createDbKey(197865L));
		dbComp.setValidEnd(Date.from(Instant.parse("2023-08-03T00:30:00Z")));

		ApiComputation apiComp = map(dbComp);

		assertNotNull(apiComp);
		assertEquals(dbComp.getId().getValue(), apiComp.getComputationId());
		assertEquals(dbComp.getAlgorithmName(), apiComp.getAlgorithmName());
		assertEquals(dbComp.getComment(), apiComp.getComment());
		assertEquals(dbComp.getAppId().getValue(), apiComp.getAppId());
		assertEquals(dbComp.getLastModified(), apiComp.getLastModified());
		assertEquals(dbComp.getAlgorithm().getName(), apiComp.getAlgorithmName());
		assertEquals(dbComp.getAlgorithmId().getValue(), apiComp.getAlgorithmId());
		assertEquals(dbComp.isEnabled(), apiComp.isEnabled());
		assertEquals(dbComp.getApplicationName(), apiComp.getApplicationName());
		assertEquals(dbComp.getValidStart(), apiComp.getEffectiveStartDate());
		assertEquals(dbComp.getValidEnd(), apiComp.getEffectiveEndDate());
		assertTrue(apiComp.isEnabled());
		assertEquals(dbComp.getGroup().getGroupName(), apiComp.getGroupName());
		assertEquals(dbComp.getGroupId().getValue(), apiComp.getGroupId());
		assertEquals(dbComp.getName(), apiComp.getName());
		assertEquals(dbComp.getProperties(), apiComp.getProps());
		assertEquals(dbComp.getParmList().stream().map(ComputationResources::map).collect(Collectors.toList()),
				apiComp.getParmList());
	}

	@Test
	void testDbComputeMap() throws Exception
	{
		ApiComputation apiComp = new ApiComputation();
		apiComp.setComputationId(16704L);
		apiComp.setName("Area Computation");
		apiComp.setAlgorithmName("Area Algorithm");
		apiComp.setComment("Computation to find the area of a given object");
		apiComp.setAppId(1L);
		apiComp.setLastModified(Date.from(Instant.parse("2023-08-01T10:30:00Z")));
		apiComp.setEnabled(true);
		apiComp.setApplicationName("Area Application");
		apiComp.setEffectiveStartDate(Date.from(Instant.parse("2023-07-01T00:30:00Z")));
		apiComp.setEffectiveEndDate(Date.from(Instant.parse("2023-08-03T00:30:00Z")));
		apiComp.setAlgorithmId(197865L);
		apiComp.setGroupName("Computation Group");
		apiComp.setGroupId(16753096L);
		DbComputation dbComp = map(apiComp);
		assertNotNull(dbComp);
		assertEquals(apiComp.getComputationId(), dbComp.getKey().getValue());
		assertEquals(apiComp.getName(), dbComp.getName());
		assertEquals(apiComp.getAlgorithmName(), dbComp.getAlgorithmName());
		assertEquals(apiComp.getComment(), dbComp.getComment());
		assertEquals(apiComp.getAppId(), dbComp.getAppId().getValue());
		assertEquals(apiComp.getLastModified(), dbComp.getLastModified());
		assertEquals(apiComp.getAlgorithmName(), dbComp.getAlgorithm().getName());
		assertEquals(apiComp.isEnabled(), dbComp.isEnabled());
		assertEquals(apiComp.getApplicationName(), dbComp.getApplicationName());
		assertEquals(apiComp.getEffectiveStartDate(), dbComp.getValidStart());
		assertEquals(apiComp.getEffectiveEndDate(), dbComp.getValidEnd());
		assertTrue(dbComp.isEnabled());
		assertEquals(apiComp.getGroupName(), dbComp.getGroup().getGroupName());
		assertEquals(apiComp.getGroupId(), dbComp.getGroupId().getValue());
		assertEquals(apiComp.getName(), dbComp.getName());
		assertEquals(apiComp.getProps(), dbComp.getProperties());
		assertEquals(apiComp.getParmList().stream().map(value ->
				{
					try
					{
						return ComputationResources.map(value);
					}
					catch(DatabaseException e)
					{
						throw new RuntimeException(e);
					}
				}).collect(Collectors.toList()),
				dbComp.getParmList());
		assertEquals(apiComp.getAlgorithmId(), dbComp.getAlgorithm().getId().getValue());
	}

	@Test
	void testComputationRefMap()
	{
		ArrayList<DbComputation> comps = new ArrayList<>();
		DbComputation dbComp = new DbComputation(DbKey.createDbKey(16704L), "Flow Computation");
		dbComp.setAlgorithmId(DbKey.createDbKey(197865L));
		dbComp.setAppId(DbKey.createDbKey(51981L));
		dbComp.setEnabled(true);
		dbComp.setComment("Computation to find the flow rate of a body of water");
		dbComp.setAlgorithmName("Flow Algorithm");
		DbCompAlgorithm dbCompAlgorithm = new DbCompAlgorithm("Flow Algorithm");
		dbCompAlgorithm.setId(DbKey.createDbKey(197865L));
		dbComp.setEnabled(true);
		comps.add(dbComp);

		ArrayList<ApiComputationRef> apiComps = map(comps);

		assertNotNull(apiComps);
		assertEquals(1, apiComps.size());
		ApiComputationRef apiComp = apiComps.get(0);
		assertNotNull(apiComp);
		assertEquals(dbComp.getId().getValue(), apiComp.getComputationId());
		assertEquals(dbComp.getName(), apiComp.getName());
		assertEquals(dbComp.getAlgorithmName(), apiComp.getAlgorithmName());
		assertEquals(dbComp.getComment(), apiComp.getDescription());
		assertEquals(dbComp.getAppId().getValue(), apiComp.getProcessId());
		assertEquals(dbComp.getApplicationName(), apiComp.getProcessName());
		assertEquals(dbComp.getAlgorithmId().getValue(), apiComp.getAlgorithmId());
	}
}
