package org.opendcs.database.model.mappers.user;

import static org.opendcs.database.model.mappers.PrefixRowMapper.addUnderscoreIfMissing;

import java.util.Map;
import java.util.function.BiConsumer;

import org.jdbi.v3.core.result.RowView;
import org.opendcs.database.model.UserBuilder;
import org.opendcs.utils.sql.GenericColumns;
import org.opendcs.database.model.IdentityProviderMapping;
import org.opendcs.database.model.Role;

public class UserBuilderReducer implements BiConsumer<Map<Long, UserBuilder>, RowView>
{
    public static final UserBuilderReducer USER_BUILDER_REDUCER = new UserBuilderReducer();
    private final String rolePrefix;
    private final String userPrefix;
    private final String idpPrefix;

    /**
     * Create reducer using the default prefixes of r_, u_, and i_ for role, user, and identity_provider
     * respectively.
     */
    public UserBuilderReducer()
    {
        this("r", "u", "i");
    }

    /**
     * Create a reduces using the provided prefixes for column names.
     * @param rolePrefix
     * @param userPrefix
     * @param identityProviderPrefix
     */
    public UserBuilderReducer(String rolePrefix, String userPrefix, String identityProviderPrefix)
    {
        this.rolePrefix = addUnderscoreIfMissing(requireValue(rolePrefix));
        this.userPrefix = addUnderscoreIfMissing(requireValue(userPrefix));
        this.idpPrefix = addUnderscoreIfMissing(requireValue(identityProviderPrefix));
    }

    @Override
    public void accept(Map<Long, UserBuilder> map, RowView rowView)
    {

        UserBuilder ub = map.computeIfAbsent(rowView.getColumn(userPrefix + GenericColumns.ID, Long.class),
                qid -> rowView.getRow(UserBuilder.class)
        );
        Long roleId = rowView.getColumn(rolePrefix + GenericColumns.ID, Long.class);
        if (roleId != null)
        {
            Role r = rowView.getRow(Role.class);
            ub.withRole(r);
        }
        String subject = rowView.getColumn(idpPrefix + GenericColumns.SUBJECT, String.class);
        if (subject != null)
        {
            IdentityProviderMapping idpM = rowView.getRow(IdentityProviderMapping.class);
            ub.withIdentityMapping(idpM);
        }

    }

    /**
     * Helper method to ensure a value is present. Similar to Objects.requireNonNull except that
     * a non-empty string is also required.
     * @param value
     * @return the provided value
     * @throws IllegalArgumentException if the provided value is null or empty after trim.
     */
    private String requireValue(String value)
    {
        if (value == null || value.trim().isEmpty())
        {
            throw new IllegalArgumentException("Prefix cannot be null or blank.");
        }

        return value;
    }
}
