-- This already exists on the Oracle Postgres database, without the expiration
-- and the CWMS Database the same way 
alter table platform 
    add constraint site_designator_unqiue 
    unique nulls not distinct (siteid, platformdesignator, expiration);