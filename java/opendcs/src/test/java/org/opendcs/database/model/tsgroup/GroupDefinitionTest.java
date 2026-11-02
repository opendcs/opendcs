package org.opendcs.database.model.tsgroup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import decodes.cwms.CwmsTsId;
import decodes.sql.DbKey;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsGroup;
import decodes.tsdb.TsGroupMemberType;

class GroupDefinitionTest
{
    private static final DbKey GROUP = DbKey.createDbKey(100);
    private static final DbKey CHILD = DbKey.createDbKey(200);
    private static final DbKey OTHER_CHILD = DbKey.createDbKey(300);
    private static final DbKey SITE = DbKey.createDbKey(1001);
    private static final DbKey OTHER_SITE = DbKey.createDbKey(1002);
    private static final DbKey DATATYPE = DbKey.createDbKey(2001);
    private static final DbKey TS = DbKey.createDbKey(3001);

    private static final TsGroupRef CHILD_REF = new TsGroupRef(CHILD, "Child A", "comp", "a");
    private static final TsGroupRef OTHER_CHILD_REF = new TsGroupRef(OTHER_CHILD, "Child B", "comp", "b");

    @Test
    void test_minimal_definition()
    {
        var group = GroupDefinition.builder("Root", "comp").build();

        assertEquals("Root", group.name());
        assertEquals("comp", group.type());
        assertEquals("", group.description());
        assertFalse(group.isSaved());
        assertEquals(DbKey.NullKey, group.id());
        assertEquals(GroupCriteria.EMPTY, group.criteria());
        assertTrue(group.subGroups().isEmpty());
    }

    @Test
    void test_build_needs_a_name_and_a_type()
    {
        assertThrows(IllegalArgumentException.class, () -> GroupDefinition.builder("  ", "comp").build());
        assertThrows(IllegalArgumentException.class, () -> GroupDefinition.builder("Root", null).build());
    }

    @Test
    void test_a_saved_reference_stays_saved()
    {
        var group = GroupDefinition.builder(new TsGroupRef(GROUP, "Root", "comp", "the root"))
                                   .description("changed")
                                   .build();

        assertTrue(group.isSaved());
        assertEquals(GROUP, group.id());
        assertEquals("changed", group.description());
    }

    @Test
    void test_everything_a_definition_holds()
    {
        var group = GroupDefinition.builder("Root", "comp")
                                   .id(GROUP)
                                   .description("the root")
                                   .timeSeries(TS)
                                   .sites(SITE)
                                   .dataTypes(DATATYPE)
                                   .attribute(TsGroupMemberType.Interval, "Hour")
                                   .attribute("ObjectType", "ISGOT")
                                   .include(CHILD_REF)
                                   .exclude(OTHER_CHILD_REF)
                                   .build();

        assertEquals(List.of(TS), group.criteria().timeSeriesKeys());
        assertEquals(List.of(SITE), group.criteria().siteIds());
        assertEquals(List.of(DATATYPE), group.criteria().dataTypeIds());
        assertTrue(group.criteria().attributes().matches("interval", "hour"));
        assertTrue(group.criteria().attributes().matches("objecttype", "isgot"));
        assertEquals(List.of(CHILD_REF), group.subGroups(GroupCombine.ADD));
        assertEquals(List.of(OTHER_CHILD_REF), group.subGroups(GroupCombine.SUBTRACT));
        assertTrue(group.subGroups(GroupCombine.INTERSECT).isEmpty());
        assertTrue(group.linksSubGroup(CHILD));
        assertFalse(group.linksSubGroup(SITE));
    }

    @Test
    void test_combining_a_sub_group_twice_sets_its_operator()
    {
        var group = GroupDefinition.builder("Root", "comp")
                                   .include(CHILD_REF)
                                   .intersect(new TsGroupRef(CHILD, "child a", "comp", "renamed"))
                                   .build();

        assertEquals(1, group.subGroups().size());
        assertEquals(List.of(), group.subGroups(GroupCombine.ADD));
        assertEquals(1, group.subGroups(GroupCombine.INTERSECT).size());
    }

    @Test
    void test_an_unsaved_sub_group_is_known_by_name()
    {
        var group = GroupDefinition.builder("Root", "comp")
                                   .include(TsGroupRef.unsaved("From Xml", "comp"))
                                   .exclude(TsGroupRef.unsaved("from xml", "comp"))
                                   .build();

        assertEquals(1, group.subGroups().size());
        assertEquals(List.of(), group.subGroups(GroupCombine.ADD));
        assertEquals(1, group.subGroups(GroupCombine.SUBTRACT).size());
    }

    @Test
    void test_removing_members()
    {
        var group = GroupDefinition.builder("Root", "comp")
                                   .timeSeries(TS)
                                   .sites(SITE)
                                   .dataTypes(DATATYPE)
                                   .attribute("Interval", "Hour", "Day")
                                   .include(CHILD_REF)
                                   .include(OTHER_CHILD_REF)
                                   .build();

        var trimmed = group.edit()
                           .withoutAttribute("interval", "HOUR")
                           .without(CHILD)
                           .clearSites()
                           .clearTimeSeries()
                           .build();

        assertTrue(trimmed.criteria().timeSeriesKeys().isEmpty());
        assertTrue(trimmed.criteria().siteIds().isEmpty());
        assertEquals(List.of(DATATYPE), trimmed.criteria().dataTypeIds());
        assertEquals(List.of("Day"), List.copyOf(trimmed.criteria().attributes().values("Interval")));
        assertEquals(List.of(OTHER_CHILD_REF), trimmed.subGroups(GroupCombine.ADD));
        assertEquals("Root", trimmed.name());
    }

    @Test
    void test_attributes_can_be_replaced_wholesale()
    {
        var group = GroupDefinition.builder("Root", "comp")
                                   .attribute("Interval", "Hour")
                                   .attributes(GroupAttributes.of("Version", "rev"))
                                   .build();

        assertTrue(group.criteria().attributes().values("Interval").isEmpty());
        assertTrue(group.criteria().attributes().matches("version", "rev"));
    }

    @Test
    void test_editing_changes_nothing_else()
    {
        var group = GroupDefinition.builder("Root", "comp")
                                   .id(GROUP)
                                   .description("the root")
                                   .timeSeries(TS)
                                   .sites(SITE)
                                   .attribute("Interval", "Hour")
                                   .include(CHILD_REF)
                                   .build();

        assertEquals(group, group.edit().build());
        assertEquals(group.criteria(), group.edit().description("other").build().criteria());
        assertEquals("other", group.edit().description("other").build().description());
    }

    @Test
    void test_a_built_definition_does_not_change_under_the_builder()
    {
        var builder = GroupDefinition.builder("Root", "comp").sites(SITE).attribute("Interval", "Hour");
        var built = builder.build();

        builder.name("Renamed").sites(OTHER_SITE).attribute("Interval", "Day").include(CHILD_REF);

        assertEquals("Root", built.name());
        assertEquals(List.of(SITE), built.criteria().siteIds());
        assertEquals(List.of("Hour"), List.copyOf(built.criteria().attributes().values("Interval")));
        assertTrue(built.subGroups().isEmpty());
    }

    @Test
    void test_a_sub_group_cannot_be_linked_twice()
    {
        var ref = GroupDefinition.builder("Root", "comp").build();

        assertThrows(IllegalArgumentException.class,
                     () -> new GroupDefinition(ref.ref(), GroupCriteria.EMPTY,
                                               List.of(new SubGroupLink(CHILD_REF, GroupCombine.ADD),
                                                       new SubGroupLink(CHILD_REF, GroupCombine.ADD))));
    }

    @Test
    void test_missing_pieces_of_a_definition_are_empty()
    {
        var group = new GroupDefinition(new TsGroupRef(GROUP, "Root", "comp", null), null, null);

        assertEquals("", group.description());
        assertEquals(GroupCriteria.EMPTY, group.criteria());
        assertTrue(group.subGroups().isEmpty());

        var withNulls = new GroupDefinition(group.ref(), group.criteria(),
                                            Arrays.asList(null, new SubGroupLink(CHILD_REF, GroupCombine.ADD)));
        assertEquals(1, withNulls.subGroups().size());
    }

    @Test
    void test_reference_identity()
    {
        var saved = new TsGroupRef(GROUP, "Root", "comp", "the root");

        assertFalse(saved.isSameGroup(null));
        assertTrue(saved.isSameGroup(new TsGroupRef(GROUP, "Renamed", "", null)));
        assertFalse(saved.isSameGroup(new TsGroupRef(CHILD, "Root", "comp", "the root")));
        assertTrue(TsGroupRef.unsaved("Child", "comp").isSameGroup(TsGroupRef.unsaved("CHILD", "other")));
        assertFalse(TsGroupRef.unsaved("", "comp").isSameGroup(TsGroupRef.unsaved("", "comp")));
        assertTrue(saved.isSameGroup(TsGroupRef.unsaved("root", "comp")));
    }

    @Test
    void test_reference_defaults()
    {
        var ref = new TsGroupRef(DbKey.NullKey, null, null, null);

        assertFalse(ref.isSaved());
        assertEquals("", ref.name());
        assertEquals("", ref.type());
        assertEquals("", ref.description());
        assertFalse(TsGroupRef.unsaved("Child", "comp").isSaved());
        assertThrows(NullPointerException.class, () -> new TsGroupRef(null, "Root", "comp", null));
    }

    @Test
    void test_reading_a_legacy_group()
    {
        var group = GroupDefinition.ofLegacy(legacyGroup());

        assertEquals(GROUP, group.id());
        assertEquals("Root", group.name());
        assertEquals("comp", group.type());
        assertEquals("the root", group.description());
        assertEquals(List.of(TS), group.criteria().timeSeriesKeys());
        assertEquals(List.of(SITE), group.criteria().siteIds());
        assertEquals(List.of(DATATYPE), group.criteria().dataTypeIds());
        assertEquals(List.of("Hour"), List.copyOf(group.criteria().attributes().values("Interval")));
        assertEquals(List.of(CHILD_REF), group.subGroups(GroupCombine.ADD));
        assertEquals(List.of(OTHER_CHILD_REF), group.subGroups(GroupCombine.SUBTRACT));
    }

    @Test
    void test_writing_a_legacy_group()
    {
        var legacy = definition().toLegacy(List.of(member(TS)));

        assertEquals(GROUP, legacy.getGroupId());
        assertEquals("Root", legacy.getGroupName());
        assertEquals("comp", legacy.getGroupType());
        assertEquals("the root", legacy.getDescription());
        assertEquals(List.of(TS), legacy.getTsMemberList().stream().map(TimeSeriesIdentifier::getKey).toList());
        assertEquals(List.of(SITE), legacy.getSiteIdList());
        assertEquals(List.of(DATATYPE), legacy.getDataTypeIdList());
        assertEquals(List.of("Hour"), legacy.getOtherMembers("interval"));
        assertEquals(List.of(CHILD), subGroupIds(legacy.getIncludedSubGroups()));
        assertEquals(List.of(OTHER_CHILD), subGroupIds(legacy.getExcludedSubGroups()));
        assertTrue(legacy.getIntersectedGroups().isEmpty());
    }

    @Test
    void test_writing_a_legacy_group_without_members()
    {
        assertTrue(definition().toLegacy().getTsMemberList().isEmpty());
    }

    @Test
    void test_legacy_sub_groups_keep_enough_to_be_shown()
    {
        var child = definition().toLegacy().getIncludedSubGroups().get(0);

        assertEquals(CHILD, child.getGroupId());
        assertEquals("Child A", child.getGroupName());
        assertEquals("comp", child.getGroupType());
        assertEquals("a", child.getDescription());
    }

    @Test
    void test_a_round_trip_through_the_legacy_object_changes_nothing()
    {
        assertEquals(definition(), GroupDefinition.ofLegacy(definition().toLegacy(List.of(member(TS)))));
    }

    @Test
    void test_of_legacy_needs_something_to_build()
    {
        assertThrows(NullPointerException.class, () -> GroupDefinition.ofLegacy(null));
        assertThrows(IllegalArgumentException.class, () -> GroupDefinition.ofLegacy(new TsGroup()));
    }

    private static GroupDefinition definition()
    {
        return GroupDefinition.builder("Root", "comp")
                              .id(GROUP)
                              .description("the root")
                              .timeSeries(TS)
                              .sites(SITE)
                              .dataTypes(DATATYPE)
                              .attribute(TsGroupMemberType.Interval, "Hour")
                              .include(CHILD_REF)
                              .exclude(OTHER_CHILD_REF)
                              .build();
    }

    private static TsGroup legacyGroup()
    {
        TsGroup legacy = new TsGroup();
        legacy.setGroupId(GROUP);
        legacy.setGroupName("Root");
        legacy.setGroupType("comp");
        legacy.setDescription("the root");
        legacy.addTsMember(member(TS));
        legacy.addSiteId(SITE);
        legacy.addDataTypeId(DATATYPE);
        legacy.addOtherMember("interval", "Hour");
        legacy.addSubGroup(stub(CHILD_REF), 'A');
        legacy.addSubGroup(stub(OTHER_CHILD_REF), 'S');
        legacy.addOtherMember("interval", "Hour");   // the editor can repeat a member
        return legacy;
    }

    private static TsGroup stub(TsGroupRef ref)
    {
        TsGroup stub = new TsGroup();
        stub.setGroupId(ref.id());
        stub.setGroupName(ref.name());
        stub.setGroupType(ref.type());
        stub.setDescription(ref.description());
        return stub;
    }

    private static TimeSeriesIdentifier member(DbKey key)
    {
        var tsid = new CwmsTsId();
        tsid.setKey(key);
        return tsid;
    }

    private static List<DbKey> subGroupIds(List<TsGroup> groups)
    {
        return groups.stream().map(TsGroup::getGroupId).toList();
    }
}
