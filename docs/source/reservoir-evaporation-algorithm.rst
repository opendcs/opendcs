ResEvapAlgorithm
-----------------

Exec Class: decodes.cwms.algo.ResEvapAlgo

The "ResEvap" algorithm aggregates and calculates
the evaporation of water from a reservoir over the period of a day. By default,
if the following parameter values are not provided they will be retrieved from the CWMS DataBase.

* Longitude
* Latitude
* TimeZone
* Coming Soon - Secchi Depth
* Coming soon - Max Temperature Depth

**Important Timezone Notes:**

* In the Computation Properties, make sure to set your aggregateTimeOffset to your timezoneOffset in the form "8 hours"
* Always set your aggregateTimeZone to "ETC/GMT"

    .. image:: ./media/resources/algorithms/im-042-ResEvap-Properties.jpg
       :alt:  ResEvap Properties
       :width: 600

**Inputs**

.. csv-table::
    :header: "Role Name", "Description", "Units"
    :widths: 25, 60, 15

    "windSpeed", "Wind speed at reservoir", "m/s"
    "airTemp", "Air temperature at reservoir", "°C"
    "relativeHumidity", "Relative humidity", "%"
    "atmPress", "Atmospheric pressure", "mbar"
    "percentLowCloud", "Low cloud cover", "%"
    "elevLowCloud", "High of low cloud cover", "m"
    "percentMidCloud", "Mid cloud cover", "%"
    "elevMidCloud", "High of mid cloud cover", "m"
    "percentHighCloud", "High cloud cover", "%"
    "elevHighCloud", "High of high cloud cover", "m"
    "elev", "Elevation of water level at reservoir", "m"


**Outputs**

.. csv-table::
    :header: "Role Name", "Description", "Units"
    :widths: 25, 60, 15

    "hourlySurfaceTemp", "Hourly surface temperature", "°C"
    "hourlyEvapDepth", "Depth of hourly evaporation", "mm"
    "dailyEvapDepth", "Depth of daily evaporation", "mm"
    "dailyEvapAsFlow", "Daily evaporation as flow", "cms"
    "hourlyFluxOut", "Hourly flux out", "W/m²"
    "hourlyFluxIn", "Hourly flux in", "W/m²"
    "hourlySolar", "Hourly solar radiation", "W/m²"
    "hourlyLatent", "Hourly latent heat flux", "W/m²"
    "hourlySensible", "Hourly sensible heat flux", "W/m²"

**Properties**

.. csv-table::
    :header: "Role Name", "Description", "Example"
    :widths: 20, 40, 40

    "wtpTsId", "Base String for Water Temp Profiles", "FTPK-Lower-D000,0m.Temp-Water.Inst.1Day.0.Rev-NWO-Evap-test"
    "reservoirId", "Location ID of reservoir", "SPK"
    "secchi", "Average secchi depth of reservoir (ft)", ""
    "zeroElevation", "Streamed elevation of reservoir (ft)", ""
    "latitude", "Latitude of reservoir", ""
    "longitude", "Longitude of reservoir", ""
    "timezone", "Time zone at reservoir location", "D%03d,%dm"
    "windShear", "Wind shear equation to be used in computation", "Donelan(default) or Fischer"
    "thermalDifCoe", "Thermal diffusivity coefficient to be used in the computation.", "1.2 (default)"
    "rating", "Rating Curve specification for Elevation-Area curve", "FTPK.Elev;Area.Linear.Step"


**Important Property Notes:**

* **wtpTsId:** This algorithm Requires that a set of Water Temperature profiles exist with the WtpTsid format of
  "FTPK-Lower-D000,0m.Temp-Water.Inst.1Day.0.Rev-NWO-Evap-test"
* **zeroElevation:** Check the rating table to find the exact and correct Zero_elevation
* **thermalDifCoe:** This is a scaling factor applied to the computed thermal diffusivity. Lower values will reduce
    temperature transfer between layers. Adjust this value down if deeper water layers warm too quickly or up if they are
    warming too slowly. The default value is 1.2.
* **rating:** This algorithm Requires that a rating table exist within your CWMS data base for the area of your reservoir
  with rating id test to "FTPK.Elev;Area.Linear.Step"


.. note::
   See :ref:`ResEvap Documentation <resevap-computation>` to better understand how the algorithm behaves.

