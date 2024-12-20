/*
 * Where Applicable, Copyright 2024 The OpenDCS Consortium or it's contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package decodes.cwms.resevapcalc;

/**
 * Class holds data for Resevap cloud cover fraction and base height by type.
 * Three types are available, "High, Mid (or Med) and Low.
 */
final public class CloudCover
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

        CloudHeightType(String name, int flag, double defaultFraction)
            {
            this.name = name;
            this.flag = flag;
            this.defaultFraction = defaultFraction;
            }

        public String getName()
            {
            return name;
            }

        public int getFlag()
            {
            return flag;
            }

        public double getDefaultFraction()
            {
            return defaultFraction;
            }

        }

    public CloudCover(double fraction, double height, CloudHeightType type)
        {
        this.fractionCloudCover = fraction;
        this.height = height;
        this.cloudType = type;
        }

    public int getCloudTypeFlag()
        {
        // ICLD is cloud type.  2 means cirrus high clouds, 3 means
        // altocumulus middle clouds, and 3 means stratus low clouds.
        int[] icld = {2, 3, 4};
        if (cloudType != null)
            {
            return cloudType.getFlag();
            }

        return 3;
        }

    public String getTypeName()
        {
        if (cloudType != null)
            {
            return cloudType.getName();
            }
        return "";
        }

    public double getDefaultFractionCloudCover()
        {
        if (cloudType != null)
            {
            return cloudType.getDefaultFraction();
            }
        return 0.0;
        }
    }
