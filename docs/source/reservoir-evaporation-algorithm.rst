
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

Important Notes:

* Make sure to set your aggregateTimeOffset to your timezoneOffset in the form "8 hours"
* Always set your aggregateTimeZone to "ETC/GMT"

    .. image:: ./media/resources/algorithms/im-042-ResEvap-Properties.jpg
       :alt:  ResEvap Properties
       :width: 600

* When setting Zero_elevation property, check rating table to find the exact and correct Zero_elevation
* This algorithm Requires that a rating table exist within your CWMS data base for the area of your reservoir
  with rating id test to "FTPK.Elev;Area.Linear.Step"
* This algorithm Requires that a set of Water Temperature profiles exist with the WtpTsid format of
  "FTPK-Lower-D000,0m.Temp-Water.Inst.1Day.0.Rev-NWO-Evap-test"

+-----------+------------------+----------------------------------------------+
|**Role**   |**Role Name**     |**Description**                               |
+===========+==================+==============================================+
|Inputs     |windSpeed         |Wind speed at reservoir in meters per second  |
|           +------------------+----------------------------------------------+
|           |airTemp           |Air temperature at reservoir in Celsius       |
|           +------------------+----------------------------------------------+
|           |relativeHumidity  |Percentage of relative humidity               |
|           +------------------+----------------------------------------------+
|           |atmPress          |Atmospheric pressure in mbar                  |
|           +------------------+----------------------------------------------+
|           |percentLowCloud   |Percentage of low cloud cover                 |
|           +------------------+----------------------------------------------+
|           |elevLowCloud      |High of low cloud cover in meters             |
|           +------------------+----------------------------------------------+
|           |percentMidCloud   |Percentage of mid cloud cover                 |
|           +------------------+----------------------------------------------+
|           |elevMidCloud      |High of mid cloud cover in meters             |
|           +------------------+----------------------------------------------+
|           |percentHighCloud  |Percentage of high cloud cover                |
|           +------------------+----------------------------------------------+
|           |elevHighCloud     |High of high cloud cover in meters            |
|           +------------------+----------------------------------------------+
|           |elev              |Elevation of water level at reservoir         |
+-----------+------------------+----------------------------------------------+
|Outputs    |hourlySurfaceTemp |                                              |
|           +------------------+----------------------------------------------+
|           |hourlyEvap        |                                              |
|           +------------------+----------------------------------------------+
|           |dailyEvap         |                                              |
|           +------------------+----------------------------------------------+
|           |dailyEvapAsFlow   |                                              |
|           +------------------+----------------------------------------------+
|           |hourlyFluxOut     |                                              |
|           +------------------+----------------------------------------------+
|           |hourlyFluxIn      |                                              |
|           +------------------+----------------------------------------------+
|           |hourlySolar       |                                              |
|           +------------------+----------------------------------------------+
|           |hourlyLatent      |                                              |
|           +------------------+----------------------------------------------+
|           |hourlySensible    |                                              |
+-----------+------------------+----------------------------------------------+
|Properties |wtpTsId           |Base String for water Temperature Profiles,   |
|           |                  |Example: FTPK-Lower-D000,0m.Temp-Water.Inst.  |
|           |                  |1Day.0.Rev-NWO-Evap                           |
|           +------------------+----------------------------------------------+
|           |reservoirId       |Location ID of reservoir                      |
|           +------------------+----------------------------------------------+
|           |secchi            |Average secchi depth of reservoir in feet     |
|           +------------------+----------------------------------------------+
|           |zeroElevation     |Streamed elevation of reservoir in feet       |
|           +------------------+----------------------------------------------+
|           |latitude          |Latitude of reservoir                         |
|           +------------------+----------------------------------------------+
|           |longitude         |Longitude of reservoir                        |
|           +------------------+----------------------------------------------+
|           |timezone          |Time zone at reservoir location,              |
|           |                  |Example value: D%03d,%dm                      |
|           +------------------+----------------------------------------------+
|           |windShear         |Wind shear equation to be utilized in         |
|           |                  |computation,(Donelan or Fischer)              |
|           +------------------+----------------------------------------------+
|           |thermalDifCoe     |Thermal diffusivity coefficient to be         |
|           |                  |utilized in computation                       |
|           +------------------+----------------------------------------------+
|           |rating            |Rating Curve specification for Elevation-Area |
|           |                  |curve, Example: FTPK.Elev;Area.Linear.Step    |
+-----------+------------------+----------------------------------------------+

.. note::
   See :ref:`ResEvap Documentation <legacy-resevap-computation>` to better understand how the algorithm behaves.

