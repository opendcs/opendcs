/*
* Where Applicable, Copyright 2025 OpenDCS Consortium and/or its contributors
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations
* under the License.
*/
package org.opendcs.database.impl.opendcs;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.opendcs.database.api.DatabaseEngine;
import org.opendcs.database.api.OpenDcsDataConstraintException;
import org.opendcs.database.api.OpenDcsDataException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SqlConstraintTranslatorTest
{
	@Test
	void testPostgresForeignKeyViolationIsConstraintException()
	{
		SQLException sqlEx = new SQLException("violates foreign key constraint", "23503");
		OpenDcsDataException result = SqlConstraintTranslator.translate("msg", DatabaseEngine.POSTGRES, sqlEx);

		assertInstanceOf(OpenDcsDataConstraintException.class, result);
		assertEquals("msg", result.getMessage());
		assertSame(sqlEx, result.getCause());
	}

	@Test
	void testPostgresUniqueViolationIsConstraintException()
	{
		SQLException sqlEx = new SQLException("duplicate key value", "23505");
		OpenDcsDataException result = SqlConstraintTranslator.translate("msg", DatabaseEngine.POSTGRES, sqlEx);

		assertInstanceOf(OpenDcsDataConstraintException.class, result);
	}

	@Test
	void testHsqldbAndH2ConstraintSqlStatesAreDetected()
	{
		assertTrue(SqlConstraintTranslator.translate("m", DatabaseEngine.HSQLDB,
				new SQLException("m", "23000")) instanceof OpenDcsDataConstraintException);
		assertTrue(SqlConstraintTranslator.translate("m", DatabaseEngine.H2,
				new SQLException("m", "23502")) instanceof OpenDcsDataConstraintException);
	}

	@Test
	void testNonConstraintSqlStateIsGenericException()
	{
		SQLException sqlEx = new SQLException("connection failure", "08001");
		OpenDcsDataException result = SqlConstraintTranslator.translate("msg", DatabaseEngine.POSTGRES, sqlEx);

		assertFalse(result instanceof OpenDcsDataConstraintException);
	}

	@Test
	void testNullSqlStateIsGenericExceptionForNonOracleEngine()
	{
		SQLException sqlEx = new SQLException("unknown", (String) null);
		OpenDcsDataException result = SqlConstraintTranslator.translate("msg", DatabaseEngine.GENERIC_SQL, sqlEx);

		assertFalse(result instanceof OpenDcsDataConstraintException);
	}

	@Test
	void testOracleChildRecordFoundErrorCodeIsConstraintException()
	{
		SQLException sqlEx = new SQLException("ORA-02292: integrity constraint violated - child record found",
				null, 2292);
		OpenDcsDataException result = SqlConstraintTranslator.translate("msg", DatabaseEngine.ORACLE, sqlEx);

		assertInstanceOf(OpenDcsDataConstraintException.class, result);
	}

	@Test
	void testOracleParentKeyNotFoundErrorCodeIsConstraintException()
	{
		SQLException sqlEx = new SQLException("ORA-02291: integrity constraint violated - parent key not found",
				null, 2291);
		OpenDcsDataException result = SqlConstraintTranslator.translate("msg", DatabaseEngine.ORACLE, sqlEx);

		assertInstanceOf(OpenDcsDataConstraintException.class, result);
	}

	@Test
	void testOracleUnrelatedErrorCodeIsGenericException()
	{
		SQLException sqlEx = new SQLException("ORA-00001: unique constraint violated", null, 1);
		OpenDcsDataException result = SqlConstraintTranslator.translate("msg", DatabaseEngine.ORACLE, sqlEx);

		assertFalse(result instanceof OpenDcsDataConstraintException);
	}

	@Test
	void testOracleErrorCodeIgnoredForNonOracleEngine()
	{
		// same vendor code as an Oracle FK violation, but a non-Oracle engine should
		// not fall back to vendor-code matching
		SQLException sqlEx = new SQLException("unrelated", null, 2292);
		OpenDcsDataException result = SqlConstraintTranslator.translate("msg", DatabaseEngine.POSTGRES, sqlEx);

		assertFalse(result instanceof OpenDcsDataConstraintException);
	}

	@Test
	void testNestedSqlExceptionIsFoundThroughCauseChain()
	{
		SQLException sqlEx = new SQLException("violates foreign key constraint", "23503");
		RuntimeException wrapper = new RuntimeException("jdbi wrapper", new RuntimeException("mid", sqlEx));

		OpenDcsDataException result = SqlConstraintTranslator.translate("msg", DatabaseEngine.POSTGRES, wrapper);

		assertInstanceOf(OpenDcsDataConstraintException.class, result);
		assertSame(wrapper, result.getCause());
	}

	@Test
	void testNoSqlExceptionInCauseChainIsGenericException()
	{
		RuntimeException ex = new RuntimeException("no sql exception here");
		OpenDcsDataException result = SqlConstraintTranslator.translate("msg", DatabaseEngine.POSTGRES, ex);

		assertFalse(result instanceof OpenDcsDataConstraintException);
		assertSame(ex, result.getCause());
	}
}
