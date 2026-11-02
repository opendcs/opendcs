# TsGroup model redesign

Status: step 1 (see 6) is implemented, the rest is still a proposal. The model types are in
`java/opendcs/src/main/java/org/opendcs/database/model/tsgroup/`, with unit tests beside them.

One change from this document as first written: the definition record is `GroupDefinition`, not `TsGroup`.
`decodes.tsdb.TsGroup` outlives this design by several steps, and a name that has to be qualified at every call
site that touches both is a tax nobody should pay. `GroupDefinition` also says what it is, and sits with
`GroupCriteria`, `GroupCombine` and `SubGroupLink`.

## 1. The problem

`decodes.tsdb.TsGroup` is four different things sharing one object and one lifecycle:

| Concern | Fields | Who actually needs it |
|---|---|---|
| **Reference** | `groupId`, `groupName`, `groupType`, `description` | group pickers, computation group dropdowns, sub-group slots, `/tsgrouprefs`, `GroupHelper.findParentsOf` |
| **Definition** | `tsMemberList`, `siteIdList`, `dataTypeIdList`, `otherMembers`, include/exclude/intersect lists | group editor, XML import/export, `writeTsGroup`/`save`, `/tsgroup` GET+POST |
| **Evaluation cache** | `expandedList`, `_isExpanded` | `GroupHelper`, `CpCompDependsUpdater`, `ComputationApp`, `CompDependsDAO`, `GroupAdder`, `/expandgroup` |
| **XML scratchpad** | `tsMemberIDList`, `siteNameList` | only `CompXio` (write) and `ImportComp.lookupObject` (read) |

Because they are fused, every read is an all-of-the-above read, and every consumer pays for it:

- `getTsGroupById` is deep **and** recursive **and** resolves a `TimeSeriesIdentifier` graph per member.
  `CpCompDependsUpdater` reads it at `CpCompDependsUpdater.java:448`, `:785`, `:921`, `:967` and then uses
  nothing but `getExpandedList()`. Same for `ComputationApp.java:877-885`, `CompDependsDAO.java:143-173`,
  `GroupAdder.java:77-91` and `/expandgroup` (`TimeSeriesResources.java:997-1002`).
- `getTsGroupList` is documented as shallow (`TsGroupDAI.java:52-53`) but returns live objects from a static
  cache that `fillCache()` fills deep (`TsGroupDAO.java:438-576`). `/tsgrouprefs`, which renders four scalar
  fields per group (`TimeSeriesResources.java:721-741`), can therefore trigger a full read of every member of
  every group.
- Sub-groups are held as `TsGroup` objects, so asking for a definition means materializing the whole subtree,
  which is what forces three independent cycle guards (`TsGroupDAO.java:55` `loopGuard`,
  `GroupHelper.java:137-142`, `TsGroupDaoImpl.java:589-615`) that do not agree with each other.
- Expansion state lives on a cached, shared, mutable object. `expandTsGroup` clears and refills
  `expandedList` in place (`GroupHelper.java:97-105`), `CpCompDependsUpdater.java:568-581` removes elements
  out of the live lists of a cache entry, and `_isExpanded` is silently reset by `clear()` (`TsGroup.java:132`)
  when the 20 minute cache expires mid-iteration — which is why four call sites carry a
  "may have timed out in the cache and reread from the db" comment.
- Two fields that exist only for XML (`tsMemberIDList`, `siteNameList`) leak into the persistent model and
  produce a live bug: `TimeSeriesResources.map(TsGroup):920-926` zips `siteNameList` with `siteIdList`
  index-by-index, but no DB read path ever populates `siteNameList`, so `GET /tsgroup` returns
  `groupSites: []` for any group whose definition did not come from a warm REST-built cache entry.

Dead surface that the split also retires: `refilter(TimeSeriesDb)` (`TsGroup.java:466-551`, zero callers, and a
`continue` on `:543` that lands in the wrong loop), `isTransient`/`setTransient` (zero callers),
`hasSubgroup`, `isInExpandedList`, `getIntervalCodeList`/`getParamTypeList`/`getDurationList`/`getVersionList`,
`rmOtherMember`, `rmFromExpandedList`, and `copy(String)` whose only caller (`DTOMappers.java:386`) calls it on
a freshly constructed empty group to clone eleven empty lists.

## 2. The model

New package `org.opendcs.database.model.tsgroup` (module `:opendcs`, next to `model.mappers`).
Values are records; nothing carries a "did I load it yet" flag.

### 2.1 Reference

```java
/**
 * Everything about a group that is known without reading its members: what a sub-group
 * slot, a picker or a list view needs. Matches ApiTsGroupRef field for field.
 */
public record TsGroupRef(DbKey id, String name, String type, String description)
{
    public static final TsGroupRef UNSAVED = new TsGroupRef(Constants.undefinedId, "", "", "");

    public TsGroupRef
    {
        Objects.requireNonNull(id, "A group reference always has an id, use TsGroupRef.unsaved(name).");
        name = name == null ? "" : name;
    }

    public static TsGroupRef unsaved(String name, String type)
    {
        return new TsGroupRef(Constants.undefinedId, name, type, "");
    }

    public boolean isSaved()
    {
        return !DbKey.isNull(id);
    }
}
```

One query, no joins, no recursion. `id`/`name`/`type` also feed the ancestor and subtree scans, which need
nothing else (`GroupHelper.java:227-266` uses only `getGroupId()` and the three sub-group id lists).

### 2.2 Sub-groups are references, not subtrees

```java
public enum GroupCombine
{
    ADD('A'),
    SUBTRACT('S'),
    INTERSECT('I');

    private final char value;

    GroupCombine(char value)
    {
        this.value = value;
    }

    public char value()
    {
        return value;
    }

    public static GroupCombine fromValue(char value)
    {
        return switch (Character.toUpperCase(value))
        {
            case 'S', 'F' -> SUBTRACT;  // 'F' is the pre-VERSION_6 spelling of subtract
            case 'I' -> INTERSECT;
            default -> ADD;             // null/blank columns read as add
        };
    }
}

/** One row of tsdb_group_member_group: this group pulls in that group, with this operator. */
public record SubGroupLink(TsGroupRef group, GroupCombine combine)
{
}
```

The definition graph is exactly one level deep, so **a cycle cannot be represented**, and reading a definition
never touches the subtree. Recursive membership becomes the evaluator's problem, where it is a set of group ids,
not an object graph.

### 2.3 Criteria

```java
/**
 * The tsdb_group_member_other rows, keyed by TSID part name. Keys are matched
 * case-insensitively because the column has no enumeratable domain: HDB stores
 * "ObjectType", and any part of the database's ts id format may appear here.
 */
public record GroupAttributes(Map<String, Set<String>> byPart)
{
    public GroupAttributes
    {
        byPart = Collections.unmodifiableMap(new TreeMap<>(String.CASE_INSENSITIVE_ORDER)
                {{ putAll(byPart == null ? Map.of() : byPart); }});
    }
    ...
}
```

The attribute container keeps **string** keys (`TsGroupMemberType` covers the common cases but HDB's
`"ObjectType"` is not in it, and `CwmsGroupHelper.java:61-204` reads nine names including ones the enum
documents as not being stored there). Typed accessors sit on top:

```java
    public Set<String> values(String tsIdPart)                 // never null
    public Set<String> values(TsGroupMemberType tsIdPart)
    public Set<String> parts()                                 // for HdbGroupHelper's raw scan
    public boolean isEmpty()
```

```java
/**
 * What makes a time series a member of a group, as stored. Explicit series are held by
 * key; resolving them to TimeSeriesIdentifier objects is a separate, opt-in read.
 */
public record GroupCriteria(List<DbKey> timeSeriesKeys, List<DbKey> siteIds, List<DbKey> dataTypeIds,
                            GroupAttributes attributes)
{
    public static final GroupCriteria EMPTY = new GroupCriteria(List.of(), List.of(), List.of(),
                                                                GroupAttributes.EMPTY);
    /** Empty criteria match nothing: both engines finish with "matches > 0". */
    public boolean matchesNothing()
    {
        return siteIds.isEmpty() && dataTypeIds.isEmpty() && attributes.isEmpty();
    }
}
```

`matchesNothing()` promotes a rule that is currently implicit in two places
(`HdbGroupHelper.java:145`, `CwmsGroupHelper.java:529`) into the model, where the editor can state it in the UI
instead of silently returning an empty group.

### 2.4 Definition

```java
/**
 * The complete, persistable definition of a time series group: its members by key, its
 * criteria and its sub-group links. Carries no evaluation state, so a definition read is
 * always one level deep and never resolves a time series.
 */
public record GroupDefinition(TsGroupRef ref, GroupCriteria criteria, List<SubGroupLink> subGroups)
{
    public static Builder builder(String name, String type);
    public static Builder builder(TsGroupRef ref);
    public static GroupDefinition ofLegacy(TsGroup legacy);        // decodes.tsdb.TsGroup

    public DbKey id();
    public String name();
    public String type();
    public String description();
    public boolean isSaved();

    public List<TsGroupRef> subGroups(GroupCombine combine);
    public boolean linksSubGroup(DbKey groupId);

    public Builder edit();
    public TsGroup toLegacy();
    public TsGroup toLegacy(Collection<? extends TimeSeriesIdentifier> explicitMembers);

    /** Mutable half of the value, for the editor, the XML parser and the REST body. */
    public static final class Builder { ... }
}
```

The constructor does the normalizing: a null criteria becomes `GroupCriteria.EMPTY`, null sub-group links are
dropped, and linking the same sub-group twice is refused with a message naming the group, because the member
table's primary key would only say that a row already exists.

### 2.5 Builder

Four things build groups today - the database, the React editor's POST body, the XML parser
(`CompXio.java:459-540`) and code (`Convert2Group.java:354-388`) - and all four today do it with eleven public
mutators on a half-built object that a caller must not look at until it is finished. The builder is the same
construction, sequenced and validated:

```java
GroupDefinition draft = GroupDefinition.builder("Alder Springs Flow", "comp")
                                       .description("all rev flow series above the dam")
                                       .timeSeries(TS_ID1, TS_ID2)
                                       .sites(SITE)
                                       .dataTypes(DT_ID1, DT_ID2)
                       .attribute("Interval", "Hour", "Day")
                       .attribute(TsGroupMemberType.Version, "rev")
                       .include(childRef)
                       .exclude(excludedRef)
                       .intersect(otherRef)
                       .build();

// editing: the editor round-trips through this instead of 20 setters
GroupDefinition updated = draft.edit()
                               .description("all rev flow series")
                               .clearTimeSeries()
                               .include(addedRef)
                               .build();
```

`build()` rejects a blank name or type - today that check lives in `TsGroupDaoImpl.save` (`:303-310`), so an
editor can construct an invalid group and only find out on save. `edit()` replaces `copy(String)`, `clear()`
and the `setIntersectedGroups(new ArrayList<>())` workaround at `TsGroupDaoImpl.java:275-276`. Adding the same
sub-group twice sets its operator rather than linking it twice: two links are the same group when both are saved
and share an id, or when the names match and either side is a name-only stub (`TsGroupRef.isSameGroup`), which is
the case the XML import needs because it has nothing but names until `ImportComp` resolves them.

`Builder` is a plain mutable holder that produces a record; there is no partially-built definition in
circulation, and nothing downstream has to guess whether a list it was handed is the internal one
(`decodes.tsdb.TsGroup` returns its live `ArrayList`s today, which is exactly what
`CpCompDependsUpdater.java:568-581` mutates).

### 2.6 Members and expansion are separate results

These two land with the code that can produce them - the `members` read in step 2 and the evaluator in step 4 -
rather than as types nothing can build yet:


```java
/** Explicit members of one group, resolved to identifiers. Its own read, because it is the expensive one. */
public record GroupMembers(List<TimeSeriesIdentifier> timeSeries, List<DbKey> unresolved)
{
    /** Members recorded in tsdb_group_member_ts that have no time_series_identifier row. */
    public boolean hasUnresolved() { return !unresolved.isEmpty(); }
}
```

```java
/**
 * The result of evaluating a group definition, never state on the definition itself.
 * Immutable, so it can be cached and shared without a caller mutating a group that
 * someone else is reading.
 */
public record GroupExpansion(TsGroupRef group, List<TimeSeriesIdentifier> timeSeries,
                             Set<DbKey> groupsEvaluated, Instant evaluatedAt)
{
    public List<DbKey> keys() { ... }
    /** Cheap fingerprint of what was evaluated, so a cache can tell a stale expansion from a live one. */
    public Object fingerprint() { ... }
}
```

`_isExpanded` disappears. The four "may have timed out in the cache" guards
(`ComputationApp.java:881`, `CompDependsDAO.java:144`, `CpCompDependsUpdater.java:556`, `:842`,
`GroupAdder.java:80`) become "do I have an expansion for this fingerprint, yes or no".

## 3. DAO: ask for the slice

The DAO stops offering one deep read and a shallow list. Methods are named after what they return, so
"how much did this cost" is visible at the call site (a fetch-plan enum was rejected: it either returns a
half-populated object or forces every caller to switch on the plan).

```java
public interface TsGroupDao extends OpenDcsDao
{
    // reference: one statement, no members, no recursion
    Optional<TsGroupRef> ref(DataTransaction tx, DbKey id) throws OpenDcsDataException;
    Optional<TsGroupRef> refByName(DataTransaction tx, String name) throws OpenDcsDataException;
    List<TsGroupRef> refs(DataTransaction tx, String groupType, int limit, int offset) throws OpenDcsDataException;

    // definition: six flat statements, exactly one level deep, no time series resolved
    Optional<GroupDefinition> definition(DataTransaction tx, DbKey id) throws OpenDcsDataException;
    Optional<GroupDefinition> definitionByName(DataTransaction tx, String name) throws OpenDcsDataException;
    List<GroupDefinition> definitions(DataTransaction tx, String groupType, int limit, int offset)
            throws OpenDcsDataException;
    Map<DbKey, GroupDefinition> definitionsInHierarchy(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    // the expensive part, on demand
    GroupMembers members(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    // group ids only, for hierarchy questions
    Set<DbKey> descendants(DataTransaction tx, DbKey id) throws OpenDcsDataException;
    Set<DbKey> ancestors(DataTransaction tx, DbKey id) throws OpenDcsDataException;

    GroupDefinition save(DataTransaction tx, GroupDefinition definition) throws OpenDcsDataException;
    void delete(DataTransaction tx, DbKey id) throws OpenDcsDataException;
    int countCompsUsing(DataTransaction tx, DbKey id) throws OpenDcsDataException;
    void removeDependenciesFor(DataTransaction tx, DbKey deletedGroupId) throws OpenDcsDataException;
}
```

Every read starts the same way: a CTE that says which groups the read applies to - one group, a type with a page
size, or a whole hierarchy walked by `UNION` - followed by one statement per member table matched against that
set. So the number of statements does not depend on how many groups are involved: `definitionsInHierarchy` costs
six statements whether the hierarchy has one group in it or forty. A batch `members(ids)` form is left to step 4,
where the access pattern is known; the two `definitionsById`-style batch forms were dropped rather than guessed at.

`save` takes an immutable definition and returns the saved one with its key. Nothing is mutated in place, so a
caller can hold on to its own object safely; the current `save` has to keep the caller's instance alive only so
`loadMembers` can refill it (`TsGroupDaoImpl.java:281-282`).

## 4. Operations: expansion as a function

Expansion is not CRUD, so it does not belong on the DAO. It goes in a new
`org.opendcs.operations.tsgroup.TsGroupOperations extends OpenDcsOperations`, resolved with
`operationsFromLookup` and implemented per engine, the way `TimeSeriesOperations` is today:

```java
public interface TsGroupOperations extends OpenDcsOperations
{
    /** Expands a definition that may never have been saved. */
    GroupExpansion expand(DataTransaction tx, GroupDefinition definition) throws OpenDcsDataException;

    /** One pass over the time series universe for every definition, instead of one pass per group. */
    Map<DbKey, GroupExpansion> expandAll(DataTransaction tx, Collection<GroupDefinition> definitions)
            throws OpenDcsDataException;

    /** Which of these groups does this series belong to, in one pass. Replaces checkGroupMembership. */
    Set<DbKey> groupsContaining(DataTransaction tx, TimeSeriesIdentifier tsid,
                                Collection<GroupDefinition> definitions) throws OpenDcsDataException;

    /** Engine-specific wildcard/criteria matching, as a value rather than as state on the evaluator. */
    CompiledCriteria compile(GroupDefinition definition);
}
```

Three workflow consequences:

- **`expand(tx, definition)` takes a definition, not a stored id.** The React editor currently cannot evaluate
  unsaved edits and says so (`EvaluateGroupModal.tsx:24-25`, `tsGroups.ts:61-69` disables the query unless
  `groupId > 0`), because `/expandgroup` only accepts a `groupid` and re-reads the stored row
  (`TimeSeriesResources.java:979-1021`). POSTing a draft to an evaluate endpoint becomes possible.
- **`CompiledCriteria` removes hidden evaluator state.** `CwmsGroupHelper` keeps nine `ArrayList<Pattern>`
  fields as instance state, cleared on every `prepareForExpand` (`:44-52`, `:66-189`), and `TimeSeriesDb
  .expandTsGroup` (`:1561`) mints a throwaway helper per call partly to dodge it. Compiled criteria returned as a
  value can be built once and reused across a batch.
- **`expandAll` inverts the hot loop.** Today `GroupHelper.doExpandTsGroup:154-167` scans the whole TSID cache per
  group, so `evalAll` costs |groups| x |time series| (that is the cost `CpCompDependsUpdater.refreshCaches:335-338`
  pays at startup). Because `GroupCriteria` is declarative, the site / data type / interval / duration / version
  filters can be pushed into SQL instead of being evaluated in Java per candidate:

  ```sql
  select tsi.id
    from time_series_identifier tsi
   where (coalesce(:siteIds, true)  is null or tsi.site_id = any(:siteIds))
     and (coalesce(:dataTypeIds, true) is null or tsi.datatype_id = any(:dataTypeIds))
     and (coalesce(:intervals, true)  is null or tsi.interval  = any(:intervals))
     and (coalesce(:durations, true)  is null or tsi.duration  = any(:durations))
     and (coalesce(:versions, true)   is null or tsi.ts_version = any(:versions))
  ```
  with the wildcard (`*`, base/sub) matching that only CWMS uses staying in Java, applied to the reduced set.
  Whether the pushdown is a `where` clause or a Java filter is then a per-engine choice behind `compile()`, not
  baked into the model.

Sub-group set algebra stays exactly where it is conceptually today - in the evaluator - but over ids:
`descendants(groupId)` (SQL closure, cycle-safe by `UNION`) then `definitionsById(...)` (bulk), then
add / remove / retain on id sets. The three disagreeing cycle guards collapse into the one that already runs
before the object graph is built (`TsGroupDaoImpl.java:589-615`).

## 5. What each caller ends up reading

| Caller | Today | With the split |
|---|---|---|
| `GET /tsgrouprefs` (`TimeSeriesResources:708`) | `getTsGroupList(null)`; cold cache = `fillCache()`: 6 bulk queries plus a TSID lookup per member of every group | `refs(tx, null, -1, -1)`: 1 query |
| `GET /tsgroup` (`:869`) | deep + recursive read of the subtree, full TSID graph per member | `definition(...)` (5 flat queries) + `members(...)` (1 query); sub-groups stay refs |
| `POST /tsgroup` (`:1106`) | 11 mutators into a mutable `TsGroup`, then a deep re-read | `Builder.build()` + `save(...)` |
| `GET /expandgroup` (`:979`) | deep object graph, then a full TSID cache scan | `descendants` + `definitionsById` + `members`, or pure SQL pushdown; also usable on unsaved drafts |
| `ComputationApp.expandForGroup` (`:867`) | deep read + expand, discards criteria | `expand(tx, definition)`; or cached `GroupExpansion` |
| `CpCompDependsUpdater.refreshCaches` (`:306`) | `fillCache()` + `evalAll()`: \|groups\| x \|TSIDs\| | `definitions(...)` + `expandAll(...)`: one pass |
| `CpCompDependsUpdater.tsCreated` (`:417`) | `checkGroupMembership` walks all groups deep | `groupsContaining(tsid, definitions)` |
| `CpCompDependsUpdater.tsDeleted` (`:532`) | mutates live lists of cached groups | `members(...)` for the affected groups, then `save` |
| `findParentsOf` / `evaluateParents` (`GroupHelper:227`) | `getTsGroupList(null)` per recursion level, deep | `ancestors(tx, id)`: 1 recursive query |
| `ExportGroup` / `ExportComp` / `CompXio.writeTsGrp` | deep reads, uses names + sub-group names only | `refs` + `definitions` (sub-groups are already refs) |
| `ImportComp.lookupObject` (`:430`) | reads the XML-only string lists off the same object | `GroupDefinition.Builder` with name-based `pendingSite` / `pendingDataType`, resolved by a `Resolver` step |
| `TsGroupDisplay` (`:67`) | deep read, then expand, then per-site lookups | `definition` + `members` + `expand` |

## 6. Migration

Each step is independently shippable and each is roughly a PR:

1. **[done]** `org.opendcs.database.model.tsgroup`: `TsGroupRef`, `GroupCombine`, `SubGroupLink`,
   `GroupAttributes`, `GroupCriteria`, `GroupDefinition` with its `Builder` and the `toLegacy`/`ofLegacy`
   adapters, plus 38 unit tests. No behavior change, nothing wired: nothing outside the package refers to it yet.
2. **[done]** `TsGroupDao` and `TsGroupDaoImpl` now speak only the new types: `ref(s)`, `refByName`,
   `definition(s)`, `definitionByName`, `definitionsInHierarchy`, `members`, `descendants`, `ancestors`,
   `save(GroupDefinition)`. `GroupMembers` joined the model package. The deep `getById`/`getByName`/`loadMembers`
   are gone rather than deprecated, because nothing outside the DAO ever called them. Measured against a real
   OpenDCS-Postgres schema: `refs` 1 statement, `definition` 6, `members` 1, `descendants` 1, and
   `definitionsInHierarchy` over a four-group tree 6 - where the old deep read cost five statements per group
   plus a time series lookup per member. Still owed: `TsGroupDaoTestIT`, and the Oracle SQL has been rendered and
   reviewed but not executed.

   Cycle handling that `loadMembers` used to do in Java is gone with it; `UNION` already terminates the walk, and
   dropping a cyclic link back is a decision about membership, so it belongs with the evaluator in step 4.
3. REST onto the new types: `refs` for `/tsgrouprefs`, `definition`+`members` for `GET /tsgroup`,
   `Builder`+`save` for `POST`, and an evaluate endpoint that accepts a posted definition. This step alone fixes
   the `groupSites: []` bug and the editor's evaluate-after-save limitation.
4. `TsGroupOperations` with the OpenDCS (Postgres/Oracle/OPENTSDB) implementation; CWMS and HDB implementations
   move the `passesParts` logic from `CwmsGroupHelper` / `HdbGroupHelper`.
5. Move `ComputationApp`, `CompDependsDAO`, `CpCompDependsUpdater`, `GroupAdder`, `ExportTimeSeries` to
   `expand`/`expandAll`/`groupsContaining`; drop the `getIsExpanded()` guards.
6. Move XML import/export to the builder and the definition type, then retire `opendcs.dao.TsGroupDAO`,
   `opendcs.dai.TsGroupDAI`, `GroupHelper` and finally `decodes.tsdb.TsGroup` (its `CompMetaData` role is one
   `getObjectType()` call in a log line at `ImportComp.java:145`).

Steps 1-3 are self-contained and already pay for the two live bugs and the shallow-list cost. 4-6 are where the
operational win (one-pass expansion, evaluate-before-save, no cache mutation) lands.

## 7. Open questions

- **Caching.** `TsGroupDaoImpl` deliberately does none. With the split, `refs` and `definitions` are cheap enough
  to re-read, and `GroupExpansion` has a fingerprint, so a small expansion cache is a separate, opt-in decision
  rather than a static 20-minute cache that every read falls into.
- **`DbKey` identity.** Nothing in the new types uses `==` on keys; `TsGroup.java:417,430`,
  `GroupHelper.java:244,251,258` and `CpCompDependsUpdater.java:566,579` do, relying on `DbKey` interning that is
  capped at 200000 entries with eviction (`DbKey.java:69-71,116`) - in the long-running daemons those call sites
  live. Worth a standalone fix regardless of this redesign.
- **HDB/CWMS parity.** `GroupCriteria` covers both engines' inputs, but HDB's `"ObjectType"` attribute and CWMS's
  base/sub wildcard grammar stay engine-specific behind `compile()`. Confirm no third vocabulary exists in the
  wild before freezing `GroupAttributes` accessors.
