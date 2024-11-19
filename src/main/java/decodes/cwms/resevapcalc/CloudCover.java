/* 
 * Copyright (c) 2018
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */
package decodes.cwms.resevapcalc;

/**
 *  Class holds data for Resevap cloud cover fraction and base height by type.
 *  Three types are available, "High, Mid (or Med) and Low.

 */
public class CloudCover
{
    public double fractionCloudCover;
    public double height;
    public CloudHeightType cloudType;
    
    // these are the default cloud cover values
    // set in solflx.f
    private static final double MISSING_VALUE_LOW = 0.54;
    private static final double MISSING_VALUE_MED = 0.0;
    private static final double MISSING_VALUE_HI = 0.0;
   
    public enum CloudHeightType
    {
        HEIGHT_LOW,
        HEIGHT_MED,
        HEIGHT_HIGH
    }

    public CloudCover( double fraction, double height, CloudHeightType type)
    {
        this.fractionCloudCover = fraction;
        this.height = height;
        this.cloudType = type;
    }

    public int getCloudTypeFlag()
    {
        // ICLD is cloud type.  2 means cirrus high clouds, 3 means
        // altocumulus middle clouds, and 3 means stratus low clouds.
        int[] icld = { 2, 3, 4 };
        if ( cloudType == CloudHeightType.HEIGHT_LOW)
        {
            return 4;
        }
        else if ( cloudType == CloudHeightType.HEIGHT_MED)
        {
            return 3;
        }
        else if ( cloudType == CloudHeightType.HEIGHT_HIGH)
        {
            return 2;
        }
        
        return 3;
    }
    
    public String getTypeName()
    {
        if ( this.cloudType == CloudHeightType.HEIGHT_LOW)
        {
            return "Height Low";
        }
        else if ( this.cloudType == CloudHeightType.HEIGHT_MED)
        {
            return "Height Med";
        }
        else if ( this.cloudType == CloudHeightType.HEIGHT_HIGH)
        {
            return "Height High";
        }
        return "";
    }
    
    public double getDefaultFractionCloudCover()
    {
         if ( this.cloudType == CloudHeightType.HEIGHT_LOW)
        {
            return MISSING_VALUE_LOW;
        }
        else if ( this.cloudType == CloudHeightType.HEIGHT_MED)
        {
            return MISSING_VALUE_MED;
        }
        else if ( this.cloudType == CloudHeightType.HEIGHT_HIGH)
        {
            return MISSING_VALUE_HI;
        }
        return 0.0;       
    }
}
