/*
 *  Copyright 2023 OpenDCS Consortium
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

package org.opendcs.odcswebclient.jetty;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ArgParserTest {
	@Test
	void testSwitchValue() {
		ArgParser parser = new ArgParser(new String[]{"-flag", "value", "anotherValue"});
		assertEquals("value", parser.switchValue("-flag", "default"),
				"ArgParser should return the value of the switch");
		assertEquals("default", parser.switchValue("-unknown", "default"),
				"ArgParser should return the default value");
	}
}
