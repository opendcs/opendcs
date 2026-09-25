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
package decodes.cwms.validation;

import decodes.cwms.CwmsFlags;

/**
 * One check performed by a {@link ScreeningCriteria} on a time series value.
 * <p>
 * The four built in checks -- {@link AbsCheck}, {@link ConstCheck},
 * {@link RocPerHourCheck} and {@link DurCheckPeriod} -- all implement this so a criteria set can
 * be treated as a single ordered collection of checks rather than four parallel typed lists.
 */
public interface ScreeningCheck
{
	/**
	 * The kind of test a check performs.
	 * <p>
	 * <b>Declaration order is the order the checks run in, and that is significant.</b>
	 * ScreeningCriteria keeps only the most severe validity it has seen -- missing beats
	 * rejected beats questionable -- and a later, less severe result is discarded along with
	 * its test bit. Reordering these would silently change which TEST_* bits end up on a value.
	 */
	enum Category
	{
		ABSOLUTE(CwmsFlags.TEST_ABSOLUTE_VALUE),
		CONSTANT(CwmsFlags.TEST_CONSTANT_VALUE),
		RATE_OF_CHANGE(CwmsFlags.TEST_RATE_OF_CHANGE),
		DURATION_MAGNITUDE(CwmsFlags.TEST_DURATION_VALUE);

		private final int testBit;

		Category(int testBit)
		{
			this.testBit = testBit;
		}

		/** @return the CwmsFlags TEST_* bit set on a value when a check of this category fails. */
		public int getTestBit()
		{
			return testBit;
		}
	}

	/**
	 * @return the validity this check asserts when it fails: one of
	 *         {@link ScreeningCriteria#ValidityQuestion}, {@link ScreeningCriteria#ValidityReject}
	 *         or {@link ScreeningCriteria#ValidityMissing}.
	 */
	char getFlag();

	/** @return the category this check belongs to, which also determines when it runs. */
	Category getCategory();
}
