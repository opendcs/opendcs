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
import org.jdbi.v3.postgres.PostgresPlugin;
import org.opendcs.spi.database.MigrationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import decodes.dbimport.DbImport;
import decodes.launcher.Profile;
import decodes.tsdb.ImportComp;
import decodes.tsdb.TimeSeriesDb;
import decodes.util.DecodesSettings;
import ilex.util.EnvExpander;
import opendcs.opentsdb.OpenTsdb;

/**
 * OpenDCSPgProvider provides support for handling installation and updates of the OpenDCS-Postgres
 * schema.
 */
public class OpenDcsPgProvider implements MigrationProvider
{
    private static final Logger log = LoggerFactory.getLogger(OpenDcsOracleProvider.class);
    public static final String NAME = "OpenDCS-Postgres";

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
        jdbi.installPlugin(new PostgresPlugin());
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
    public List<File> getDecodesData()
    {
        List<File> files = new ArrayList<>();
        String decodesData[] = {
            "${DCSTOOL_HOME}/edit-db/enum",
            "${DCSTOOL_HOME}/edit-db/eu/EngineeringUnitList.xml",
            "${DCSTOOL_HOME}/edit-db/datatype/DataTypeEquivalenceList.xml",
            "${DCSTOOL_HOME}/edit-db/presentation",
            "${DCSTOOL_HOME}/edit-db/loading-app"};
        fillFiles(files, decodesData, ".xml");
        if (log.isTraceEnabled())
        {
            for (File f: files)
            {
                log.trace("Importing '{}'", f.getAbsolutePath());
            }
        }
        return files;
    }

    @Override
    public List<File> getComputationData()
    {
        List<File> files = new ArrayList<>();
        String computationData[] = {
            "${DCSTOOL_HOME}/imports/comp-standard/algorithms.xml",
            "${DCSTOOL_HOME}/imports/comp-standard/Division.xml",
            "${DCSTOOL_HOME}/imports/comp-standard/Multiplication.xml"
            };
        fillFiles(files, computationData, ".xml");
        return files;
    }

    private void fillFiles(List<File> files, String fileNames[], String suffix)
    {
        for (String filename: fileNames)
        {
            File f = new File(EnvExpander.expand(filename, System.getProperties()));
            if (f.exists() && f.canRead() && f.isFile())
            {
                files.add(f);
            }
            else if (f.exists() && f.canRead() && f.isDirectory())
            {
                try
                {
                    files.addAll(Files.walk(f.toPath())
                                      .map(p -> p.toFile())
                                      .filter(file -> file.isFile())
                                      .filter(file -> file.getAbsolutePath().endsWith(suffix))
                                      .collect(Collectors.toList())
                        );
                }
                catch (IOException ex)
                {
                    log.atError()
                       .setCause(ex)
                       .log("Unable to process diretory '{}'", f.getAbsolutePath());
                }
            }
            else if(f.exists() && !f.canRead())
            {
                log.error("File '{}' is not readable. Skipping.", f.getAbsolutePath());
            }
            else
            {
                log.error("File '{}' is not present. Skipping.", f.getAbsolutePath());
            }
        }
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
        List<File> decodesFiles = getDecodesData();
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
                TimeSeriesDb tsDb = new OpenTsdb();

                tsDb.connect("utility", creds);

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
}
