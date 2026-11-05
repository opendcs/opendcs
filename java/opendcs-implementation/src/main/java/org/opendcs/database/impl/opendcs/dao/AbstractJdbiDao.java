/*
 * Where Applicable, Copyright 2026 OpenDCS Consortium and/or its contributors
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
package org.opendcs.database.impl.opendcs.dao;

import static java.util.Objects.requireNonNull;

import org.jdbi.v3.stringtemplate4.StringTemplateSqlLocator;
import org.opendcs.database.api.OpenDcsDataException;
import org.stringtemplate.v4.STGroup;

/**
 * Abstract base class that provides common functionality for database DAOs using StringTemplate.
 * Extracts the STGroup acquisition logic into a shared helper method.
 * Use {@link #getTemplate(String)} to acquire a template instance by name.
 */
public abstract class AbstractJdbiDao
{
    protected final STGroup queries;

    /**
     * Initializes this DAO with the STGroup from StringTemplateSqlLocator.
     * Must be called by subclasses (either in a constructor or before using templates).
     *
     * @param templateGroupId The ID of the class whose templates to locate; STGroup will be looked up automatically
     */
    protected AbstractJdbiDao(Class<?> templateGroupId)
    {
        this.queries = requireNonNull(StringTemplateSqlLocator.findStringTemplateGroup(templateGroupId),
                "STGroup not found for class: " + templateGroupId);
    }

    /**
     * Acquires a template instance from the STGroup by name.
     * Checks for null and throws an OpenDcsDataException with a descriptive error message
     * if the template is not found.
     *
     * @param name The name of the template (e.g., "SELECT", "MERGE", "DELETE")
     * @return An ST instance if the template exists, or null
     * @throws OpenDcsDataException if the instance cannot be found within the given group.
     */
    protected org.stringtemplate.v4.ST getTemplate(String name) throws OpenDcsDataException
    {
        var template = queries.getInstanceOf(name);
        if (template == null)
        {
            throw new OpenDcsDataException("Could not find template: " + name);
        }
        return template;
    }
}
