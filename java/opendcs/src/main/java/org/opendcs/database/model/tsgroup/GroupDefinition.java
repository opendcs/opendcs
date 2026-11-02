/*
* Where Applicable, Copyright 2026 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package org.opendcs.database.model.tsgroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import decodes.sql.DbKey;
import decodes.tsdb.TimeSeriesIdentifier;
import decodes.tsdb.TsGroup;
import decodes.tsdb.TsGroupMember;
import decodes.tsdb.TsGroupMemberType;

/**
 * The complete, persistable definition of a time series group: who and what belongs to
 * it, and which other groups it pulls in.
 *
 * It holds no evaluation state, and its sub-groups are references rather than nested
 * definitions, so reading a definition is always one level deep and never resolves a
 * time series. Which time series a definition works out to is a separate question, with a
 * separate answer.
 *
 * Build one with {@link #builder(String, String)}, or change one with {@link #edit()}.
 */
public record GroupDefinition(TsGroupRef ref, GroupCriteria criteria, List<SubGroupLink> subGroups)
{
    public GroupDefinition
    {
        Objects.requireNonNull(ref, "A group definition always has a reference, even when the "
                                    + "group isn't in the database yet.");
        criteria = criteria == null ? GroupCriteria.EMPTY : criteria;

        List<SubGroupLink> links = new ArrayList<>();
        if (subGroups != null)
        {
            subGroups.stream().filter(Objects::nonNull).forEach(links::add);
        }
        for (int i = 0; i < links.size(); i++)
        {
            for (int j = i + 1; j < links.size(); j++)
            {
                if (links.get(i).group().isSameGroup(links.get(j).group()))
                {
                    throw new IllegalArgumentException("Group '" + ref.name() + "' links the sub-group '"
                                                       + links.get(i).group().name() + "' more than once.");
                }
            }
        }
        subGroups = List.copyOf(links);
    }

    public static Builder builder(String name, String type)
    {
        return new Builder(TsGroupRef.unsaved(name, type));
    }

    public static Builder builder(TsGroupRef ref)
    {
        return new Builder(ref);
    }

    public DbKey id()
    {
        return ref.id();
    }

    public String name()
    {
        return ref.name();
    }

    public String type()
    {
        return ref.type();
    }

    public String description()
    {
        return ref.description();
    }

    public boolean isSaved()
    {
        return ref.isSaved();
    }

    /** @return the sub-groups pulled in with the given operator, in the order they were added. */
    public List<TsGroupRef> subGroups(GroupCombine combine)
    {
        Objects.requireNonNull(combine, "Which kind of sub-group to look for has to be given.");
        return subGroups.stream()
                        .filter(link -> link.combine() == combine)
                        .map(SubGroupLink::group)
                        .toList();
    }

    /** @return true when this group pulls the given group in, however it is combined. */
    public boolean linksSubGroup(DbKey groupId)
    {
        return groupId != null && subGroups.stream().anyMatch(link -> groupId.equals(link.id()));
    }

    /** @return a builder holding everything in this definition, to change and rebuild. */
    public Builder edit()
    {
        return Builder.of(this);
    }

    /**
     * Converts a group as the rest of OpenDCS still holds it, {@link TsGroup}, into a
     * definition.
     *
     * The name-only lists that the XML import/export programs use,
     * {@code getSiteNameList()} and {@code getTsMemberIDList()}, have no counterpart here:
     * they are scratch space for resolving names to keys, and a definition is only
     * meaningful once that has happened. Explicit members are kept by key, so their
     * display names are not carried over either.
     *
     * @param legacy the group to convert. It has to have a name and a type, as it would to
     *        be saved.
     */
    public static GroupDefinition ofLegacy(TsGroup legacy)
    {
        Objects.requireNonNull(legacy, "No group to convert.");

        Builder builder = builder(new TsGroupRef(legacy.getGroupId(), legacy.getGroupName(),
                                                 legacy.getGroupType(), legacy.getDescription()));
        if (legacy.getTsMemberList() != null)
        {
            legacy.getTsMemberList().stream()
                  .filter(Objects::nonNull)
                  .map(TimeSeriesIdentifier::getKey)
                  .forEach(builder::timeSeries);
        }
        if (legacy.getSiteIdList() != null)
        {
            builder.sites(legacy.getSiteIdList());
        }
        if (legacy.getDataTypeIdList() != null)
        {
            builder.dataTypes(legacy.getDataTypeIdList());
        }
        if (legacy.getOtherMembers() != null)
        {
            for (TsGroupMember member : legacy.getOtherMembers())
            {
                if (member != null)
                {
                    builder.attribute(member.getMemberType(), member.getMemberValue());
                }
            }
        }
        addLegacySubGroups(builder, legacy.getIncludedSubGroups(), GroupCombine.ADD);
        addLegacySubGroups(builder, legacy.getExcludedSubGroups(), GroupCombine.SUBTRACT);
        addLegacySubGroups(builder, legacy.getIntersectedGroups(), GroupCombine.INTERSECT);
        return builder.build();
    }

    /**
     * @return the legacy group object for this definition. Its list of explicit members is
     *         empty, because a definition holds them by key; use
     *         {@link #toLegacy(Collection)} where the identifiers themselves are needed.
     */
    public TsGroup toLegacy()
    {
        return toLegacy(List.of());
    }

    /**
     * @param explicitMembers this group's explicit members resolved to identifiers, normally
     *        from a read of tsdb_group_member_ts. Members of the group that are not in the
     *        collection are left out of the result.
     * @return the legacy group object for this definition.
     */
    public TsGroup toLegacy(Collection<? extends TimeSeriesIdentifier> explicitMembers)
    {
        TsGroup legacy = new TsGroup();
        legacy.setGroupId(ref.id());
        legacy.setGroupName(ref.name());
        legacy.setGroupType(ref.type());
        legacy.setDescription(ref.description());

        criteria.siteIds().forEach(legacy::addSiteId);
        criteria.dataTypeIds().forEach(legacy::addDataTypeId);
        criteria.attributes().byPart()
                .forEach((part, values) -> values.forEach(value -> legacy.addOtherMember(part, value)));

        if (explicitMembers != null)
        {
            explicitMembers.stream().filter(Objects::nonNull).forEach(legacy::addTsMember);
        }

        for (SubGroupLink link : subGroups)
        {
            legacy.addSubGroup(toLegacyStub(link.group()), link.combine().value());
        }
        return legacy;
    }

    private static void addLegacySubGroups(Builder builder, List<TsGroup> children, GroupCombine combine)
    {
        if (children != null)
        {
            children.stream().filter(Objects::nonNull)
                    .forEach(child -> builder.combine(fromLegacyRef(child), combine));
        }
    }

    private static TsGroupRef fromLegacyRef(TsGroup legacy)
    {
        return new TsGroupRef(legacy.getGroupId(), legacy.getGroupName(), legacy.getGroupType(),
                              legacy.getDescription());
    }

    /** The legacy API wants a group object in a sub-group slot, even for a reference. */
    private static TsGroup toLegacyStub(TsGroupRef ref)
    {
        TsGroup stub = new TsGroup();
        stub.setGroupId(ref.id());
        stub.setGroupName(ref.name());
        stub.setGroupType(ref.type());
        stub.setDescription(ref.description());
        return stub;
    }

    /**
     * Builds or edits a {@link GroupDefinition}. Members are added rather than assigned, and
     * combining the same sub-group twice sets its operator rather than linking it twice, so
     * an editor can apply changes one at a time without reading the definition back first.
     */
    public static final class Builder
    {
        private TsGroupRef ref;
        private final Set<DbKey> timeSeriesKeys = new LinkedHashSet<>();
        private final Set<DbKey> siteIds = new LinkedHashSet<>();
        private final Set<DbKey> dataTypeIds = new LinkedHashSet<>();
        private final Map<String, Set<String>> attributes = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        private final List<SubGroupLink> subGroups = new ArrayList<>();

        private Builder(TsGroupRef ref)
        {
            this.ref = Objects.requireNonNull(ref, "A group definition always has a reference.");
        }

        private static Builder of(GroupDefinition definition)
        {
            Builder builder = new Builder(definition.ref());
            builder.timeSeriesKeys.addAll(definition.criteria().timeSeriesKeys());
            builder.siteIds.addAll(definition.criteria().siteIds());
            builder.dataTypeIds.addAll(definition.criteria().dataTypeIds());
            definition.criteria().attributes().byPart()
                      .forEach((part, values) -> builder.attribute(part, values.toArray(String[]::new)));
            definition.subGroups().forEach(link -> builder.combine(link.group(), link.combine()));
            return builder;
        }

        public Builder id(DbKey id)
        {
            return ref(new TsGroupRef(id, ref.name(), ref.type(), ref.description()));
        }

        public Builder name(String name)
        {
            return ref(new TsGroupRef(ref.id(), name, ref.type(), ref.description()));
        }

        public Builder type(String type)
        {
            return ref(new TsGroupRef(ref.id(), ref.name(), type, ref.description()));
        }

        public Builder description(String description)
        {
            return ref(new TsGroupRef(ref.id(), ref.name(), ref.type(), description));
        }

        private Builder ref(TsGroupRef ref)
        {
            this.ref = Objects.requireNonNull(ref, "A group definition always has a reference.");
            return this;
        }

        public Builder timeSeries(DbKey... keys)
        {
            return timeSeries(keys == null ? List.of() : Arrays.asList(keys));
        }

        public Builder timeSeries(Collection<? extends DbKey> keys)
        {
            if (keys != null)
            {
                keys.stream().filter(Objects::nonNull).forEach(timeSeriesKeys::add);
            }
            return this;
        }

        public Builder clearTimeSeries()
        {
            timeSeriesKeys.clear();
            return this;
        }

        public Builder sites(DbKey... ids)
        {
            return sites(ids == null ? List.of() : Arrays.asList(ids));
        }

        public Builder sites(Collection<? extends DbKey> keys)
        {
            if (keys != null)
            {
                keys.stream().filter(Objects::nonNull).forEach(siteIds::add);
            }
            return this;
        }

        public Builder clearSites()
        {
            siteIds.clear();
            return this;
        }

        public Builder dataTypes(DbKey... ids)
        {
            return dataTypes(ids == null ? List.of() : Arrays.asList(ids));
        }

        public Builder dataTypes(Collection<? extends DbKey> keys)
        {
            if (keys != null)
            {
                keys.stream().filter(Objects::nonNull).forEach(dataTypeIds::add);
            }
            return this;
        }

        public Builder clearDataTypes()
        {
            dataTypeIds.clear();
            return this;
        }

        public Builder attribute(TsGroupMemberType part, String... values)
        {
            return attribute(part == null ? null : part.toString(), values);
        }

        /** Adds values to a part's list, which is what a group asks a time series to match. */
        public Builder attribute(String part, String... values)
        {
            if (part != null && !part.isBlank() && values != null)
            {
                Arrays.stream(values)
                      .filter(value -> value != null && !value.isBlank())
                      .forEach(value -> valuesOf(part).add(value));
            }
            return this;
        }

        /** Replaces every part and value, e.g. to apply a definition read back from the database. */
        public Builder attributes(GroupAttributes attributes)
        {
            this.attributes.clear();
            if (attributes != null)
            {
                attributes.byPart().forEach((part, values) -> valuesOf(part).addAll(values));
            }
            return this;
        }

        public Builder withoutAttribute(String part, String value)
        {
            Set<String> values = attributes.get(part);
            if (values != null && value != null)
            {
                values.remove(value);
                if (values.isEmpty())
                {
                    attributes.remove(part);
                }
            }
            return this;
        }

        public Builder clearAttributes()
        {
            attributes.clear();
            return this;
        }

        public Builder include(TsGroupRef group)
        {
            return combine(group, GroupCombine.ADD);
        }

        public Builder exclude(TsGroupRef group)
        {
            return combine(group, GroupCombine.SUBTRACT);
        }

        public Builder intersect(TsGroupRef group)
        {
            return combine(group, GroupCombine.INTERSECT);
        }

        /** Adds a sub-group, or sets the operator for one that is already linked. */
        public Builder combine(TsGroupRef group, GroupCombine combine)
        {
            if (group != null && combine != null)
            {
                subGroups.removeIf(link -> link.group().isSameGroup(group));
                subGroups.add(new SubGroupLink(group, combine));
            }
            return this;
        }

        public Builder without(DbKey subGroupId)
        {
            if (subGroupId != null)
            {
                subGroups.removeIf(link -> subGroupId.equals(link.id()));
            }
            return this;
        }

        public Builder clearSubGroups()
        {
            subGroups.clear();
            return this;
        }

        public GroupDefinition build()
        {
            if (ref.name().isBlank())
            {
                throw new IllegalArgumentException("Cannot build a group definition without a name.");
            }
            if (ref.type().isBlank())
            {
                throw new IllegalArgumentException("Cannot build the group '" + ref.name()
                                                   + "' without a type.");
            }
            return new GroupDefinition(ref,
                                       new GroupCriteria(new ArrayList<>(timeSeriesKeys),
                                                         new ArrayList<>(siteIds),
                                                         new ArrayList<>(dataTypeIds),
                                                         new GroupAttributes(attributes)),
                                       subGroups);
        }

        private Set<String> valuesOf(String part)
        {
            return attributes.computeIfAbsent(part, p -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER));
        }

    }
}
