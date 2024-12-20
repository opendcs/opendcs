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
 * This class holds some location reservoir specific values (lat, lon, instrument height)
 * used for the meteorological computations.
 */
final public class ReservoirLocationInfo
    {
    // package access to these variables
    public final double latitude;
    public final double longitude;
    public final double instrumentHeight;
    public double gmtOffset;

    public final double ru; //sensor windHeight
    public final double rt; //sensor tempHeigth
    public final double rq; //sensor relHeight

    public ReservoirLocationInfo(double latitude, double longitude, double instrumentHeight, double gmtOffset, double ru, double rt, double rq)
        {
        this.latitude = latitude;
        this.longitude = longitude;
        this.instrumentHeight = instrumentHeight;
        this.gmtOffset = gmtOffset;
        this.rt = rt;
        this.ru = ru;
        this.rq = rq;
        }
    }
