/**
This is  just a really simplified subset to test our variuos concepts
Test database is Derby to simplify development of the tests.
If we wanted the following names to be case-insenstive a lower case unique index would be required
most databases support this.
*/
create table cp_algorithm(
    id int not null primary key,
    name varchar(255) not null unique,
    exec_class varchar(255) not null
);

create table cp_computation(
    id int not null primary key,
    name varchar(255) not null unique, 
    algorithm_id int not null references cp_algorithm(id)
);

create table timeseries(
    id int not null primary key,
    name varchar(255) not null unique
);

create table timeseries_value(
    timeseries_id int not null references timeseries(id),
    date_time timestamp not null,
    value double,
    primary key(timeseries_id,date_time)

);

create table cp_comp_depends(
    timeseries_id int not null references timeseries(id),
    computation_id int not null references cp_computation(id),
    primary key(timeseries_id,computation_id)
);


insert into cp_algorithm(id,name,exec_class) 
       values (1,'Add','decodes.tsdb.algo.Add'),
              (2,'Copy','decodes.tsdb.algo.Copy');

insert into cp_computation(id,name,algorithm_id)
       values (1,'AddComp',1),
              (2,'CopyComp',2);

insert into timeseries(id,name)
       values (1,'To Add'),
              (2,'To Copy'),
              (3,'To Add 2');

insert into cp_comp_depends(timeseries_id,computation_id)
       values (1,1), (2,2), (3,1);
