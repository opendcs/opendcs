package org.opendcs.database.impl.opendcs.jdbi.mapper.exceptions;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.DatabaseEngine;
import org.opendcs.database.api.exceptions.data.RelatedDataConstraintException;
import org.opendcs.database.api.exceptions.data.UniqueConstraintViolationException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class OpenDcsExceptionHandlerTest
{
	@Test
	void testOracleUniqueConstraintErrorCodeIsUniqueConstraintViolationException()
	{
		var handler = new OpenDcsExceptionHandler(DatabaseEngine.ORACLE);
		SQLException sqlEx = new SQLException("ORA-00001: unique constraint violated", null, 1);

		assertThrows(UniqueConstraintViolationException.class, () -> handler.handle(sqlEx, null));
	}

	@Test
	void testOracleChildRecordFoundErrorCodeIsRelatedDataConstraintException()
	{
		var handler = new OpenDcsExceptionHandler(DatabaseEngine.ORACLE);
		SQLException sqlEx = new SQLException("ORA-02292: integrity constraint violated - child record found",
				null, 2292);

		assertThrows(RelatedDataConstraintException.class, () -> handler.handle(sqlEx, null));
	}

	@Test
	void testOracleParentKeyNotFoundErrorCodeIsRelatedDataConstraintException()
	{
		var handler = new OpenDcsExceptionHandler(DatabaseEngine.ORACLE);
		SQLException sqlEx = new SQLException("ORA-02291: integrity constraint violated - parent key not found",
				null, 2291);

		assertThrows(RelatedDataConstraintException.class, () -> handler.handle(sqlEx, null));
	}

	@Test
	void testOracleUnrelatedErrorCodeDoesNotThrow()
	{
		var handler = new OpenDcsExceptionHandler(DatabaseEngine.ORACLE);
		SQLException sqlEx = new SQLException("some other error", null, 12345);

		assertDoesNotThrow(() -> handler.handle(sqlEx, null));
	}

	@Test
	void testOracleErrorCodeIgnoredForNonOracleEngine()
	{
		// Postgres already sets a standard SQLState and is handled by the generic
		// constraint handler; this handler should not also guess based on a vendor
		// code that happens to collide with an Oracle one.
		var handler = new OpenDcsExceptionHandler(DatabaseEngine.POSTGRES);
		SQLException sqlEx = new SQLException("unrelated", null, 2292);

		assertDoesNotThrow(() -> handler.handle(sqlEx, null));
	}
}
