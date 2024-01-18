package org.opendcs.regression_tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ComputationConfigurationRequired;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;
import org.opendcs.fixtures.helpers.BackgroundTsDbApp;

import decodes.sql.DbKey;
import decodes.tsdb.CpCompDependsUpdater;
import decodes.tsdb.CpDependsNotify;
import decodes.tsdb.TimeSeriesDb;
import opendcs.dao.CompDependsDAO;
import opendcs.dao.CompDependsNotifyDAO;

@DecodesConfigurationRequired({
    "shared/test-sites.xml",
    "${DCSTOOL_HOME}/schema/cwms/cwms-import.xml",
    "shared/presgrp-regtest.xml"
})
@ComputationConfigurationRequired("shared/loading-apps.xml")
public class CompDependsDaoTestIT extends AppTestBase
{
    @ConfiguredField
    public TimeSeriesDb db;

    BackgroundTsDbApp<?> app;


    @AfterAll
    public void stop_apps() throws Exception
    {
        if(app != null)
        {
            app.stop();
        }
    }

    @Test
    public void test_compdepends_operations() throws Exception
    {
        try(CompDependsDAO cd = (CompDependsDAO)db.makeCompDependsDAO();
            CompDependsNotifyDAO cdnDao = (CompDependsNotifyDAO)db.makeCompDependsNotifyDAO();)
        {
            cd.clearScratchpad();
            cd.doModify("delete from cp_comp_depends", new Object[0]);
            assertTrue(cd.getResults("select event_type from cp_depends_notify", 
                       rs -> rs.getString(0)).isEmpty());

            app = BackgroundTsDbApp.forApp(CpCompDependsUpdater.class,
                                            "compdepends",
                                            configuration.getPropertiesFile(),
                                            new File(configuration.getUserDir(),"cdn-test.log"),
                                            environment, "-O");
            long now = System.currentTimeMillis();
            while(app.isRunning() && (System.currentTimeMillis() - now) < 20000 /* 20 seconds */)
            {
                try
                {
                    Thread.sleep(20000);
                }
                catch (InterruptedException ex)
                {
                    /* do nothing, loop again */
                }
            }
            
            assertFalse(cd.getResults("select event_type from cp_depends_notify", 
                       rs -> rs.getString(0)).isEmpty());
        }
    }
}
