package org.opendcs.regression_tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opendcs.fixtures.helpers.TestResources.getResource;

import java.io.File;
import java.sql.Connection;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opendcs.fixtures.AppTestBase;
import org.opendcs.fixtures.annotations.ComputationConfigurationRequired;
import org.opendcs.fixtures.annotations.ConfiguredField;
import org.opendcs.fixtures.annotations.DecodesConfigurationRequired;
import org.opendcs.fixtures.annotations.TsdbAppRequired;
import org.opendcs.fixtures.assertions.Waiting;
import org.opendcs.fixtures.helpers.BackgroundTsDbApp;
import org.opendcs.fixtures.Programs;
import org.opendcs.fixtures.spi.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import decodes.sql.DbKey;
import decodes.sql.KeyGenerator;
import decodes.tsdb.ComputationApp;
import decodes.tsdb.CpCompDependsUpdater;
import decodes.tsdb.DataCollection;
import decodes.tsdb.TimeSeriesDb;
import decodes.tsdb.TimeSeriesIdentifier;
import opendcs.dai.LoadingAppDAI;
import opendcs.dai.SiteDAI;
import opendcs.dai.TimeSeriesDAI;
import opendcs.dao.DaoBase;

/**
 * test importing decodes database
 */
public class ImportXmlNetworkListTestIT extends AppTestBase
{
    private static final Logger log = LoggerFactory.getLogger(ImportXmlNetworkListTestIT.class);

    @ConfiguredField
    protected TimeSeriesDb db;


    @BeforeAll
    public void load_initial_xml() throws Exception
    {
        log.info("Importing initial xml config");
        Configuration config = this.configuration;
        File propertiesFile = config.getPropertiesFile();

        File logFile = new File(config.getUserDir(),"ImportXmlTestID.log");
        String xmlFileName = getResource(config, "shared/test-sites.xml");


        //Programs.DbImport(logFile, propertiesFile, environment, exit, properties,xmlFileName);


    }

    @Test
    public void importNetworkList()
    {

       // Programs.DbImport(null, null, environment, exit, properties, null);
        // TODO:  import another xml file with overlapping sites/platforms with different officeID


    }

    @AfterAll
    public void cleanup_if_needed() throws Exception
    {
    }

}
