package org.opendcs.model.cwms;

import decodes.sql.DbKey;
import opendcs.dao.CachableDbObject;
import org.opendcs.model.SiteReferenceValue;

import java.util.Date;

/**
 * Inner class representing a Location Level Value
 * Implements CachableDbObject for caching support
 */
public class CwmsSiteReferenceValue implements CachableDbObject, SiteReferenceValue
{
    private String locationLevelId;
    private double levelValue;
    private Date levelDate;
    private int qualityCode;
    private String units;
    private String comment;
    private String cacheKey;

    public CwmsSiteReferenceValue() {}

    public CwmsSiteReferenceValue(String locationLevelId, double levelValue,
                                  Date levelDate, String units)
    {
        this.locationLevelId = locationLevelId;
        this.levelValue = levelValue;
        this.levelDate = levelDate;
        this.units = units;
        this.qualityCode = 0;
    }

    // CachableDbObject implementation
    @Override
    public DbKey getKey()
    {
        // Location levels don't have numeric keys, use null key
        return DbKey.NullKey;
    }

    @Override
    public String getUniqueName()
    {
        return cacheKey != null ? cacheKey : locationLevelId;
    }

    // Getters and setters
    public String getLocationLevelId() { return locationLevelId; }
    public void setLocationLevelId(String locationLevelId) {
        this.locationLevelId = locationLevelId;
    }

    public double getLevelValue() { return levelValue; }
    public void setLevelValue(double levelValue) {
        this.levelValue = levelValue;
    }

    public Date getLevelDate() { return levelDate; }
    public void setLevelDate(Date levelDate) {
        this.levelDate = levelDate;
    }

    public int getQualityCode() { return qualityCode; }
    public void setQualityCode(int qualityCode) {
        this.qualityCode = qualityCode;
    }

    public String getUnits() { return units; }
    public void setUnits(String units) {
        this.units = units;
    }

    public String getComment() { return comment; }
    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getCacheKey() { return cacheKey; }
    public void setCacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
    }

    @Override
    public String toString()
    {
        return String.format("LocationLevel[id=%s, value=%.3f %s, date=%s]",
                locationLevelId, levelValue, units, levelDate);
    }
}