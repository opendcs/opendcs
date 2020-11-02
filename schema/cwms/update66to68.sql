--------------------------------------------------------------------------
-- This script updates OPENDCS 6.6 CCP Schema to OpenDCS 6.8.
-- IT MUST BE EXECUTED BY THE CCP SCHEMA OWNER.
--------------------------------------------------------------------------

-----------------------------------------------------------------------------
-- This software was written by Cove Software, LLC ("COVE") under contract 
-- to the United States Government. 
-- No warranty is provided or implied other than specific contractual terms
-- between COVE and the U.S. Government
-- 
-- Copyright 2019 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
-- All rights reserved.
-----------------------------------------------------------------------------

declare
  v_version number;
  v_offset number(18);
  v_maxid number(18);
  v_nextval number;
  v_stmt varchar2(1000);
begin
  -- Make sure old db version is 17, otherwise abort the upgrade.
  select max(version_num) into v_version from decodesdatabaseversion;
  if v_version <> 17 then
    raise_application_error(-20000, 'Incorrect DB Version for upgrade. Must be 17.');
  end if;

  -- Retrieve the offset being used for cwms_seq
  select cwms_20.cwms_seq.nextval into v_offset from dual;
  v_offset := mod(v_offset, 1000);

  -- sequence for platform table
  select max(id) into v_maxid from platform;
  v_nextval := trunc(v_maxid / 1000) * 1000 + 1000 + v_offset;
  if v_nextval is null then
    v_nextval := v_offset;
  end if;
  v_stmt := 'create sequence platformidseq increment by 1000 start with ' ||v_nextval|| ' nocache';
  execute immediate v_stmt;
  v_stmt := 'grant select on ccp.platformidseq to ccp_users';
  execute immediate v_stmt;
  v_stmt := 'create or replace public synonym platformidseq for ccp.platformidseq';
  execute immediate v_stmt;
  
  -- cp_algorithm
  select max(algorithm_id) into v_maxid from cp_algorithm;
  v_nextval := trunc(v_maxid / 1000) * 1000 + 1000 + v_offset;
  if v_nextval is null then
    v_nextval := v_offset;
  end if;
  v_stmt := 'create sequence cp_algorithmidseq increment by 1000 start with ' ||v_nextval|| ' nocache';
  execute immediate v_stmt;
  v_stmt := 'grant select on ccp.cp_algorithmidseq to ccp_users';
  execute immediate v_stmt;
  v_stmt := 'create or replace public synonym cp_algorithmidseq for ccp.cp_algorithmidseq';
  execute immediate v_stmt;

  -- cp_computation
  select max(computation_id) into v_maxid from cp_computation;
  v_nextval := trunc(v_maxid / 1000) * 1000 + 1000 + v_offset;
  if v_nextval is null then
    v_nextval := v_offset;
  end if;
  v_stmt := 'create sequence cp_computationidseq increment by 1000 start with ' ||v_nextval|| ' nocache';
  execute immediate v_stmt;
  v_stmt := 'grant select on ccp.cp_computationidseq to ccp_users';
  execute immediate v_stmt;
  v_stmt := 'create or replace public synonym cp_computationidseq for ccp.cp_computationidseq';
  execute immediate v_stmt;

  -- datapresentation
  select max(id) into v_maxid from datapresentation;
  v_nextval := trunc(v_maxid / 1000) * 1000 + 1000 + v_offset;
  if v_nextval is null then
    v_nextval := v_offset;
  end if;
  v_stmt := 'create sequence datapresentationidseq increment by 1000 start with ' ||v_nextval|| ' nocache';
  execute immediate v_stmt;
  v_stmt := 'grant select on ccp.datapresentationidseq to ccp_users';
  execute immediate v_stmt;
  v_stmt := 'create or replace public synonym datapresentationidseq for ccp.datapresentationidseq';
  execute immediate v_stmt;

  -- datasource
  select max(id) into v_maxid from datasource;
  v_nextval := trunc(v_maxid / 1000) * 1000 + 1000 + v_offset;
  if v_nextval is null then
    v_nextval := v_offset;
  end if;
  v_stmt := 'create sequence datasourceidseq increment by 1000 start with ' ||v_nextval|| ' nocache';
  execute immediate v_stmt;
  v_stmt := 'grant select on ccp.datasourceidseq to ccp_users';
  execute immediate v_stmt;
  v_stmt := 'create or replace public synonym datasourceidseq for ccp.datasourceidseq';
  execute immediate v_stmt;

  -- datatype
  select max(id) into v_maxid from datatype;
  v_nextval := trunc(v_maxid / 1000) * 1000 + 1000 + v_offset;
  if v_nextval is null then
    v_nextval := v_offset;
  end if;
  v_stmt := 'create sequence datatypeidseq increment by 1000 start with ' ||v_nextval|| ' nocache';
  execute immediate v_stmt;
  v_stmt := 'grant select on ccp.datatypeidseq to ccp_users';
  execute immediate v_stmt;
  v_stmt := 'create or replace public synonym datatypeidseq for ccp.datatypeidseq';
  execute immediate v_stmt;
 
  -- decodesscript
  select max(id) into v_maxid from decodesscript;
  v_nextval := trunc(v_maxid / 1000) * 1000 + 1000 + v_offset;
  if v_nextval is null then
    v_nextval := v_offset;
  end if;
  v_stmt := 'create sequence decodesscriptidseq increment by 1000 start with ' ||v_nextval|| ' nocache';
  execute immediate v_stmt;
  v_stmt := 'grant select on ccp.decodesscriptidseq to ccp_users';
  execute immediate v_stmt;
  v_stmt := 'create or replace public synonym decodesscriptidseq for ccp.decodesscriptidseq';
  execute immediate v_stmt;

  -- enum
  select max(id) into v_maxid from enum;
  v_nextval := trunc(v_maxid / 1000) * 1000 + 1000 + v_offset;
  if v_nextval is null then
    v_nextval := v_offset;
  end if;
  v_stmt := 'create sequence enumidseq increment by 1000 start with ' ||v_nextval|| ' nocache';
  execute immediate v_stmt;
  v_stmt := 'grant select on ccp.enumidseq to ccp_users';
  execute immediate v_stmt;
  v_stmt := 'create or replace public synonym enumidseq for ccp.enumidseq';
  execute immediate v_stmt;

  -- equipmentmodel NOTE this is an oddball. Sequence name is 'equipmentidseq'
  select max(id) into v_maxid from equipmentmodel;
  v_nextval := trunc(v_maxid / 1000) * 1000 + 1000 + v_offset;
  if v_nextval is null then
    v_nextval := v_offset;
  end if;
  v_stmt := 'create sequence equipmentidseq increment by 1000 start with ' ||v_nextval|| ' nocache';
  execute immediate v_stmt;
  v_stmt := 'grant select on ccp.equipmentidseq to ccp_users';
  execute immediate v_stmt;
  v_stmt := 'create or replace public synonym equipmentidseq for ccp.equipmentidseq';
  execute immediate v_stmt;

   -- hdb_loading_application
  select max(loading_application_id) into v_maxid from hdb_loading_application;
  v_nextval := trunc(v_maxid / 1000) * 1000 + 1000 + v_offset;
  if v_nextval is null then
    v_nextval := v_offset;
  end if;
  v_stmt := 'create sequence hdb_loading_applicationidseq increment by 1000 start with ' ||v_nextval|| ' nocache';
  execute immediate v_stmt;
  v_stmt := 'grant select on ccp.hdb_loading_applicationidseq to ccp_users';
  execute immediate v_stmt;
  v_stmt := 'create or replace public synonym hdb_loading_applicationidseq for ccp.hdb_loading_applicationidseq';
  execute immediate v_stmt;

  -- networklist
  select max(id) into v_maxid from networklist;
  v_nextval := trunc(v_maxid / 1000) * 1000 + 1000 + v_offset;
  if v_nextval is null then
    v_nextval := v_offset;
  end if;
  v_stmt := 'create sequence networklistidseq increment by 1000 start with ' ||v_nextval|| ' nocache';
  execute immediate v_stmt;
  v_stmt := 'grant select on ccp.networklistidseq to ccp_users';
  execute immediate v_stmt;
  v_stmt := 'create or replace public synonym networklistidseq for ccp.networklistidseq';
  execute immediate v_stmt;

  -- platformconfig
  select max(id) into v_maxid from platformconfig;
  v_nextval := trunc(v_maxid / 1000) * 1000 + 1000 + v_offset;
  if v_nextval is null then
    v_nextval := v_offset;
  end if;
  v_stmt := 'create sequence platformconfigidseq increment by 1000 start with ' ||v_nextval|| ' nocache';
  execute immediate v_stmt;
  v_stmt := 'grant select on ccp.platformconfigidseq to ccp_users';
  execute immediate v_stmt;
  v_stmt := 'create or replace public synonym platformconfigidseq for ccp.platformconfigidseq';
  execute immediate v_stmt;

  -- presentationgroup
  select max(id) into v_maxid from presentationgroup;
  v_nextval := trunc(v_maxid / 1000) * 1000 + 1000 + v_offset;
  if v_nextval is null then
    v_nextval := v_offset;
  end if;
  v_stmt := 'create sequence presentationgroupidseq increment by 1000 start with ' ||v_nextval|| ' nocache';
  execute immediate v_stmt;
  v_stmt := 'grant select on ccp.presentationgroupidseq to ccp_users';
  execute immediate v_stmt;
  v_stmt := 'create or replace public synonym presentationgroupidseq for ccp.presentationgroupidseq';
  execute immediate v_stmt;

  -- routingspec
  select max(id) into v_maxid from routingspec;
  v_nextval := trunc(v_maxid / 1000) * 1000 + 1000 + v_offset;
  if v_nextval is null then
    v_nextval := v_offset;
  end if;
  v_stmt := 'create sequence routingspecidseq increment by 1000 start with ' ||v_nextval|| ' nocache';
  execute immediate v_stmt;
  v_stmt := 'grant select on ccp.routingspecidseq to ccp_users';
  execute immediate v_stmt;
  v_stmt := 'create or replace public synonym routingspecidseq for ccp.routingspecidseq';
  execute immediate v_stmt;

  -- schedule_entry
  select max(schedule_entry_id) into v_maxid from schedule_entry;
  v_nextval := trunc(v_maxid / 1000) * 1000 + 1000 + v_offset;
  if v_nextval is null then
    v_nextval := v_offset;
  end if;
  v_stmt := 'create sequence schedule_entryidseq increment by 1000 start with ' ||v_nextval|| ' nocache';
  execute immediate v_stmt;
  v_stmt := 'grant select on ccp.schedule_entryidseq to ccp_users';
  execute immediate v_stmt;
  v_stmt := 'create or replace public synonym schedule_entryidseq for ccp.schedule_entryidseq';
  execute immediate v_stmt;

  -- tsdb_group
  select max(group_id) into v_maxid from tsdb_group;
  v_nextval := trunc(v_maxid / 1000) * 1000 + 1000 + v_offset;
  if v_nextval is null then
    v_nextval := v_offset;
  end if;
  v_stmt := 'create sequence tsdb_groupidseq increment by 1000 start with ' ||v_nextval|| ' nocache';
  execute immediate v_stmt;
  v_stmt := 'grant select on ccp.tsdb_groupidseq to ccp_users';
  execute immediate v_stmt;
  v_stmt := 'create or replace public synonym tsdb_groupidseq for ccp.tsdb_groupidseq';
  execute immediate v_stmt;

  -- unitconverter
  select max(id) into v_maxid from unitconverter;
  v_nextval := trunc(v_maxid / 1000) * 1000 + 1000 + v_offset;
  if v_nextval is null then
    v_nextval := v_offset;
  end if;
  v_stmt := 'create sequence unitconverteridseq increment by 1000 start with ' ||v_nextval|| ' nocache';
  execute immediate v_stmt;
  v_stmt := 'grant select on ccp.unitconverteridseq to ccp_users';
  execute immediate v_stmt;
  v_stmt := 'create or replace public synonym unitconverteridseq for ccp.unitconverteridseq';
  execute immediate v_stmt;

  -----------------------------------------------------------------
  -- Finally, update the database version numbers in the database
  -----------------------------------------------------------------
  delete from DecodesDatabaseVersion;
  insert into DecodesDatabaseVersion values(68, 'Updated to OpenDCS 6.8');
  delete from tsdb_database_version;
  insert into tsdb_database_version values(68, 'Updated to OpenDCS 6.8');
  commit;
end;
/


-- In case you need to retry, do this first:
--drop sequence platformidseq;
--drop sequence cp_algorithmidseq;
--drop sequence cp_computationidseq;
--drop sequence datapresentationidseq;
--drop sequence datasourceidseq;
--drop sequence datatypeidseq;
--drop sequence decodesscriptidseq;
--drop sequence enumidseq;
--drop sequence equipmentidseq;
--drop sequence hdb_loading_applicationidseq;
--drop sequence networklistidseq;
--drop sequence platformconfigidseq;
--drop sequence presentationgroupidseq;
--drop sequence routingspecidseq;
--drop sequence schedule_entryidseq;
--drop sequence tsdb_groupidseq;
--drop sequence unitconverteridseq;
--delete from DecodesDatabaseVersion;
--insert into DecodesDatabaseVersion values(17, 'downgraded to 17');
--delete from tsdb_database_version;
--insert into tsdb_database_version values(17, 'downgraded to 17');
--commit;
