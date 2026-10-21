package org.opendcs.odcsapi.res;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.opendcs.odcsapi.errorhandling.MissingParameterException;
import org.opendcs.odcsapi.errorhandling.WebAppException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ImportResourcesTest
{
    private final ImportResources resource = new ImportResources();

    @Test
    void emptyBodyIsMissingParameter()
    {
        assertThrows(MissingParameterException.class,
                () -> resource.importFile("decodes", false, false, "   "));
    }

    @Test
    void unsupportedImportTypeIsNotImplemented()
    {
        WebAppException ex = assertThrows(WebAppException.class,
                () -> resource.importFile("computations", false, false, "<xml/>"));
        assertEquals(Response.Status.NOT_IMPLEMENTED.getStatusCode(), ex.getStatus());
        assertTrue(ex.getErrMessage().contains("computations"));
    }
}
