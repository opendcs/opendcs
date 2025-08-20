package fixtures;

import java.sql.Connection;

import opendcs.dao.DatabaseConnectionOwner;


public interface TestConnectionOwner extends DatabaseConnectionOwner
{
    public void setConnection(Connection conn);
}
