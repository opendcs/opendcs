package opendcs.dao;

import decodes.tsdb.DbIoException;

import java.util.Date;

/**
 * Abstract interface for Location Level Data Access Object.
 * This interface defines the core operations for reading location level data,
 * which can be implemented for different database types (CWMS, HDB, OpenDCS, etc.)
 */
public interface LocationLevelDAO
{
    /**
     * Get the latest location level value for a given location level
     * @param locationLevelId The location level identifier (format varies by database)
     * @return The latest LocationLevelValue or null if none found
     * @throws DbIoException on database error
     */
    LocationLevelValue getLatestLocationLevelValue(String locationLevelId) 
        throws DbIoException;
    
    /**
     * Get the latest location level value with unit conversion
     * @param locationLevelId The location level identifier
     * @param targetUnits The desired units for the value (e.g., "ft", "m", "in")
     * @param sourceUnits The source units to filter by (if null, no filter applied)
     * @return The latest LocationLevelValue or null if none found
     * @throws DbIoException on database error
     */
    LocationLevelValue getLatestLocationLevelValue(String locationLevelId, 
        String targetUnits, String sourceUnits) throws DbIoException;
    
    /**
     * Close the DAO and free any resources
     */
    void close();
    
    /**
     * Clear any cached data
     */
    void clearCache();
    
    /**
     * Inner class representing a Location Level Value
     */
    public static class LocationLevelValue
    {
        private String locationLevelId;
        private double levelValue;
        private Date levelDate;
        private int qualityCode;
        private String units;
        private String comment;
        
        public LocationLevelValue() {}
        
        public LocationLevelValue(String locationLevelId, double levelValue, 
            Date levelDate, String units)
        {
            this.locationLevelId = locationLevelId;
            this.levelValue = levelValue;
            this.levelDate = levelDate;
            this.units = units;
            this.qualityCode = 0;
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
        
        @Override
        public String toString()
        {
            return String.format("LocationLevel[id=%s, value=%.3f %s, date=%s]",
                locationLevelId, levelValue, units, levelDate);
        }
    }
}