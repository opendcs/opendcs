package org.opendcs.utils;

import java.util.Optional;

import ilex.util.Pair;

/** Parses identifiers that optionally include a parenthesized display name. */
public final class TimeSeriesIdentifierParser
{
    private TimeSeriesIdentifierParser()
    {
    }

    /**
     * @param uniqueString identifier, optionally followed by a display name in parentheses
     * @return the identifier without surrounding whitespace before the display name, and the optional display name
     */
    public static Pair<String, Optional<String>> extractDisplayName(String uniqueString)
    {
        String displayName = null;
        String retUniqueString = uniqueString;
        int paren = uniqueString.lastIndexOf('(');
        if (paren > 0 && uniqueString.trim().endsWith(")"))
        {
            displayName = uniqueString.substring(paren + 1);
            retUniqueString = uniqueString.substring(0, paren).trim();
            int endParen = displayName.indexOf(')');
            if (endParen > 0)
            {
                displayName = displayName.substring(0, endParen);
            }
        }
        return Pair.of(retUniqueString, Optional.ofNullable(displayName));
    }
}
