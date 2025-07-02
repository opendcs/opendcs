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
    String getName();
    /**
     * If a given implementation requires any custom placeholders
     * within their usage will be acquired from here.
     *
     * Implementations *should* return an unmodifiable Map.
     * @return
     */
    Map<String,String> getPlaceholderValues();

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
    List<MigrationProperty> getPlaceHolderDescriptions();

    /**
     * If this database implementation uses specific Jdbi plugins
     * and the migration code needs them override this function
     * to include them.
     *
     * An implementation may save this instance for usage in
     * updating information.
     * @param jdbi an already prepared Jdbi instance.
     */
    default void registerJdbiPlugins(Jdbi jdbi)
    {
    }

    /**
     * Loads the baseline data required for operations
     * @param profile Profile of the system being manipulated.
     * @param username user with permissions to write data to this database
     * @param password
     */
     void loadBaselineData(Profile profile, String username, String password) throws IOException;

    /**
     * For those implementations that require placeholders, retrieve them
     * and set the values.
     *
     * A JDBI handle is provided and can use to query database information.
     * @param jdbi
     */
    default void determineCurrentPlaceHolders(Jdbi jdbi)
    {
    }

    /**
     * List of Schemas that flyway needs to be aware of and possibly create
     * @return
     */
    default List<String> schemas()
    {
        return new ArrayList<>();
    }

    /**
     * Default roles require for admin operations.
     * @return
     */
    default List<String> getAdminRoles()
    {
        ArrayList<String> roles = new ArrayList<>();
        roles.add("OTSDB_MGR");
        roles.add("OTSDB_ADMIN");
        return roles;
    }

    /**
     * Inform flyway as to whether it is responsible for the schema creation
     * @return
     */
    default boolean createSchemas()
    {
        return false;
    }

    void createUser(Jdbi jdbi, String username, String password, List<String> roles);

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
