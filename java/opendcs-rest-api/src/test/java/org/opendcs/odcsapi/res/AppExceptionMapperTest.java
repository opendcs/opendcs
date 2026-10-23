package org.opendcs.odcsapi.res;

import java.sql.SQLException;

import decodes.tsdb.ConstraintException;
import decodes.tsdb.DbIoException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.opendcs.database.api.OpenDcsDataException;
import org.opendcs.database.api.OpenDcsDataRuntimeException;
import org.opendcs.database.api.exceptions.data.OpenDcsConstraintException;
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
	void testRelatedDataConstraintIsConflictWithInUseMessage()
	{
		var ex = new RelatedDataConstraintException(new SQLException("violates foreign key constraint \"site_fk\""));

		Response response = mapper.toResponse(ex);

		assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
		assertEquals("Cannot perform operation because the data is in use.", messageOf(response));
	}

	@Test
	void testUniqueConstraintIsConflictWithAlreadyExistsMessage()
	{
		var ex = new UniqueConstraintViolationException(new SQLException("duplicate key value violates unique constraint"));

		Response response = mapper.toResponse(ex);

		assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
		assertEquals("Cannot perform operation because the data already exists.", messageOf(response));
	}

	@Test
	void testOtherConstraintIsConflict()
	{
		var ex = new OpenDcsConstraintException(new SQLException("violates check constraint"));

		Response response = mapper.toResponse(ex);

		assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
		assertEquals("Cannot perform operation because of a data constraint.", messageOf(response));
	}

	@Test
	void testConstraintThrownByDaoKeepsItsMessage()
	{
		var ex = new RelatedDataConstraintException(
				"Cannot delete network list with ID 5 because it is used by the following routing specs: Spec1");

		Response response = mapper.toResponse(ex);

		assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
		assertEquals("Cannot delete network list with ID 5 because it is used by the following routing specs: Spec1",
				messageOf(response));
	}

	@Test
	void testRuntimeWrappedConstraintIsConflict()
	{
		var ex = new OpenDcsDataRuntimeException(new RelatedDataConstraintException("fk violation"));

		Response response = mapper.toResponse(ex);

		assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
	}

	@Test
	void testConstraintWrappedByResourceIsConflict()
	{
		var constraint = new RelatedDataConstraintException(new SQLException("violates foreign key constraint"));
		var ex = new WebAppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Unable to delete site",
				new OpenDcsDataException("Unable to complete transaction", constraint));

		Response response = mapper.toResponse(ex);

		assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
		assertEquals("Cannot perform operation because the data is in use.", messageOf(response));
	}

	@Test
	void testUnwrappedErrorKeepsItsStatus()
	{
		var ex = new WebAppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Unable to delete site",
				new OpenDcsDataException("connection lost"));

		Response response = mapper.toResponse(ex);

		assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
		assertEquals("Unable to delete site", messageOf(response));
	}

	@Test
	void testLegacyConstraintIsConflictWithItsOwnMessage()
	{
		var ex = new ConstraintException("Cannot delete algorithm with ID=5 because 3 computations rely on it.");

		Response response = mapper.toResponse(ex);

		assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
		assertEquals("Cannot delete algorithm with ID=5 because 3 computations rely on it.", messageOf(response));
	}

	@Test
	void testWrappedLegacyConstraintIsConflictWithItsOwnMessage()
	{
		var constraint = new ConstraintException("Cannot delete algorithm with ID=5 because 3 computations rely on it.");
		var ex = new DbException("Unable to delete algorithm", new DbIoException("Unable to delete algorithm", constraint));

		Response response = mapper.toResponse(ex);

		assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
		assertEquals("Cannot delete algorithm with ID=5 because 3 computations rely on it.", messageOf(response));
	}

	private static String messageOf(Response response)
	{
		return assertInstanceOf(Status.class, response.getEntity()).getMessage();
	}
}
