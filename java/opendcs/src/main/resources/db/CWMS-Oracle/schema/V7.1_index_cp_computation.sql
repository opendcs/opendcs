------------------------------------------------------------------------------
-- CWMS/CCP Performance Improvements 
------------------------------------------------------------------------------

-----------------------------------------------------------------------------
-- 05MAY2025 - JDK - Initial Commit. CP_COMPUTATION indexing to remove table
--                   scans and disk reads. Issue #1089.
--                   https://github.com/opendcs/opendcs/issues/1089
-----------------------------------------------------------------------------

CREATE INDEX CP_COMPUTATION_IDX1 ON CP_COMPUTATION (COMPUTATION_ID ASC, ENABLED ASC, LOADING_APPLICATION_ID ASC, 'X' ASC) ;

