package org.opendcs.odcsapi.res;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import decodes.db.NetworkList;
import decodes.db.NetworkListEntry;
import decodes.db.NetworkListList;
import decodes.sql.DbKey;
import org.junit.jupiter.api.Test;
import org.opendcs.odcsapi.beans.ApiNetList;
import org.opendcs.odcsapi.beans.ApiNetListItem;
import org.opendcs.odcsapi.beans.ApiNetlistRef;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opendcs.odcsapi.res.NetlistResources.map;

final class NetlistResourcesTest
{
	@Test
	void testNetworkListMap() throws Exception
	{
		NetworkListList nll = new NetworkListList();
		NetworkList nl = new NetworkList("Test");
		nl.lastModifyTime = Date.from(Instant.parse("2021-07-01T00:00:00Z"));
		nl.siteNameTypePref = "TestPref";
		nl.transportMediumType = "GOES";
		nl.setId(DbKey.createDbKey(750556L));

		nll.add(nl);

		List<ApiNetlistRef> netlistRefs = map(nll);

		assertNotNull(netlistRefs);
		assertEquals(1, netlistRefs.size());
		assertEquals(nl.getDisplayName(), netlistRefs.get(0).getName());
		assertEquals(nl.getKey().getValue(), netlistRefs.get(0).getNetlistId());
		assertEquals(nl.transportMediumType, netlistRefs.get(0).getTransportMediumType());
		assertEquals(nl.lastModifyTime, netlistRefs.get(0).getLastModifyTime());
		assertEquals(nl.siteNameTypePref, netlistRefs.get(0).getSiteNameTypePref());
	}

	@Test
	void testNetworkMap() throws Exception
	{
		NetworkList nl = new NetworkList("Test");
		nl.setId(DbKey.createDbKey(750556L));
		nl.name = "Test";
		nl.transportMediumType = "GOES";
		nl.lastModifyTime = Date.from(Instant.parse("2021-07-01T00:00:00Z"));
		nl.siteNameTypePref = "TestPref";
		HashMap<String, NetworkListEntry> entries = new HashMap<>();
		NetworkListEntry nle = new NetworkListEntry(nl, "TestEntry");
		entries.put("TestEntry", nle);
		nl.networkListEntries = entries;

		ApiNetList apiNetList = map(nl);

		assertNotNull(apiNetList);
		assertEquals(nl.getDisplayName(), apiNetList.getName());
		assertEquals(nl.getKey().getValue(), apiNetList.getNetlistId());
		assertEquals(nl.transportMediumType, apiNetList.getTransportMediumType());
		assertEquals(nl.lastModifyTime, apiNetList.getLastModifyTime());
		assertEquals(nl.siteNameTypePref, apiNetList.getSiteNameTypePref());
		assertEquals(1, apiNetList.getItems().size());
		ApiNetListItem nle2 = apiNetList.getItems().get("TestEntry".toUpperCase());
		assertNotNull(nle2);
		assertEquals(nle.getDescription(), nle2.getDescription());
		assertEquals(nle.getPlatformName(), nle2.getPlatformName());
		assertEquals(nle.getTransportId(), nle2.getTransportId());
	}

	@Test
	void testApiNetListMap() throws Exception
	{
		ApiNetList apiNetList = new ApiNetList();
		apiNetList.setName("Test");
		apiNetList.setNetlistId(750556L);
		apiNetList.setTransportMediumType("GOES");
		apiNetList.setLastModifyTime(Date.from(Instant.parse("2021-07-01T00:00:00Z")));
		HashMap<String, ApiNetListItem> entries = new HashMap<>();
		ApiNetListItem nle = new ApiNetListItem();
		nle.setDescription("Test Entry");
		nle.setPlatformName("Test Platform");
		nle.setTransportId("Test Transport ID");
		entries.put("Test Transport ID".toUpperCase(), nle);
		apiNetList.setItems(entries);

		NetworkList nl = map(apiNetList);

		assertNotNull(nl);
		assertEquals(apiNetList.getName(), nl.getDisplayName());
		assertEquals(apiNetList.getTransportMediumType(), nl.transportMediumType);
		assertEquals(apiNetList.getNetlistId(), nl.getKey().getValue());
		assertEquals(apiNetList.getLastModifyTime(), nl.lastModifyTime);
		assertEquals(1, nl.networkListEntries.size());
		NetworkListEntry nle2 = nl.networkListEntries.get("Test Transport ID".toUpperCase());
		assertNotNull(nle2);
		assertEquals(nle.getDescription(), nle2.getDescription());
		assertEquals(nle.getPlatformName(), nle2.getPlatformName());
		assertEquals(nle.getTransportId(), nle2.getTransportId());
	}

	@Test
	void testGetSingleWord()
	{
		String input = "This is a test input";
		String output = NetlistResources.getSingleWord(input);
		assertEquals("This", output);

		input = "Val[{ue: status}]";
		output = NetlistResources.getSingleWord(input);
		assertEquals("Val", output);

		input = "";
		output = NetlistResources.getSingleWord(input);
		assertEquals("", output);

		input = "goes-self-timed{}";
		output = NetlistResources.getSingleWord(input);
		assertEquals("goes-self-timed", output);
	}
}