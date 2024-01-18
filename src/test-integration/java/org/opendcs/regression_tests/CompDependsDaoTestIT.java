package org.opendcs.regression_tests;

import org.junit.jupiter.api.Test;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ComputationConfigurationRequired;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;

import decodes.tsdb.TimeSeriesDb;
import opendcs.dai.CompDependsDAI;

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


    @Test
    public void test_compdepends_operations() throws Exception
    {
        try(CompDependsDAI dao = db.makeCompDependsDAO();)
        {
            
        }
    }
}
