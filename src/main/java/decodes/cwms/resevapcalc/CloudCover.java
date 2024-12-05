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
   
    public enum CloudHeightType
    {
        HEIGHT_LOW("Height Low", 4, 0.54),
        HEIGHT_MED("Height Med", 3, 0.0),
        HEIGHT_HIGH("Height High", 2, 0.0);

        private final String name;
        private final int flag;
        private final double defaultFraction;

        CloudHeightType(String name, int flag, double defaultFraction){
            this.name = name;
            this.flag = flag;
            this.defaultFraction = defaultFraction;
        }

        public String getName(){
            return name;
        }

        public int getFlag(){
            return flag;
        }

        public double getDefaultFraction(){
            return defaultFraction;
        }

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
        if ( cloudType != null )
        {
            return cloudType.getFlag();
        }
        
        return 3;
    }
    
    public String getTypeName()
    {
        if ( cloudType != null )
        {
            return cloudType.getName();
        }
        return "";
    }
    
    public double getDefaultFractionCloudCover()
    {
        if ( cloudType != null )
        {
            return cloudType.getDefaultFraction();
        }
        return 0.0;       
    }
}
