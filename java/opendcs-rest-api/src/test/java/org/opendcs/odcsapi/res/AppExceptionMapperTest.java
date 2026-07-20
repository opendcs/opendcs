package org.opendcs.odcsapi.res;

import decodes.tsdb.ConstraintException;
import decodes.tsdb.TsdbException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.opendcs.database.api.OpenDcsDataConstraintException;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.api.OpenDcsDataRuntimeException;
import org.opendcs.database.api.exceptions.data.RelatedDataConstraintException;
import org.opendcs.database.api.exceptions.data.UniqueConstraintViolationException;
import org.opendcs.odcsapi.beans.Status;
import org.opendcs.odcsapi.dao.DbException;
import org.opendcs.odcsapi.errorhandling.WebAppException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

final class AppExceptionMapperTest
{
	private final AppExceptionMapper mapper = new AppExceptionMapper();

	@Test
	void testOpenDcsDataConstraintExceptionMapsTo409()
	{
		OpenDcsDataConstraintException ex = new OpenDcsDataConstraintException("Site 5 is still referenced");

		Response response = mapper.toResponse(ex);

		assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
		Status status = assertInstanceOf(Status.class, response.getEntity());
		assertEquals("Site 5 is still referenced", status.getMessage());
	}

	@Test
	void testOpenDcsDataExceptionMapsTo500WithGenericMessage()
	{
		OpenDcsDataException ex = new OpenDcsDataException("some internal detail that shouldn't leak");

		Response response = mapper.toResponse(ex);

		assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
		Status status = assertInstanceOf(Status.class, response.getEntity());
		assertEquals("There was an error.  Please contact your sys admin.", status.getMessage());
	}

	@Test
	void testOpenDcsDataRuntimeExceptionDelegatesToCause()
	{
		OpenDcsDataConstraintException cause = new OpenDcsDataConstraintException("wrapped constraint failure");
		OpenDcsDataRuntimeException ex = new OpenDcsDataRuntimeException(cause);

		Response response = mapper.toResponse(ex);

		assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
		Status status = assertInstanceOf(Status.class, response.getEntity());
		assertEquals("wrapped constraint failure", status.getMessage());
	}

	@Test
	void testConstraintExceptionMapsTo409()
	{
		ConstraintException ex = new ConstraintException("network list still in use");

		Response response = mapper.toResponse(ex);

		assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
		Status status = assertInstanceOf(Status.class, response.getEntity());
		assertEquals("network list still in use", status.getMessage());
	}

	@Test
	void testRelatedDataConstraintExceptionMapsTo409WithInUseMessage()
	{
		RelatedDataConstraintException ex = new RelatedDataConstraintException("fk violation");

		Response response = mapper.toResponse(ex);

		assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
		Status status = assertInstanceOf(Status.class, response.getEntity());
		assertEquals("Cannot perform operation because  data in use.", status.getMessage());
	}

	@Test
	void testUniqueConstraintViolationExceptionMapsTo409WithDuplicateMessage()
	{
		UniqueConstraintViolationException ex = new UniqueConstraintViolationException("dup");

		Response response = mapper.toResponse(ex);

		assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
		Status status = assertInstanceOf(Status.class, response.getEntity());
		assertEquals("Cannot perform operation because  data already exists.", status.getMessage());
	}

	@Test
	void testTsdbExceptionMapsTo500()
	{
		TsdbException ex = new TsdbException("db down");

		Response response = mapper.toResponse(ex);

		assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
	}

	@Test
	void testDbExceptionMapsTo400()
	{
		DbException ex = new DbException("save failed");

		Response response = mapper.toResponse(ex);

		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
	}

	@Test
	void testDbExceptionWithDuplicateKeyCauseMapsToSpecificMessage()
	{
		DbException ex = new DbException("save failed",
				new RuntimeException("duplicate key value violates unique constraint \"idx\""));

		Response response = mapper.toResponse(ex);

		assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
		Status status = assertInstanceOf(Status.class, response.getEntity());
		assertEquals("There was an error saving this.  The most likely error is that there is a duplicate key "
				+ "value.  Please contact your system administrator for more information.", status.getMessage());
	}

	@Test
	void testWebApplicationExceptionPreservesStatus()
	{
		WebApplicationException ex = new NotFoundException("not found");

		Response response = mapper.toResponse(ex);

		assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
	}

	@Test
	void testInternalServerErrorExceptionHidesMessage()
	{
		InternalServerErrorException ex = new InternalServerErrorException("leaky stack trace detail");

		Response response = mapper.toResponse(ex);

		assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
		Status status = assertInstanceOf(Status.class, response.getEntity());
		assertEquals("Internal Server Error", status.getMessage());
	}

	@Test
	void testUnsupportedOperationExceptionMapsTo501()
	{
		UnsupportedOperationException ex = new UnsupportedOperationException("not yet");

		Response response = mapper.toResponse(ex);

		assertEquals(Response.Status.NOT_IMPLEMENTED.getStatusCode(), response.getStatus());
	}

	@Test
	void testWebAppExceptionPreservesStatusAndMessage()
	{
		WebAppException ex = new WebAppException(Response.Status.CONFLICT.getStatusCode(), "conflict message");

		Response response = mapper.toResponse(ex);

		assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
		Status status = assertInstanceOf(Status.class, response.getEntity());
		assertEquals("conflict message", status.getMessage());
	}

	@Test
	void testUnknownExceptionFallsBackTo500()
	{
		Response response = mapper.toResponse(new RuntimeException("totally unexpected"));

		assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
	}
}
