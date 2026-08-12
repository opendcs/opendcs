package org.opendcs.database.impl.cwms.jdbi.mapper.exception;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.exceptions.data.RelatedDataConstraintException;
import org.opendcs.database.api.exceptions.data.UniqueConstraintViolationException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class CwmsExceptionMapperTest
{
	private final CwmsExceptionMapper handler = new CwmsExceptionMapper();

	@Test
	void testOracleUniqueConstraintErrorCodeIsUniqueConstraintViolationException()
	{
		SQLException sqlEx = new SQLException("ORA-00001: unique constraint violated", null, 1);

		assertThrows(UniqueConstraintViolationException.class, () -> handler.handle(sqlEx, null));
	}

	@Test
	void testOracleChildRecordFoundErrorCodeIsRelatedDataConstraintException()
	{
		SQLException sqlEx = new SQLException("ORA-02292: integrity constraint violated - child record found",
				null, 2292);

		assertThrows(RelatedDataConstraintException.class, () -> handler.handle(sqlEx, null));
	}

	@Test
	void testOracleParentKeyNotFoundErrorCodeIsRelatedDataConstraintException()
	{
		SQLException sqlEx = new SQLException("ORA-02291: integrity constraint violated - parent key not found",
				null, 2291);

		assertThrows(RelatedDataConstraintException.class, () -> handler.handle(sqlEx, null));
	}

	@Test
	void testUnrelatedErrorCodeDoesNotThrow()
	{
		SQLException sqlEx = new SQLException("some other error", null, 12345);

		assertDoesNotThrow(() -> handler.handle(sqlEx, null));
	}
}
