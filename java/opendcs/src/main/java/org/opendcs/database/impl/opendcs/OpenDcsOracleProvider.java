package org.opendcs.database.impl.opendcs;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import decodes.dbimport.DbImport;
import decodes.launcher.Profile;
import decodes.tsdb.ImportComp;
import decodes.tsdb.TimeSeriesDb;
import decodes.util.DecodesSettings;

/**
 * OpenDcsOracleProvider provides support for handling installation and updates of the OpenDCS-Oracle
 * schema.
 */
public class OpenDcsOracleProvider implements MigrationProvider
{
    private static final Logger log = LoggerFactory.getLogger(OpenDcsOracleProvider.class);
    public static final String NAME = "OpenDCS-Oracle";

    private Map<String,String> placeholders = new HashMap<>();
    private static final List<MigrationProvider.MigrationProperty> properties = new ArrayList<>();

    static
    {
        properties.add(
            new MigrationProperty(
                "NUM_TS_TABLES", Integer.class,
                "How many tables should be used to partition numeric timeseries data."));
        properties.add(
            new MigrationProperty(
                "NUM_TEXT_TABLES", Integer.class,
                "How many tables should be used to balance text timeseries data."));
        properties.add(
            new MigrationProperty("TABLES_SPACE_SPEC", String.class, "")
        );
        properties.add(new MigrationProperty("TSDB_ADM_SCHEMA", String.class,""));
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
            try(Call createUser = h.createCall("call create_user(:user,:pw)");
                Call assignRole = h.createCall("call assign_role(:user,:role)");)
            {
                createUser.bind("user",username)
                          .bind("pw", password)
                          .invoke();
                for(String role: roles)
                {
                    assignRole.bind("user",username)
                              .bind("role",role)
                              .invoke();
                }
            }
        });
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
        List<File> computationFiles = MigrationHelper.getComputationData(log);
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
        theSchemas.add("OTSDB_ADM");
        return theSchemas;
    }
}
