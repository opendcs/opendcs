/*
* Copyright 2024-2025 OpenDCS Consortium and/or its contributors
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
package org.opendcs.spi.database;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
     * Retrieve the legacy database version so we can appropraitely baseline
     * @param jdbi
     * @return Legacy version appropriate for Flyway to pick up, or none if it can't be found.
     */
    default Optional<String> getLegacyVersion(Jdbi jdbi)
    {
        return jdbi.withHandle(h ->
        {
            int version = h.select("select version_num from decodesdatabaseversion order by version_num desc")
                           .mapTo(Integer.class)
                           .first();
            String baselineVersion = String.format("%.01f", version/1.0f);

            return Optional.of(baselineVersion);
        });
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
