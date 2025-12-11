package org.opendcs.odcsapi.res;

import java.util.ArrayList;
import java.util.List;

import decodes.db.DataType;
import decodes.db.DataTypeSet;
import decodes.db.EngineeringUnit;
import decodes.db.EngineeringUnitList;
import decodes.db.UnitConverter;
import decodes.db.UnitConverterDb;
import decodes.db.UnitConverterSet;
import decodes.sql.DbKey;
import org.junit.jupiter.api.Test;
import org.opendcs.odcsapi.beans.ApiDataType;
import org.opendcs.odcsapi.beans.ApiUnit;
import org.opendcs.odcsapi.beans.ApiUnitConverter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opendcs.odcsapi.res.DatatypeUnitResources.map;
import static org.opendcs.odcsapi.res.DatatypeUnitResources.ucDbMap;
import static org.opendcs.odcsapi.res.DatatypeUnitResources.ucMap;

final class DatatypeUnitResourcesTest
{
	@Test
	void testMapUnitConverter() throws Exception
	{
		ApiUnitConverter auc = new ApiUnitConverter();
		auc.setFromAbbr("ft");
		auc.setToAbbr("m");
		auc.setAlgorithm("None");
		auc.setA(1.0);
		auc.setB(2.0);
		auc.setC(3.0);
		auc.setD(4.0);
		auc.setE(5.0);
		auc.setF(6.0);
		auc.setUcId(1234L);
		UnitConverterDb ucs = ucDbMap(auc);

		assertNotNull(ucs);
		ucs.prepareForExec();
		assertTrue(ucs.isPrepared());
		assertEquals(1234L, ucs.getId().getValue());
		assertEquals("ft->m", ucs.toString());
		assertEquals("ft", ucs.fromAbbr);
		assertEquals("m", ucs.toAbbr);
		assertEquals("None", ucs.algorithm);
		assertEquals(1.0, ucs.coefficients[0]);
		assertEquals(2.0, ucs.coefficients[1]);
		assertEquals(3.0, ucs.coefficients[2]);
		assertEquals(4.0, ucs.coefficients[3]);
		assertEquals(5.0, ucs.coefficients[4]);
		assertEquals(6.0, ucs.coefficients[5]);
	}

	@Test
	void testUcMap() throws Exception
	{
		ApiUnitConverter auc = new ApiUnitConverter();
		auc.setFromAbbr("ft");
		auc.setToAbbr("m");
		auc.setAlgorithm("None");
		auc.setA(1.0);
		auc.setB(2.0);
		auc.setC(3.0);
		auc.setD(4.0);
		auc.setE(5.0);
		auc.setF(6.0);
		auc.setUcId(1234L);

		UnitConverter uc = ucMap(auc);

		assertNotNull(uc);
		assertEquals("ft", uc.getFromAbbr());
		assertEquals("m", uc.getToAbbr());
	}

	@Test
	void testDbUcMap()
	{
		UnitConverterDb ucdb = new UnitConverterDb("ft", "m");
		ucdb.algorithm = "none";
		ucdb.coefficients[0] = 1.0;
		ucdb.coefficients[1] = 2.0;
		ucdb.coefficients[2] = 3.0;
		ucdb.coefficients[3] = 4.0;
		ucdb.coefficients[4] = 5.0;
		ucdb.coefficients[5] = 6.0;
		ucdb.forceSetId(DbKey.createDbKey(1234L));

		ApiUnitConverter auc = map(ucdb);

		assertNotNull(auc);
		assertEquals(ucdb.fromAbbr, auc.getFromAbbr());
		assertEquals(ucdb.toAbbr, auc.getToAbbr());
		assertEquals(ucdb.algorithm, auc.getAlgorithm());
		assertEquals(ucdb.coefficients[0], auc.getA());
		assertEquals(ucdb.coefficients[1], auc.getB());
		assertEquals(ucdb.coefficients[2], auc.getC());
		assertEquals(ucdb.coefficients[3], auc.getD());
		assertEquals(ucdb.coefficients[4], auc.getE());
		assertEquals(ucdb.coefficients[5], auc.getF());
		assertEquals(ucdb.getId().getValue(), auc.getUcId());
	}

	@Test
	void testUnitListMap()
	{
		EngineeringUnitList eul = new EngineeringUnitList();
		EngineeringUnit eu = new EngineeringUnit("C", "Celsius", "Metric", "temperature");
		EngineeringUnit eu2 = new EngineeringUnit("K", "Kelvin", "Metric", "temperature");
		eul.add(eu);
		eul.add(eu2);

		ArrayList<ApiUnit> apiUnits = map(eul);
		assertNotNull(apiUnits);
		assertEquals(2, apiUnits.size());
		assertEquals("C", apiUnits.get(0).getAbbr());
		assertEquals("Celsius", apiUnits.get(0).getName());
		assertEquals("Metric", apiUnits.get(0).getFamily());
		assertEquals("temperature", apiUnits.get(0).getMeasures());
		assertEquals("K", apiUnits.get(1).getAbbr());
		assertEquals("Kelvin", apiUnits.get(1).getName());
		assertEquals("Metric", apiUnits.get(1).getFamily());
		assertEquals("temperature", apiUnits.get(1).getMeasures());
	}

	@Test
	void testUnitConvertMap()
	{
		UnitConverterSet unitSet = new UnitConverterSet();
		UnitConverterDb unitDb = new UnitConverterDb("ft", "m");
		unitDb.algorithm = "none";
		unitDb.coefficients[0] = 1.0;
		unitDb.coefficients[1] = 2.0;
		unitDb.coefficients[2] = 3.0;
		unitDb.coefficients[3] = 4.0;
		unitDb.coefficients[4] = 5.0;
		unitDb.coefficients[5] = 6.0;
		unitSet.addDbConverter(unitDb);
		unitDb.forceSetId(DbKey.createDbKey(1234L));
		List<ApiUnitConverter> unitConverterList = map(unitSet);
		assertNotNull(unitConverterList);
		assertEquals(1, unitConverterList.size());
		ApiUnitConverter unitConverter = unitConverterList.get(0);
		assertNotNull(unitConverter);
		assertEquals(unitDb.fromAbbr, unitConverter.getFromAbbr());
		assertEquals(unitDb.toAbbr, unitConverter.getToAbbr());
		assertEquals(unitDb.algorithm, unitConverter.getAlgorithm());
		assertEquals(unitDb.coefficients[0], unitConverter.getA());
		assertEquals(unitDb.coefficients[1], unitConverter.getB());
		assertEquals(unitDb.coefficients[2], unitConverter.getC());
		assertEquals(unitDb.coefficients[3], unitConverter.getD());
		assertEquals(unitDb.coefficients[4], unitConverter.getE());
		assertEquals(unitDb.coefficients[5], unitConverter.getF());
		assertEquals(unitDb.getId().getValue(), unitConverter.getUcId());
	}

	@Test
	void testDataTypeSetMap() throws Exception
	{
		DataTypeSet dts = new DataTypeSet();
		DataType dt = new DataType("SHEF-PE", "TEST_CODE");
		dt.setId(DbKey.createDbKey(1234L));
		dt.setDisplayName("Test Display Name");
		dts.add(dt);
		DataType dt2 = new DataType("EPA-CODE", "995842215");
		dt2.setId(DbKey.createDbKey(5678L));
		dt2.setDisplayName("Test Display Name 2");
		dts.add(dt2);

		ArrayList<ApiDataType> apiDataTypes = map(dts);
		assertNotNull(apiDataTypes);
		assertEquals(2, apiDataTypes.size());
		ApiDataType apiDataType = apiDataTypes.get(0);
		assertEquals(dt.getCode(), apiDataType.getCode());
		assertEquals(dt.getId().getValue(), apiDataType.getId());
		assertEquals(dt.getDisplayName(), apiDataType.getDisplayName());
		assertEquals(dt.getStandard(), apiDataType.getStandard());
		ApiDataType apiDataType2 = apiDataTypes.get(1);
		assertEquals(dt2.getCode(), apiDataType2.getCode());
		assertEquals(dt2.getId().getValue(), apiDataType2.getId());
		assertEquals(dt2.getDisplayName(), apiDataType2.getDisplayName());
		assertEquals(dt2.getStandard(), apiDataType2.getStandard());
	}
}
