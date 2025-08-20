/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
* 
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software 
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations 
* under the License.
*/
package decodes.cwms.database;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Call;
import org.opendcs.database.DatabaseService;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.spi.database.MigrationHelper;
import org.opendcs.spi.database.MigrationProvider;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;
import decodes.dbimport.DbImport;
import decodes.launcher.Profile;
import decodes.tsdb.ImportComp;
import decodes.tsdb.TimeSeriesDb;
import decodes.util.DecodesSettings;

/**
 * CwmsOracleProvider provides support for handling installation and updates of the OpenDCS-CWMS-Oracle
 * schema.
 */
public class CwmsOracleProvider implements MigrationProvider
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
    public static final String NAME = "CWMS-Oracle";

    private Map<String,String> placeholders = new HashMap<>();
    private static final List<MigrationProvider.MigrationProperty> properties = new ArrayList<>();

    static
    {
        properties.add(
            new MigrationProperty(
                "CWMS_SCHEMA", String.class,
                "Name of the CWMS Schema to reference"));
        properties.add(
            new MigrationProperty(
                "CCP_SCHEMA", String.class,
                "Name of CCP schema to create."));
        properties.add(
            new MigrationProperty(
                "DEFAULT_OFFICE_CODE", Integer.class,
                "Integer value of the default office to assign"));
        properties.add(
            new MigrationProperty(
                "DEFAULT_OFFICE", String.class,
                "Ascii value of the default office to assign"));
        properties.add(
            new MigrationProperty(
                "TABLE_SPACE_SPEC", String.class,
                "If data will be on a separate table space indicate the line here."));
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public Map<String, String> getPlaceholderValues()
    {
        return Collections.unmodifiableMap(placeholders);
    }

    @Override
    public void setPlaceholderValue(String name, String value)
    {
        placeholders.put(name,value);
    }

    @Override
    public void registerJdbiPlugins(Jdbi jdbi)
    {
    }

    @Override
    public List<MigrationProperty> getPlaceHolderDescriptions()
    {
        return Collections.unmodifiableList(properties);
    }

    @Override
    public void createUser(Jdbi jdbi, String username, String password, List<String> roles)
    {
        jdbi.useTransaction(h ->
        {
            try(Call createUser = h.createCall("call ccp.create_user(:user,:pw)");
                Call createCwmsUser = h.createCall("call cwms_sec.create_user(:user,:pw, null, null)");
                Call assignRole = h.createCall("call cwms_sec.add_user_to_group(:user,:role,:office)");)
            {

                createUser.bind("user", username)
                          .bind("pw", password)
                          .invoke();

                createCwmsUser.bind("user",username)
                          .bind("pw", password)
                          .invoke();
                for(String role: roles)
                {
                    assignRole.bind("user",username)
                              .bind("role",role)
                              .bind("office", placeholders.get("DEFAULT_OFFICE"))
                              .invoke();
                }
            }
        });
    }

    private List<File> getComputationData()
    {
        String computationData[] =
        {
            "${DCSTOOL_HOME}/schema/cwms/cwms-comps.xml"
        };
        List<File> rval = MigrationHelper.getComputationData(log);
        rval.addAll(MigrationHelper.getUsableFiles(computationData, ".xml",log));
        return rval;
    }

    @Override
    public void loadBaselineData(Profile profile, String username, String password) throws IOException
    {
        /* Initializing database with default data */
        System.setProperty("DCS_USER", username);
        System.setProperty("DCS_PASS", password);
        Properties creds = new Properties();
        creds.put("username", username);
        creds.put("password", password);
        List<File> decodesFiles = MigrationHelper.getDecodesData(log);
        List<File> computationFiles = getComputationData();
        try
        {
            File tmp = Files.createTempFile("dcsprofile",".profile").toFile();
            tmp.deleteOnExit();
            Profile tmpProfile = Profile.getProfile(tmp);
            DecodesSettings settings = DecodesSettings.fromProfile(profile);
            settings.DbAuthFile="java-auth-source:password=DCS_PASS,username=DCS_USER";
            settings.saveToProfile(tmpProfile);
            if (!decodesFiles.isEmpty())
            {
                List<String> fileNames = decodesFiles.stream()
                                                     .map(f -> f.getAbsolutePath())
                                                     .collect(Collectors.toList());
                log.info("Loading baseline decodes data.");
                DbImport dbImportObj = new DbImport(tmpProfile, null, false, false,
                                                    false, false, true, null,
                                                    null, null, null, fileNames);
                dbImportObj.importDatabase();
            }

            if (!computationFiles.isEmpty())
            {
                List<String> fileNames = computationFiles.stream()
                                                     .map(f -> f.getAbsolutePath())
                                                     .collect(Collectors.toList());
                log.info("Loading baseline computation data.");

                OpenDcsDatabase database = DatabaseService.getDatabaseFor("utility", settings);
                TimeSeriesDb tsDb = database.getLegacyDatabase(TimeSeriesDb.class).get();

                ImportComp compImport = new ImportComp(tsDb, false, false, fileNames);
                compImport.runApp();
            }
        }
        catch (Exception ex)
        {
            log.atError()
                .setCause(ex)
                .log("Failed to load baseline data.");
            throw new IOException("Unable to import baseline data.", ex);
        }
        finally
        {
            System.clearProperty("DCS_PASS");
            System.clearProperty("DCS_USER");
        }
    }

    public List<String> schemas()
    {
        ArrayList<String> theSchemas = new ArrayList<>();
        theSchemas.add("CCP");
        return theSchemas;
    }

    public boolean createSchemas()
    {
        return true;
    }

    @Override
    public List<String> getAdminRoles()
    {
        ArrayList<String> roles = new ArrayList<>();
        roles.add("CCP MGR");
        roles.add("CCP PROC");
        roles.add("CWMS Users");
        return roles;
    }
}
