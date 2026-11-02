package org.opendcs.database.model.tsgroup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import decodes.sql.DbKey;
import decodes.tsdb.TsGroupMemberType;

class GroupCriteriaTest
{
    private static final DbKey SITE = DbKey.createDbKey(1001);
    private static final DbKey OTHER_SITE = DbKey.createDbKey(1002);
    private static final DbKey DATATYPE = DbKey.createDbKey(2001);
    private static final DbKey TS = DbKey.createDbKey(3001);

    @Test
    void test_attribute_parts_are_matched_ignoring_case()
    {
        var attrs = GroupAttributes.of("Interval", "Hour");

        assertEquals(List.of("Hour"), List.copyOf(attrs.values("interval")));
        assertEquals(List.of("Hour"), List.copyOf(attrs.values("INTERVAL")));
        assertTrue(attrs.matches("INTERVAL", "hour"));
        assertFalse(attrs.matches("Interval", "Day"));
        assertFalse(attrs.matches("Duration", "Hour"));
    }

    @Test
    void test_typed_attribute_access()
    {
        var attrs = new GroupAttributes(attributes("version", "rev", "hdc"));

        assertEquals(2, attrs.values(TsGroupMemberType.Version).size());
        assertTrue(attrs.matches(TsGroupMemberType.Version.toString(), "REV"));
    }

    @Test
    void test_attribute_values_are_deduplicated_ignoring_case()
    {
        var attrs = GroupAttributes.of("Interval", "Hour", "hour", "HOUR", "Day");

        assertEquals(2, attrs.values("Interval").size());
        assertTrue(attrs.values("Interval").contains("Hour"));
    }

    @Test
    void test_blank_parts_and_values_are_dropped()
    {
        Map<String, Set<String>> source = new HashMap<>();
        source.put("Interval", setOf("Hour", "", null));
        source.put("  ", setOf("whatever"));
        source.put("Duration", setOf("", null));
        source.put(null, setOf("Hour"));

        var attrs = new GroupAttributes(source);

        assertEquals(Set.of("Interval"), Set.copyOf(attrs.parts()));
        assertTrue(attrs.matches("interval", "hour"));
        assertFalse(attrs.matches("Duration", "Hour"));
    }

    @Test
    void test_attributes_of_nothing()
    {
        assertTrue(GroupAttributes.EMPTY.isEmpty());
        assertTrue(new GroupAttributes(null).isEmpty());
        assertTrue(GroupAttributes.of("Interval").isEmpty());
        assertTrue(GroupAttributes.of(null, "Hour").isEmpty());
        assertEquals(Set.of(), GroupAttributes.EMPTY.values("Interval"));
    }

    @Test
    void test_attributes_are_read_only()
    {
        var attrs = GroupAttributes.of("Interval", "Hour");

        assertThrows(UnsupportedOperationException.class, () -> attrs.values("Interval").add("Day"));
        assertThrows(UnsupportedOperationException.class, () -> attrs.byPart().put("Duration", Set.of()));
    }

    @Test
    void test_attributes_copy_their_source()
    {
        var source = attributes("Interval", "Hour");
        var attrs = new GroupAttributes(source);

        source.put("Duration", setOf("0Hour"));

        assertEquals(1, attrs.parts().size());
    }

    @Test
    void test_criteria_of_nothing()
    {
        var criteria = new GroupCriteria(null, null, null, null);

        assertTrue(criteria.timeSeriesKeys().isEmpty());
        assertTrue(criteria.siteIds().isEmpty());
        assertTrue(criteria.dataTypeIds().isEmpty());
        assertTrue(criteria.attributes().isEmpty());
        assertTrue(criteria.isEmpty());
        assertTrue(criteria.matchesNothing());
        assertEquals(criteria, GroupCriteria.EMPTY);
    }

    @Test
    void test_criteria_drops_repeats_and_nulls_keeping_order()
    {
        var criteria = new GroupCriteria(Arrays.asList(TS, TS, null),
                                         Arrays.asList(SITE, OTHER_SITE, SITE),
                                         Arrays.asList(null, DATATYPE),
                                         GroupAttributes.of("Interval", "Hour"));

        assertEquals(List.of(TS), criteria.timeSeriesKeys());
        assertEquals(List.of(SITE, OTHER_SITE), criteria.siteIds());
        assertEquals(List.of(DATATYPE), criteria.dataTypeIds());
    }

    @Test
    void test_an_unsaved_key_is_kept_so_that_saving_can_complain_about_it()
    {
        var criteria = new GroupCriteria(List.of(DbKey.NullKey), List.of(), List.of(), null);

        assertEquals(List.of(DbKey.NullKey), criteria.timeSeriesKeys());
    }

    @Test
    void test_explicit_members_alone_still_match_nothing()
    {
        var criteria = new GroupCriteria(List.of(TS), List.of(), List.of(), null);

        assertTrue(criteria.matchesNothing());
        assertFalse(criteria.isEmpty());
    }

    @Test
    void test_any_criterion_stops_matching_nothing()
    {
        assertFalse(new GroupCriteria(List.of(), List.of(SITE), List.of(), null).matchesNothing());
        assertFalse(new GroupCriteria(List.of(), List.of(), List.of(DATATYPE), null).matchesNothing());
        assertFalse(new GroupCriteria(List.of(), List.of(), List.of(),
                                      GroupAttributes.of("Interval", "Hour")).matchesNothing());
    }

    @Test
    void test_criteria_is_read_only()
    {
        var criteria = new GroupCriteria(List.of(TS), List.of(SITE), List.of(DATATYPE), null);

        assertThrows(UnsupportedOperationException.class, () -> criteria.siteIds().add(OTHER_SITE));
    }

    private static Map<String, Set<String>> attributes(String part, String... values)
    {
        Map<String, Set<String>> ret = new HashMap<>();
        ret.put(part, setOf(values));
        return ret;
    }

    private static Set<String> setOf(String... values)
    {
        return new LinkedHashSet<>(Arrays.asList(values));
    }
}
