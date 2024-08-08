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
        height_low, 
        height_med,
        height_high
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
        if ( cloudType == CloudHeightType.height_low)
        {
            return 4;
        }
        else if ( cloudType == CloudHeightType.height_med)
        {
            return 3;
        }
        else if ( cloudType == CloudHeightType.height_high)
        {
            return 2;
        }
        
        return 3;
    }
    
    public String getTypeName()
    {
        if ( this.cloudType == CloudHeightType.height_low)
        {
            return "Height Low";
        }
        else if ( this.cloudType == CloudHeightType.height_med)
        {
            return "Height Med";
        }
        else if ( this.cloudType == CloudHeightType.height_high)
        {
            return "Height High";
        }
        return "";
    }
    
    public double getDefaultFractionCloudCover()
    {
         if ( this.cloudType == CloudHeightType.height_low)
        {
            return MISSING_VALUE_LOW;
        }
        else if ( this.cloudType == CloudHeightType.height_med)
        {
            return MISSING_VALUE_MED;
        }
        else if ( this.cloudType == CloudHeightType.height_high)
        {
            return MISSING_VALUE_HI;
        }
        return 0.0;       
    }
}
