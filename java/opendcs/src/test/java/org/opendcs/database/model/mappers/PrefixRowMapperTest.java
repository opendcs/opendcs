package org.opendcs.database.model.mappers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opendcs.database.model.mappers.PrefixRowMapper.addUnderscoreIfMissing;

class PrefixRowMapperTest
{
    
    @Test
    void test_prefix_logic()
    {
        final var blankPrefix = addUnderscoreIfMissing("");
        assertEquals("", blankPrefix);

        final var wsPrefix = addUnderscoreIfMissing("     \t \t  \t \r \n");
        assertEquals("", wsPrefix);

        final var withoutUnderScore = addUnderscoreIfMissing("p");
        assertEquals("p_", withoutUnderScore);

        final var withUnderScore = addUnderscoreIfMissing("p_");
        assertEquals("p_", withUnderScore);

        final var nullPrefix = addUnderscoreIfMissing(null);
        assertEquals("", nullPrefix);
    }
}
