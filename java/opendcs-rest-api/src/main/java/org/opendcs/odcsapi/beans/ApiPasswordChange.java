package org.opendcs.odcsapi.beans;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

public record ApiPasswordChange(@Schema(requiredMode = RequiredMode.REQUIRED) String currentPassword, @Schema(requiredMode = RequiredMode.REQUIRED) String newPassword)
{
}
