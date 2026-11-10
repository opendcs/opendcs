package org.opendcs.database.impl.opendcs.jdbi.column.text;

import java.nio.charset.StandardCharsets;

public record Base64String(String value)
{
    public Base64String(byte[] bytes)
    {
        this(new String(bytes, StandardCharsets.UTF_8));
    } 
};
