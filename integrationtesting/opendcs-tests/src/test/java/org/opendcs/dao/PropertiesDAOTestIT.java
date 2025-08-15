package org.opendcs.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.api.dao.PropertiesDAO;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.EnableIfTsDb;

import decodes.sql.DbKey;
import opendcs.dai.LoadingAppDAI;


class PropertiesDAOTestIT extends AppTestBase
{
    @ConfiguredField
    OpenDcsDatabase db;

    @Test
    @EnableIfTsDb({"OpenDCS-Postgres", "OpenDCS-Oracle"})
    void test_write_property() throws Exception
    {
        PropertiesDAO propsDAO = db.getDao(PropertiesDAO.class).get();
        DbKey appKey = DbKey.NullKey;

        final Properties propsIn = new Properties();
        propsIn.setProperty("TestProp","testValue");
        propsIn.setProperty("2nd Test Prop", "Test Value 2");

        try (LoadingAppDAI appDAI = db.getDao(LoadingAppDAI.class).get();
            DataTransaction tx = db.newTransaction())
        {
            appKey = appDAI.getComputationApp("compproc").getAppId();
            
            propsDAO.writeProperties(tx, "ref_loading_application_prop", "loading_application_id", appKey, propsIn);
        }

        try (DataTransaction tx = db.newTransaction())
        {
            final Properties propsOut = propsDAO.readProperties(tx, "ref_loading_application_prop", "loading_application_id", appKey);
            propsIn.forEach((k,v) ->
            {
                String stored = propsOut.getProperty((String)k, "");
                assertEquals(v, stored);
            });
        }
    }
    
}
