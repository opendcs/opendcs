package org.opendcs.odcsapi.res;

import java.util.Properties;

import decodes.db.EquipmentModel;
import decodes.sql.DbKey;
import org.junit.jupiter.api.Test;
import org.opendcs.odcsapi.beans.ApiEquipmentModel;
import org.opendcs.odcsapi.beans.ApiEquipmentModelRef;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opendcs.odcsapi.res.EquipmentResources.map;
import static org.opendcs.odcsapi.res.EquipmentResources.mapFull;
import static org.opendcs.odcsapi.res.EquipmentResources.mapRef;

final class EquipmentResourcesTest
{
	@Test
	void testMapRef() throws Exception
	{
		EquipmentModel em = new EquipmentModel();
		em.setId(DbKey.createDbKey(42L));
		em.name = "GOES-DCP-1";
		em.equipmentType = "goes";
		em.company = "Sutron";
		em.model = "9210B";
		em.description = "Sutron 9210B data logger";

		ApiEquipmentModelRef ref = mapRef(em);

		assertNotNull(ref);
		assertEquals(42L, ref.getEquipmentId());
		assertEquals(em.name, ref.getName());
		assertEquals(em.equipmentType, ref.getEquipmentType());
		assertEquals(em.company, ref.getCompany());
		assertEquals(em.model, ref.getModel());
		assertEquals(em.description, ref.getDescription());
	}

	@Test
	void testMapRefNullId() throws Exception
	{
		EquipmentModel em = new EquipmentModel();
		em.name = "No-ID-Model";

		ApiEquipmentModelRef ref = mapRef(em);

		assertNotNull(ref);
		assertNull(ref.getEquipmentId());
		assertEquals(em.name, ref.getName());
	}

	@Test
	void testMapFull() throws Exception
	{
		Properties props = new Properties();
		props.setProperty("key1", "value1");
		props.setProperty("key2", "value2");

		EquipmentModel em = new EquipmentModel();
		em.setId(DbKey.createDbKey(99L));
		em.name = "Full-Model";
		em.equipmentType = "satellite";
		em.company = "Acme";
		em.model = "X100";
		em.description = "A full equipment model";
		em.properties = props;

		ApiEquipmentModel api = mapFull(em);

		assertNotNull(api);
		assertEquals(99L, api.getEquipmentId());
		assertEquals(em.name, api.getName());
		assertEquals(em.equipmentType, api.getEquipmentType());
		assertEquals(em.company, api.getCompany());
		assertEquals(em.model, api.getModel());
		assertEquals(em.description, api.getDescription());
		assertEquals(props, api.getProperties());
	}

	@Test
	void testMapFullNullId() throws Exception
	{
		EquipmentModel em = new EquipmentModel();
		em.name = "No-ID-Full";
		em.equipmentType = "local";

		ApiEquipmentModel api = mapFull(em);

		assertNotNull(api);
		assertNull(api.getEquipmentId());
		assertEquals(em.name, api.getName());
		assertEquals(em.equipmentType, api.getEquipmentType());
	}

	@Test
	void testMapApiToEquipmentModel() throws Exception
	{
		Properties props = new Properties();
		props.setProperty("prop", "val");

		ApiEquipmentModel apiModel = new ApiEquipmentModel();
		apiModel.setEquipmentId(7L);
		apiModel.setName("API-Model");
		apiModel.setEquipmentType("goes");
		apiModel.setCompany("Corp");
		apiModel.setModel("M200");
		apiModel.setDescription("Test model");
		apiModel.setProperties(props);

		EquipmentModel em = map(apiModel);

		assertNotNull(em);
		assertEquals(7L, em.getId().getValue());
		assertEquals(apiModel.getName(), em.name);
		assertEquals(apiModel.getEquipmentType(), em.equipmentType);
		assertEquals(apiModel.getCompany(), em.company);
		assertEquals(apiModel.getModel(), em.model);
		assertEquals(apiModel.getDescription(), em.description);
		assertEquals(props, em.properties);
	}

	@Test
	void testMapApiToEquipmentModelNullId() throws Exception
	{
		ApiEquipmentModel apiModel = new ApiEquipmentModel();
		apiModel.setName("No-ID-API");
		apiModel.setEquipmentType("local");

		EquipmentModel em = map(apiModel);

		assertNotNull(em);
		assertNull(apiModel.getEquipmentId());
		assertEquals(apiModel.getName(), em.name);
	}
}
