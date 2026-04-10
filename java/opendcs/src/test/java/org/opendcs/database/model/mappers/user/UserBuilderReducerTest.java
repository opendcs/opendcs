package org.opendcs.database.model.mappers.user;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class UserBuilderReducerTest
{
    @Test
    void test_prefix_options()
    {
        // We're really just making sure here we didn't got too overboard
        // with the validation logic
        assertDoesNotThrow(new Executable()
        {
            @Override
            public void execute() throws Throwable
            {
                @SuppressWarnings("unused")
                UserBuilderReducer dut = new UserBuilderReducer("r", "u", "i");
            }
        });
    }

    @ParameterizedTest
    @CsvSource(value =
    {
        "null, null, null",
        "r, null, null",
        "r, u, null",
        "r, u, ''",
        "r, '', ''",
        "'', '', ''"
    },
    nullValues = "null")
    void test_bad_prefixes_throw_error(String role, String user, String idp)
    {
        assertThrows(IllegalArgumentException.class, () ->
        {
            @SuppressWarnings("unused")
            UserBuilderReducer dut = new UserBuilderReducer(role, role, idp);
        });
    }
}
