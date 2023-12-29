package org.opendcs.spi.database;

import java.util.List;
import java.util.Map;

import org.jdbi.v3.core.Jdbi;

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
     * For those implementations that require placeholders, retrieve them
     * and set the values.
     *
     * A JDBI handle is provided and can use to query database information.
     * @param jdbi
     */
    default public void determineCurrentPlaceHolders(Jdbi jdbi)
    {
    }

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
