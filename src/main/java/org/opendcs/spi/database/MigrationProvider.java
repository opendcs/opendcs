package org.opendcs.spi.database;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jdbi.v3.core.Jdbi;

import decodes.launcher.Profile;

public interface MigrationProvider
{
    /**
     * The name used for this implementation.
     * @return
     */
    public String getName();
    /**
     * If a given implementation requires any custom placeholders
     * within their usage will be acquired from here.
     *
     * Implementations *should* return an unmodifiable Map.
     * @return
     */
    public Map<String,String> getPlaceholderValues();

    /**
     * Set a specific placeholder value
     * @param name
     * @param value
     */
    public void setPlaceholderValue(String name, String value);

    /**
     * Retrieves a map of required properties for this implementation.
     * @return
     */
    public List<MigrationProperty> getPlaceHolderDescriptions();

    /**
     * If this database implementation uses specific Jdbi plugins
     * and the migration code needs them override this function
     * to include them.
     *
     * An implementation may save this instance for usage in
     * updating information.
     * @param jdbi an already prepared Jdbi instance.
     */
    default public void registerJdbiPlugins(Jdbi jdbi)
    {
    }

    /**
     * Loads the baseline data required for operations
     * @param profile Profile of the system being manipulated.
     * @param username user with permissions to write data to this database
     * @param password
     */
    public void loadBaselineData(Profile profile, String username, String password) throws IOException;

    /**
     * Get baseline Decodes data, like enums, datatype, equipment, presentation groups, etc
     * @return
     */
    public List<File> getDecodesData();
    /**
     * Get Baseline computation get, like Algorithm definitions, Loading Apps, etc.
     * @return
     */
    public List<File> getComputationData();

    /**
     * For those implementations that require placeholders, retrieve them
     * and set the values.
     *
     * A JDBI handle is provided and can use to query database information.
     * @param jdbi
     */
    default public void determineCurrentPlaceHolders(Jdbi jdbi)
    {
    }

    default List<String> schemas()
    {
        return new ArrayList<>();
    }

    public void createUser(Jdbi jdbi, String username, String password, List<String> roles);

    public static class MigrationProperty
    {
        public final String name;
        public final Class<?> type;
        public final String description;

        public MigrationProperty(String name, Class<?> type, String description)
        {
            this.name = name;
            this.type = type;
            this.description = description;
        }
    }
}
