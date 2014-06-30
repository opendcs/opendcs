--------------------------------------------------------------------------------
-- Header for the combined sql file that defines an OPENDCS TSDB under Oracle --
--
-- Cove Software, LLC
--------------------------------------------------------------------------------

-----------------------------------------------------------------------------
-- This software was written by Cove Software, LLC ("COVE") under contract 
-- to the United States Government. 
-- No warranty is provided or implied other than specific contractual terms
-- between COVE and the U.S. Government
-- 
-- Copyright 2014 U.S. Army Corps of Engineers, Hydrologic Engineering Center.
-- All rights reserved.
-----------------------------------------------------------------------------

set echo on
spool combined.log

whenever sqlerror continue
set define on
@@defines.sql

