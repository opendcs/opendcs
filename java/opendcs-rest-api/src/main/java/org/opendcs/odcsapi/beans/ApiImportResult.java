/*
 *  Copyright 2026 OpenDCS Consortium and its Contributors
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

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of an import operation.")
public final class ApiImportResult
{
	@Schema(description = "Type of import that was performed.", example = "decodes")
	private String importType = null;
	@Schema(description = "True if the import was a validate-only (preview) run.", example = "true")
	private boolean validateOnly = false;
	@Schema(description = "True if the import (or validation) completed without errors.", example = "true")
	private boolean success = false;
	@Schema(description = "Human readable summary of the import result.")
	private String message = null;

	public String getImportType()
	{
		return importType;
	}
	public void setImportType(String importType)
	{
		this.importType = importType;
	}
	public boolean isValidateOnly()
	{
		return validateOnly;
	}
	public void setValidateOnly(boolean validateOnly)
	{
		this.validateOnly = validateOnly;
	}
	public boolean isSuccess()
	{
		return success;
	}
	public void setSuccess(boolean success)
	{
		this.success = success;
	}
	public String getMessage()
	{
		return message;
	}
	public void setMessage(String message)
	{
		this.message = message;
	}
}
