package org.opendcs.utils.json;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

public final class Helpers
{
    private Helpers()
    {
        /* utilitity class */
    }

    public static String getTextField(JsonNode root, String field) throws IOException
    {
        var tmp = root.get(field);
        if (tmp == null || tmp.isNull())
        {
            throw new IOException("OpenID Configuration does not contain a '" + field + "'' entry.");
        }
        return tmp.asText();
    }   
}
