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

import java.util.Properties;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Represents a full equipment model record, including identification fields and arbitrary properties.")
public final class ApiEquipmentModel
{
    @Schema(description = "The unique numeric identifier for the equipment model.", example = "1")
    private Long equipmentId = null;

    @Schema(description = "The unique name of the equipment model.", example = "GOES-DCP-1")
    private String name = null;

    @Schema(description = "The type of equipment (must match a value in the EquipmentType reference list).",
            example = "goes")
    private String equipmentType = null;

    @Schema(description = "The equipment manufacturer.", example = "Sutron")
    private String company = null;

    @Schema(description = "The manufacturer model number.", example = "9210B")
    private String model = null;

    @Schema(description = "A description of the equipment.", example = "Sutron 9210B data logger")
    private String description = null;

    @Schema(description = "Arbitrary key-value properties associated with the equipment model.")
    private Properties properties = new Properties();

    public Long getEquipmentId()
    {
        return equipmentId;
    }

    public void setEquipmentId(Long equipmentId)
    {
        this.equipmentId = equipmentId;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getEquipmentType()
    {
        return equipmentType;
    }

    public void setEquipmentType(String equipmentType)
    {
        this.equipmentType = equipmentType;
    }

    public String getCompany()
    {
        return company;
    }

    public void setCompany(String company)
    {
        this.company = company;
    }

    public String getModel()
    {
        return model;
    }

    public void setModel(String model)
    {
        this.model = model;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public Properties getProperties()
    {
        return properties;
    }

    public void setProperties(Properties properties)
    {
        this.properties = properties;
    }
}
