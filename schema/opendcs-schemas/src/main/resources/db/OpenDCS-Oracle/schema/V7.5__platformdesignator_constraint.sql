alter table platform
    add constraint site_designator_expiration_unique
    unique (siteid, platformdesignator, expiration);

alter table platform
    drop constraint site_designator_unique;