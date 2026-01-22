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

package org.opendcs.odcsapi.beans;

import java.util.Date;

import io.swagger.v3.oas.annotations.media.Schema;

/** Used to encapsulate a raw message returned by GET message or sent to POST decode */
@Schema(description = "Encapsulates a raw message, including metadata about the message and transmission details.")
public final class ApiRawMessage
{

	// Base64 encoded binary message to preserve original whitespace
	@Schema(description = "Base64-encoded representation of the raw binary message to preserve formatting and content.",
			example = "Q0UzMUQwMzAyMzEyOTEyMzQ1NUc0NSswTk4xNjFFTjIwMDAyN2JCMURBTXRBTXRBTXRBTXM6WUIgMTMuNTkgIA==")
	private String base64 = null;

	public String getBase64()
	{
		return base64;
	}

	public void setBase64(String base64)
	{
		this.base64 = base64;
	}

}
