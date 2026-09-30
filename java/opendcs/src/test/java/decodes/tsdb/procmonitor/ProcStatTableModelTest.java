/*
* Where Applicable, Copyright 2026 OpenDCS Consortium and/or its contributors
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
package decodes.tsdb.procmonitor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class ProcStatTableModelTest
{
	@Test
	void queueColumnIsOnlyPresentForQueueMonitoring()
	{
		ProcStatTableModel standardModel = new ProcStatTableModel(null, false);
		assertEquals(8, standardModel.getColumnCount());
		assertEquals(Boolean.class, standardModel.getColumnClass(7));

		ProcStatTableModel queueModel = new ProcStatTableModel(null, true);
		assertEquals(9, queueModel.getColumnCount());
		assertEquals(Long.class, queueModel.getColumnClass(7));
		assertEquals(Boolean.class, queueModel.getColumnClass(8));
	}
}
