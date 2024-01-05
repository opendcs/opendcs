package org.opendcs.database.impl.opendcs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdbi.v3.core.Jdbi;
import org.opendcs.spi.database.MigrationProvider;

public class OpenDcsPgProvider implements MigrationProvider
{
    public static final String NAME = "OpenDCS-Postgres";

    private Map<String,String> placeholders = new HashMap<>();
    private static final List<MigrationProvider.MigrationProperty> properties = new ArrayList<>();

    static
    {
        properties.add(
            new MigrationProperty(
                "NUM_TS_TABLES", Integer.class,
                "How many tables should be used to balance numeric timeseries data."));
        properties.add(
            new MigrationProperty(
                "NUM_TEXT_TABLES", Integer.class,
                "How many tables should be used to balance text timeseries data."));
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public Map<String, String> getPlaceholderValues()
    {
        return Collections.unmodifiableMap(placeholders);
    }

    @Override
    public void setPlaceholderValue(String name, String value)
    {
        placeholders.put(name,value);
    }

    @Override
    public List<MigrationProperty> getPlaceHolderDescriptions()
    {
        return Collections.unmodifiableList(properties);
    }

    @Override
    public void createUser(Jdbi jdbi, String username, String password, List<String> roles)
    {
        jdbi.useHandle(h -> 
        {
            h.execute("select create_user(?,?)", username, password);
            for(String role: roles)
            {
                h.execute("GRANT quote_ident(?) to quote_ident(?)", role, username);
            }
        });
    }

}
