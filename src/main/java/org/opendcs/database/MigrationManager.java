package org.opendcs.database;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.jdbi.v3.core.Jdbi;
import org.opendcs.spi.database.MigrationProvider;

/**
 * Utility class to handle schema installation and updates in various situations.
 * This class and it's supporting classes <b>may</b> duplicate behavior see in
 * others classes such as the DAO.
 *
 * This is expected for two reasons:
 * 1. Simple demonstration of modernization and newer concepts
 * 2. It's going to be used from the IZPACK installer so we don't want to
 *    depend on the entire OpenDCS install for just a few features.
 */
public final class MigrationManager
{
    private final DataSource ds;
    private final Jdbi jdbi;
    private final FluentConfiguration flywayConfig;
    private final String implementation;
    private final MigrationProvider migrationProvider;

    public MigrationManager(DataSource ds, String implementation) throws NoMigrationProvider
    {
        this.ds = ds;
        this.implementation = implementation;
        this.jdbi = Jdbi.create(ds);
        this.migrationProvider = getProviderFor(implementation,jdbi);
        flywayConfig = Flyway.configure()
                             .dataSource(ds)
                             .schemas("public")
                             .locations("db/"+implementation)
                             .validateMigrationNaming(true);
    }

    /**
     * Retrieves the Currently installed flyway history version, tsdb version, and decodes version.
     * @return
     */
    public DatabaseVersion getVersion()
    {
        return DatabaseVersion.NOT_INSTALLED;
    }

    /**
     * Retrieve the migration provider so that appropriate values can be set.
     * @return
     */
    public MigrationProvider getMigrationProvider()
    {
        return migrationProvider;
    }

    public void migrate()
    {
        flywayConfig.placeholders(migrationProvider.getPlaceholderValues())
                    .load()
                    .migrate();
    }

    public static class DatabaseVersion
    {
        public static final DatabaseVersion NOT_INSTALLED = new DatabaseVersion(null, null,
                                                                                null, null,
                                                                                new ArrayList<RepeatableMigration>());

        public final String decodesVersion;
        public final String tsdbVersion;
        public final String flywayMigration;
        public final Date   dateInstalled;
        public final List<RepeatableMigration> repeatableMigrations;

        public DatabaseVersion(String decodes, String tsdb, String flyway,
                               Date dateInstalled, List<RepeatableMigration> repeatableMigrations)
        {
            this.decodesVersion = decodes;
            this.tsdbVersion = tsdb;
            this.flywayMigration = flyway;
            this.dateInstalled = dateInstalled;
            this.repeatableMigrations = Collections.unmodifiableList(repeatableMigrations);
        }
    }

    public static class RepeatableMigration
    {
        public final String migrationFile;
        public final String checkSum;
        public final Date dateInstalled;

        public RepeatableMigration(String migrationFile, String checkSum, Date dateInstalled)
        {
            this.migrationFile = migrationFile;
            this.checkSum = checkSum;
            this.dateInstalled = dateInstalled;
        }
    }

    public final Jdbi getJdbiHandle()
    {
        return jdbi;
    }

    public static MigrationProvider getProviderFor(String implementation, Jdbi jdbi) throws NoMigrationProvider
    {
        Objects.requireNonNull(implementation, "An implementation name MUST be provided.");
        ServiceLoader<MigrationProvider> loader = ServiceLoader.load(MigrationProvider.class);
        Iterator<MigrationProvider> providers = loader.iterator();
        while(providers.hasNext())
        {
            MigrationProvider provider = providers.next();
            if (provider.getName().equals(implementation))
            {
                provider.registerJdbiPlugins(jdbi);
                provider.determineCurrentPlaceHolders(jdbi);
                return provider;
            }
        }
        throw new NoMigrationProvider("No migration provider was found for this implementation.");
    }
}