---------------------------------------------------------------------------
--
-- Sutron Ilex CCP Database on ORACLE
-- Database Version: 8        Date: 2013/05/02
-- Company: Sutron Corporation
--  Writer: GC
--
---------------------------------------------------------------------------
---------------------------------------------------------------------------
-- This script creates the tables for DECODES and Time-Series based on
-- Version 8 of PostGres Database DDL.
---------------------------------------------------------------------------
set echo on
spool create_CCPDB_Objects.out

whenever sqlerror continue
set define on
@@defines.sql

define sys_schema     = &1
define sys_passwd     = &2
define tns_name       = &3

define dflt_office_code = sys_context('CCPENV','CCP_OFFICE_CODE');

connect &sys_schema/&sys_passwd@&tns_name as sysdba;

alter session set current_schema = &ccp_schema;

---------------------------------------------------------------------------
-- Database version
---------------------------------------------------------------------------
-- The current decodesdatabaseversion table has only one single record.
-- If the decodesdatabaseversion table exists, it is the DB version 7
-- or above; if the databaseversion table exists, it must be version 6;
-- if neither exists, this must be version 5. Some sql code acts
-- differently depending on the database version.
---------------------------------------------------------------------------
create table decodesdatabaseversion
(
    version number not null,
    options varchar(400)
) &ts_name;

insert into decodesdatabaseversion values(8, null);

---------------------------------------------------------------------------
-- Sites, Site Names, and Site Properties
---------------------------------------------------------------------------
create table site
(
    id integer not null,
    latitude varchar(24),
    longitude varchar(24),
    nearestcity varchar(64),
    state varchar(24),
    region varchar(64),
    timezone varchar(64),
    country varchar(64),
    elevation float,
    elevunitabbr varchar(24),
    description varchar(800)
) &ts_name;

-- Guarantees no two sites have the same id:
create unique index site_ididx on site (id) &ts_name;

create table sitename
(
    siteid integer not null,
    nametype varchar(24) not null,
    sitename varchar(64) not null,
    dbnum varchar(2),
    agency_cd varchar(5)
) &ts_name;

-- Guarantees at most one site name of a given time per site.
create unique index sitename_idtypeidx
    on sitename (siteid, nametype, dbnum, agency_cd) &ts_name;

-- Site properties are new for DECODES DB version 8
create table site_property
(
    site_id integer not null,
    prop_name varchar(24) not null,
    prop_value varchar(240) not null
) &ts_name;

-- Guarantees property names are unique within a site
create unique index site_property_idnameidx
    on site_property (site_id, prop_name) &ts_name;

---------------------------------------------------------------------------
-- Equipmentmodel and Equipmentproperty
---------------------------------------------------------------------------
create table equipmentmodel
(
    id integer not null,
    name varchar(24) not null,
    company varchar(64),
    model varchar(64),
    description varchar(400),
    equipmenttype varchar(24),
    db_office_code integer default &dflt_office_code
) &ts_name;

-- Guarantees no two equipmentmodels have the same id:
create unique index equipmentmodel_ididx
    on equipmentmodel (id) &ts_name;

-- Guarantees no two equipmentmodels have the same name:
create unique index equipmentmodel_nmidx
    on equipmentmodel (name, db_office_code) &ts_name;

create table equipmentproperty
(
    equipmentid integer not null,
    name varchar(24) not null,
    value varchar(240) not null
) &ts_name;

-- Guarantees property names are unique within an equipmentmodel:
create unique index equipmentproperty_idnameidx
    on equipmentproperty (equipmentid, name) &ts_name;

---------------------------------------------------------------------------
-- Enumeration and Enumvalue
---------------------------------------------------------------------------
create table enum
(
    id integer not null,
    name varchar(24) not null,
    defaultvalue varchar(24),
    db_office_code integer default &dflt_office_code
) &ts_name;

-- Guarantees no two enums have the same id:
create unique index enumididx on enum(id) &ts_name;

-- Guarantees no two enums have the same name:
create unique index enums_nmidx
    on enum (name, db_office_code) &ts_name;

create table enumvalue
(
    enumid integer not null,
    enumvalue varchar(24) not null,
    description varchar(400),
    execclass varchar(160),
    editclass varchar(160),
    sortnumber integer
) &ts_name;

-- Guarantees each enumvalue is unique within an enum.
create unique index enumvalueidx
    on enumvalue(enumid, enumvalue) &ts_name;

---------------------------------------------------------------------------
-- Datatype and Equivalence
---------------------------------------------------------------------------
create table datatype
(
    id integer not null,
    standard varchar(24) not null,
    code varchar(24) not null,
    db_office_code integer default &dflt_office_code
) &ts_name;

-- Guarantees no two datatypes have the same id:
create unique index datatype_ididx on datatype (id) &ts_name;

-- Guarantees no two datatype have the same standard and code:
create unique index datatype_nmidx
    on datatype (standard, code, db_office_code) &ts_name;

-- An entry in the datatypeequivalence table says that the two
-- data types represent the same type of data, but in different standards.
-- for example epa 00063 is equivalent to shef hg
create table datatypeequivalence
(
    id0 integer not null,
    id1 integer not null
) &ts_name;

-- guarantees that each equivalence assertion is unique.
create unique index datatypeequivalence_id1idx
    on datatypeequivalence (id0, id1) &ts_name;

---------------------------------------------------------------------------
-- Platform, Platformproperty, Platformsensor, Platformsensorproperty,
-- Nwissitedbreference, and Transportmedium
---------------------------------------------------------------------------
create table platform
(
    id integer not null,
    agency varchar(64),
    isproduction varchar(5) default 'false',
    siteid integer,
    configid integer,
    description varchar(400),
    lastmodifytime date,
    expiration date,
    platformdesignator varchar(24)
) &ts_name;

create table platformproperty
(
    platformid integer not null,
    name varchar(24) not null,
    value varchar(240) not null
) &ts_name;

-- Guarantees no two platforms have the same id:
create unique index platform_ididx on platform (id) &ts_name;

create table platformsensor
(
    platformid integer not null,
    sensornumber integer not null,
    siteid integer,
    dd_nu integer
) &ts_name;

create table platformsensorproperty
(
    platformid integer not null,
    sensornumber integer not null,
    name varchar(24) not null,
    value varchar(240) not null
) &ts_name;

--
-- mjm 3/18/2008 - this doesn't appear to be used anywhere!!
-- nwis will need this extra table:
--
create table nwissitedbreference
(
    siteid integer not null,
    dbnum varchar(2)
) &ts_name;

create table transportmedium
(
    platformid integer not null,
    mediumtype varchar(24) not null,
    mediumid varchar(64),   -- holds dcp address or other identifier
    scriptname varchar(64), -- soft link to script in this platform's config.
    channelnum integer,
    assignedtime integer,
    transmitwindow integer,
    transmitinterval integer,
    equipmentid integer,
    timeadjustment integer,
    preamble char,
    timezone varchar(64),
    db_office_code integer default &dflt_office_code
) &ts_name;

-- Guarantees no two transportmedia have same type and id.
create unique index transportmediumidx
    on transportmedium(mediumtype,mediumid,db_office_code) &ts_name;

---------------------------------------------------------------------------
-- Platform Configurations & Subordinate Entities
---------------------------------------------------------------------------
create table platformconfig
(
    id integer not null,
    name varchar(64) not null,
    description varchar(400),
    equipmentid integer,
    db_office_code integer default &dflt_office_code
) &ts_name;

-- guarantees no two platformconfigs have the same id:
create unique index platformconfigididx
    on platformconfig(id) &ts_name;

-- guarantees no two platformconfigs have the same name:
create unique index platformconfignameidx
    on platformconfig(name, db_office_code) &ts_name;

create table configsensor
(
    configid integer not null,
    sensornumber integer not null,
    sensorname varchar(64),
    recordingmode char,
    recordinginterval integer,     -- # seconds
    timeoffirstsample integer,     -- second of day
    equipmentid integer,
    absolutemin float,
    absolutemax float,
    stat_cd varchar(5)
) &ts_name;

-- this relation associates a data type with a sensor.
-- a sensor may have mulptiple data types, but only one for each standard.
create table configsensordatatype
(
    configid integer not null,
    sensornumber integer not null,
    datatypeid integer not null
) &ts_name;

create table configsensorproperty
(
    configid integer not null,
    sensornumber integer not null,
    name varchar(24) not null,
    value varchar(240) not null
) &ts_name;

---------------------------------------------------------------------------
-- Decoding Scripts & Subordinate Entities
---------------------------------------------------------------------------
create table decodesscript
(
    id integer not null,
    configid integer not null,
    name varchar(64) not null,
    type varchar(24) not null,
    dataorder char         -- a=ascending d=descending
) &ts_name;

-- guarantees no two decodesscripts have the same id:
create unique index decodesscriptidx
    on decodesscript(id) &ts_name;

-- guarantees script names are unique within a platformconfig:
create unique index decodesscriptnmidx
    on decodesscript(configid, name) &ts_name;

create table formatstatement
(
    decodesscriptid integer not null,
    sequencenum integer not null,
    label varchar(24) not null,
    format varchar(400)
) &ts_name;

-- guarantees each format statement has a unique sequence within a script:
create unique index formatstatementidx on
    formatstatement(decodesscriptid, sequencenum);

create table scriptsensor
(
    decodesscriptid integer not null,
    sensornumber integer not null,
    unitconverterid integer
) &ts_name;

-- guarantees each scriptsensor has unique number within a script:
create unique index scriptsensoridx
    on scriptsensor(decodesscriptid, sensornumber) &ts_name;

---------------------------------------------------------------------------
-- Routing Specs
---------------------------------------------------------------------------
create table routingspec
(
    id integer not null,
    name varchar(64) not null,
    datasourceid integer,
    enableequations varchar(5) default 'false',
    useperformancemeasurements varchar(5) default 'false',
    outputformat varchar(24),
    outputtimezone varchar(64),
    presentationgroupname varchar(64),
    sincetime varchar(80),
    untiltime varchar(80),
    consumertype varchar(24),
    consumerarg varchar(400),
    lastmodifytime date,
    isproduction varchar(5) default 'false',
    db_office_code integer default &dflt_office_code
) &ts_name;

-- guarantees no two routingspecs have the same id:
create unique index routingspecididx
    on routingspec(id) &ts_name;

-- guarantees no two routingspecs have the same name:
create unique index routingspecnmidx
    on routingspec(name, db_office_code) &ts_name;

-- associates a routing spec to a network list:
create table routingspecnetworklist
(
    routingspecid integer not null,
    networklistname varchar(64) not null
) &ts_name;

create table routingspecproperty
(
    routingspecid integer not null,
    name varchar(24) not null,
    value varchar(240) not null
) &ts_name;

---------------------------------------------------------------------------
-- Data Sources
---------------------------------------------------------------------------
create table datasource
(
    id integer not null,
    name varchar(64) not null,
    datasourcetype varchar(24) not null,
    datasourcearg varchar(400),
    db_office_code integer default &dflt_office_code
) &ts_name;

-- guarantees no two datasources have the same id:
create unique index datasource_ididx
    on datasource(id) &ts_name;

-- guarantees no two datasources have the same name:
create unique index datasource_nmidx
    on datasource(name, db_office_code) &ts_name;

create table datasourcegroupmember
(
    groupid integer not null,
    sequencenum integer not null,
    memberid integer not null
) &ts_name;

---------------------------------------------------------------------------
-- Network Lists
---------------------------------------------------------------------------
create table networklist
(
    id integer not null,
    name varchar(64) not null,
    transportmediumtype varchar(24),
    sitenametypepreference varchar(24),
    lastmodifytime date not null,
    db_office_code integer default &dflt_office_code
) &ts_name;

-- guarantees no two networklists have the same id:
create unique index networklist_ididx
    on networklist(id) &ts_name;

-- guarantees no two networklists have the same name:
create unique index networklist_nmidx
    on networklist(name, db_office_code) &ts_name;

create table networklistentry
(
    networklistid integer not null,
    transportid varchar(64) not null
) &ts_name;

---------------------------------------------------------------------------
-- Presentation Groups
---------------------------------------------------------------------------
create table presentationgroup
(
    id integer not null,
    name varchar(64) not null,
    inheritsfrom integer,
    lastmodifytime date,
    isproduction varchar(5) default 'false',
    db_office_code integer default &dflt_office_code
) &ts_name;

-- guarantees no two presentationgroups have the same id:
create unique index presgrp_ididx
    on presentationgroup(id) &ts_name;

-- guarantees no two presentationgroups have the same name:
create unique index presgrp_nmidx
    on presentationgroup(name, db_office_code) &ts_name;

create table datapresentation
(
    id integer not null,
    groupid integer not null,
    datatypeid integer,
    unitabbr varchar(24),
    equipmentid integer,
    maxdecimals integer
) &ts_name;

create table roundingrule
(
    datapresentationid integer not null,
    upperlimit float,
    sigdigits integer not null
) &ts_name;

---------------------------------------------------------------------------
-- Engineering Units and Conversions
---------------------------------------------------------------------------
create table engineeringunit
(
    unitabbr varchar(24) not null,
    name varchar(64) not null,
    family varchar(24),
    measures varchar(24),
    db_office_code integer default &dflt_office_code
) &ts_name;

-- Guarantees no two engineeringunit have the same unitabbr:
create unique index engineeringunit_nmidx
    on engineeringunit (unitabbr, db_office_code) &ts_name;

create table unitconverter
(
    id integer not null,
    fromunitsabbr varchar(24),
    tounitsabbr varchar(24),
    algorithm varchar(24),
    -- meaning of coeffients depends on the algorithm:
    a float,
    b float,
    c float,
    d float,
    e float,
    f float,
    db_office_code integer default &dflt_office_code
) &ts_name;

-- guarantees no two unitconverters have the same id:
create unique index unitconverterididx
    on unitconverter(id) &ts_name;

-- Note: We DON'T put a unique index on from/to abbreviations
-- because raw converters all have "raw" as the from abbreviation.
-- Many different raw converters may have the same from/to values.


---------------------------------------------------------------------------
-- Tempest Time Serial Data Tables
---------------------------------------------------------------------------
create table tsdb_database_version
(
    version number not null,
    description varchar(400) not null
) &ts_name;

insert into tsdb_database_version (version,description) values (8, 'CWMS DATABASE with multiple offices');
commit;

---------------------------------------------------------------------------
-- properties on the database itself. this table allows for
-- easy addition of new options and features.
-- currently supported properties:
--    datatypestandardpref
--    maxdataage
--    sitenametypepref
---------------------------------------------------------------------------
create table tsdb_property
(
    prop_name varchar(24) not null,
    prop_value varchar(240) not null
) &ts_name;

--create unique index tsdb_property_nameidx
--    on tsdb_property(prop_name) &ts_name;

alter table tsdb_property add constraint tsdb_property_pk
    primary key (prop_name) using index &ts_name;

---------------------------------------------------------------------------
-- dcp_trans_day_map specifies the begin time (day) for each
-- daily dcp_trans_xx table.
---------------------------------------------------------------------------
create table dcp_trans_day_map
(
    table_suffix varchar(4) not null,  -- this is the suffix to the table name for transmit records.
    day_number integer                 -- day 0 = jan 1, 1970, null means this suffix not used
) &ts_name;

--create unique index dcp_trans_day_map_idx
--    on dcp_trans_day_map(table_suffix) &ts_name;

alter table dcp_trans_day_map add constraint dcp_trans_day_map_pk
    primary key (table_suffix) using index &ts_name;

---------------------------------------------------------------------------
-- any application that connects to the db and writes data is a
-- 'loading application'. and should have an entry in the
-- hdb_loading_application table.
---------------------------------------------------------------------------
create table hdb_loading_application
(
    loading_application_id integer not null,
    loading_application_name varchar(24) not null,
    manual_edit_app char(1) default 'n',
    cmmnt varchar(1000),
    db_office_code integer default &dflt_office_code
) &ts_name;

alter table hdb_loading_application add constraint hdb_loading_application_pk
    primary key (loading_application_id) using index &ts_name;

create unique index hdb_loading_application_idx
    on hdb_loading_application(loading_application_name, db_office_code) &ts_name;


---------------------------------------------------------------------------
-- Every loading app may have a set of properties.
---------------------------------------------------------------------------
create table ref_loading_application_prop
(
    loading_application_id integer not null,
    prop_name varchar(64) not null,
    prop_value varchar(240) not null
) &ts_name;

--create unique index ref_loading_application_idx
--    on ref_loading_application_prop(loading_application_id, prop_name) &ts_name;

alter table ref_loading_application_prop add constraint ref_loading_application_pro_pk
    primary key (loading_application_id, prop_name) using index &ts_name;

---------------------------------------------------------------------------
-- This table ensures that only one instance of a comp proc runs at a time
---------------------------------------------------------------------------
create table cp_comp_proc_lock
(
    loading_application_id integer not null,
    pid integer not null,
    host varchar(400) not null,
    heartbeat date not null,
    cur_status varchar(64)         -- brief current status for display.
) &ts_name;

--create unique index cp_comp_proc_lock_idx
--    on cp_comp_proc_lock (loading_application_id) &ts_name;

alter table cp_comp_proc_lock add constraint cp_comp_proc_lock_pk
    primary key (loading_application_id) using index &ts_name;

---------------------------------------------------------------------------
-- holds information from the daps platform description table
-- (pdt) this is downloaded periodically from noaa.
---------------------------------------------------------------------------
create table goes_pdt
(
    agency varchar(8),
    dcp_address integer not null,
    hex_dcp_address varchar(8) not null,
    st_channel integer,
    rd_channel integer,
    st_first_xmit_sod integer,
    st_xmit_interval integer,
    st_xmit_window integer,
    baud integer,
    data_format char,
    state_abbr varchar(2),
    location_code char,
    description varchar(48),
    latitude float,
    longitude float,
    location_type char,
    manufacturer varchar(24),
    model varchar(24),
    unknown_flag1 char,
    nmc_flag char,
    nmc_descriptor varchar(8),
    maintainer varchar(24),
    telnum1 varchar(24),
    telnum2 varchar(24),
    shefcodes varchar(64),
    lastmodified timestamp not null,
    num_failures integer,
    unknown_flag2 char
) &ts_name;

--create unique index goes_pdt_addridx
--    on goes_pdt(dcp_address) &ts_name;

alter table goes_pdt add constraint goes_pdt_pk
    primary key (dcp_address) using index &ts_name;

create table goes_cdt
(
    goes_channel integer not null,
    xmit_type char,
    baud integer
) &ts_name;

--create unique index goes_cdt_chanidx
--    on goes_cdt(goes_channel) &ts_name;

alter table goes_cdt add constraint goes_cdt_pk
    primary key (goes_channel) using index &ts_name;


---------------------------------------------------------------------------
-- This part defines the computation meta-data classes on database.
-- All table and attribute names are compatible with the USBR HDB,
-- so changed should be made with extreme caution!
---------------------------------------------------------------------------
-- Computation tasklist
create table cp_comp_tasklist
(
    record_num integer not null,
    loading_application_id integer not null,
    site_datatype_id integer not null,
    interval varchar(24),                      -- not req'd by some dbs
    table_selector varchar(80),                -- store "parmtype.duration.version"
    value float,                               -- not req'd for deleted data
    date_time_loaded date not null,
    start_date_time date not null,
    delete_flag varchar(1) default 'N',        -- 'N': not delete; 'Y': TS data deleted; 'U': TS code changed
    model_run_id integer default null,         -- will be null for real data
    flags integer not null,
    source_id integer,                         -- may be null
    fail_time date default null,               -- may be null
    quality_code integer,                      -- add this field for using cwms DB
    unit_id varchar(16),                       -- add this field for using cwms DB
    version_date date                          -- add this field for using cwms DB
) &ts_name;

--create unique index cp_comp_tasklist_idx
--    on cp_comp_tasklist (record_num) &ts_name;

alter table cp_comp_tasklist add constraint cp_comp_tasklist_pk
    primary key (record_num) using index &ts_name;

--create unique index cp_comp_tasklist_app_idx
--    on cp_comp_tasklist(record_num, loading_application_id) &ts_name;

create unique index cp_comp_tasklist_idx_app
    on cp_comp_tasklist(loading_application_id, record_num) &ts_name; 
    
    
---------------------------------------------------------------------------
-- Computation algorithm.
---------------------------------------------------------------------------
create table cp_algorithm
(
    algorithm_id integer not null,
    algorithm_name varchar(64) not null,
    exec_class varchar(160),
    cmmnt varchar(1000),
    db_office_code integer default &dflt_office_code
) &ts_name;

--create unique index cp_algorithm_id_idx
--    on cp_algorithm (algorithm_id) &ts_name;

alter table cp_algorithm add constraint cp_algorithm_pk
    primary key (algorithm_id) using index &ts_name;

create unique index cp_algorithm_idx
    on cp_algorithm (algorithm_name, db_office_code) &ts_name;


-- this table stores info about time-series params for an algorithm.
create table cp_algo_ts_parm
(
    algorithm_id integer not null,
    algo_role_name varchar(24) not null,
    parm_type varchar(24) not null
) &ts_name;

--create unique index cp_algo_ts_parm_idx on
--    cp_algo_ts_parm (algorithm_id, algo_role_name) &ts_name;

alter table cp_algo_ts_parm add constraint cp_algo_ts_parm_pk
    primary key (algorithm_id, algo_role_name) using index &ts_name;

--  foreign key algorithm_id for table cp_algo_ts_parm
alter table cp_algo_ts_parm add constraint cp_algo_ts_parm_fk
foreign key (algorithm_id) references cp_algorithm (algorithm_id)
on delete cascade;


-- this table stores additional named properties that apply to an algorithm.
create table cp_algo_property
(
    algorithm_id integer not null,
    prop_name varchar(48) not null,
    prop_value varchar(240) not null
) ;

--create unique index cp_algo_property_idx on
--    cp_algo_property (algorithm_id, prop_name) &ts_name;

alter table cp_algo_property add constraint cp_algo_property_pk
    primary key (algorithm_id, prop_name) using index &ts_name;

-- foreign key for table cp_algo_property for algorithm_id of table cp_algorithm
alter table cp_algo_property add constraint cp_algo_property_fk
foreign key (algorithm_id) references cp_algorithm (algorithm_id)
on delete cascade;


---------------------------------------------------------------------------
-- Computation data results
---------------------------------------------------------------------------
create table cp_computation
(
    computation_id integer not null,
    computation_name varchar(64) not null,
    algorithm_id integer,                -- must be assigned to execute.
    cmmnt varchar(1000),
    loading_application_id integer,      -- app to execute this comp.
    date_time_loaded date not null,
    enabled varchar(1) default 'n',
    effective_start_date_time date,
    effective_end_date_time date,
    group_id integer,
    db_office_code integer default &dflt_office_code
) &ts_name;

--create unique index cp_computation_id_idx
--    on cp_computation(computation_id) &ts_name;

alter table cp_computation
    add constraint cp_computation_ck check (enabled in ('Y','N'))
    add constraint cp_computation_pk primary key (computation_id)
    using index &ts_name;

create unique index cp_computation_idx
    on cp_computation(computation_name, db_office_code) &ts_name;

-- foreign key for table cp_computation of algorithm_id and loading_application_id
alter table cp_computation
    add constraint cp_computation_fk_a foreign key (algorithm_id)
        references cp_algorithm (algorithm_id)
    add constraint cp_computation_fk_b foreign key (loading_application_id)
        references hdb_loading_application (loading_application_id);


-- this table stores additional info about time-series params for a computation.
create table cp_comp_ts_parm
(
    computation_id integer not null,
    algo_role_name varchar(24) not null,
    site_datatype_id integer not null,
    interval varchar(24),
    table_selector varchar(80),            -- store "parmtype.duration.version" here
    delta_t integer default 0,
    model_id integer default null,         -- null for real data
    datatype_id integer default null,      -- for variable-site output params
    delta_t_units varchar(24) default null -- new feature
) &ts_name;

--create unique index cp_comp_ts_parm_idx on
--    cp_comp_ts_parm(computation_id, algo_role_name) &ts_name;

alter table cp_comp_ts_parm add constraint cp_comp_ts_parm_pk
    primary key (computation_id, algo_role_name) using index &ts_name;


-- this table stores additional named properties that apply to a computation.
create table cp_comp_property
(
    computation_id integer not null,
    prop_name varchar(48) not null,
    prop_value varchar(240) not null
) &ts_name;

--create unique index cp_comp_property_idx on
--    cp_comp_property (computation_id, prop_name) &ts_name;

alter table cp_comp_property add constraint cp_comp_property_pk
    primary key (computation_id, prop_name) using index &ts_name;

-- foreign key for table cp_comp_property of computation_id on table cp_computation
alter table cp_comp_property add constraint cp_comp_property_fk
foreign key (computation_id)
references cp_computation (computation_id) on delete cascade;


-- this table is an optimization used by trigger & java code to quickly
-- find computations that depend on a given data_id (sdi).
-- note: for usbr, this table will need to include the supplemental fields.
create table cp_comp_depends
(
    site_datatype_id integer not null,
    computation_id integer not null
-- for usbr add the following to uniquely specify a time series
--  interval varchar(24) not null,
--  table_selector varchar(24),
--  model_id integer default null
) &ts_name;

--create unique index cp_comp_depends_idx
--    on cp_comp_depends(site_datatype_id, computation_id) &ts_name;

alter table cp_comp_depends add constraint cp_comp_depends_pk
    primary key (site_datatype_id, computation_id) using index &ts_name;


---------------------------------------------------------------------------
-- Tables for Time Series Groups
---------------------------------------------------------------------------
create table tsdb_group
(
    group_id integer not null,
    group_name varchar(64) not null,
    group_type varchar(24) not null,
    group_description varchar(1000),
    db_office_code integer default &dflt_office_code
) &ts_name;

alter table tsdb_group add constraint tsdb_group_pk
    primary key (group_id) using index &ts_name;

create unique index tsdb_group_idx
    on tsdb_group(group_name, db_office_code) &ts_name;

create table tsdb_group_member_ts
(
    group_id integer not null,
    data_id integer not null     -- Equivalent to ts_code in CWMS
) &ts_name;

alter table tsdb_group_member_ts add constraint tsdb_group_member_ts_pk
    primary key (group_id, data_id) using index &ts_name;

alter table tsdb_group_member_ts add constraint tsdb_group_member_ts_fk
    foreign key (group_id) references tsdb_group (group_id)
    on delete cascade;

create table tsdb_group_member_group
(
    parent_group_id integer not null,
    child_group_id integer not null,
    include_group varchar2(1) default 'A'     --A: add; S: substract; I: intersect
) &ts_name;

alter table tsdb_group_member_group add constraint tsdb_group_member_group_pk
    primary key (parent_group_id, child_group_id) using index &ts_name;

alter table tsdb_group_member_group add constraint tsdb_group_member_group_fkp
    foreign key (parent_group_id) references tsdb_group (group_id)
    on delete cascade;

alter table tsdb_group_member_group add constraint tsdb_group_member_group_fkc
    foreign key (child_group_id) references tsdb_group (group_id)
    on delete cascade;


create table tsdb_group_member_dt
(
    group_id integer not null,
    data_type_id integer not null
) &ts_name;

alter table tsdb_group_member_dt add constraint tsdb_group_member_dt_pk
    primary key (group_id, data_type_id) using index &ts_name;

alter table tsdb_group_member_dt add constraint tsdb_group_member_dt_fk
    foreign key (group_id) references tsdb_group (group_id)
    on delete cascade;


create table tsdb_group_member_site
(
    group_id integer not null,
    site_id integer not null
) &ts_name;

alter table tsdb_group_member_site add constraint tsdb_group_member_site_pk
    primary key (group_id, site_id) using index &ts_name;

alter table tsdb_group_member_site add constraint tsdb_group_member_site_fk
    foreign key (group_id) references tsdb_group (group_id)
    on delete cascade;


-- groups can be restricted by other attributes like interval, duration, version.
-- this associates a interval code to a group
create table tsdb_group_member_other
(
    group_id integer not null,
    member_type varchar2(24) not null,
    member_value varchar(200) not null
) &ts_name;

alter table tsdb_group_member_other add constraint tsdb_group_member_other_pk
    primary key (group_id, member_type, member_value) using index &ts_name;

alter table tsdb_group_member_other add constraint tsdb_group_member_other_fk
    foreign key (group_id) references tsdb_group (group_id)
    on delete cascade;


---------------------------------------------------------------------------
-- This script defines alarms and severities for CCP
---------------------------------------------------------------------------
---------------------------------------------------------------------------
-- Defines a possible action to take when an appropriate alarm is generated
---------------------------------------------------------------------------
CREATE TABLE tsdb_alarm_action
(
    action_id INTEGER NOT NULL,
    action_name VARCHAR(24) NOT NULL,
    action_type VARCHAR(24) NOT NULL,
    enabled CHAR(1) DEFAULT 'Y',
    current_data_only CHAR(1) DEFAULT 'Y',
    description VARCHAR(1000),
    db_office_code integer default &dflt_office_code
) &ts_name;


alter table tsdb_alarm_action add constraint tsdb_alarm_action_pk
    primary key (action_id) using index &ts_name;

create unique index tsdb_alarm_action_idx
    on tsdb_alarm_action(action_name, db_office_code) &ts_name;


---------------------------------------------------------------------------
-- Set of properties on the action
---------------------------------------------------------------------------
CREATE TABLE tsdb_alarm_action_property
(
    action_id INTEGER NOT NULL,
    prop_name VARCHAR(24) NOT NULL,
    prop_value VARCHAR(240) NOT NULL
) &ts_name;

alter table tsdb_alarm_action_property add constraint tsdb_alarm_action_property_pk
    primary key (action_id, prop_name) using index &ts_name;


---------------------------------------------------------------------------
-- Severity corresponds to a color and a set of actions
---------------------------------------------------------------------------
CREATE TABLE tsdb_severity
(
    severity_id INTEGER NOT NULL,
    severity_name VARCHAR(64) NOT NULL,
    description VARCHAR(1000),
    last_modified TIMESTAMP NOT NULL,
    db_office_code integer default &dflt_office_code
) &ts_name;

alter table tsdb_severity add constraint tsdb_severity_pk
    primary key (severity_id) using index &ts_name;

create unique index tsdb_severity_idx
    on tsdb_severity(severity_name, db_office_code) &ts_name;

---------------------------------------------------------------------------
-- This associates a severity to a set of actions (many to many)
---------------------------------------------------------------------------
CREATE TABLE tsdb_severity_action_assoc
(
    severity_id INTEGER NOT NULL,
    action_id INTEGER NOT NULL
) &ts_name;

alter table tsdb_severity_action_assoc add constraint tsdb_severity_action_assoc_pk
    primary key (severity_id, action_id) using index &ts_name;

---------------------------------------------------------------------------
-- This holds currently-asserted alarms.
---------------------------------------------------------------------------
CREATE TABLE tsdb_alarm_assertion
(
    ts_code INTEGER NOT NULL,
    source_id INTEGER NOT NULL,
    severity_id INTEGER NOT NULL,
    flag_value INTEGER NOT NULL,
    message VARCHAR(256) NOT NULL,
    assert_time TIMESTAMP NOT NULL,
    cmnt VARCHAR(1024)
) &ts_name;

alter table tsdb_alarm_assertion add constraint tsdb_alarm_assertion_pk
    primary key (ts_code, source_id) using index &ts_name;

---------------------------------------------------------------------------
-- This holds a history of alarms.
---------------------------------------------------------------------------
CREATE TABLE tsdb_alarm_history
(
    ts_code INTEGER NOT NULL,
    source_id INTEGER NOT NULL,
    severity_id INTEGER NOT NULL,
    flag_value INTEGER NOT NULL,
    message VARCHAR(256) NOT NULL,
    assert_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    was_dismissed CHAR(1) NOT NULL,
    dismissed_by VARCHAR(24),
    cmnt VARCHAR(1024)
) &ts_name;

alter table tsdb_alarm_history add constraint tsdb_alarm_history_pk
    primary key (ts_code, source_id, assert_time) using index &ts_name;

---------------------------------------------------------------------------
-- Create the sequences (cp_comp_tasklistidseq and all ccp_seq_XXX)
---------------------------------------------------------------------------
create sequence cp_comp_tasklistidseq minvalue 1 start with 1 maxvalue 2000000000 nocache cycle;

declare
  l_sql_txt    varchar2(500);
begin
  l_sql_txt := 'create sequence ccp_seq minvalue :l_num start with :l_num increment by 100 nocache';
  execute immediate replace(l_sql_txt, ':l_num', &cwms_schema..cwms_sec.get_this_db_office_code);
end;
/

spool off;
exit;
