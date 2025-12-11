/*
 *  Copyright 2025 OpenDCS Consortium and its Contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License")
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.opendcs.odcsapi.opendcs_dep;

import java.util.stream.Stream;

import decodes.db.Database;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opendcs.odcsapi.beans.ApiPropSpec;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class PropSpecHelperTest
{

	private Database oldDb;

	@BeforeEach
	void setup()
	{

		oldDb = Database.getDb();
		if(oldDb == null)
		{
			//Workaround for PMParser using global instance
			Database.setDb(new Database());
		}
	}

	@AfterEach
	void tearDown()
	{
		Database.setDb(oldDb);
	}

	@ParameterizedTest
	@MethodSource("classNames")
	void testPropSpecClasses(String className)
	{
		ApiPropSpec[] apiPropSpecs = assertDoesNotThrow(() -> PropSpecHelper.getPropSpecs(className), "Failed to get prop specs for " + className);
		assertNotNull(apiPropSpecs, "Prop specs for " + className + " is null");
	}

	static Stream<String> classNames()
	{
		return Stream.of(PropSpecHelper.ClassName.values()).map(Object::toString);
	}
}
