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

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;

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

    @Schema(description = "Session-bound upload ID returned by preview and consumed by confirmation.")
    private String uploadId;

    public String getUploadId()
    {
        return uploadId;
    }

    public void setUploadId(String uploadId)
    {
        this.uploadId = uploadId;
    }

    @Schema(description = "Importer selected from the uploaded content.", example = "dbimport")
    private String importer;
    @Schema(description = "UTF-8 size of the uploaded file, in bytes.")
    private long sizeBytes;
    @Schema(description = "Preview counts: XML object element occurrences (including nested DECODES objects), "
            + "or distinct timeSeries identifiers and valid samples. These are file counts, not database changes.")
    private Map<String, Long> counts = new LinkedHashMap<>();
    @Schema(description = "Earliest valid time series sample as an ISO-8601 UTC timestamp; null for XML imports.")
    private String firstSampleTime;
    @Schema(description = "Latest valid time series sample as an ISO-8601 UTC timestamp; null for XML imports.")
    private String lastSampleTime;
    @Schema(description = "File inspection warnings. Preview does not check database conflicts or dependencies.")
    private List<String> warnings = new ArrayList<>();

    public String getImporter()
    {
        return importer;
    }

    public void setImporter(String importer)
    {
        this.importer = importer;
    }

    public long getSizeBytes()
    {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes)
    {
        this.sizeBytes = sizeBytes;
    }

    public Map<String, Long> getCounts()
    {
        return counts;
    }

    public void setCounts(Map<String, Long> counts)
    {
        this.counts = counts;
    }

    public String getFirstSampleTime()
    {
        return firstSampleTime;
    }

    public void setFirstSampleTime(String firstSampleTime)
    {
        this.firstSampleTime = firstSampleTime;
    }

    public String getLastSampleTime()
    {
        return lastSampleTime;
    }

    public void setLastSampleTime(String lastSampleTime)
    {
        this.lastSampleTime = lastSampleTime;
    }

    public List<String> getWarnings()
    {
        return warnings;
    }

    public void setWarnings(List<String> warnings)
    {
        this.warnings = warnings;
    }

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
