package org.opendcs.data;

import org.opendcs.database.DatabaseKey;

public interface Organization
{
    /**
     * For roles that aren't specific to an Organization or implementations
     * that don't yet support organizations.
     */
    Organization NULL_ORG = new DefaultOrganization();

    /**
     * In database identifier for this key
     * @return
     */
    DatabaseKey getId();

    /**
     * Short name of this Organization
     * @return
     */
    String getName();

    /**
     * Full display name of this organization.
     * Default returns the result of {@see getName}
     * @return
     */
    default String getDisplayName()
    {
        return getName();
    }

    Organization getReportsToOffice();


    final class DefaultOrganization implements Organization
    {
        private DefaultOrganization()
        {
            /* we only need the one instance. */
        }

        @Override
        public DatabaseKey getId()
        {
            return null;
        }

        @Override
        public String getName()
        {
            return "";
        }

        @Override
        public String getDisplayName()
        {
            return "Default Organization";
        }

        @Override
        public Organization getReportsToOffice()
        {
            return null;
        }

    };
}
