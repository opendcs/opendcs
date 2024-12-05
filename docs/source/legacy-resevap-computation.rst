March 27, 2020

**Prepared For:**

U.S. Army Corps of Engineers

**Contractor:**

Resource Management Associates

1756 Picasso Avenue, Suite G

Davis, CA 95618

**Contact:**

John F. DeGeorge

Peter Morris

(530) 564-7043

**Author:**

| Caleb DeChant
| Steve Andrews

Table of contentS

`1 Background <#background>`__ `1 <#background>`__

`1.1 Scope of Work <#scope-of-work>`__ `1 <#scope-of-work>`__

`1.2 Technical Exhibit <#technical-exhibit>`__
`1 <#technical-exhibit>`__

`2 Introduction <#introduction>`__ `2 <#introduction>`__

`3 ResEvap Software Distribution and
USAGE <#resevap-software-distribution-and-usage>`__
`2 <#resevap-software-distribution-and-usage>`__

`4 ResEvap Jython Sequence of
Operations <#resevap-jython-sequence-of-operations>`__
`4 <#resevap-jython-sequence-of-operations>`__

`5 Java Compute Routines <#java-compute-routines>`__
`5 <#java-compute-routines>`__

`5.1 Core Routines <#core-routines>`__ `5 <#core-routines>`__

`5.1.1 Input Data <#input-data>`__ `5 <#input-data>`__

`5.1.2 Output Data <#output-data>`__ `6 <#output-data>`__

`5.2 Evaporation Computations <#evaporation-computations>`__
`7 <#evaporation-computations>`__

`5.2.1 Static Variables <#static-variables>`__ `8 <#static-variables>`__

`5.2.2 Iterative Computations <#iterative-computations>`__
`9 <#iterative-computations>`__

`5.3 Radiation Computations <#radiation-computations>`__
`11 <#radiation-computations>`__

`5.3.1 Shortwave Radiation <#shortwave-radiation>`__
`11 <#shortwave-radiation>`__

`5.3.2 Longwave Radiation <#longwave-radiation>`__
`14 <#longwave-radiation>`__

`5.4 Vertical Temperature Profile
Computations <#vertical-temperature-profile-computations>`__
`16 <#vertical-temperature-profile-computations>`__

`6 Appendix 1: Variable
Definitions <#appendix-1-variable-definitions>`__
`19 <#appendix-1-variable-definitions>`__

`6.1 Evaporation Computations <#evaporation-computations-1>`__
`19 <#evaporation-computations-1>`__

`6.2 Radiation Computations <#radiation-computations-1>`__
`20 <#radiation-computations-1>`__

`6.3 Vertical Temperature Profile
Computations <#vertical-temperature-profile-computations-1>`__
`21 <#vertical-temperature-profile-computations-1>`__

`7 Appendix 2: Constant Values <#appendix-2-constant-values>`__
`22 <#appendix-2-constant-values>`__

`8 Appendix 3: Example Input Files <#appendix-3-example-input-files>`__
`24 <#appendix-3-example-input-files>`__

`8.1 ResEvap Properties <#resevap-properties>`__
`24 <#resevap-properties>`__

`8.2 Reservoir Information <#reservoir-information>`__
`24 <#reservoir-information>`__

`9 Appendix 4: Recommendations <#appendix-4-recommendations>`__
`25 <#appendix-4-recommendations>`__

`10 References <#references>`__ `28 <#references>`__

Background
==========

Scope of Work
-------------

ResEvap is an Omaha District application which was ported to JAVA and
was modified to be able to interact with the CWMS database through usage
of the REGI database APIs. The documentation will cover:

• Reviewing the ResEvap source code and examining the application at
runtime by debugging with an IDE.

• Develop a clear understanding of the computations from an engineering
perspective.

• Author a document that describes ResEvap. Note potential improvements
to ResEvap and incorrect behavior discovered during the analysis.

Technical Exhibit
-----------------

Task Item 2: Author an engineering and software requirements document
detailing the ResEvap computation.

Background: ResEvap is an Omaha Corps office application for computing
reservoir evaporation. This application was originally developed in
Fortran. The Omaha Corps office contracted RMA in 2015 to port ResEvap
from Fortran to Java and Jython, as is without changing the
computational logic. ResEvap was modified to be able to interact with
the CWMS database through usage of the REGI database APIs. ResEvap’s
algorithms, computations, work flow, and database interactions need to
be documented in detail to ensure that the computational procedure is as
expected.

Documentation will entail detailed descriptions of the computations
performed within ResEvap. Since there is currently no documentation of
the computational procedure, this will require:

1. Reviewing the ResEvap source code and examining the application at
runtime by debugging with an IDE.

2. Developing a clear understanding of the computations from an
engineering perspective. This might require communications with the
Omaha office for clarifications on expected behavior.

3. Authoring a document that describes ResEvap. Noted in this document
will be both potential improvements to ResEvap and incorrect behavior
discovered during the analysis. Recommended solutions for improvements
and incorrect behavior will be documented.

A draft version of the document will be delivered to the Corps suitable
for review. The contractor will revise the document based on the Corps
review.

Deliverables for this task shall include the document detailing the
current behavior and computations done with ResEvap, and recommendations
for its improvement.

Introduction
============

ResEvap is a reservoir evaporation computational utility developed for
the reservoirs in the Missouri River basin. Development of ResEvap was
motivated by the need for accurate estimates of evaporation to properly
manage reservoirs, in conjunction with the known limitation of
evaporation observations (e.g. Evaporation Pans). The underlying
algorithm was developed with state-of-the-art evaporation and water
temperature modeling techniques, and was subsequently written in Fortran
by the USACE (Daly 2005). In order to improve the usability of ResEvap
throughout the USACE, the model was converted to Java and Jython/Python
by RMA. At the time of the original conversion, the desired outcome was
a direct translation of the computation logic from Fortran to Java with
Jython/Python scripting used to orchestrate the computations. Since that
time, a need for documentation of the computational routines within
ResEvap has developed, motivating the production of this document.

This documentation is intended to describe both the core computational
functionality, as well as the entry points to the computational routines
currently used by the USACE. The existing entry point for the ResEvap
computations is the Jython Processor, described in the next section.
Next, the computations, including the evaporation, radiation and
vertical temperature profile models are described. Descriptions of the
computations are supported in Appendices 1 and 2, which contain variable
definitions and constant value listings. Appendix 3 provides example
input files. Appendix 4 provides recommendations for the existing
ResEvap model, and last there is a list of references.

ResEvap Software Distribution and USAGE
=======================================

ResEvap is currently distributed as an archive that is deployed onto the
CWMS Solaris system. The ResEvap archive is self-contained with only
dependencies onto to the CWMS_HOME configuration area, mainly for
database connectivity. The ResEvap archive has a combination of embedded
Java jars, along with a Solaris JRE, to ensure that it can be
independently versioned from CWMS. Independent versioning is necessary
due to the independent release cycles of ResEvap and CWMS. ResEvap Java
computations are orchestrated using a combination of Jython and shell
scripting. The ResEvap shell script, resevap.csh, starts the Java JRE
using Jython’s main class and the ResEvap Jython script. When running
the ResEvap Jython script, additional parameterization is specified for
the reservoir name and the start and end date/times.

In operation, Omaha will be running ResEvap using a cron job that calls
the ResEvap shell script. The cron job script will iterate through the
all of the reservoirs calling into the ResEvap shell script with a time
window that is from 5 days back from current day to the current day.
This cron job will be run on a daily basis. ResEvap will also be run
manually for a given project using a variety of time windows, for a
month, a season, or a year for example. There are no plans to run
ResEvap into the future as a forecasting model, it will only be run
using observed data.

The Java ResEvap computational model is independent of the I/O layer
that retrieves and stores data to the CWMS database. ResEvap utilizes a
Jython script to perform I/O and database access. This includes the
business of extracting information from the CWMS database, orchestrating
the calls into the computational code, and posting results back to the
CWMS database. The Jython scripting language was primarily chosen to
allow the Omaha district office to maintain and enhance ResEvap. Coding
in Jython provides the benefit of strongly separating the API between
the Java computations and the orchestration layer above the
computations. Additionally, the use of Jython allows for development of
ResEvap implementations that are backed by databases other than CWMS
Oracle. For example, DSS or sourcing data from external agency web
sources.

Arguments passed to ResEvap by the shell script consist of both Java
Virtual Machine (JVM) arguments and Jython arguments. The JVM arguments
are described in .

Table 1-JVM arguments passed to the ResEvap Jython processor

+----------------------+-----------------------------------------------+
| JVM Argument         | Description                                   |
+======================+===============================================+
| cwms.officeId        | Three letter office ID                        |
+----------------------+-----------------------------------------------+
| java.library.path    | Path to compiled library dependencies         |
|                      | (javaHecLib.dll)                              |
+----------------------+-----------------------------------------------+
| python.path          | Path to the Python jars and libraries         |
+----------------------+-----------------------------------------------+
| DatasetVa            | Path to the parameter unit definition file    |
| lidParameterListFile |                                               |
+----------------------+-----------------------------------------------+
| UNIT_FILE            | Path to the unit conversion definition file   |
+----------------------+-----------------------------------------------+
| java                 | Path to the logging configuration file        |
| .util.logging.config |                                               |
+----------------------+-----------------------------------------------+
| cwm                  | Path to the JOOQ jars                         |
| s.db.impl.classpaths |                                               |
+----------------------+-----------------------------------------------+
| database.passwd      | Path to the database password file            |
+----------------------+-----------------------------------------------+
| database.name        | Name of the database                          |
+----------------------+-----------------------------------------------+

| The Jython script requires additional arguments beyond the JVM
  arguments. These arguments are the reservoir id (e.g. RES= PA18), the
  start time (e.g. START=2019-04-23T00:00:00) and the end time (e.g.
  END=2019-04-30T00:00:00). The start and end time should be specified
  in ISO 8601 format,
| https://www.iso.org/iso-8601-date-and-time-format.html.

In addition to these arguments, the Jython processor requires the
ResEvap properties file to be located in the current working directory,
and to be named “resevap.properties”. This file contains the version
name (used for building the output time series identifier), the working
directory that should be used when running ResEvap, and the location
level identifiers for obtaining the maximum temperature depth and secchi
depth. An example of this file is presented in Appendix 4. Additionally,
a file containing the Reservoir Information must be provided, an example
of which is shown in Appendix 4. Note that this file should have an
identical name to the RES python argument. This file contains the
latitude, longitude, time zone, rating curve, and input data time series
identifiers. Although the file contains latitude, longitude and time
zone information, these parameters are overridden by the location
information in the CWMS database. The current release of ResEvap
requires that there be a synchronization between the maximum temperature
depth location level, the number of water temperature profile time
series, and the elevation extents of the elevation;area rating curve.
The maximum temperature depth determines the number of required water
temperature profiles. The elevation;area curve’s elevation extents must
cover the all of the valid elevations that might occur given variance in
pool elevation. This must currently be manually configured through
setting the level value, establishing and identifiying the profile time
series in the database and the reservoir file, then editing the database
rating curve as needed. A recommendation has been made to calculate the
number water temperature profile time series using the depth level, this
is covered in Appendix 4: Recommendations.

ResEvap Jython Sequence of Operations
=====================================

Based on the information provided, the Jython processor populates the
ResEvap compute objects with the necessary parameters and meteorological
data to compute the evaporation, energy balance and water temperature
profile. This includes parsing the input files, loading time series,
location levels, and rating curves from the CWMS database, and building
the necessary compute objects. The Jython processor allows specification
of nearly all of the compute parameters, but the reservoir layer
thickness and meteorological measurement heights are hard-coded to be
0.5m and 10m, respectively. An exception to the reservoir layers being
hard coded to be 0.5m is the layer at the specified maximum depth, this
bottom layer will most likely be thinner than 0.5m to avoid exceeding
the maximum depth. An example of this would be a maximum depth of 40.3m.
The last layer would be 0.3m instead of 0.5m.

An important note is that the Jython processor attempts to retrieve a
value for each water temperature profile that is at the starting time.
In the event that it cannot retrieve a value from the time series, the
Jython processor will adjust the retrieval time to utilize the time
series’ interval offset, and again attempt to retrieve the initial
profile temperatures. If initial values are not retrieved by either
method, the compute will fail.

The Jython processor runs the compute with the information specified
above. After the ResEvap computations complete, the output time series
are parsed from the ResEvap compute objects. Although ResEvap creates
output data files, these are not accessed by the Jython processor. The
resulting time series are converted from Time Series Containers to CWMS
DataSetTx objects, and then stored to the CWMS database. The Time Series
Containers produced by ResEvap contain a DSS pathname, which is
transformed into the CWMS time series identifier when storing to the
database. This allows ResEvap to control the identifier for each output
time series, with only the office identifier, location and version being
customizable. The parameter, parameter type, time step, and units are
all pre-determined within ResEvap. The output time series include hourly
water surface temperature, sensible heat flux, latent heat flux, solar
radiation, downwelling longwave radiation, upwelling longwave radiation,
and evaporation, daily evaporation, evaporation as flow, and daily water
profile temperatures. Note that the water temperature profile values are
computed at an hourly time step, but only saved to the CWMS database as
a daily time series. After storing these time series to the CWMS
database, the database connection is closed, and the script is
completed.

Java Compute Routines
=====================

Core Routines
-------------

The Java entry point is a class called ResEvap. This class is tasked
with building the necessary compute objects, looping through each hourly
time step, organizing the results, and creating legacy text-based
results files. The ResEvap compute object is constructed by the Jython
processor, and subsequently provided with meteorological data and
reservoir information. Based on this information, ResEvap runs the
compute, at an hourly time-step, and then writes the output time series
to text files.

Computation of evaporation within ResEvap is a two-part process. These
computations are broken into the reservoir surface temperature
computations, through a full temperature profile model, and the
evaporation from the water surface. The temperature at the surface of
the reservoir is a key variable in estimating evaporation, but it is
rarely measured. Therefore, ResEvap must compute the temperature profile
within the reservoir, based on initial conditions and meteorological
observations. Temperature profile modeling in ResEvap assumes that the
vertical temperature profile is governed by the energy transfer through
the water surface, which requires that the inflows and outflows are
negligible components of the energy balance. This is appropriate for the
Missouri River watershed (Daly 2005), which ResEvap was designed for,
but this assumption should be carefully analyzed when applying to new
watersheds. Four heat transfer modes are computed: sensible, latent,
shortwave radiation and longwave radiation. Computation of these fluxes
are described in the “Evaporation Computations” and “Radiation
Computations” sections of this document. Based on these fluxes, and the
initial water temperature at each layer in the profile, the temperatures
are updated as described in the “Vertical Temperature Profile
Computations” section.

**Note: Appendix 1 has a definition of all variables in the
computations, and Appendix 2 has a definition of all the constants
used**.

Input Data
~~~~~~~~~~

Input data for ResEvap includes a combination of meteorological
observations and reservoir physical parameters. The time series
observations are provided in time series form, and are summarized in .
In this table, note that the cloud heights are estimated by ResEvap if
not provided, making them an optional parameter.

Table 2-Time series input data required for ResEvap computations

+-----------------------+--------------------+-------------+----------+
| Parameter             | Parameter Type     | Time Step   | Units    |
+=======================+====================+=============+==========+
| Wind Speed            | Instantaneous      | 1Hour       | .. ma    |
|                       |                    |             | th:: \fr |
|                       |                    |             | ac{m}{s} |
+-----------------------+--------------------+-------------+----------+
| Air Temperature       | Instantaneous      | 1Hour       | ..       |
|                       |                    |             | math:: { |
|                       |                    |             | ^\circ}C |
+-----------------------+--------------------+-------------+----------+
| Relative Humidity     | Instantaneous      | 1Hour       | .. m     |
|                       |                    |             | ath:: \% |
+-----------------------+--------------------+-------------+----------+
| Air Pressure          | Instantaneous      | 1Hour       | .. m     |
|                       |                    |             | ath:: mb |
+-----------------------+--------------------+-------------+----------+
| Low Cloud Fraction    | Instantaneous      | 1Hour       | .. m     |
|                       |                    |             | ath:: \% |
+-----------------------+--------------------+-------------+----------+
| Low Cloud Height      | Instantaneous      | 1Hour       | ..       |
|                       |                    |             | math:: m |
+-----------------------+--------------------+-------------+----------+
| Middle Cloud Fraction | Instantaneous      | 1Hour       | .. m     |
|                       |                    |             | ath:: \% |
+-----------------------+--------------------+-------------+----------+
| Middle Cloud Height   | Instantaneous      | 1Hour       | ..       |
|                       |                    |             | math:: m |
+-----------------------+--------------------+-------------+----------+
| High Cloud Fraction   | Instantaneous      | 1Hour       | .. m     |
|                       |                    |             | ath:: \% |
+-----------------------+--------------------+-------------+----------+
| High Cloud Height     | Instantaneous      | 1Hour       | ..       |
|                       |                    |             | math:: m |
+-----------------------+--------------------+-------------+----------+
| Reservoir Pool        | Instantaneous      | 1Hour       | ..       |
| Elevation             |                    |             | math:: m |
+-----------------------+--------------------+-------------+----------+
| Water Temperature     | Instantaneous      | 1Day        | ..       |
| (Each Layer)          |                    |             | math:: { |
|                       |                    |             | ^\circ}C |
+-----------------------+--------------------+-------------+----------+

In addition to time series data, ResEvap requires the start date, end
date, GMT offset, version name, latitude, longitude, observation heights
for wind speed, air temperature and relative humidity, and the
elevation-area rating curve. Note that ResEvap is not aware of vertical
datum info. All elevation input data must be supplied in the same
vertical datum.

Output Data
~~~~~~~~~~~

ResEvap produces both meteorological and water temperature information,
which is written to text files and returned to the Jython processor for
storage into the CWMS database. summarizes the time series data produced
by ResEvap.

Table 3-Output data produced by ResEvap

+-----------------------+--------------------+-------------+----------+
| Parameter             | Parameter Type     | Time Step   | Units    |
+=======================+====================+=============+==========+
| Solar Radiation       | Instantaneous      | 1Hour       | .        |
|                       |                    |             | . math:: |
|                       |                    |             |  \frac{W |
|                       |                    |             | }{m^{2}} |
+-----------------------+--------------------+-------------+----------+
| Downwelling Longwave  | Instantaneous      | 1Hour       | .        |
| Radiation             |                    |             | . math:: |
|                       |                    |             |  \frac{W |
|                       |                    |             | }{m^{2}} |
+-----------------------+--------------------+-------------+----------+
| Upwelling Longwave    | Instantaneous      | 1Hour       | .        |
| Radiation             |                    |             | . math:: |
|                       |                    |             |  \frac{W |
|                       |                    |             | }{m^{2}} |
+-----------------------+--------------------+-------------+----------+
| Water Surface         | Instantaneous      | 1Hour       | ..       |
| Temperature           |                    |             | math:: { |
|                       |                    |             | ^\circ}C |
+-----------------------+--------------------+-------------+----------+
| Sensible Heat Flux    | Instantaneous      | 1Hour       | .        |
|                       |                    |             | . math:: |
|                       |                    |             |  \frac{W |
|                       |                    |             | }{m^{2}} |
+-----------------------+--------------------+-------------+----------+
| Latent Heat Flux      | Instantaneous      | 1Hour       | .        |
|                       |                    |             | . math:: |
|                       |                    |             |  \frac{W |
|                       |                    |             | }{m^{2}} |
+-----------------------+--------------------+-------------+----------+
| Evaporation           | Instantaneous,     | 1Hour, 1Day | .. m     |
|                       | Cumulative         |             | ath:: mm |
+-----------------------+--------------------+-------------+----------+
| Evaporation as Flow   | Average            | 1Day        | .        |
|                       |                    |             | . math:: |
|                       |                    |             |  \frac{m |
|                       |                    |             | ^{3}}{s} |
+-----------------------+--------------------+-------------+----------+
| Water Temperature     | Instantaneous      | 1Hour       | ..       |
| (Each Layer)          |                    |             | math:: { |
|                       |                    |             | ^\circ}C |
+-----------------------+--------------------+-------------+----------+

ResEvap builds these output time series based on the input time window,
location and version name. As the compute progresses in time, the hourly
time series are filled with compute results. At the end of the
simulation, the evaporation is accumulated to daily, and then the
evaporation as flow is computed from the daily evaporation, and the
reservoir surface area. This is converted based on the following
equation:

:math:`{E_{f}}_{t} = E_{t}{A_{s}}_{t}`

Where :math:`{E_{f}}_{t}` is the evaporation as flow at time :math:`t`,
:math:`E_{t}` is the evaporation rate at time :math:`t`, and
:math:`{A_{s}}_{t}` is the reservoir surface area at time :math:`t`.

Time series data is saved to text files as well. Time series data is
reported in text files named "testtout_java.dat", "wtout_java.dat",
reporting the meteorological/surface flux and water temperature time
series information, respectively. The meteorological/surface flux
results file reports values at every hour for the wind speed, air
temperature, relative humidity, air pressure, water surface temperature,
:math:`u_{*}`, :math:`{R_{e}}^{*}`, Obhukov Length, sensible heat flux,
latent heat flux, solar radiation flux, downwelling longwave radiation
flux, upwelling longwave radiation flux, and evaporation. The water
temperature results file reports the temperature of every layer, for
every hour computed, in :math:`℃`.

ResEvap also saves diagnostic information to text files, which can be
used for debugging purposes. Reservoir profile and energy balance
reports are provided in files named "xout_java.dat" and
"xout2_java.dat", respectively. The reservoir profile information
includes the depth to each layer, the thickness of each layer, the area
of each layer, the elevation of each layer, and the volume of each
layer. The energy balance report contains the water surface elevation,
total thermal energy, the change in total thermal energy, the total
thermal energy input, the total thermal energy at the end of the time
step, the relative difference between the change in thermal energy and
the total (net) energy input (should be ~0), and the reservoir surface
area.

A recommendation has been added to Appendix 4: Recommendations, to
prevent the generation of these files unless ResEvap is being run in a
debug mode.

Evaporation Computations
------------------------

Evaporation computations are performed in the EvapWater class. The
evaporation computations rely on 8 input variables, water surface
temperature (:math:`T_{s}`), air temperature measurement
(:math:`\widehat{T_{a}}`), reference height of the air temperature
measurements (:math:`h_{T}`), relative humidity measurement
(:math:`\widehat{RH}`), reference height of the relative humidity
measurements (:math:`h_{q}`), wind speed measurement
(:math:`\widehat{u}`), reference height of the wind speed measurements
(:math:`h_{u}`), the measured air pressure (:math:`\widehat{p_{a}}`) and
latent heat of vaporization (:math:`l_{v}`). Note that all variables are
described in Appendix 1, and all variables with a :math:`\widehat{\ }`
accent are observed data. From these variables, an iterative computation
is performed to produce the output variables: sensible heat
(:math:`H_{s}`), latent heat (:math:`H_{l}`), and evaporation
(:math:`E`). Iterations are required due to the implicit definition of
the turbulent transfer coefficients, where the exchanges of momentum,
energy and mass are codependent with the Obukhov length (:math:`l_{o}`).
Therefore, the computations setup initial estimates of the transfer
coefficients (:math:`C_{D}` for wind, :math:`C_{T}` for temperature and
:math:`C_{q}` for humidity) then estimate the Obukhov length, and
iteratively recompute the turbulent exchange scales (:math:`u_{*}` for
wind, :math:`t_{*}` for temperature and :math:`q_{*}` for humidity)
until convergence. Based on the turbulent exchange values, the resulting
evaporation, sensible heat and latent heat may be computed as follows:

:math:`H_{l} = - \rho_{a}l_{v}u_{*}q_{*}`

:math:`H_{s} = - \rho_{a}c_{p}^{T_{s}}u_{*}t_{*}`

.. math:: E = \frac{H_{l}}{l_{v}\rho_{w}}(86400\frac{s}{day}10^{3}\frac{mm}{m})

Static Variables
~~~~~~~~~~~~~~~~

Evaporation computations start by computing several values that are
static across the iterative algorithm. These include the vertically
averaged air temperature (:math:`\overline{T_{a}}`), the potential
temperature (:math:`\theta_{r}`), the vertically averaged specific
humidity (:math:`\overline{q}`), the density of the air
(:math:`\rho_{a}`), and the kinematic viscosity (:math:`\nu_{s}`).
Additionally, the :math:`\mathrm{\Delta}_{t}`\ and
:math:`\mathrm{\Delta}_{q}` terms are computed, which represent
differences in temperature and specific humidity required for computing
the Monin-Obukhov similarity scaling parameters. These initial
computations are described in the equations below:

:math:`\overline{T_{a}} = .5\left( T_{s} - \widehat{T_{a}} \right)`

:math:`\mathrm{\Delta}_{t} = T_{s} - \theta_{r}`

Where :math:`\theta_{r}` is the potential temperature, as computed
below:

.. math:: \theta_{r} = \widehat{T_{a}} + \frac{g}{c_{p}^{\widehat{T_{a}}}}h_{t}

:math:`c_{p}^{T} = 1005.60\  + (T - T_{FP})(0.017211\  + \ 0.000392(T - T_{FP}))`

Where :math:`g` is the gravitational acceleration, :math:`T_{FP}` is the
freezing point in Kelvin, and :math:`c_{p}^{T}` is the specific heat of
air based on reference temperature :math:`T`. In the above formulation
:math:`c_{p}^{T}` is only valid for the range
:math:`- 233.15K < T < 313.15K`, which is will only rarely be exceeded
for surface reservoirs within the USA.

:math:`\mathrm{\Delta}_{q} = q_{s} - q_{r}`

.. math:: q = \frac{\rho_{v}}{\rho_{a}}\ 

:math:`\rho_{a} = \rho_{d} + \rho_{v} = \frac{100e(1 - .000537*S)m_{w}}{R_{g}T_{a}} + 1.2923(\frac{T_{FP}}{T_{a}})(\frac{\widehat{p_{a}}}{1013.25})`

Where :math:`\rho_{a}` is the density of the air at the water surface,
:math:`\rho_{d}` is the density of dry air, :math:`\rho_{v}` is the
water vapor density, :math:`R_{g}` is the ideal gas constant, :math:`e`
is the vapor pressure, :math:`S` is the salinity (assumed to be zero),
:math:`m_{w}` is the molecular weight of water, and :math:`q_{s}` is
solved by setting :math:`T_{a} = T_{s}` and :math:`RH = 1`, and
:math:`q_{r}` is computed by setting :math:`T_{a} = \widehat{T_{a}}` and
:math:`RH = \widehat{RH}`.

:math:`e_{s} = \left\{ \begin{matrix}
{\left( .00000346\widehat{p_{a}} + 1.0007 \right)6.1121e}^{(\frac{17.502{(T}_{a} - T_{FP})}{240.97 + {(T}_{a} - T_{FP})})} & over\ water \\
{\left( .00000418\widehat{p_{a}} + 1.0003 \right)6.1115e}^{(\frac{22.452{(T}_{a} - T_{FP})}{272.55 + {(T}_{a} - T_{FP})})} & over\ ice \\
\end{matrix} \right.\ `

:math:`e = \widehat{\frac{RH}{100}}e_{s}`

Where :math:`e_{s}` is the saturation vapor pressure, and :math:`e` is
the actual vapor pressure.

Additionally, the following computations require the kinematic viscosity
of the air at the water surface, which is described below.

.. math:: \nu_{s} = .00001326(1.0\  + \ T_{s}*(.006542\  + \ T_{s}*(.000008301\  - \ .00000000484T_{s})))

Finally, the latent heat of vaporization or sublimation is needed for
computing the latent heat flux, which is described below.

.. math::

   l_{v} = \left\{ \begin{matrix}
   \left( 28.34\  - \ 0.00149\left( T_{s} - T_{k} \right) \right)10^{5} & T_{s} < T_{FP} \\
   \left( 25\  - \ 0.02274\left( T_{s} - T_{k} \right) \right)10^{5} & T_{s} \geq T_{FP} \\
   \end{matrix} \right.\ 

Based on these static variables, the iterative solution of the
evaporation can begin.

Iterative Computations
~~~~~~~~~~~~~~~~~~~~~~

After computation of the initial variables, an initial iteration is
performed to estimate the Monin-Obukhov similarity (MOS) scaling
parameters (:math:`u_{*}`, :math:`T_{*}` and\ :math:`\ q_{*}`), which
represent the turbulent exchanges of latent and sensible heat
(:math:`H_{l}` and :math:`H_{s}`). These initial estimates assume
neutral stratification (i.e :math:`\frac{h_{u}}{l_{o}} = 0`). Estimating
these parameters requires an initial estimate of the wind friction
velocity (:math:`u_{*}`), as shown below:

.. math:: u_{*} = \widehat{u}\sqrt{C_{d}}

Where the drag coefficient (:math:`C_{d}`) is initially estimated as:

.. math:: {C_{d}}_{0} = (0.37\  + \ 0.137\widehat{u})10^{- 3}

Note that the shear velocity is not allowed to drop below 0.01. The
remaining computations require roughness lengths for momentum
(:math:`z_{u}`), temperature (:math:`z_{T}`) and humidity
(:math:`z_{q}`), which are estimated by the COARE algorithm (Fairall et
al., 1996).

.. math:: z_{u} = h_{u}e^{\frac{- \kappa\ }{\sqrt{C_{d}}}} + C_{s}\frac{\nu_{s}}{\ u_{*}}

:math:`z_{T} = a_{t}\frac{\nu_{s}}{u_{*}}{{R_{e}}^{*}}^{b_{t}}`

:math:`z_{q} = a_{q}\frac{\nu_{s}}{u_{*}}{{R_{e}}^{*}}^{b_{q}}`

Where :math:`C_{s}` is the smooth surface coefficient, *𝜅* is the von
Karman constant, :math:`{R_{e}}^{*}` is the roughness Reynolds number
defined below, and the COARE algorithm coefficients
(:math:`a_{t},\ b_{t},\ a_{q},\ b_{q}`) are performed with a table
lookup based on :math:`{R_{e}}^{*}` (see Table 4).

:math:`{R_{e}}^{*} = \frac{\ u_{*}z_{u}}{\nu_{s}}`

Table 4-Coefficients for the COARE algorithm

+-------------+-------------+-------------+-------------+-------------+
| +--------+  | +--------+  | +--------+  | +--------+  | +--------+  |
| | .. mat |  | | ..     |  | | ..     |  | | ..     |  | | ..     |  |
| | h:: {\ |  | | math:: |  | | math:: |  | | math:: |  | | math:: |  |
| | mathbf |  | |  \math |  | |  \math |  | |  \math |  | |  \math |  |
| | {R}_{\ |  | | bf{a}_ |  | | bf{b}_ |  | | bf{a}_ |  | | bf{b}_ |  |
| | mathbf |  | | {\math |  | | {\math |  | | {\math |  | | {\math |  |
| | {e}}}^ |  | | bf{t}} |  | | bf{t}} |  | | bf{q}} |  | | bf{q}} |  |
| | {\math |  | +========+  | +========+  | +========+  | +========+  |
| | bf{*}} |  | +--------+  | +--------+  | +--------+  | +--------+  |
| +========+  |             |             |             |             |
| +--------+  |             |             |             |             |
+=============+=============+=============+=============+=============+
| 0.135       | 0.177       | 0           | 0.292       | 0           |
+-------------+-------------+-------------+-------------+-------------+
| 0.16        | 1.376       | 0.929       | 1.808       | 0.826       |
+-------------+-------------+-------------+-------------+-------------+
| 1           | 1.376       | 0.929       | 1.808       | 0.826       |
+-------------+-------------+-------------+-------------+-------------+
| 3           | 1.026       | -0.599      | 1.393       | -0.528      |
+-------------+-------------+-------------+-------------+-------------+
| 10          | 1.625       | -1.018      | 1.956       | -0.87       |
+-------------+-------------+-------------+-------------+-------------+
| 30          | 4.661       | -1.475      | 4.994       | -1.297      |
+-------------+-------------+-------------+-------------+-------------+
| 100         | 34.904      | -2.067      | 30.709      | -1.845      |
+-------------+-------------+-------------+-------------+-------------+
| 300         | 1667.19     | -2.907      | 1448.68     | -2.682      |
+-------------+-------------+-------------+-------------+-------------+
| 1000        | 5.88E+05    | -3.935      | 2.98E+05    | -3.616      |
+-------------+-------------+-------------+-------------+-------------+

Based on the roughness lengths, the transfer coefficients can be
computed as follows:

:math:`C_{m} = \frac{{\kappa\ }^{2}}{\left( \ln\left( \frac{h_{u}}{h_{m}} \right) - \psi_{m} \right)(\ln\left( \frac{z_{0}}{z_{m}} \right) - \psi_{m})}`

where

:math:`h_{m}`\ =\ :math:`h_{u}`,\ :math:`\ z_{m}`\ =\ :math:`z_{u}`,
:math:`\psi_{m} = \psi_{u}` for :math:`C_{D}`

:math:`h_{m}`\ =\ :math:`h_{T}`,\ :math:`\ z_{m}`\ =\ :math:`z_{T}`,
:math:`\psi_{m} = \psi_{T}` for :math:`C_{T}`

:math:`h_{m}`\ =\ :math:`h_{q}`,\ :math:`\ z_{m}`\ =\ :math:`z_{q}`,
:math:`\psi_{m} = \psi_{q}` for :math:`C_{q}`

:math:`\psi_{m} = \left\{ \begin{matrix}
\begin{matrix}
2ln\left( 0.5(1\  + x) \right) + 2ln\left( 0.5\left( 1\  + x^{2} \right) \right) - 2\tan^{- 1}(x) + 1.570796 \\
2ln(0.5*(1\  + x^{2})) \\
\end{matrix} & \begin{matrix}
\zeta < 0 & m = u \\
\zeta < 0 & m = T\ or\ q \\
\end{matrix} \\
0 & \zeta = 0 \\
\begin{matrix}
 - (0.7\zeta + \ 0.75(\zeta - \ 14.3)e^{- .35\zeta} + \ 10.7) \\
 - (0.7\zeta + \ 10.7) \\
\end{matrix} & \begin{matrix}
\zeta \leq 250 \\
\zeta > 250 \\
\end{matrix} \\
\end{matrix} \right.\ `

:math:`\zeta = \frac{h_{m}}{l_{o}}`

:math:`{x = (1 - \ 16\zeta)}^{.25}`

Where :math:`\psi_{m} = 0` for the initial iteration. From the above
equations, the initial MOS scaling parameters can be computed as
follows:

.. math:: t_{*} = - \frac{C_{T}\widehat{u}\mathrm{\Delta}_{T}}{u_{*}}

.. math:: q_{*} = - \frac{C_{q}\widehat{u}\mathrm{\Delta}_{q}}{u_{*}}

The final step in the first iteration is to compute the Obukhov length
(:math:`l_{o}`) as follows:

.. math:: l_{o} = \frac{\frac{\overline{T_{a}}u_{*}\ }{kg}}{t_{*} + (\frac{.61\overline{T_{a}}q_{*}}{1 + .61\overline{q}})}

W ith these initial estimates, the evaporation routine will begin
iteratively estimating the MOS similarity scales, where a maximum of 20
iterations will be performed. The stopping criteria of the process is
when

.. math:: \frac{\left| {u_{*}}_{i} - {u_{*}}_{i - 1} \right|}{{u_{*}}_{i}} < .001\ and\ \frac{\left| {t_{*}}_{i} - {t_{*}}_{i - 1} \right|}{{t_{*}}_{i}} < .001\ and\ \frac{\left| {q_{*}}_{i} - {q_{*}}_{i - 1} \right|}{{q_{*}}_{i}} < .001

Where :math:`i` denotes the iteration number. The iterations proceed as
follows. Compute the transfer coefficients (:math:`C_{D}`, :math:`C_{T}`
and :math:`C_{q}`) with :math:`h_{u} = 10m`, and current estimates of
:math:`l_{o}`, :math:`z_{u}`, :math:`z_{T}` and :math:`z_{q}`, and
subsequently estimate the MOS similarity scales. Recompute the transfer
coefficients based on the current MOS similarity scales and the actual
:math:`h_{u}`. Modify wind speed to account for gustiness as shown
below.

.. math::

   u = \left\{ \begin{matrix}
   \sqrt{{\widehat{u}}^{2} + {1.25}^{2}\left( u_{*}\left( \frac{- 600.0}{\kappa\ l_{o}} \right)^{\frac{1}{3}} \right)^{2}} & unstable\ stratification\ (l_{o} < 0) \\
   \widehat{u} + .5 & stable\ stratification\ (0 \leq l_{o} < 1000) \\
   \widehat{u} & neutral\ stratification\ (l_{o} \leq 1000) \\
   \end{matrix} \right.\ 

Finally recompute the MOS similarity scales and the Obukhov length, then
apply the convergence test. After the interative process is completed,
compute the sensible heat, latent heat and evaporative fluxes.

Radiation Computations
----------------------

Shortwave Radiation
~~~~~~~~~~~~~~~~~~~

Solar radiation provides energy to the water surface during daylight
hours, and is therefore a key component of the energy balance. The
intensity of solar radiation reaching the water surface is a function of
both the zenith angle of the sun, and the extent to which the atmosphere
obscures radiation. The zenith angle is affected by both seasonal and
diurnal cycles, as well as the latitude (:math:`\varphi`) of the
reservoir. All computations of solar angles are based on Woolf (1968).
Seasonal affects on the solar radiation are represented by the
declination angle (:math:`\delta`), which ranges from -23.44 to 23.44.
Computations of the declination angle requires the below equation, which
converts the day of year to an angle:

:math:`d = \frac{360}{365.242}(JD - 1)`

Where :math:`JD` is the Julian day, with :math:`JD = 1` on January
1\ :sup:`st`. This can be converted to the declination angle below:

:math:`\sin(\delta) = sin(23.44)sin(279.9348 + \ d + 1.914827\sin(d) - 0.079525\cos(d) + \ \ \ \ \ \ \ \ \ \ \ \ \ \ \ \ \ \ 0.019938(2\sin(d)\cos(d)) - 0.001639(2\cos^{2}(d) - 1)`

The diurnal fluctuations of solar radiation are represented by the Hour
Angle (:math:`h_{s}`), as computed below:

:math:`h_{s} = 15\left( h_{gmt} - M \right) - lon`

Where :math:`h_{gmt}` is the hour of the day in GMT, :math:`lon` is the
longitude, and :math:`M` is the time of meridian passage computed below.

:math:`M = 12 + 0.12357\sin(d) - 0.004289\cos(d) + 0.153809\left( 2\sin(d)\cos(d) \right) + 0.060783(2\cos^{2}(d) - 1)`

Based on the declination, the latitude and the hour angle, the zenith
angle may be computed as follows:

:math:`\cos\left( \theta_{s} \right) = \sin(\varphi)\sin(\delta) + cos(\varphi)\cos{(\delta)cos(h_{s}})`

:math:`\theta_{s} = \cos^{- 1}(\cos\left( \theta_{s} \right))`

Based on the zenith angle, and the cloud cover fraction at the low,
middle and high layers of the atmosphere, the solar radiation reaching
the water surface is computed based on Shapiro (1987). In this document,
the derivation of the general case to the 3-layer implementation is not
provided, due to it’s complexity. For information on this derivation,
see Shapiro (1987) This is strictly the equation for the 3-layer case
used in ResEvap.

:math:`I_{s \downarrow} = \frac{S_{e}T_{l}T_{m}T_{h}}{d_{l}\left( d_{h}d_{m} - R_{h}R_{l}{T_{m}}^{2} \right) - d_{h}R_{m}R_{w}{T_{l}}^{2} - R_{h}R_{w}{T_{m}}^{2}{T_{l}}^{2}}`

Where :math:`I_{s \downarrow}` is the incoming solar radiation at the
water surface, :math:`T_{l}`, :math:`T_{m}`, and :math:`T_{h}` are the
transmissivities of the low, middle and high atmospheric layers,
:math:`R_{l}`, :math:`R_{m}`, and :math:`R_{h}` are the reflectance of
the low middle and high atmospheric layers, :math:`d_{l}`,
:math:`d_{m}`, and :math:`d_{h}` are the interactions between the
different layers and :math:`S_{e}` is the extraterrestrial solar
radiation on a horizontal plane in :math:`\frac{W}{m^{2}}`.

:math:`d_{h} = 1 - R_{h}R_{m}`

:math:`d_{m} = 1 - R_{m}R_{l}`

:math:`d_{l} = 1 - R_{l}R_{g}`

:math:`S_{e} = 1369.2\left( 1.0001399 + 0.0167261cos(\frac{2\pi(JD - 2)}{365.242}) \right)^{2}\cos\left( \theta_{s} \right)`

In the above equations, :math:`R_{k}` and :math:`T_{k}` are a composite
of the overcast (:math:`R_{k}^{o}`,\ :math:`\ T_{k}^{o}`) and clear sky
(:math:`R_{k}^{c}`, :math:`T_{k}^{c}`) values, where a weight is
determined based on the zenith angle and the fractional cloud cover
(:math:`{f_{c}}_{k}`) in e ach layer :math:`k`, and coefficients from
Table 5, Table 6, Table 7, Table 8, Table 9.

:math:`R_{k} = W_{k}R_{k}^{o} + \left( 1 - W_{k} \right)R_{k}^{c}`

:math:`T_{k} = W_{k}T_{k}^{o} + \left( 1 - W_{k} \right)T_{k}^{c}`

:math:`R_{k}^{c} = {r_{k}^{c}}_{0} + {r_{k}^{c}}_{1}\cos\left( \theta_{s} \right) + {r_{k}^{c}}_{2}{\cos\left( \theta_{s} \right)}^{2} + {r_{k}^{c}}_{3}{\cos\left( \theta_{s} \right)}^{3}`

:math:`R_{k}^{o} = {r_{k}^{o}}_{0} + {r_{k}^{o}}_{1}\cos\left( \theta_{s} \right) + {r_{k}^{o}}_{2}{\cos\left( \theta_{s} \right)}^{2} + {r_{k}^{o}}_{3}{\cos\left( \theta_{s} \right)}^{3}`

:math:`T_{k}^{c} = {t_{k}^{c}}_{0} + {t_{k}^{c}}_{1}\cos\left( \theta_{s} \right) + {t_{k}^{c}}_{2}{\cos\left( \theta_{s} \right)}^{2} + {t_{k}^{c}}_{3}{\cos\left( \theta_{s} \right)}^{3}`

:math:`T_{k}^{o} = {t_{k}^{o}}_{0} + {t_{k}^{o}}_{1}\cos\left( \theta_{s} \right) + {t_{k}^{o}}_{2}{\cos\left( \theta_{s} \right)}^{2} + {t_{k}^{o}}_{3}{\cos\left( \theta_{s} \right)}^{3}`

:math:`W_{k} = \left\{ \begin{matrix}
0 & f_{c} < .05 \\
1 & f_{c} > .95 \\
{c_{k}}_{o} + {c_{k}}_{1}\cos\left( \theta_{s} \right) + {c_{k}}_{2}{f_{c}}_{k} + {c_{k}}_{3}\cos\left( \theta_{s} \right){f_{c}}_{k} + {c_{k}}_{4}{\cos\left( \theta_{s} \right)}^{2} + {c_{k}}_{5}{{f_{c}}_{k}}^{2} & otherwise \\
\end{matrix} \right.\ `

Table 5-Coefficients for clear sky reflectivity computations

+-------------+-------------+-------------+-------------+-------------+
|             | .. mat      | .. mat      | .. mat      | .. mat      |
|             | h:: {\mathb | h:: {\mathb | h:: {\mathb | h:: {\mathb |
|             | f{r}_{\math | f{r}_{\math | f{r}_{\math | f{r}_{\math |
|             | bf{k}}^{\ma | bf{k}}^{\ma | bf{k}}^{\ma | bf{k}}^{\ma |
|             | thbf{c}}}_{ | thbf{c}}}_{ | thbf{c}}}_{ | thbf{c}}}_{ |
|             | \mathbf{0}} | \mathbf{1}} | \mathbf{2}} | \mathbf{3}} |
+=============+=============+=============+=============+=============+
| Low         | .15946      | -.42185     | .48800      | -.18492     |
+-------------+-------------+-------------+-------------+-------------+
| Mid         | .15325      | -.39620     | .42095      | -.14200     |
+-------------+-------------+-------------+-------------+-------------+
| High        | .12395      | -.34765     | .39478      | -.14627     |
+-------------+-------------+-------------+-------------+-------------+

Table 6-Coefficients for the clear sky transmissivity computations

+-------------+-------------+-------------+-------------+-------------+
|             | .. mat      | .. mat      | .. mat      | .. mat      |
|             | h:: {\mathb | h:: {\mathb | h:: {\mathb | h:: {\mathb |
|             | f{t}_{\math | f{t}_{\math | f{t}_{\math | f{t}_{\math |
|             | bf{k}}^{\ma | bf{k}}^{\ma | bf{k}}^{\ma | bf{k}}^{\ma |
|             | thbf{c}}}_{ | thbf{c}}}_{ | thbf{c}}}_{ | thbf{c}}}_{ |
|             | \mathbf{0}} | \mathbf{1}} | \mathbf{2}} | \mathbf{3}} |
+=============+=============+=============+=============+=============+
| Low         | .68679      | .71012      | -.71463     | .22339      |
+-------------+-------------+-------------+-------------+-------------+
| Mid         | .69318      | .68227      | -.64289     | .17910      |
+-------------+-------------+-------------+-------------+-------------+
| High        | .76977      | .49407      | -.44647     | .11558      |
+-------------+-------------+-------------+-------------+-------------+

Table 7-Coefficients for the overcast reflectivity computations

+-------------+-------------+-------------+-------------+-------------+
|             | .. mat      | .. mat      | .. mat      | .. mat      |
|             | h:: {\mathb | h:: {\mathb | h:: {\mathb | h:: {\mathb |
|             | f{r}_{\math | f{r}_{\math | f{r}_{\math | f{r}_{\math |
|             | bf{k}}^{\ma | bf{k}}^{\ma | bf{k}}^{\ma | bf{k}}^{\ma |
|             | thbf{o}}}_{ | thbf{o}}}_{ | thbf{o}}}_{ | thbf{o}}}_{ |
|             | \mathbf{0}} | \mathbf{1}} | \mathbf{2}} | \mathbf{3}} |
+=============+=============+=============+=============+=============+
| Low         | .69143      | -.14419     | -.05100     | .06682      |
+-------------+-------------+-------------+-------------+-------------+
| Mid         | .61394      | -.01469     | -.17400     | .14215      |
+-------------+-------------+-------------+-------------+-------------+
| High        | .42111      | -.04002     | -.51833     | .40540      |
+-------------+-------------+-------------+-------------+-------------+

Table 8-Coefficients for the overcast transmissivity computations

+-------------+-------------+-------------+-------------+-------------+
|             | .. mat      | .. mat      | .. mat      | .. mat      |
|             | h:: {\mathb | h:: {\mathb | h:: {\mathb | h:: {\mathb |
|             | f{t}_{\math | f{t}_{\math | f{t}_{\math | f{t}_{\math |
|             | bf{k}}^{\ma | bf{k}}^{\ma | bf{k}}^{\ma | bf{k}}^{\ma |
|             | thbf{o}}}_{ | thbf{o}}}_{ | thbf{o}}}_{ | thbf{o}}}_{ |
|             | \mathbf{0}} | \mathbf{1}} | \mathbf{2}} | \mathbf{3}} |
+=============+=============+=============+=============+=============+
| Low         | .15785      | .32410      | -.14458     | .01457      |
+-------------+-------------+-------------+-------------+-------------+
| Mid         | .23865      | .20143      | -.01183     | -.07892     |
+-------------+-------------+-------------+-------------+-------------+
| High        | .43562      | .26094      | .36428      | -.38556     |
+-------------+-------------+-------------+-------------+-------------+

Table 9-Coefficients for the clear sky and overcast weighting
computations

+---------+---------+---------+---------+---------+--------+--------+
|         | .. mat  | .. mat  | .. mat  | .. mat  | .. mat | .. mat |
|         | h:: {\m | h:: {\m | h:: {\m | h:: {\m | h:: {\ | h:: {\ |
|         | athbf{c | athbf{c | athbf{c | athbf{c | mathbf | mathbf |
|         | }_{\mat | }_{\mat | }_{\mat | }_{\mat | {c}_{\ | {c}_{\ |
|         | hbf{k}} | hbf{k}} | hbf{k}} | hbf{k}} | mathbf | mathbf |
|         | }_{\mat | }_{\mat | }_{\mat | }_{\mat | {k}}}_ | {k}}}_ |
|         | hbf{o}} | hbf{1}} | hbf{2}} | hbf{3}} | {\math | {\math |
|         |         |         |         |         | bf{4}} | bf{5}} |
+=========+=========+=========+=========+=========+========+========+
| low     | 1.512   | -1.176  | -2.160  | 1.420   | -0.032 | 1.422  |
+---------+---------+---------+---------+---------+--------+--------+
| mid     | 1.429   | -1.207  | -2.008  | 0.853   | 0.324  | 1.582  |
+---------+---------+---------+---------+---------+--------+--------+
| high    | 1.552   | -1.957  | -1.762  | 2.067   | 0.448  | 0.932  |
+---------+---------+---------+---------+---------+--------+--------+

Longwave Radiation
~~~~~~~~~~~~~~~~~~

Longwave radiation both adds and removes energy from the reservoir.
Outgoing longwave radiation (:math:`I_{l \uparrow})` is the energy
emitted by the reservoir, representing a loss of energy, and
:math:`T_{s}` is a function of the water surface temperature, as shown
in the equation below:

:math:`I_{l \uparrow} = \varepsilon_{w}\sigma{T_{s}}^{4}`

Where :math:`\sigma` is the Stefan-Boltzmann constant and
:math:`\varepsilon_{w}` is the emissivity of water.

Incoming longwave radiation (:math:`I_{l \downarrow}`) is radiation
emitted by the atmosphere that reaches the water surface. Within
ResEvap, the incoming longwave radiation is computed as the sum of the
clear sky component (:math:`{I_{l \downarrow}}_{clear}`) and the cloud
component (:math:`{I_{l \downarrow}}_{cloud}`).

:math:`I_{l \downarrow} = {I_{l \downarrow}}_{clear} + {I_{l \downarrow}}_{cloud}`

The clear sky component is a function of the emissivity of the
atmosphere (:math:`\varepsilon_{atm}`), and the measured air
temperature:

:math:`{I_{l \downarrow}}_{clear} = \varepsilon_{atm}\sigma{\widehat{T_{a}}}^{4}`

Where the emissivity of the atmosphere is a function of the vapor
pressure of the atmosphere (:math:`e_{a}`) and measured air temperature,
based on Crawford et al. (1999):

:math:`\varepsilon_{atm} = 1.24\left( \frac{{\ e}_{a}}{\widehat{T_{a}}} \right)^{\frac{1}{7}}`

Similar to the evaporation computations, the vapor pressure is a
function of the saturation vapor pressure and the relative humidity:

:math:`{\ e}_{a} = \widehat{RH}*e_{s}`

Unlike the evaporation computations, the saturation vapor pressure is
computed with the Clausius-Clapeyron equation:

:math:`e_{s} = 6.13e^{\frac{l_{v}}{R_{v}}\left( \frac{1}{T_{k}} - \frac{1}{\widehat{\widehat{T_{a}}}} \right)}`

Where :math:`l_{v}` is the latent heat of vaporization, :math:`R_{v}` is
the gas constant for water vapor (461 :math:`\frac{J}{kg*K}`). Note that
this is different than the formulation of saturation vapor pressure used
in the evaporation computations. This difference is likely a result of
the radiation model not using air pressure, but the differing
computations is expected to have negligible effects on the resulting
longwave radiation computations.

:math:`l_{v} = \left( 3.166659 - .00243\widehat{T_{a}} \right)10^{6}`

Similar to :math:`e_{s}`, the formulation of :math:`l_{v}` is different
than in the evaporation computations. To be numerically equivalent, the
equation would be
:math:`l_{v} = \left( 3.1211431 - .002274\widehat{T_{a}} \right)10^{6}`.
Although different, this is still expected to have negligible impacts on
the resulting longwave radiation computations.

The incoming longwave radiation from the cloud component of the
atmosphere is a function of the cloud cover in each layer
(:math:`{f_{c}}_{k}`) and the height of the clouds in each layer
(:math:`{h_{c}}_{k}`), as shown below:

:math:`{I_{l \downarrow}}_{cloud} = {f_{c}}_{l}\left( 94\  - \ 5.8{h_{c}}_{l} \right) + {f_{c}}_{m}(1 - {f_{c}}_{l})\left( 94\  - \ 5.8{h_{c}}_{m} \right) +`
:math:`{f_{c}}_{h}(1 - {f_{c}}_{m})(1 - {f_{c}}_{l})\left( 94\  - \ 5.8{h_{c}}_{h} \right)`

:math:`{h_{c}}_{k} = \left( \begin{matrix}
{h_{c}}_{k}\ (observed) & observed\ height\ available \\
a\  - \ b*(1.0\  - \left| \ cos(c*(lat\  - \ d)) \right|) & otherwise \\
\end{matrix} \right.\ `

Table 10, Table 11, Table 12, Table 13 provide the coefficients for
computing the cloud heights in the absence of observations.

Table 10-Cloud height coefficients for winter and latitude<25

+-------------+-------------+-------------+-------------+-------------+
|             | a           | b           | c           | d           |
+=============+=============+=============+=============+=============+
| low         | 1.05        | 0.6         | 5.0         | 25.0        |
+-------------+-------------+-------------+-------------+-------------+
| mid         | 4.1         | 0.3         | 4.0         | 25.0        |
+-------------+-------------+-------------+-------------+-------------+
| high        | 7.          | 1.5         | 3.0         | 30.0        |
+-------------+-------------+-------------+-------------+-------------+

Table 11-Cloud height coefficients for winter and latitude>25

+-------------+-------------+-------------+-------------+-------------+
|             | a           | b           | c           | d           |
+=============+=============+=============+=============+=============+
| low         | 1.05        | 0.6         | 1.5         | 25.0        |
+-------------+-------------+-------------+-------------+-------------+
| mid         | 4.1         | 2.0         | 1.7         | 25.0        |
+-------------+-------------+-------------+-------------+-------------+
| high        | 7.          | 1.5         | 3.0         | 30.0        |
+-------------+-------------+-------------+-------------+-------------+

Table 12-Cloud height coefficients for not winter season and latitude<25

+-------------+-------------+-------------+-------------+-------------+
|             | a           | b           | c           | d           |
+=============+=============+=============+=============+=============+
| low         | 1.15        | 0.45        | 5.0         | 25.0        |
+-------------+-------------+-------------+-------------+-------------+
| mid         | 4.1         | 2.0         | 1.7         | 25.0        |
+-------------+-------------+-------------+-------------+-------------+
| high        | 7.          | 1.5         | 3.0         | 30.0        |
+-------------+-------------+-------------+-------------+-------------+

Table 13-Cloud height coefficients for non-winter season and latitude>25

+-------------+-------------+-------------+-------------+-------------+
|             | a           | b           | c           | d           |
+=============+=============+=============+=============+=============+
| low         | 1.15        | 0.6         | 1.5         | 25.0        |
+-------------+-------------+-------------+-------------+-------------+
| mid         | 4.4         | 1.2         | 3.0         | 25.0        |
+-------------+-------------+-------------+-------------+-------------+
| high        | 7.          | 1.5         | 3.0         | 30.0        |
+-------------+-------------+-------------+-------------+-------------+

Vertical Temperature Profile Computations
-----------------------------------------

Vertical transfer of heat within a reservoir is assumed to be a
one-dimensional process, where the reservoir is assumed to be laterally
homogeneous. This allows for ignoring effects of reservoir inflows and
outflows. In the event that there is a large lateral variation in
temperature (i.e. long run-of-the-river reservoirs), these computations
will be unreliable. Based on this assumption, vertical transfer of heat
is modeled first by assuming stable reservoir stratification, accounting
for diffusion of heat, and then accounting for any convective or
turbulent mixing that occurs in the reservoir profile. Vertical
diffusion of heat within a one-dimensional reservoir is governed by the
equation below (Hondzo and Stefan 1993).

:math:`\frac{dT_{w}}{dt} = \frac{1}{A}\frac{d}{dz}\left( K_{z}A\frac{dT_{w}}{dz} \right) + \frac{I_{z}}{\rho_{w}c_{p}}`

:math:`T_{w}` is the water temperature in K, :math:`A` is the area
through which the heat is transferred, :math:`K_{z}` is the thermal
diffusivity, :math:`z` is the depth, :math:`I_{z}` is the net radiation,
:math:`\rho_{w}` is the density or water, and :math:`c_{p}` is the heat
capacity. In order to initialize the computations, the density and heat
capacity must be updated for each layer.

:math:`{\rho_{w}}_{i} = 1000 - .019549\left| {T_{w}}_{i} - 277.15 \right|^{1.68}`

:math:`{c_{p}}_{i} = 4174.9 + 1.6659\left( e^{\frac{307.65 - {T_{w}}_{i}}{10.6}} + e^{- \frac{307.65 - {T_{w}}_{i}}{10.6}} \right)`

In the above equations, :math:`i` is the index of the layer, where
:math:`i = 1` is the bottom layer of the temperature profile. Next the
thermal diffusivity is computed for each layer as follows.

:math:`{K_{z}}_{i} = .00012\left( .000817{A_{s}}^{.56}\left( {N_{i}}^{2} \right)^{- .43} \right)`

:math:`{N_{i}}^{2} = max(.00007,\ \frac{g}{\overline{\rho_{w}}}\frac{{\rho_{w}}_{i} - {\rho_{w}}_{i - 1}}{z_{i} - z_{i - 1}})`

:math:`\overline{\rho_{w}} = \frac{\sum_{i = 1}^{N}{{\rho_{w}}_{i}V_{i}}}{\sum_{i = 1}^{N}V_{i}}`

Where :math:`\overline{\rho_{w}}` is the average density over the entire
water column, :math:`z_{i}` is the depth of the top of layer :math:`i`,
:math:`N_{i}` is the stability frequency of layer :math:`i`, and
:math:`A_{s}` is the water surface area. Note that
:math:`\overline{\rho_{w}}` is computed as a volumetric average, but
should be the vertical average since this is a one-dimensional model.
Additionally, the net radiation of layer :math:`i` is computed as
follows.

:math:`{I_{z}}_{i} = \left\{ \begin{matrix}
\left( I_{s \downarrow}\beta(1 - \alpha) + I_{l \downarrow} - I_{l \uparrow} - H_{l} - H_{s} \right)\frac{A_{i}}{V_{i}} & Surface\ Layer \\
I_{s \downarrow}\beta(1 - \alpha)\frac{\left( e^{- \kappa_{a}z_{i}}A_{i} - e^{- \kappa_{a}z_{i - 1}}A_{i - 1} \right)}{V_{i}} & otherwise \\
\end{matrix} \right.\ `

:math:`\kappa_{a} = \frac{1.7}{SD}`

Where :math:`I_{s \downarrow}` is the incoming shortwave radiation,
:math:`\beta` is the fraction of shortwave radiation that penetrates the
water surface (:math:`\beta = 0.4` is assumed), :math:`\alpha` is the
albedo (:math:`\alpha = 0.08` is assumed for water), :math:`A_{i}^{u}`
is the area of the top of layer :math:`i`, :math:`\kappa_{a}` is the
bulk extinction coefficient for shortwave radiation, :math:`SD` is the
secchi depth, :math:`I_{l \downarrow}` is the incoming longwave
radiation, :math:`I_{l \uparrow}` is the outgoing longwave radiation,
:math:`H_{l}` is the latent heat flux and :math:`H_{s}` is the sensible
heat flux. The assumed values for :math:`\beta` and :math:`\alpha` are
reasonable for this application, and can range from 0 to 1. Radiation
computations and heat fluxes are described in previous sections. The
necessary areas for diffusion computations are described below.

:math:`A_{i} = f_{rating}\left( z_{i} \right)`

:math:`\overline{A_{i}} = \frac{A_{i} - A_{i - 1}}{2}`

In the above equations, :math:`f_{rating}` is the elevation-area rating
function, :math:`A_{i}`\ is the area of the top of layer :math:`i`, and
:math:`\overline{A_{i}}` is the average area of layer :math:`i`. Based
on the known information, ResEvap applies a discretized form of the
vertical heat diffusion equation. Discretization of the vertical
diffusion equation is performed below, using the theta method.

:math:`\frac{{T_{w}}_{i}^{t + 1} - {T_{w}}_{i}^{t}\ }{\mathrm{\Delta}t} = \frac{1}{\overline{A_{i}}}\frac{1}{\mathrm{\Delta}z}\left\lbrack {K_{z}}_{i}A_{i}\frac{{T_{w}}_{i + 1}^{t + \theta} - {T_{w}}_{i}^{t + \theta}}{\mathrm{\Delta}z} \right\rbrack + \frac{{I_{z}}_{i}}{{\rho_{w}}_{i}{c_{p}}_{i}}`

:math:`{T_{w}}_{i}^{t + \theta} = \theta{T_{w}}_{i}^{t + 1} + (1 - \theta){T_{w}}_{i}^{t}`

Where :math:`{T_{w}}_{i}^{t}` is the temperature at the start of the
timestep for layer :math:`i`, :math:`{T_{w}}_{i}^{t + 1}` is the
temperature at the end of the time step for layer :math:`i`,
:math:`A_{i}` is the area through which the heat is transferred, and
:math:`\theta` is the implicitness factor, which typically ranges from
:math:`0.5 \leq \theta \leq 1`.

The solution for this equation follows the form below.

:math:`a_{i}{T_{w}}_{i - 1}^{t + 1} + b_{i}{T_{w}}_{i}^{t + 1} + c_{i}{T_{w}}_{i + 1}^{t + 1} = {T_{w}}_{i}^{t} + (1 - \theta)\left( x^{u}\left( {T_{w}}_{i + 1}^{t} - {T_{w}}_{i}^{t} \right) - x^{l}\left( {T_{w}}_{i}^{t} - {T_{w}}_{i - 1}^{t} \right) \right) + \frac{{I_{z}}_{i}}{{\rho_{w}}_{i}{c_{p}}_{i}}`

:math:`x^{u} = \frac{\mathrm{\Delta}tA_{i}^{u}}{{\mathrm{\Delta}z}_{i}\overline{A_{i}}}\frac{\frac{{K_{z}}_{i + 1}{\mathrm{\Delta}z}_{i + 1}}{{\rho_{w}}_{i + 1}{c_{p}}_{i + 1}} + \frac{{K_{z}}_{i}{\mathrm{\Delta}z}_{i}}{{\rho_{w}}_{i}{c_{p}}_{i}}}{.5\left( {\mathrm{\Delta}z}_{i + 1} + {\mathrm{\Delta}z}_{i} \right)^{2}}`

:math:`x^{l} = \frac{\mathrm{\Delta}tA_{i}^{l}}{{\mathrm{\Delta}z}_{i}\overline{A_{i}}}\frac{\frac{{K_{z}}_{i - 1}{\mathrm{\Delta}z}_{i - 1}}{{\rho_{w}}_{i - 1}{c_{p}}_{i - 1}} + \frac{{K_{z}}_{i}{\mathrm{\Delta}z}_{i}}{{\rho_{w}}_{i}{c_{p}}_{i}}}{.5\left( {\mathrm{\Delta}z}_{i - 1} + {\mathrm{\Delta}z}_{i} \right)^{2}}`

:math:`a_{i} = - {\theta x}^{l}`

:math:`b_{i} = 1 + {\theta x}^{u} + {\theta x}^{l}`

:math:`c_{i} = - {\theta x}^{u}`

In the above equations, ResEvap assumes :math:`\theta = 1`, which makes
it a fully implicit solution. The provided equation is solved with the
tridiagonal algorithm, where :math:`a_{i}`, :math:`b_{i}` and
:math:`c_{i}` are the diagonal vectors, and the vector
:math:`{T_{w}}_{1:N}^{t + 1}` is being solved.

At this point, the full surface profile has been modeled, assuming
diffusion is the primary mode of heat transfer within the reservoir.
This assumption will fail if the stratification in the reservoir has
become unstable, forcing convective mixing between layers, or if the
wind over the reservoir creates turbulent mixing. Modeling the effects
of convective and turbulent mixing is performed by progressively mixing
downward from the surface, until there is insufficient kinetic energy to
mix deeper into the reservoir. The combined depth of the layers affected
by mixing is referred to as the surface mixing layer (SML). Working
downward from the surface, the potential energy of the SML, assuming
layer :math:`i` is included, is evaluated as follows:

:math:`{{PE}_{SML}}_{i} = g\left( {\rho_{SML}}_{i - 1}V_{i - 1:N}\left( {z_{SML}^{com}}_{i - 1} - z_{i - 2} \right) - \left( \rho_{i}V_{i:N}\left( {z^{com}}_{i:N} - z_{i - 2} \right) + \rho_{i - 1}V_{i - 1}\left( {z^{com}}_{i:i} - z_{i - 2} \right) \right) \right)`

:math:`V_{i:N} = \sum_{k = i}^{N}V_{k}`

:math:`{T_{SML}}_{i} = \frac{\sum_{k = i}^{N}{{T_{w}}_{k}V_{k}{c_{p}}_{k}}}{\sum_{k = i}^{N}{V_{k}{c_{p}}_{k}}}`

:math:`{\rho_{SML}}_{i} = 1000 - .019549\left| {T_{SML}}_{i} - 277.15 \right|^{1.68}`

:math:`{z_{SML}^{com}}_{i} = {\rho_{SML}}_{i:N}\sum_{k = i}^{N}\frac{V_{k}\left( z_{k} + z_{k - 1} \right)}{2}`

:math:`{z^{com}}_{i:j} = \sum_{k = i}^{j}\frac{\rho_{k}V_{k}\left( z_{k} + z_{k - 1} \right)}{2}`

Where :math:`{\rho_{SML}}_{i}` is the density of the SML with layer
:math:`i` included, :math:`{T_{SML}}_{i}` is the temperature of the SML
with layer :math:`i` included, :math:`{z_{SML}^{com}}_{i}` is the center
of mass of the SML with layer :math:`i` included, and
:math:`{z^{com}}_{i:j}\ ` is the center of mass of layers :math:`i`
through :math:`j`. :math:`{{PE}_{SML}}_{i}` is the difference in
potential energy of the SML with layer :math:`i` included and excluded
from the mixed layer. If :math:`{{PE}_{SML}}_{i} < 0`, then there is
sufficient energy due to density instability to force mixing of layers
:math:`i - 1:N`. In this case, the temperature of layers :math:`i - 1:N`
is set to :math:`{T_{w}}_{i:N}`, and the :math:`{{PE}_{SML}}_{i - 1}` is
subsequently checked. Once a layer is identified where
:math:`{{PE}_{SML}}_{i} \geq 0`, the density profile is considered
stable. At this point, it is still possible deeper layers are in the
SML, due to the combined convective and wind driven turbulent energy.
Therefore, the turbulent kinetic energy (TKE) must be computed, and
compared against the potential energy.

:math:`{TKE}_{i:N} = {{Ke}_{c}}_{i:N} + {{Ke}_{u}}_{i:N}`

:math:`{{Ke}_{c}}_{i:N} = \frac{\varepsilon_{c}g}{\rho_{N}\mathrm{\Delta}t}\left( \sum_{k = i}^{N}\left( \rho_{k}\left( z_{k} - z_{k - 1} \right)\frac{\left( z_{k} + z_{k - 1} \right)}{2} \right) - \frac{\left( z_{N} + z_{i - 1} \right)}{2}\sum_{k = i}^{N}\left( \rho_{k}\left( z_{k} - z_{k - 1} \right) \right) \right)`

:math:`{{Ke}_{u}}_{i:N} = \varepsilon_{u}\rho_{N}A_{N}{u_{*}^{w}}^{3}\mathrm{\Delta}t`

:math:`u_{*}^{w} = u_{*}\sqrt{\frac{\rho_{a}}{\rho_{N}}}`

Where :math:`{{Ke}_{c}}_{i:N}` is the kinetic energy of the SML with
layer :math:`i` included and :math:`{{Ke}_{u}}_{i:N}` is the kinetic
energy from wind with layer :math:`i` included. If
:math:`{TKE}_{i:N} \geq {{PE}_{mix}}_{i}`, then layer :math:`i` is
considered in the SML, and the computations checks the deeper layer. If
:math:`{TKE}_{i:N} > {{PE}_{mix}}_{i}`, then the computation of vertical
temperature profile is complete. At this point, the reservoir surface
temperature computations have comple ted, and ResEvap moves on to the
next time step. After the final time step, ResEvap reports data in the
output reports and returns the results to the Jython processor.

Appendix 1: Variable Definitions
================================

.. _evaporation-computations-1:

Evaporation Computations
------------------------

+-------+----------------------------------------------+--------------+
| Var   | Description                                  | Units        |
| iable |                                              |              |
+=======+==============================================+==============+
| .. m  | specific heat of dry air, based on           | .. math:: \f |
| ath:: | temperature :math:`T`                        | rac{J}{kg*K} |
|  c_{p |                                              |              |
| }^{T} |                                              |              |
+-------+----------------------------------------------+--------------+
| .     | 10-m, neutral-stability drag coefficient     | unitless     |
| . mat | (from Donelan (1982))                        |              |
| h:: { |                                              |              |
| c_{d} |                                              |              |
| }_{0} |                                              |              |
+-------+----------------------------------------------+--------------+
| .. ma | Transfer coefficient for wind                | unitless     |
| th::  |                                              |              |
| C_{D} |                                              |              |
+-------+----------------------------------------------+--------------+
| .. ma | Transfer coefficient for humidity            | unitless     |
| th::  |                                              |              |
| C_{q} |                                              |              |
+-------+----------------------------------------------+--------------+
| .. ma | Transfer coefficient for temperature         | unitless     |
| th::  |                                              |              |
| C_{T} |                                              |              |
+-------+----------------------------------------------+--------------+
| .. ma | saturation vapor pressure                    | .            |
| th::  |                                              | . math:: hPa |
| e_{s} |                                              |              |
+-------+----------------------------------------------+--------------+
| .     | vapor pressure                               | .            |
| . mat |                                              | . math:: hPa |
| h:: e |                                              |              |
+-------+----------------------------------------------+--------------+
| .     | evaporation                                  | .. math:: \f |
| . mat |                                              | rac{mm}{day} |
| h:: E |                                              |              |
+-------+----------------------------------------------+--------------+
| .. ma | latent heat flux                             | .            |
| th::  |                                              | . math:: \fr |
| H_{l} |                                              | ac{W}{m^{2}} |
+-------+----------------------------------------------+--------------+
| .. ma | sensible heat flux                           | .            |
| th::  |                                              | . math:: \fr |
| H_{s} |                                              | ac{W}{m^{2}} |
+-------+----------------------------------------------+--------------+
| .     | height of relative humidity measurement      | .. math:: m  |
| . mat |                                              |              |
| h:: h |                                              |              |
| _{RH} |                                              |              |
+-------+----------------------------------------------+--------------+
| .. ma | height of air temperature measurement        | .. math:: m  |
| th::  |                                              |              |
| h_{T} |                                              |              |
+-------+----------------------------------------------+--------------+
| .. ma | height of wind measurement                   | .. math:: m  |
| th::  |                                              |              |
| h_{u} |                                              |              |
+-------+----------------------------------------------+--------------+
| .. ma | Obukhov length                               | .. math:: m  |
| th::  |                                              |              |
| l_{o} |                                              |              |
+-------+----------------------------------------------+--------------+
| .. ma | latent heat of vaporization or sublimation   | .. math::    |
| th::  |                                              | \frac{J}{kg} |
| l_{v} |                                              |              |
+-------+----------------------------------------------+--------------+
| .. ma | air pressure                                 | .. math:: mb |
| th::  |                                              |              |
| p_{a} |                                              |              |
+-------+----------------------------------------------+--------------+
| .. ma | specific humidity at water surface           | unitless     |
| th::  |                                              |              |
| q_{s} |                                              |              |
+-------+----------------------------------------------+--------------+
| .. ma | specific humidity at reference temperature   | unitless     |
| th::  | height                                       |              |
| q_{r} |                                              |              |
+-------+----------------------------------------------+--------------+
| .. ma | humidity scale for air column stability      | unitless     |
| th::  |                                              |              |
| q_{*} |                                              |              |
+-------+----------------------------------------------+--------------+
| .     | roughness Reynolds number                    | unitless     |
| . mat |                                              |              |
| h:: { |                                              |              |
| R_{e} |                                              |              |
| }^{*} |                                              |              |
+-------+----------------------------------------------+--------------+
| ..    | relative humidity                            | unitless     |
|  math |                                              |              |
| :: RH |                                              |              |
+-------+----------------------------------------------+--------------+
| .     | salinity                                     | .            |
| . mat |                                              | . math:: psu |
| h:: S |                                              |              |
+-------+----------------------------------------------+--------------+
| .. ma | temperature scale for air column stability   | unitless     |
| th::  |                                              |              |
| t_{*} |                                              |              |
+-------+----------------------------------------------+--------------+
| .. ma | air temperature                              | .. math:: K  |
| th::  |                                              |              |
| T_{a} |                                              |              |
+-------+----------------------------------------------+--------------+
| .. ma | air temperature measurement at reference     | .. math:: K  |
| th::  | height :math:`h_{T}`                         |              |
| \wide |                                              |              |
| hat{T |                                              |              |
| _{a}} |                                              |              |
+-------+----------------------------------------------+--------------+
| .. ma | water surface temperature                    | .. math:: K  |
| th::  |                                              |              |
| T_{s} |                                              |              |
+-------+----------------------------------------------+--------------+
| .     | average air temperature over the surface air | .. math:: K  |
| . mat | layer (from water surface to :math:`h_{T})`  |              |
| h:: \ |                                              |              |
| overl |                                              |              |
| ine{T |                                              |              |
| _{a}} |                                              |              |
+-------+----------------------------------------------+--------------+
| .. ma | water temperature                            | .. math:: K  |
| th::  |                                              |              |
| T_{w} |                                              |              |
+-------+----------------------------------------------+--------------+
| .     | measured windspeed                           | .. math::    |
| . mat |                                              |  \frac{m}{s} |
| h:: \ |                                              |              |
| wideh |                                              |              |
| at{u} |                                              |              |
+-------+----------------------------------------------+--------------+
| .     | adjusted wind speed                          |              |
| . mat |                                              |              |
| h:: u |                                              |              |
+-------+----------------------------------------------+--------------+
| .. ma | wind friction velocity                       | unitless     |
| th::  |                                              |              |
| u_{*} |                                              |              |
+-------+----------------------------------------------+--------------+
| .. ma | windspeed at reference height                | .. math::    |
| th::  |                                              |  \frac{m}{s} |
| u_{r} |                                              |              |
+-------+----------------------------------------------+--------------+
| .. ma | roughness length for momentum                | .. math:: m  |
| th::  |                                              |              |
| z_{u} |                                              |              |
+-------+----------------------------------------------+--------------+
| .. ma | roughness length for temperature             | .. math:: m  |
| th::  |                                              |              |
| z_{T} |                                              |              |
+-------+----------------------------------------------+--------------+
| .. ma | roughness length for humidity                | .. math:: m  |
| th::  |                                              |              |
| z_{q} |                                              |              |
+-------+----------------------------------------------+--------------+
| .. ma | dry dry adiabatic laps rate                  | .. math::    |
| th::  |                                              |  \frac{K}{m} |
| \Gamm |                                              |              |
| a_{d} |                                              |              |
+-------+----------------------------------------------+--------------+
| .. ma | totential potential temperature (air         | .. math:: K  |
| th::  | temperature at water-air interface)          |              |
| \thet |                                              |              |
| a_{r} |                                              |              |
+-------+----------------------------------------------+--------------+
| ..    | water vapor density                          | ..           |
| math: |                                              |  math:: \fra |
| : \rh |                                              | c{kg}{m^{3}} |
| o_{v} |                                              |              |
+-------+----------------------------------------------+--------------+
| ..    | density of air                               | ..           |
| math: |                                              |  math:: \fra |
| : \rh |                                              | c{kg}{m^{3}} |
| o_{a} |                                              |              |
+-------+----------------------------------------------+--------------+
| ..    | dry density of air                           | ..           |
| math: |                                              |  math:: \fra |
| : \rh |                                              | c{kg}{m^{3}} |
| o_{d} |                                              |              |
+-------+----------------------------------------------+--------------+
| ..    | kinematic viscosity of air                   | .            |
|  math |                                              | . math:: \fr |
| :: \n |                                              | ac{m^{2}}{s} |
| u_{s} |                                              |              |
+-------+----------------------------------------------+--------------+

.. _radiation-computations-1:

Radiation Computations
----------------------

+-------+----------------------------------------------+--------------+
| Var   | Description                                  | Units        |
| iable |                                              |              |
+=======+==============================================+==============+
| .. m  | vapor pressure of the atmosphere             | .            |
| ath:: |                                              | . math:: hPa |
|  {\ e |                                              |              |
| }_{a} |                                              |              |
+-------+----------------------------------------------+--------------+
| .. m  | saturation vapor pressure                    | .            |
| ath:: |                                              | . math:: hPa |
|  {\ e |                                              |              |
| }_{s} |                                              |              |
+-------+----------------------------------------------+--------------+
| .     | fractional cloud cover of layer :math:`k`    | unitless     |
| . mat |                                              |              |
| h:: { |                                              |              |
| f_{c} |                                              |              |
| }_{k} |                                              |              |
+-------+----------------------------------------------+--------------+
| .     | height of clouds in layer :math:`k`          | .. math:: m  |
| . mat |                                              |              |
| h:: { |                                              |              |
| h_{c} |                                              |              |
| }_{k} |                                              |              |
+-------+----------------------------------------------+--------------+
| ..    | hour of day in GMT                           | ..           |
|  math |                                              | math:: hours |
| :: h_ |                                              |              |
| {gmt} |                                              |              |
+-------+----------------------------------------------+--------------+
| .. ma | hour angle of the sun                        | .. mat       |
| th::  |                                              | h:: {^\circ} |
| h_{s} |                                              |              |
+-------+----------------------------------------------+--------------+
| .     | incoming solar radiation reaching the water  | .            |
| . mat | surface                                      | . math:: \fr |
| h:: I |                                              | ac{W}{m^{2}} |
| _{s \ |                                              |              |
| downa |                                              |              |
| rrow} |                                              |              |
+-------+----------------------------------------------+--------------+
| .. m  | upwelling longwave radiation from the water  | .            |
| ath:: | surface                                      | . math:: \fr |
|  I_{l |                                              | ac{W}{m^{2}} |
|  \upa |                                              |              |
| rrow} |                                              |              |
+-------+----------------------------------------------+--------------+
| .     | downwelling longwave radiation reaching the  | .            |
| . mat | water surface                                | . math:: \fr |
| h:: I |                                              | ac{W}{m^{2}} |
| _{l \ |                                              |              |
| downa |                                              |              |
| rrow} |                                              |              |
+-------+----------------------------------------------+--------------+
| .     | clear sky component of the downwelling       | .            |
| . mat | longwave radiation                           | . math:: \fr |
| h:: { |                                              | ac{W}{m^{2}} |
| I_{l  |                                              |              |
| \down |                                              |              |
| arrow |                                              |              |
| }}_{c |                                              |              |
| lear} |                                              |              |
+-------+----------------------------------------------+--------------+
| .     | overcast component of the downwelling        | .            |
| . mat | longwave radiation                           | . math:: \fr |
| h:: { |                                              | ac{W}{m^{2}} |
| I_{l  |                                              |              |
| \down |                                              |              |
| arrow |                                              |              |
| }}_{c |                                              |              |
| loud} |                                              |              |
+-------+----------------------------------------------+--------------+
| ..    | Julian date where :math:`JD = 1` on January  | ..           |
|  math | 1\ :sup:`st`                                 |  math:: days |
| :: JD |                                              |              |
+-------+----------------------------------------------+--------------+
| .. ma | latent heat of vaporization                  | .. math::    |
| th::  |                                              | \frac{J}{kg} |
| l_{v} |                                              |              |
+-------+----------------------------------------------+--------------+
| .. ma | reflectance of layer :math:`k`               | unitless     |
| th::  |                                              |              |
| R_{k} |                                              |              |
+-------+----------------------------------------------+--------------+
| .. ma | reflectance of the water surface             | unitless     |
| th::  |                                              |              |
| R_{g} |                                              |              |
+-------+----------------------------------------------+--------------+
| ..    | measured relative humidity                   | unitless     |
|  math |                                              |              |
| :: \w |                                              |              |
| ideha |                                              |              |
| t{RH} |                                              |              |
+-------+----------------------------------------------+--------------+
| .. ma | extraterrestrial solar radiation on a        | .            |
| th::  | horizontal plane                             | . math:: \fr |
| S_{e} |                                              | ac{W}{m^{2}} |
+-------+----------------------------------------------+--------------+
| .. ma | measured air temperature                     | .. math:: K  |
| th::  |                                              |              |
| \wide |                                              |              |
| hat{T |                                              |              |
| _{a}} |                                              |              |
+-------+----------------------------------------------+--------------+
| .. ma | transmissivity of layer :math:`k`            | unitless     |
| th::  |                                              |              |
| T_{k} |                                              |              |
+-------+----------------------------------------------+--------------+
| .. ma | water surface temperature                    | .. math:: K  |
| th::  |                                              |              |
| T_{s} |                                              |              |
+-------+----------------------------------------------+--------------+
| .     | solar declination angle                      | .. mat       |
| . mat |                                              | h:: {^\circ} |
| h:: \ |                                              |              |
| delta |                                              |              |
+-------+----------------------------------------------+--------------+
| .. ma | solar zenith angle                           | .. mat       |
| th::  |                                              | h:: {^\circ} |
| \thet |                                              |              |
| a_{s} |                                              |              |
+-------+----------------------------------------------+--------------+

.. _vertical-temperature-profile-computations-1:

Vertical Temperature Profile Computations
-----------------------------------------

+---------+--------------------------------------------+--------------+
| V       | Description                                | Units        |
| ariable |                                            |              |
+=========+============================================+==============+
| .       | top area of layer :math:`i`                | ..           |
| . math: |                                            | math:: m^{2} |
| : A_{i} |                                            |              |
+---------+--------------------------------------------+--------------+
| .. ma   | average area of layer :math:`i`            | ..           |
| th:: \o |                                            | math:: m^{2} |
| verline |                                            |              |
| {A_{i}} |                                            |              |
+---------+--------------------------------------------+--------------+
| .. math | heat capacity of water at                  | .. math:: \f |
| :: {c_{ | layer\ :math:`\ i`                         | rac{J}{kg*K} |
| p}}_{i} |                                            |              |
+---------+--------------------------------------------+--------------+
| .. math | radiative energy flux for                  | .            |
| :: {I_{ | layer\ :math:`\ i`                         | . math:: \fr |
| z}}_{i} |                                            | ac{w}{M^{3}} |
+---------+--------------------------------------------+--------------+
| .. ma   | convective kinetic energy of layer         | .. math::    |
| th:: {{ | :math:`i` through the surface layer        | \frac{J}{kg} |
| Ke}_{c} |                                            |              |
| }_{i:N} |                                            |              |
+---------+--------------------------------------------+--------------+
| .. ma   | wind driven kinetic energy of layer        | .. math::    |
| th:: {{ | :math:`i` through the surface layer        | \frac{J}{kg} |
| Ke}_{u} |                                            |              |
| }_{i:N} |                                            |              |
+---------+--------------------------------------------+--------------+
| .. math | thermal diffusivity of layer :math:`i`     | .            |
| :: {K_{ |                                            | . math:: \fr |
| z}}_{i} |                                            | ac{m^{2}}{s} |
+---------+--------------------------------------------+--------------+
| .       | stability frequency of layer :math:`i`     | .. math::    |
| . math: |                                            |  \frac{1}{s} |
| : N_{i} |                                            |              |
+---------+--------------------------------------------+--------------+
| .. ma   | Secchi Depth                               | .. math:: m  |
| th:: SD |                                            |              |
+---------+--------------------------------------------+--------------+
| .. math | water temperature of layer :math:`i`       | .. math:: K  |
| :: {T_{ |                                            |              |
| w}}_{i} |                                            |              |
+---------+--------------------------------------------+--------------+
| .. math | total kinetic energy of layer :math:`i`    | .. math::    |
| :: {TKE | through the surface layer                  | \frac{J}{kg} |
| }_{i:N} |                                            |              |
+---------+--------------------------------------------+--------------+
| ..      | temperature of the SML if layer :math:`i`  | .. math:: K  |
|  math:: | is the lowest layer                        |              |
|  {T_{SM |                                            |              |
| L}}_{i} |                                            |              |
+---------+--------------------------------------------+--------------+
| .       | volume of layer :math:`i`                  | ..           |
| . math: |                                            | math:: m^{3} |
| : V_{i} |                                            |              |
+---------+--------------------------------------------+--------------+
| ..      | volume of water from layer :math:`i` to    | ..           |
| math::  | the surface                                | math:: m^{3} |
| V_{i:N} |                                            |              |
+---------+--------------------------------------------+--------------+
| .       | depth of the center of mass for the SML,   | .. math:: m  |
| . math: | if later :math:`i`\ is the lowest layer    |              |
| : {z_{S | included in SML                            |              |
| ML}^{co |                                            |              |
| m}}_{i} |                                            |              |
+---------+--------------------------------------------+--------------+
| .. m    | depth of the center of mass of layers      | .. math:: m  |
| ath:: { | :math:`i` through :math:`j`                |              |
| z^{com} |                                            |              |
| }_{i:j} |                                            |              |
+---------+--------------------------------------------+--------------+
| .. m    | convective turbulent energy dissipation    | .. ma        |
| ath:: \ |                                            | th:: \frac{m |
| varepsi |                                            | ^{2}}{s^{3}} |
| lon_{c} |                                            |              |
+---------+--------------------------------------------+--------------+
| .. m    | wind driven turbulent energy dissipation   | .. ma        |
| ath:: \ |                                            | th:: \frac{m |
| varepsi |                                            | ^{2}}{s^{3}} |
| lon_{u} |                                            |              |
+---------+--------------------------------------------+--------------+
| .. mat  | bulk extinction coefficient for            | .. math::    |
| h:: \ka | penetrating shortwave radiation            |  \frac{1}{m} |
| ppa_{a} |                                            |              |
+---------+--------------------------------------------+--------------+
| ..      | density of water at layer :math:`i`        | ..           |
| math::  |                                            |  math:: \fra |
| {\rho_{ |                                            | c{kg}{m^{3}} |
| w}}_{i} |                                            |              |
+---------+--------------------------------------------+--------------+
| .       | average water density across the entire    | ..           |
| . math: | profile                                    |  math:: \fra |
| : \over |                                            | c{kg}{m^{3}} |
| line{\r |                                            |              |
| ho_{w}} |                                            |              |
+---------+--------------------------------------------+--------------+
| .. ma   | density of the SML if layer :math:`i` is   | ..           |
| th:: {\ | the lowest layer                           |  math:: \fra |
| rho_{SM |                                            | c{kg}{m^{3}} |
| L}}_{i} |                                            |              |
+---------+--------------------------------------------+--------------+

Appendix 2: Constant Values
===========================

+-------+--------------------------+-----------------+-----------------+
| Var   | Description              | Value           | Units           |
| iable |                          |                 |                 |
+=======+==========================+=================+=================+
| .. ma | smooth surface           | .. math:: 0.135 | unitless        |
| th::  | coefficient              |                 |                 |
| C_{s} |                          |                 |                 |
+-------+--------------------------+-----------------+-----------------+
| .     | acceleration due to      | .. math:: 9.81  | .. math::       |
| . mat | gravity                  |                 | \frac{m}{s^{2}} |
| h:: g |                          |                 |                 |
+-------+--------------------------+-----------------+-----------------+
| .. ma | molecular weight of      | .. m            | .. math::       |
| th::  | water                    | ath:: 0.0180160 | \frac{kg}{mole} |
| m_{w} |                          |                 |                 |
+-------+--------------------------+-----------------+-----------------+
| .. ma | the ideal gas constant   | ..              | .. math:: \     |
| th::  |                          |  math:: 8.31441 | frac{J}{mole*K} |
| R_{g} |                          |                 |                 |
+-------+--------------------------+-----------------+-----------------+
| .. ma | gas constant for water   | .. math:: 461   | .. math::       |
| th::  | vapor                    |                 |  \frac{J}{kg*K} |
| R_{v} |                          |                 |                 |
+-------+--------------------------+-----------------+-----------------+
| .. ma | reflectivity of water    | .. math:: 0.2   | unitless        |
| th::  |                          |                 |                 |
| R_{w} |                          |                 |                 |
+-------+--------------------------+-----------------+-----------------+
| .     | freezing point in Kelvin | .               | .. math:: K     |
| . mat |                          | . math:: 273.15 |                 |
| h:: T |                          |                 |                 |
| _{FP} |                          |                 |                 |
+-------+--------------------------+-----------------+-----------------+
| .     | albedo of water          | .. math:: 0.08  | unitless        |
| . mat |                          |                 |                 |
| h:: \ |                          |                 |                 |
| alpha |                          |                 |                 |
+-------+--------------------------+-----------------+-----------------+
| .. ma | light penetration        | .. math:: 0.4   | unitless        |
| th::  | fraction                 |                 |                 |
| \beta |                          |                 |                 |
+-------+--------------------------+-----------------+-----------------+
| .. ma | convective dissipation   | .. math:: 0.5   | .. math:: \fra  |
| th::  |                          |                 | c{m^{2}}{s^{3}} |
| \vare |                          |                 |                 |
| psilo |                          |                 |                 |
| n_{c} |                          |                 |                 |
+-------+--------------------------+-----------------+-----------------+
| .. ma | stirring dissipation     | .. math:: 0.4   | .. math:: \fra  |
| th::  |                          |                 | c{m^{2}}{s^{3}} |
| \vare |                          |                 |                 |
| psilo |                          |                 |                 |
| n_{s} |                          |                 |                 |
+-------+--------------------------+-----------------+-----------------+
| .. ma | emissivity of water      | .. math:: 0.98  | unitless        |
| th::  |                          |                 |                 |
| \vare |                          |                 |                 |
| psilo |                          |                 |                 |
| n_{w} |                          |                 |                 |
+-------+--------------------------+-----------------+-----------------+
| .     | von Karman constant      | .. math:: 0.4   | unitless        |
| . mat |                          |                 |                 |
| h:: \ |                          |                 |                 |
| kappa |                          |                 |                 |
+-------+--------------------------+-----------------+-----------------+
| .     | Stefan-Boltzman constant | .. math:        | .. math:: \frac |
| . mat |                          | : 5.67*10^{- 8} | {W}{m^{2}K^{4}} |
| h:: \ |                          |                 |                 |
| sigma |                          |                 |                 |
+-------+--------------------------+-----------------+-----------------+
| .     | theta method factor      | .. math:: 1     | unitless        |
| . mat |                          |                 |                 |
| h:: \ |                          |                 |                 |
| theta |                          |                 |                 |
+-------+--------------------------+-----------------+-----------------+

NOTE: The Stefan-Boltzman constant is :math:`5.669*10^{- 8}` in the
computation of the incoming longwave radiation, which is slightly
different than the rest of the computations. This is considered an
insignificant difference.

Appendix 3: Example Input Files
===============================

ResEvap Properties
------------------

# ResEvap script parameters that are constants

ResEvap.versionOut=test_vers

ResEvap.workingDir=sample_data

# DB Id for obtaining the max temperature depth (thus max layer)

ResEvap.maxTempDepthId=Depth.Const.0.Max Temperature Prof Depth

# DB Id for secchi depth from data base

ResEvap.secchiDepthId=Depth.Const.0.Secchi Depth

Reservoir Information
---------------------

Reservoir:PA18

Secchi:0.54864

Zero elevation:1070.0

Lat:-60.7306

Long:38.5632

GMT Offset:6

Timezone:US/Central

Rating:PA18.Elev;Area.Linear.Step

1:SWT:KOMA.Speed-Wind.Inst.1Hour.0.Nwo-Evap:m/s

2:SWT:KOMA.Temp-Air.Inst.1Hour.0.nwo-evap:C

3:SWT:KOMA.%-RelativeHumidity.Inst.1Hour.0.nwo-evap:%

4:SWT:KOMA.Pres-ATMOSPHERIC.Inst.1Hour.0.nwo-evap:mb

5:SWT:KOMA.%-Cloud-Low.Inst.1Hour.0.nwo-evap:%

6:SWT:KOMA.Elev-Cloud-Low.Inst.1Hour.0.nwo-evap:m

7:SWT:KOMA.%-Cloud-Mid.Inst.1Hour.0.nwo-evap:%

8:SWT:KOMA.Elev-Cloud-Mid.Inst.1Hour.0.nwo-evap:m

9:SWT:KOMA.%-Cloud-High.Inst.1Hour.0.nwo-evap:%

10:SWT:KOMA.Elev-Cloud-High.Inst.1Hour.0.nwo-evap:m

11:SWT:PA18.Elev.Inst.1Hour.0.nwo-evaptest:ft

30:SWT:PA18.Temp-Water-0,0m.Inst.1Day.0.nwo-evaptest:C

31:SWT:PA18.Temp-Water-0,5m.Inst.1Day.0.nwo-evaptest:C

32:SWT:PA18.Temp-Water-1,0m.Inst.1Day.0.nwo-evaptest:C

33:SWT:PA18.Temp-Water-1,5m.Inst.1Day.0.nwo-evaptest:C

34:SWT:PA18.Temp-Water-2,0m.Inst.1Day.0.nwo-evaptest:C

35:SWT:PA18.Temp-Water-2,5m.Inst.1Day.0.nwo-evaptest:C

36:SWT:PA18.Temp-Water-3,0m.Inst.1Day.0.nwo-evaptest:C

37:SWT:PA18.Temp-Water-3,5m.Inst.1Day.0.nwo-evaptest:C

38:SWT:PA18.Temp-Water-4,0m.Inst.1Day.0.nwo-evaptest:C

39:SWT:PA18.Temp-Water-4,5m.Inst.1Day.0.nwo-evaptest:C

40:SWT:PA18.Temp-Water-5,0m.Inst.1Day.0.nwo-evaptest:C

41:SWT:PA18.Temp-Water-5,5m.Inst.1Day.0.nwo-evaptest:C

42:SWT:PA18.Temp-Water-6,0m.Inst.1Day.0.nwo-evaptest:C

43:SWT:PA18.Temp-Water-6,5m.Inst.1Day.0.nwo-evaptest:C

44:SWT:PA18.Temp-Water-7,0m.Inst.1Day.0.nwo-evaptest:C

45:SWT:PA18.Temp-Water-7,5m.Inst.1Day.0.nwo-evaptest:C

46:SWT:PA18.Temp-Water-8,0m.Inst.1Day.0.nwo-evaptest:C

47:SWT:PA18.Temp-Water-8,5m.Inst.1Day.0.nwo-evaptest:C

48:SWT:PA18.Temp-Water-9,0m.Inst.1Day.0.nwo-evaptest:C

49:SWT:PA18.Temp-Water-9,5m.Inst.1Day.0.nwo-evaptest:C

50:SWT:PA18.Temp-Water-10,0m.Inst.1Day.0.nwo-evaptest:C

51:SWT:PA18.Temp-Water-10,5m.Inst.1Day.0.nwo-evaptest:C

52:SWT:PA18.Temp-Water-11,0m.Inst.1Day.0.nwo-evaptest:C

53:SWT:PA18.Temp-Water-11,5m.Inst.1Day.0.nwo-evaptest:C

54:SWT:PA18.Temp-Water-12,0m.Inst.1Day.0.nwo-evaptest:C

55:SWT:PA18.Temp-Water-12,5m.Inst.1Day.0.nwo-evaptest:C

56:SWT:PA18.Temp-Water-13,0m.Inst.1Day.0.nwo-evaptest:C

57:SWT:PA18.Temp-Water-13,5m.Inst.1Day.0.nwo-evaptest:C

58:SWT:PA18.Temp-Water-14,0m.Inst.1Day.0.nwo-evaptest:C

59:SWT:PA18.Temp-Water-14,5m.Inst.1Day.0.nwo-evaptest:C

60:SWT:PA18.Temp-Water-15,0m.Inst.1Day.0.nwo-evaptest:C

61:SWT:PA18.Temp-Water-15,5m.Inst.1Day.0.nwo-evaptest:C

62:SWT:PA18.Temp-Water-16,0m.Inst.1Day.0.nwo-evaptest:C

63:SWT:PA18.Temp-Water-16,5m.Inst.1Day.0.nwo-evaptest:C

64:SWT:PA18.Temp-Water-17,0m.Inst.1Day.0.nwo-evaptest:C

65:SWT:PA18.Temp-Water-17,5m.Inst.1Day.0.nwo-evaptest:C

66:SWT:PA18.Temp-Water-18,0m.Inst.1Day.0.nwo-evaptest:C

67:SWT:PA18.Temp-Water-18,5m.Inst.1Day.0.nwo-evaptest:C

68:SWT:PA18.Temp-Water-19,0m.Inst.1Day.0.nwo-evaptest:C

69:SWT:PA18.Temp-Water-19,5m.Inst.1Day.0.nwo-evaptest:C

70:SWT:PA18.Temp-Water-20,0m.Inst.1Day.0.nwo-evaptest:C

71:SWT:PA18.Temp-Water-20,5m.Inst.1Day.0.nwo-evaptest:C

Appendix 4: Recommendations
===========================

As a result of developing this document, a list of recommendations
regarding ResEvap have been developed:

1) Implement Automated Testing

As a purely computational tool, ResEvap would benefit from having an
automated test procedure. Such an automated test process should include
running the ResEvap for multiple different datasets, and comparing
against expected results. By implementing automated testing, there are
benefits in the QA and the development processes. For QA, it removes the
need to have staff review results when new versions of ResEvap are
created. For development, an automated test system allows developers to
quickly test that changes do not have unintended consequence, allowing
for the identification of bugs earlier in the development process. Both
of these benefits have the potential to reduce costs and improve speed
of developing new ResEvap builds.

2) | Simplify ResEvap input/output variable configuration
   | Currently, ResEvap has the output time series version established
     in resevap.properties and all input data including reservoir
     information, rating curves, and time series identifiers specified
     in the reservoir configuration file. Recommended is that the
     configuration of ResEvap be simplified allowing re-use of variables
     defined in resevap.properties as keyword replacement patterns in
     the reservoir file. Additionally, the reservoir file content should
     be examined to more clearly indicate default (ordinarily supplied
     by the database) versus required fields. ResEvap should be updated
     to perform a validation of its input data to generate warnings
     and/or failure states up to terminating the application when input
     is not correctly defined. ResEvap does not generate a warning or
     failure state if the input time series and output time series do
     not match. Suggested is that the application fail and log a severe
     error state if the time series do not match.

3) | ResEvap initialization process
   | At the start of the year, the water temperature profiles for all
     reservoirs are initialized using a separate jython script named,
     Evap_Initialization.py. This script is coded to be run under a
     Windows environment as it has UI elements. Recommended is that a
     similar operation be added to ResEvap that is able to establish
     initial water temperatures for the profiles. Given the environment,
     this operation would need to be developed as command line arguments
     to ResEvap or as a separate shell script from ResEvap.

4) Compare ResEvap against other water temperature models

ResEvap has a complex water temperature profile model. This model is
similar, but has distinct differences from other water temperature
models. Therefore, it is suggested that ResEvap be compared against
other water temperature profile models to compare accuracy and
efficiency. This comparison has the potential to identify deficiencies
in the existing ResEvap program, and to identify it’s strengths over
other strategies.

5) Change program name

ResEvap does much more than simply compute reservoir evaporation, which
the name suggests. It’s a fully integrated reservoir energy balance
model. Renaming the program to reflect the sophistication within the
program may be helpful as other districts consider its use.

6) Add additional user configuration

A few values are hard-coded within ResEvap that could be user
configurable. Measurement heights, water temperature layer thickness and
the theta parameter for discretization are all forced to be specific
values. For measurement heights, the height at which wind speed, air
temperature and relative humidity are measured are all forced to be 10m.
These measurements likely occur at different heights, and the
computations can support changing these values. For water temperature
layer thickness, the layers are forced to be 0.5 meters, but this could
be altered if the user desired finer or coarser vertical resolution. One
important note is that changing the layer thickness could lead to model
instability. One potential remedy for instability is changing the model
time-step. Finally, the discretization of the vertical heat diffusion
equation is performed with the theta method, but forces theta to be 1,
representing a fully implicit solution. ResEvap actually supports theta
ranging from 0 to 1, and therefore this parameter should be adjustable
by the user.

7) Add vertical datum support

Vertical datums are ignored within ResEvap, but are critical for
ensuring proper application of the elevation-area rating curve. This is
because the elevation measurements and rating curve can have different
vertical datums, and would therefore lead to incorrect area computations
without datum adjustments. Therefore, it is recommended that vertical
datum support be added to the ResEvap computations.

8) Add frustum computation

Evaporation as flow is computed by assuming that the reservoir banks are
vertical at each time step. This generally leads to an over-estimation
of flow, as the reservoir area becomes smaller as the pool elevation
decreases. In order to improve the accuracy of evaporation as flow
computations, it is recommended that the frustum computations be
implemented.

9) Add support for solar radiation observations

Currently, ResEvap requires cloud fractions and heights for three
different atmospheric layers, meaning there are six time series used for
computing the radiation balance. Within the radiation balance, the solar
radiation is the dominant variable, and therefore the inputs could be
greatly simplified by replacing cloud cover fractions with solar
radiation observations. This would require additional considerations for
longwave radiation, which are typically performed by backing out
effective cloud cover from the difference between the observed and
computed clear sky solar radiation.

10) Fix vertically averaged density computations

The vertically averaged density, within the vertical temperature profile
computation, is computed as a volumetric average. Since the model is
1-dimensional, the vertical averages should not be volumetric. By
computing as a volumetric average, the densities become inconsistent
with the vertically integrated temperatures. The average density should
replace the volume average with a depth average, which ensures
consistency between model variables.

11) Allow for storage of hourly temperature profiles

Although ResEvap computes hourly temperature profiles, only daily time
series are saved to the CWMS database. ResEvap should be saving the most
granular time series available, which would provide the maximum
information possible. This would have the benefit of avoiding daylight
savings considerations when saving, and allow for initialization at
times other than midnight. Alternatively, if daily time series is
preferred, ResEvap should transition to using Local Regular Time Series
to ensure proper accounting of daylight savings time.

12) | Re-code the ResEvap shell script as a bash script
    | The ResEvap script was originally coded as a CSH script as
      required by the Corps. CWMS has adopted BASH as the standard shell
      scripting language. The ResEvap script should be migrated to BASH
      for ease in maintenance and staying in parity with CWMS.

13) Cleanup unused file production

ResEvap creates text report files that are not used by NWO, which are
described in section 5.1.2. Since these files are unused currently, it
is recommended that flags be added to suppress these files, so that they
are not generated. Once these flags are developed, update the Jython
code to leverage those flags to prevent creation of these files.

14) Allow for use of fog/smoke layer computations

The parameters for the fog/smoke layer effects on shortwave radiation
exist in ResEvap, but these are never used. The program should be
updated to allow for use of these parameters, which would allow for
direct assessment of fog/smoke effects on the incoming solar radiation.

References
==========

Crawford, T.M, C.E. Duchon (1999) An Improved Parameterization for
Estimating

Effective Atmospheric Emissivity for Use in Calculating Daytime
Downwelling

Longwave Radiation. Journal of Applied Meteorology, Volume 38, Issue 4
(April

1999) pp 474-480

Daly (2005), Reservoir Evaporation, U.S. Army Engineering Research and

Development Center, November 2015

Fairall, C.W., E.F. Bradley, D.P. Rogers, J.B. Edson, and G.S. Young,
1996: Bulk

parameterization of air-sea fluxes for Tropical Ocean-Global Atmosphere

Coupled-Ocean Atmosphere Response Experiment. J. Geophys. Res., 101,
3747–3764.

Hondzo, M., and H. Stefan (1993) Lake Water Temperature Simulation
Model. Journal of

Hydraulic Engineering, Vol. 119, No. 11, November, 1993 pp 1251-1273

Shapiro, R. (1987) A simple model for the calculation of the flux of
direct and diffuse

solar radiation through the atmosphere. Air Force Geophysics Laboratory,

Hanscom AFB MA 01731 AFGL-TR-87-0200

Woolf, H. M. (1968) On the computation of solar elevation angles and the
determination

of sunrise and sunset times. NASA TM X-1646, National Aeronautics and
Space

Administration, Washington, D. C. September 1968
