package org.opendcs.model.cwms;

import decodes.sql.DbKey;
import opendcs.dao.CachableDbObject;

import java.util.Date;

/**
 * Inner class representing a Location Level Specification
 * Implements CachableDbObject for caching support
 */
public class LocationLevelSpec implements CachableDbObject
{
    private DbKey key = DbKey.NullKey;
    private String locationLevelId;
    private String specifiedLevelId;
    private String parameterId;
    private String parameterTypeId;
    private String durationId;
    private String locationId;
    private double constantValue;  // The constant level value
    private String levelUnitsId;
    private Date levelDate;
    private String levelComment;
    private String unitSystem;
    private Double attributeValue;
    private String attributeUnit;
    private String attributeParameterId;

    public LocationLevelSpec()
    {
    }

    @Override
    public DbKey getKey()
    {
        return key;
    }

    public void setKey(DbKey key)
    {
        this.key = key;
    }

    @Override
    public String getUniqueName()
    {
        // Return the location level ID as the unique name
        return locationLevelId;
    }

    // Getters and setters
    public String getLocationLevelId() { return locationLevelId; }
    public void setLocationLevelId(String locationLevelId) { this.locationLevelId = locationLevelId; }

    public String getSpecifiedLevelId() { return specifiedLevelId; }
    public void setSpecifiedLevelId(String specifiedLevelId) { this.specifiedLevelId = specifiedLevelId; }

    public String getParameterId() { return parameterId; }
    public void setParameterId(String parameterId) { this.parameterId = parameterId; }

    public String getParameterTypeId() { return parameterTypeId; }
    public void setParameterTypeId(String parameterTypeId) { this.parameterTypeId = parameterTypeId; }

    public String getDurationId() { return durationId; }
    public void setDurationId(String durationId) { this.durationId = durationId; }

    public String getLocationId() { return locationId; }
    public void setLocationId(String locationId) { this.locationId = locationId; }

    public double getConstantValue() { return constantValue; }
    public void setConstantValue(double constantValue) { this.constantValue = constantValue; }

    public String getLevelUnitsId() { return levelUnitsId; }
    public void setLevelUnitsId(String levelUnitsId) { this.levelUnitsId = levelUnitsId; }

    public Date getLevelDate() { return levelDate; }
    public void setLevelDate(Date levelDate) { this.levelDate = levelDate; }

    public String getLevelComment() { return levelComment; }
    public void setLevelComment(String levelComment) { this.levelComment = levelComment; }

    public String getUnitSystem() { return unitSystem; }
    public void setUnitSystem(String unitSystem) { this.unitSystem = unitSystem; }

    public Double getAttributeValue() { return attributeValue; }
    public void setAttributeValue(Double attributeValue) { this.attributeValue = attributeValue; }

    public String getAttributeUnit() { return attributeUnit; }
    public void setAttributeUnit(String attributeUnit) { this.attributeUnit = attributeUnit; }

    public String getAttributeParameterId() { return attributeParameterId; }
    public void setAttributeParameterId(String attributeParameterId) { this.attributeParameterId = attributeParameterId; }
}