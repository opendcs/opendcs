package decodes.datasource;

import java.util.Properties;

import com.google.auto.service.AutoService;

@AutoService(PMParser.Provider.class)
public class TauPmParser implements PMParser.Provider
{
    public static final String HEADER_TYPE = "tau";

    @Override
    public PMParser create(String headerType)
    {
        CsvPMParser ret = null;
        if (HEADER_TYPE.equalsIgnoreCase(headerType))
        {
            ret = new CsvPMParser();
            ret.idcol = 1;
            ret.datetimecol = 2;
            ret.datetimefmt = "dd-MMM-yyyy   ,HH:mm:ss";
            ret.timezone = "MST";
            ret.datacol = 4;
            ret.delim = ",";
            ret.headerType = "tau";
            ret.mediumType = "tau";
            ret.setProperties(new Properties());
        }
        return ret;
    }
}
