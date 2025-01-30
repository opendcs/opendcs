package org.opendcs.odcsapi.res;

import java.util.ArrayList;
import java.util.HashMap;

import decodes.db.DbEnum;
import decodes.db.EnumValue;
import decodes.sql.DbKey;
import org.junit.jupiter.api.Test;
import org.opendcs.odcsapi.beans.ApiRefList;
import org.opendcs.odcsapi.beans.ApiRefListItem;
import org.opendcs.odcsapi.beans.ApiSeason;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opendcs.odcsapi.res.ReflistResources.map;
import static org.opendcs.odcsapi.res.ReflistResources.mapSeasons;
import static org.opendcs.odcsapi.res.ReflistResources.mapToEnum;

final class ReflistResourcesTest
{
	@Test
	void testEnumMap() throws Exception
	{
		ApiRefList refList = new ApiRefList();
		refList.setReflistId(55674L);
		refList.setDefaultValue("Summer");
		refList.setDescription("Month in summer");
		refList.setEnumName("Season enum");
		HashMap<String, ApiRefListItem> items = new HashMap<>();
		ApiRefListItem item = new ApiRefListItem();
		item.setValue("June");
		item.setDescription("Month in summer");
		item.setSortNumber(1);
		items.put(refList.getEnumName(), item);
		refList.setItems(items);

		DbEnum dbEnum = mapToEnum(refList);

		assertNotNull(dbEnum);
		assertEquals(refList.getReflistId(), dbEnum.getId().getValue());
		assertEquals(refList.getItems().get(refList.getEnumName()).getDescription(),
				dbEnum.getDescription());
		assertEquals(refList.getDefaultValue(), dbEnum.getDefault());
		assertEquals(refList.getItems().size(), dbEnum.size());
		assertEquals(refList.getEnumName(), dbEnum.enumName);
	}

	@Test
	void testSeasonMap() throws Exception
	{
		DbEnum dbEnum = new DbEnum("season");
		dbEnum.setDescription("Seasons of the year");
		dbEnum.setDefault("Autumn");
		dbEnum.setId(DbKey.createDbKey(55674L));
		EnumValue enumValue = new EnumValue(dbEnum, "m");
		enumValue.setDescription("Meter"); // NOT MAPPED
		enumValue.setSortNumber(1);
		enumValue.setEditClassName("start end UTC");
		enumValue.setExecClassName("Integer.class"); // NOT MAPPED
		dbEnum.addValue(enumValue);

		ArrayList<ApiSeason> seasons = mapSeasons(dbEnum);

		assertNotNull(seasons);
		ApiSeason season = seasons.get(0);
		assertEquals(enumValue.getValue(), season.getAbbr());
		assertEquals(enumValue.getFullName(), season.getName());
		assertEquals(enumValue.getEditClassName(),
				String.format("%s %s %s", season.getStart(), season.getEnd(), season.getTz()));
	}

	@Test
	void testApiSeasonMap()
	{
		DbEnum dbEnum = new DbEnum("season");
		EnumValue season = new EnumValue(dbEnum, "m");
		season.setDescription("Meter");
		String startEndTz = "start end UTC";
		season.setEditClassName(startEndTz);

		ApiSeason apiSeason = map(season);

		assertNotNull(apiSeason);
		assertEquals(season.getValue(), apiSeason.getAbbr());
		assertEquals(season.getDescription(), apiSeason.getName());
		assertEquals("start", apiSeason.getStart());
		assertEquals("end", apiSeason.getEnd());
		assertEquals("UTC", apiSeason.getTz());
	}

	@Test
	void testSeasonApiMap()
	{
		DbEnum dbEnum = new DbEnum("season");
		ApiSeason season = new ApiSeason();
		season.setAbbr("m");
		season.setName("Meter");
		season.setStart("start");
		season.setEnd("end");
		season.setTz("UTC");
		season.setSortNumber(1);

		EnumValue result = map(season, dbEnum);

		assertNotNull(result);
		assertEquals(season.getAbbr(), result.getValue());
		assertEquals(season.getName(), result.getDescription());
		String[] startEndTzArray = result.getEditClassName().split(" ");
		assertEquals(season.getStart(), startEndTzArray[0]);
		assertEquals(season.getEnd(), startEndTzArray[1]);
		assertEquals(season.getTz(), startEndTzArray[2]);
	}

	@Test
	void testEnumSeasonMap()
	{
		ApiSeason season = new ApiSeason();
		String abbr = "m";
		season.setAbbr(abbr);
		season.setName("Meter");
		season.setStart("start");
		season.setEnd("end");
		season.setSortNumber(1);
		season.setTz("UTC");

		DbEnum dbEnum = map(season, abbr);

		assertNotNull(dbEnum);
		assertEquals(season.getName(), dbEnum.getUniqueName());
	}

	@Test
	void testEnumListMap() throws Exception
	{
		DbEnum dbEnum = new DbEnum("Unique Enum Name");
		dbEnum.setDescription("A unique enum");
		dbEnum.setId(DbKey.createDbKey(55674L));
		dbEnum.setDefault("defaultValue");
		EnumValue enumValue = new EnumValue(dbEnum, "Value");
		enumValue.setSortNumber(8);
		enumValue.setEditClassName("String.class");
		enumValue.setExecClassName("Integer.class");
		enumValue.setDescription("A value");
		dbEnum.addValue(enumValue);

		ApiRefList refList = map(dbEnum);

		assertNotNull(refList);
		assertEquals(dbEnum.getId().getValue(), refList.getReflistId());
		assertEquals(dbEnum.getDescription(), refList.getDescription());
		assertEquals(dbEnum.getDefault(), refList.getDefaultValue());
		assertEquals(dbEnum.getUniqueName(), refList.getEnumName());
		assertEquals(1, refList.getItems().size());
		ApiRefListItem item = refList.getItems().get(dbEnum.getUniqueName());
		assertNotNull(item);
		assertEquals(enumValue.getDescription(), item.getDescription());
		assertEquals(enumValue.getSortNumber(), item.getSortNumber());
		assertEquals(enumValue.getEditClassName(), item.getEditClassName());
		assertEquals(enumValue.getExecClassName(), item.getExecClassName());
	}
}