===============================================================================
 This README file includes the information of CCP (CWMS Computation Processor)
 Database Installation User Instructions and Release Notes
===============================================================================

-------------------------------------------------------------------------------
 Installation Instructions
-------------------------------------------------------------------------------
 (1) This ccp-db-install-x-x.tgz contains DB scripts to install all DB objects
     that CCP and DECODES applications require. User needs to be sure, before
     installing the scripts, the CWMS 2.X and CCP DTK 5.X should be available.
     Please see the CCP-DTK-x.x installation manual for complete instructions
     and prerequisites.

 (2) To install the new CCP DB against the existing CWMS DB, run the script as
       > sh ./create_CCPDB.sh

     To update the existed CCP DB objects, run the script as
       > sh ./update_CCPDB.sh

 (3) Make sure you have installed the CCP software before importing ENUM, COMP,
     Presentation, and so on. To import these XML data, run the script as
       > sh ./import_CCPDB.sh

 (4) Should you have any issue during the CCP DB installation, please check all
     the *.out files or send them to ccp-support@sutron.com

 This software is prepared for U.S. Army Corps of Engineers by the Sutron Corp.
 USACE may install this software without licensing constraints.



-------------------------------------------------------------------------------
 Release Notes
-------------------------------------------------------------------------------
 CCP-DB-5-1 and CCP-DTK-5-1
 ==========================

 2012-02-08
 Modify the cwms_ccp package with adding authid_current_user onto the package
 creation and changing the zav_cwms_ts_id table name to the av_cwms_ts_id table
 name since HEC had fixed the delayed response issue for cwms ts_id creation.

 2012-03-22
 Modify the cwms_ccp package with changing notify_for_comp procedure for the
 TSDataStored when store rule is delete_insert, improving the performance for
 notify_tsdatastored() and notify_tsdatadeleted() with using date variables other
 than timestamps in the query, swapping the loop orders, and etc.

 2012-07-10
 Modify the cwms_ccp package with using cwms_v_ts_id public synonym other than
 av_ts_id table. Add the grant option permission in the cwms_v_ts_id to CCP.

 Modify the CCP sequence definition with using ccp_seq sequence except for
 cp_comp_tasklistseq.


-------------------------------------------------------------------------------
 CCP-DB-5-2 and CCP-DTK-5-2
 ==========================

 2012-11-26
 Modify the cwms_ccp package with changing notify_for_comp procedure to avoid
 the exceptions during processing the data in for-loop.

 Modify the Import_CCPDB.sh with adding the cwms rating algorithms and removing
 TSDB-Limit-Algo.xml.


-------------------------------------------------------------------------------
 CCP-DB-5-3 and CCP-DTK-5-3
 ==========================

 2013-06-20
 (1) Modify four CCP tables (DATATYPE, ENGINEERINGUNIT, TRANSPORTMEDIUM, and
     UNITCONVERTER) with adding DB_OFFICE_CODE column, and also altering the
     DB_OFFICE_CODE default value as sys_context('CCPENV','CCP_OFFICE_CODE').

 (2) Remove the unique indexes (DATATYPECODE_IDIDX, TRANSPORTMEDIUM_NMIDX, and
     EUABBRIDX) from these tables (ENGINEERINGUNIT, TRANSPORTMEDIUM, and
     DATATYPE).

 (3) Add the CWMS_CCP_VPD package to provide the VPD APIs for multi-offices
     and users. Also implement the VPD policies to apply to those CCP tables
     with the DB_OFFICE_CODE column.

 (4) Add the three CCP user roles (CCP_USERS_R, CCP_USERS_M, and CCP_USERS_W)
     to be used in the VPD policies with the assigned CWMS App roles (CCP_Mgr,
     CCP_Reviewer, and CCP_Proc).

     Important!!! When a CCP DB user is created, the above three roles have
     to be granted to the user but disabled.

 (5) Build a script to generate several CCP test users for the VPD testing.
     During the CCP DB installation, this script is not executed. It is the
     user's decision whether or not to run this script.

 (6) When connecting to the CWMS/CCP database, user needs to execute the
     following API in order to query CCP tables from either Oracle utilities
     (SQL*Plus or Oracle SQL Developer) or Java program.

     Ex. Given Office_ID = MVR;

        execute CWMS_CCP_VPD.SET_SESSION_OFFICE_ID('MVR');
        commit;


-------------------------------------------------------------------------------
