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

import decodes.db.Database;
import decodes.db.NetworkList;
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
    private Database decodesDatabase;


    @BeforeAll
    public void load_initial_xml() throws Exception
    {
        log.info("Importing initial xml config");
        Configuration config = this.configuration;
        File propertiesFile = config.getPropertiesFile();

        File logFile = new File(config.getUserDir(),"ImportXmlTestID.log");
        String xmlFileName = getResource(config, "shared/test-sites.xml");


        Programs.DbImport(logFile, propertiesFile, environment, exit, properties,xmlFileName);


    }

    @Test
    public void importNetworkList() throws Throwable
    {

        Configuration config = this.configuration;
        File propertiesFile = config.getPropertiesFile();

        File logFile = new File(config.getUserDir(),"ImportXmlTestID.log");
        String xmlFileName = getResource(config, "shared/MVR-NETLIST-SHORT.xml");

        // gradlew clean :testing:opendcs-tests:test -Pno.docs=true  -Popendcs.test.engine=OpenDCS-XML  --tests org.opendcs.regression_tests.ImportXmlNetworkListTestIT --debug-jvm

        Programs.DbImport(logFile, propertiesFile, environment, exit, properties,xmlFileName);
        Database db = configuration.getDecodesDatabase();
        System.out.println("networkListList size: " + db.networkListList.size());
	  
	  for(NetworkList nl : db.networkListList.getList())
	  {
		System.out.print("NetworkList: "+nl.name +" size: "+nl.size());
	  }

        NetworkList nl = db.networkListList.find("MVR-RIVERGAGES-DAS");

        assertEquals(2,nl.size(),"MVR-RIVERGAGES-DAS network list should have two items");

       // Programs.DbImport(null, null, environment, exit, properties, null);
        // TODO:  import another xml file with overlapping sites/platforms with different officeID
        

    }

    @AfterAll
    public void cleanup_if_needed() throws Exception
    {
    }

}
