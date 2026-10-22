package org.opendcs.odcsapi.res;

import org.junit.jupiter.api.Test;
import org.opendcs.odcsapi.errorhandling.MissingParameterException;

import static org.junit.jupiter.api.Assertions.assertThrows;

final class ImportResourcesTest
{
    private final ImportResources resource = new ImportResources();

    @Test
    void emptyBodyIsMissingParameter()
    {
        assertThrows(MissingParameterException.class,
                () -> resource.importDecodes(false, false, "   "));
        assertThrows(MissingParameterException.class,
                () -> resource.importComputations(false, false, false, "   "));
    }
}
