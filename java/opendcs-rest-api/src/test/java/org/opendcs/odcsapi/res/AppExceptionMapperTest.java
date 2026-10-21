package org.opendcs.odcsapi.res;

import decodes.tsdb.ConstraintException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.opendcs.database.api.OpenDcsDataRuntimeException;
import org.opendcs.database.api.exceptions.data.OpenDcsConstraintException;
import org.opendcs.database.api.exceptions.data.RelatedDataConstraintException;
import org.opendcs.database.api.exceptions.data.UniqueConstraintViolationException;
import org.opendcs.odcsapi.beans.Status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

final class AppExceptionMapperTest
{
	private final AppExceptionMapper mapper = new AppExceptionMapper();

	@Test
	void testRelatedDataConstraintIsConflictWithInUseMessage()
	{
		Response response = mapper.toResponse(new RelatedDataConstraintException("fk violation"));

		assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
		assertEquals("Cannot perform operation because the data is in use.", messageOf(response));
	}

	@Test
	void testUniqueConstraintIsConflictWithAlreadyExistsMessage()
	{
		Response response = mapper.toResponse(new UniqueConstraintViolationException("duplicate key"));

		assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
		assertEquals("Cannot perform operation because the data already exists.", messageOf(response));
	}

	@Test
	void testOtherConstraintIsConflict()
	{
		Response response = mapper.toResponse(new OpenDcsConstraintException("check violation"));

		assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
		assertEquals("Cannot perform operation because of a data constraint.", messageOf(response));
	}

	@Test
	void testRuntimeWrappedConstraintIsConflict()
	{
		var ex = new OpenDcsDataRuntimeException(new RelatedDataConstraintException("fk violation"));

		Response response = mapper.toResponse(ex);

		assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
	}

	@Test
	void testLegacyConstraintIsConflictWithItsOwnMessage()
	{
		var ex = new ConstraintException("Cannot delete algorithm with ID=5 because 3 computations rely on it.");

		Response response = mapper.toResponse(ex);

		assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
		assertEquals("Cannot delete algorithm with ID=5 because 3 computations rely on it.", messageOf(response));
	}

	private static String messageOf(Response response)
	{
		return assertInstanceOf(Status.class, response.getEntity()).getMessage();
	}
}
