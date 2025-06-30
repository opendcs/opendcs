-- CCP Schema Update
-- June 2025
-- Purpose :  More support for Multi Office, some size increases
-- Strategy:
--          - add db_office_code as needed
--          - db_office_code often added to existing unique constraints


 
 -- increase description size for platformconfig table; see: https://github.com/opendcs/opendcs/issues/1154
ALTER TABLE ${CCP_SCHEMA}.PLATFORMCONFIG MODIFY (DESCRIPTION VARCHAR2(4000)); 
 -- , Error Message = ORA-12899: value too large for column "CCP"."DATASOURCE"."DATASOURCEARG" (actual: 413, maximum: 400)
 --- https://github.com/opendcs/opendcs/issues/672
ALTER TABLE ${CCP_SCHEMA}.DATASOURCE MODIFY (DATASOURCEARG VARCHAR(4000));

CREATE OR REPLACE PROCEDURE add_db_office_code (
  p_owner IN VARCHAR2,   -- e.g. 'CCP'
  p_table IN VARCHAR2    -- e.g. 'PLATFORM'
) AS
  v_cnt NUMBER;
BEGIN
  SELECT COUNT(*) 
    INTO v_cnt
    FROM all_tab_columns
   WHERE owner       = UPPER(p_owner)
     AND table_name  = UPPER(p_table)
     AND column_name = 'DB_OFFICE_CODE';

  IF v_cnt = 0 THEN
    EXECUTE IMMEDIATE
      'ALTER TABLE '
      || UPPER(p_owner) || '.' || UPPER(p_table)
      || ' ADD (DB_OFFICE_CODE NUMBER DEFAULT ${DEFAULT_OFFICE_CODE} NOT NULL)';
  END IF;
END;
/


BEGIN
${CCP_SCHEMA}.add_db_office_code('${CCP_SCHEMA}','PLATFORM');
${CCP_SCHEMA}.add_db_office_code('${CCP_SCHEMA}','DATAPRESENTATION');
${CCP_SCHEMA}.add_db_office_code('${CCP_SCHEMA}','SITE_PROPERTY');
${CCP_SCHEMA}.add_db_office_code('${CCP_SCHEMA}','SITENAME');
END;
/

ALTER TABLE ${CCP_SCHEMA}.PLATFORM ADD CONSTRAINT OFFICE_SITE_DESIGNATOR_UNIQUE
 UNIQUE (  DB_OFFICE_CODE , SITEID , PLATFORMDESIGNATOR , EXPIRATION );
ALTER TABLE ${CCP_SCHEMA}.PLATFORM DROP CONSTRAINT SITE_DESIGNATOR_UNIQUE;

ALTER TABLE ${CCP_SCHEMA}.DATAPRESENTATION ADD CONSTRAINT PRES_UNIQUE 
UNIQUE ( DB_OFFICE_CODE, GROUPID , DATATYPEID  );

ALTER TABLE ${CCP_SCHEMA}.DATAPRESENTATION DROP CONSTRAINT PRES_DT_UNIQUE;


ALTER TABLE ${CCP_SCHEMA}.SITENAME DROP PRIMARY KEY;

ALTER TABLE ${CCP_SCHEMA}.SITENAME
  ADD CONSTRAINT SITENAME_PK
    PRIMARY KEY (DB_OFFICE_CODE, SITEID, NAMETYPE);
    

ALTER TABLE ${CCP_SCHEMA}.SITE_PROPERTY DROP PRIMARY KEY;

ALTER TABLE ${CCP_SCHEMA}.SITE_PROPERTY
  ADD CONSTRAINT SITE_PROPERTY_PK
    PRIMARY KEY (DB_OFFICE_CODE, SITE_ID, PROP_NAME);