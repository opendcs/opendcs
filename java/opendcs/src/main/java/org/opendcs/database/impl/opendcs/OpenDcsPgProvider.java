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
import org.opendcs.authentication.identityprovider.impl.builtin.BuiltInIdentityProvider;
import org.opendcs.database.DatabaseService;
import org.opendcs.database.JdbiTransaction;
import org.opendcs.database.TransactionContextImpl;
import org.opendcs.database.api.DataTransaction;
import org.opendcs.database.api.DatabaseEngine;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.api.OpenDcsDatabase;
import org.opendcs.database.dai.UserManagementDao;
import org.opendcs.database.impl.opendcs.dao.UserManagementImpl;
import org.opendcs.database.impl.opendcs.jdbi.column.databasekey.DatabaseKeyArgumentFactory;
import org.opendcs.database.impl.opendcs.jdbi.column.databasekey.DatabaseKeyColumnMapper;
import org.opendcs.database.model.IdentityProvider;
import org.opendcs.database.model.Role;
import org.opendcs.spi.database.MigrationHelper;
import org.opendcs.spi.database.MigrationProvider;
import org.opendcs.utils.logging.OpenDcsLoggerFactory;
import org.slf4j.Logger;

import decodes.dbimport.DbImport;
import decodes.launcher.Profile;
import decodes.sql.DbKey;
import decodes.tsdb.ImportComp;
import decodes.tsdb.TimeSeriesDb;
import decodes.util.DecodesSettings;

/**
 * OpenDCSPgProvider provides support for handling installation and updates of the OpenDCS-Postgres
 * schema.
 */
public class OpenDcsPgProvider implements MigrationProvider
{
    private static final Logger log = OpenDcsLoggerFactory.getLogger();
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
        jdbi.registerArgument(new DatabaseKeyArgumentFactory());
        jdbi.registerColumnMapper(new DatabaseKeyColumnMapper());
    }

    @Override
    public List<MigrationProperty> getPlaceHolderDescriptions()
    {
        return Collections.unmodifiableList(properties);
    }

    @Override
    public void createUser(Jdbi jdbi, String username, String password, List<String> roles)
    {
        final var context = new TransactionContextImpl(null, null, DatabaseEngine.POSTGRES);
        jdbi.useTransaction(h ->
        {
            var tx = new JdbiTransaction(h, context);
            var dao = new UserManagementImpl();

            try(Call createUser = h.createCall("call create_user(:user,:pw)");
                Call assignRole = h.createCall("call assign_role(:user,:role)");)
            {
                setupIdentityProvider(tx, dao);
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
            catch (OpenDcsDataException ex)
            {
                throw new RuntimeException("Unable to setup builtin identity provider.", ex);
            }
        });
    }

    private void setupIdentityProvider(DataTransaction tx, UserManagementDao dao) throws OpenDcsDataException
    {
        var providers = dao.getIdentityProviders(tx, -1, -1);
        for (var provider: providers)
        {
            if (provider instanceof BuiltInIdentityProvider)
            {
                return;
            }
        }
        dao.addRole(tx, new Role(null, "ODCS_API_GUEST", null, null));
        dao.addRole(tx, new Role(null, "ODCS_API_USER", null, null));
        dao.addRole(tx, new Role(null, "ODCS_API_ADMIN", null, null));

        var newProvider = new BuiltInIdentityProvider(DbKey.NullKey, "builttin", null, Map.of());

        dao.addIdentityProvider(tx, newProvider);
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
}
