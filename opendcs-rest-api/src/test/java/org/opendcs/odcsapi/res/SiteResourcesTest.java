package org.opendcs.odcsapi.res;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import decodes.db.Site;
import decodes.db.SiteList;
import decodes.db.SiteName;
import org.junit.jupiter.api.Test;
import org.opendcs.odcsapi.beans.ApiSite;
import org.opendcs.odcsapi.beans.ApiSiteRef;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opendcs.odcsapi.res.SiteResources.map;

final class SiteResourcesTest
{
	@Test
	void testMapSiteList()
	{
		SiteList sl = new SiteList();
		Site site1 = siteBuilder("Albuquerque");
		Site site2 = siteBuilder("Santa Fe");
		sl.addSite(site1);
		sl.addSite(site2);

		List<ApiSiteRef> siteRefs = map(sl);

		assertNotNull(siteRefs);
		assertNotNull(siteRefs.get(0));
		assertNotNull(siteRefs.get(1));

		// Site 1
		assertEquals(site1.getDescription(), siteRefs.get(0).getDescription());
		assertEquals(site1.getPublicName(), siteRefs.get(0).getPublicName());
		assertEquals(site1.getId().getValue(), siteRefs.get(0).getSiteId());
		for(Iterator<SiteName> it = site1.getNames(); it.hasNext(); )
		{
			final SiteName sn = it.next();
			assertTrue(siteRefs.get(0).getSitenames().containsKey(sn.getNameType()));
			assertEquals(sn.getNameValue(), siteRefs.get(0).getSitenames().get(sn.getNameType()));
		}

		// Site 2
		assertEquals(site2.getDescription(), siteRefs.get(1).getDescription());
		assertEquals(site2.getPublicName(), siteRefs.get(1).getPublicName());
		assertEquals(site2.getId().getValue(), siteRefs.get(1).getSiteId());
		for(Iterator<SiteName> it = site2.getNames(); it.hasNext(); )
		{
			final SiteName sn = it.next();
			assertTrue(siteRefs.get(1).getSitenames().containsKey(sn.getNameType()));
			assertEquals(sn.getNameValue(), siteRefs.get(1).getSitenames().get(sn.getNameType()));
		}
	}

	@Test
	void testMapSite() throws Exception
	{
		ApiSite apiSite = new ApiSite();
		apiSite.setActive(true);
		apiSite.setSiteId(1234L);
		apiSite.setCountry("USA");
		apiSite.setDescription("This is a test pump site");
		apiSite.setElevation(10.0);
		apiSite.setElevUnits("m");
		apiSite.setLocationtype("PUMP");
		apiSite.setTimezone("America/Denver");
		HashMap<String, String> sitenames = new HashMap<>();
		sitenames.put("PUMP", "Albuquerque");
		apiSite.setSitenames(sitenames);
		apiSite.setNearestCity("Albuquerque");
		apiSite.setState("NM");
		apiSite.setLatitude("35.0844");
		apiSite.setLongitude("-106.6506");
		apiSite.setLastModified(Date.from(Instant.now()));
		apiSite.setPublicName("Albuquerque Pump Station");

		Site result = map(apiSite);

		assertNotNull(result);
		assertEquals(apiSite.getDescription(), result.getDescription());
		assertEquals(apiSite.getPublicName(), result.getPublicName());
		assertEquals(apiSite.getElevation(), result.getElevation());
		assertEquals(apiSite.getElevUnits(), result.getElevationUnits());
		assertEquals(apiSite.isActive(), result.isActive());
		assertEquals(apiSite.getSiteId(), result.getId().getValue());
		assertEquals(apiSite.getLocationType(), result.getLocationType());
		assertEquals(apiSite.getLastModified(), result.getLastModifyTime());
		assertEquals(apiSite.getCountry(), result.country);
		assertEquals(apiSite.getTimezone(), result.timeZoneAbbr);
		assertEquals(apiSite.getNearestCity(), result.nearestCity);
		assertEquals(apiSite.getState(), result.state);
		for (Map.Entry<String, String> entry : apiSite.getSitenames().entrySet())
		{
			assertEquals(entry.getValue(), result.getName(entry.getKey()).getNameValue());
		}
	}

	@Test
	void testApiSiteMap()
	{
		Site site = siteBuilder("Albuquerque");
		ApiSite apiSite = SiteResources.map(site);

		assertNotNull(apiSite);
		assertEquals(site.getDescription(), apiSite.getDescription());
		assertEquals(site.getPublicName(), apiSite.getPublicName());
		assertEquals(site.getElevation(), apiSite.getElevation());
		assertEquals(site.getElevationUnits(), apiSite.getElevUnits());
		assertEquals(site.isActive(), apiSite.isActive());
		assertEquals(site.getId().getValue(), apiSite.getSiteId());
		assertEquals(site.getLocationType(), apiSite.getLocationType());
		assertEquals(site.getLastModifyTime(), apiSite.getLastModified());
		assertEquals(site.country, apiSite.getCountry());
		assertEquals(site.timeZoneAbbr, apiSite.getTimezone());
		assertEquals(site.nearestCity, apiSite.getNearestCity());
		assertEquals(site.state, apiSite.getState());
		for (Iterator<SiteName> it = site.getNames(); it.hasNext(); )
		{
			SiteName sn = it.next();
			assertEquals(sn.getNameValue(), apiSite.getSitenames().get(sn.getNameType()));
		}
	}

	private static Site siteBuilder(String name)
	{
		Site site = new Site();
		site.setActive(true);
		site.country = "USA";
		site.state = "NM";
		site.latitude = "35.0844";
		site.longitude = "-106.6506";
		site.setDescription("This is a test pump site");
		site.setElevation(10.0 * name.length());
		site.setPublicName(name);
		site.setElevationUnits("m");
		site.setLocationType("PUMP");
		site.setLastModifyTime(Date.from(Instant.now()));
		site.addName(new SiteName(site, "PUMP", name));
		return site;
	}

}
