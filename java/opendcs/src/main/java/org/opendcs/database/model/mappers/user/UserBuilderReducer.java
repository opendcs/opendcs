package org.opendcs.database.model.mappers.user;

import java.util.Map;
import java.util.function.BiConsumer;

import org.jdbi.v3.core.result.RowView;
import org.opendcs.database.model.UserBuilder;
import org.opendcs.database.model.IdentityProviderMapping;
import org.opendcs.database.model.Role;

public class UserBuilderReducer implements BiConsumer<Map<Long, UserBuilder>, RowView>
{
    public static final UserBuilderReducer USER_BUILDER_REDUCER = new UserBuilderReducer();

    public UserBuilderReducer()
    {
    }

    @Override
    public void accept(Map<Long, UserBuilder> map, RowView rowView)
    {

        UserBuilder ub = map.computeIfAbsent(rowView.getColumn("u_id", Long.class),
                qid -> rowView.getRow(UserBuilder.class)
        );
        Long roleId = rowView.getColumn("r_id", Long.class);
        if (roleId != null)
        {
            Role r = rowView.getRow(Role.class);
            ub.withRole(r);
        }
        String subject = rowView.getColumn("i_subject", String.class);
        if (subject != null)
        {
            IdentityProviderMapping idpM = rowView.getRow(IdentityProviderMapping.class);
            ub.withIdentityMapping(idpM);
        }

    }

}