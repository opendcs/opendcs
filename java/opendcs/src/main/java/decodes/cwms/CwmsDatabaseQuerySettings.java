package decodes.cwms;

import org.opendcs.database.DatabaseQuerySettings;

public class CwmsDatabaseQuerySettings implements DatabaseQuerySettings
{
    @Override
    public boolean numericDate()
    {
        return false;
    }    
}
