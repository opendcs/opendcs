package org.opendcs.database;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.cobraparser.html.domimpl.HTMLElementBuilder.P;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.jdbi.v3.core.Jdbi;

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
    private DataSource ds;
    private Jdbi jdbi;
    private FluentConfiguration flywayConfig;
    private final String implementation;

    /**
     * these much match on every instance. The values can be set but the
     * quantity in the database takes priority so file checksums will match.
     *
     * They are required for the first run. NOTE ignored for any implementation
     * that doesn't use them; they are set as a flyway placeholder.
     */
    private int numText = -1;
    private int numNumeric = -1;

    public MigrationManager(DataSource ds, String implementation)
    {
        this.ds = ds;
        this.implementation = implementation;
        this.jdbi = Jdbi.create(ds);
        flywayConfig = Flyway.configure()
                             .dataSource(ds)
                             .schemas("public")
                             .locations("db/"+implementation)
                             .validateMigrationNaming(true);
    }

    /**
     * Retrieve the current number of numeric tables that was set for this database
     * @return
     */
    public int getNumberOfNumericTables()
    {
        return numText;
    }

    public void setNumberOfNumericTable(int numText)
    {
        this.numText = numText;
    }

    /**
     * Retrieve the current number of text tables that was set for this database.
     *
     */
    public int getNumberOfTextTables()
    {
        return numNumeric;
    }

    public void setNumberOfTextTables(int numText)
    {
        this.numText = numText;
    }

    /**
     * Retrieves the Currently installed flyway history version, tsdb version, and decodes version.
     * @return
     */
    public DatabaseVersion getVersion()
    {
        return DatabaseVersion.NOT_INSTALLED;
    }

    public void migrate()
    {
        Map<String,String> placeHolders = new HashMap<>();
        placeHolders.put("NUM_TS_TABLES",Integer.toString(getNumberOfNumericTables()));
        placeHolders.put("NUM_TEXT_TABLES",Integer.toString(getNumberOfNumericTables()));
        flywayConfig.placeholders(placeHolders)
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
}