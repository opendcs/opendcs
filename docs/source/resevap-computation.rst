.. _resevap-computation:
.. Unit Constants utilized in ResEvap documentation
.. |degC| replace:: :math:`{^\circ}C`
.. |deg| replace:: :math:`{^\circ}`
.. |tempK| replace:: :math:`K`
.. |percent| replace:: :math:`\%`
.. |m| replace:: :math:`m`
.. |m2| replace:: :math:`m^{2}`
.. |m3| replace:: :math:`m^{3}`
.. |mm| replace:: :math:`mm`
.. |mb| replace:: :math:`mb`
.. |hPa| replace:: :math:`hPa`
.. |i| replace:: :math:`i`
.. |j| replace:: :math:`j`
.. |k| replace:: :math:`k`

.. numerator to the left of the denominator options:
.. |m/s| replace:: :math:`m / s`
.. |m3/s| replace:: :math:`{m^{3}} / s`
.. |m2/s| replace:: :math:`{m^{2}} / {s}`
.. |m2/s3| replace:: :math:`{m^{2}} / {s^{3}}`
.. |W/m2| replace:: :math:`W / {m^{2}}`
.. |W/m3| replace:: :math:`W / {m^{3}}`
.. |J/kg| replace:: :math:`J / kg`
.. |J/kgK| replace:: :math:`\dfrac{J}{kg*K}`
.. |kg/m3| replace:: :math:`{kg} / {m^{3}}`


######################
ResEvap Documentation
######################

Introduction
==============

ResEvap is a reservoir evaporation computational utility originally developed for the
reservoirs in the Missouri River basin. Development of ResEvap was motivated
by the need for accurate estimates of evaporation to properly manage reservoirs,
in conjunction with the known limitation of evaporation observations
(e.g. Evaporation Pans). The underlying algorithm was developed with
state-of-the-art evaporation and water temperature modeling techniques by the USACE
:ref:`(Daly 2005) <References>`. In order to improve the usability of ResEvap throughout
the USACE, the model was converted from Fortran to Java and Jython/Python by RMA in 2015.
Cloud migration efforts and ease of access prompted a new conversion to Java utilizing
OpenDCS libraries in 2024 by the USACE.

This documentation is intended to describe the core computational
functionality.

The computations, including the evaporation, radiation, and vertical temperature
profile models are described below. Descriptions of the computations are supported
in `Appendix 1`_ and `Appendix 2`_, which contain variable definitions and
constant value listings, and last there is a list of references.

ResEvap Class Sequence of Operations
======================================

Based on the information provided in the OpenDCS computation, ResEvapAlgo
populates the compute objects with the necessary parameters and meteorological
data to compute the evaporation, energy balance, and water temperature profile.
The algorithm allows specification of nearly all of the compute parameters, but the
reservoir layer thickness and meteorological measurement heights are hard-coded
to be 0.5m and 10m, respectively. An exception to the reservoir layers being
hard coded to be 0.5m is the layer at the specified maximum depth, this bottom
layer will most likely be thinner than 0.5m to avoid exceeding the maximum
depth. An example of this would be a maximum depth of 40.3m. The last layer
would be 0.3m instead of 0.5m.

An important note is that the algorithm attempts to retrieve a value for
each water temperature profile that is at the starting time. In the event that
it cannot retrieve a value from the time series, the algorithm will adjust the
retrieval time to utilize the time series’ interval offset, and again attempt to
retrieve the initial profile temperatures. If initial values are not retrieved by
either method, the compute will fail.

After the ResEvap computations are complete, the output time series are parsed from
the OpenDCS TimeSeries objects. The output time series include hourly water surface
temperature, sensible heat flux, latent heat flux, solar radiation, downwelling longwave
radiation, upwelling longwave radiation, and evaporation, daily evaporation, evaporation
as flow, and daily water profile temperatures. Note that the water temperature profile
values are computed at an hourly time step, but only saved to the CWMS database
as a daily time series. After storing these time series to the CWMS database, the database
connection is closed, and the algorithm is completed.

Java Compute Routines
=====================

Core Routines
--------------

The Java entry point is a class called ResEvap. This class is tasked with
building the necessary compute objects, looping through each hourly time step,
and organizing the results. The ResEvap compute object is constructed by the
ResEvapAlgo class, and subsequently provided with meteorological data and
reservoir information. Based on this information, ResEvap runs the compute at
an hourly time-step.

Computation of evaporation within ResEvap is a two-part process. These
computations are broken into the reservoir surface temperature computations,
through a full temperature profile model, and the evaporation from the water
surface. The temperature at the surface of the reservoir is a key variable in
estimating evaporation, but it is rarely measured. Therefore, ResEvap must
compute the temperature profile within the reservoir, based on initial
conditions and meteorological observations. Temperature profile modeling in
ResEvap assumes that the vertical temperature profile is governed by the energy
transfer through the water surface, which requires that the inflows and outflows
are negligible components of the energy balance. This is appropriate for the
Missouri River watershed :ref:`(Daly 2005) <References>`, which ResEvap was
designed for, but this assumption should be carefully analyzed when applying to
new watersheds. Four heat transfer modes are computed: sensible, latent,
shortwave radiation and longwave radiation. Computation of these fluxes are
described in the “Evaporation Computations” and “Radiation Computations” sections
of this document. Based on these fluxes, and the initial water temperature at
each layer in the profile, the temperatures are updated as described in the
“Vertical Temperature Profile Computations” section.

**Note:** `Appendix 1`_ **has a definition of all variables in the computations,
and** `Appendix 2`_ **has a definition of all the constants used.**

Input Data
~~~~~~~~~~

Input data for ResEvap includes a combination of meteorological observations and
reservoir physical parameters. The time series observations are provided in time
series form, and are summarized in Table 2. In this table, note that the cloud
heights are estimated by ResEvap if not provided, making them an optional
parameter. The input time series is required but the computation will still
calculate if there is missing data.

.. _Table 2:

*Table 2 - Time series input data required for ResEvap computations*

.. csv-table::
   :header: "Parameter", "Parameter Type", "Time Step", "Units", "Default", "Optional"
   :widths: auto

   "Wind Speed", "Instantaneous", "1Hour", "|m/s|", "None", "No"
   "Air Temperature", "Instantaneous", "1Hour", "|degC|", "None", "No"
   "Relative Humidity", "Instantaneous", "1Hour", "|percent|", "None", "No"
   "Air Pressure", "Instantaneous", "1Hour", "|mb|", "None", "No"
   "Low Cloud Fraction", "Instantaneous", "1Hour", "|percent|", "0.54", "Yes"
   "Low Cloud Height", "Instantaneous", "1Hour", "|m|", "Estimated", "Yes"
   "Middle Cloud Fraction", "Instantaneous", "1Hour", "|percent|", "0.0", "Yes"
   "Middle Cloud Height", "Instantaneous", "1Hour", "|m|", "Estimated", "Yes"
   "High Cloud Fraction", "Instantaneous", "1Hour", "|percent|", "0.0", "Yes"
   "High Cloud Height", "Instantaneous", "1Hour", "|m|", "Estimated", "Yes"
   "Reservoir Pool Elevation", "Instantaneous", "1Hour", "|m|", "None", "No"
   "Water Temperature (0.5-m layers)", "Instantaneous", "1Day", "|degC|", "None", "No"


In addition to time series data, ResEvap requires the GMT offset, version name,
latitude, longitude, and observation heights for wind speed, air temperature and
relative humidity. It also requires an elevation-area rating curve. Note that ResEvap
does not incorporate vertical datum information, so elevation readings must use the
same vertical datum as the rating curve.

Output Data
~~~~~~~~~~~

ResEvap produces both meteorological and water temperature information which is
returned to the ResEvapAlgo class for storage into the CWMS database. Table 3
summarizes the time series data produced by ResEvap. Note that the Water Temperature
layers must be in the same increment as the input data, which by default is 0.5m.

*Table 3 - Output data produced by ResEvap computations*

.. csv-table::
   :header: "Parameter", "Parameter Type", "Time Step", "Units"
   :widths: auto

   "Solar Radiation", "Instantaneous", "1Hour", "|W/m2|"
   "Downwelling Longwave Radiation", "Instantaneous", "1Hour", "|W/m2|"
   "Upwelling Longwave Radiation", "Instantaneous", "1Hour", "|W/m2|"
   "Water Surface Temperature", "Instantaneous", "1Hour", "|degC|"
   "Sensible Heat Flux", "Instantaneous", "1Hour", "|W/m2|"
   "Latent Heat Flux", "Instantaneous", "1Hour", "|W/m2|"
   "Evaporation", "Instantaneous, Cumulative", "1Hour, 1Day", "|mm|"
   "Evaporation as Flow", "Average", "1Day", "|m3/s|"
   "Water Temperature (0.5-m layers)", "Instantaneous", "1Hour", "|degC|"


ResEvap builds these output time series based on the location and version name.
As the compute progresses in time, the hourly time series are filled with compute
results. At the end of the simulation, hourly evaporation values are aggregated to produce
daily evaporation totals. Evaporation as flow is then computed from the total daily evaporation volume.

Evaporation volume at each hourly timestep is computed using the average evaporation depth and
the average pool elevation for that hour. A frustum-based method is used to account for the change
in reservoir surface area with elevation.

Each hourly evaporation volume is computed as:

:math:`V_{t} = \dfrac{E_{t}}{3} \left( A_{1_t} + A_{2_t} + \sqrt{A_{1_t} A_{2_t}} \right)`

Where:

    .. csv-table::
       :widths: auto

       ":math:`V_{t}`", "Evaporation volume at time :math:`t`"
       ":math:`E_{t}`", "Evaporation depth at time :math:`t`"
       ":math:`A_{1_t}`", "Reservoir surface area at elevation :math:`e_t` (before evaporation)"
       ":math:`A_{2_t}`", "Reservoir surface area at elevation :math:`e_t - E_t` (after evaporation)"

The total daily evaporation volume, calculated as the sum of the hourly volumes, is converted to flow as:

:math:`E_{f_t} = \frac{\sum V_{t}}{T}`

Where:

    .. csv-table::
        :widths: auto

        ":math:`E_{f_t}`", "Evaporation as flow at time :math:`t`"
        ":math:`\sum V_{t}`", "Total daily evaporation volume"
        ":math:`T`", "Number of seconds in the day (typically 86400, or 82800 if daylight savings applies)"


Evaporation Computations
------------------------

Evaporation computations are performed in the EvapWater class. The evaporation
computations rely on nine input variables:

    .. csv-table::
       :header: "Parameter", "Description"
       :widths: auto

       ":math:`T_{s}`", "Water surface temperature"
       ":math:`\widehat{T_{a}}`", "Air temperature measurement"
       ":math:`h_{T}`", "Reference height of the air temperature measurements"
       ":math:`\widehat{RH}`", "Relative humidity measurement"
       ":math:`h_{q}`", "Reference height of the relative humidity measurements"
       ":math:`\hat{u}`", "Wind speed measurement"
       ":math:`h_{u}`", "Reference height of the wind speed measurements"
       ":math:`\widehat{p_{a}}`", "The measured air pressure"
       ":math:`l_{v}`", "Latent heat of vaporization"

Note that all variables are described in `Appendix 1`_, and all variables with
a :math:`\widehat{\ }` accent are observed data. From these variables, an
iterative computation is performed to produce the output variables: sensible
heat (:math:`H_{s}`), latent heat (:math:`H_{l}`), and evaporation (:math:`E`).
Iterations are required due to the implicit definition of the turbulent transfer
coefficients, where the exchanges of momentum, energy and mass are codependent
with the Obukhov length (:math:`l_{o}`). Therefore, the computations setup
initial estimates of the transfer coefficients
(:math:`C_{D}` for wind, :math:`C_{T}` for temperature and :math:`C_{q}` for humidity)
then estimate the Obukhov length, and iteratively recompute the turbulent
exchange scales (:math:`u_{*}` for wind, :math:`t_{*}` for temperature and
:math:`q_{*}` for humidity) until convergence. Based on the turbulent exchange
values, the resulting evaporation, sensible heat and latent heat may be
computed as follows:

:math:`H_{l} = - \rho_{a}l_{v}u_{*}q_{*}`

:math:`H_{s} = - \rho_{a}c_{p}^{T_{s}}u_{*}t_{*}`

:math:`E = \dfrac{H_{l}}{l_{v}\rho_{w}} \left(86400 \frac{s}{day} 10^{3} \frac{mm}{m} \right)`

Static Variables
~~~~~~~~~~~~~~~~

Evaporation computations start by computing several values that are static
across the iterative algorithm. These include:

    .. csv-table::
        :widths: auto

        :math:`\overline{T_{a}}`, "Vertically averaged air temperature"
        :math:`\theta_{r}`, "Potential temperature"
        :math:`\overline{q}`, "Vertically averaged specific humidity"
        :math:`\rho_{a}`, "Density of the air"
        :math:`\nu_{s}`, "Kinematic viscosity"

Additionally, the :math:`\mathrm{\Delta}_{t}` and :math:`\mathrm{\Delta}_{q}` terms are
computed, which represent differences in temperature and specific humidity
required for computing the Monin-Obukhov similarity scaling parameters.
These initial computations are described in the equations below:

:math:`\overline{T_{a}} = 0.5\left( T_{s} - \widehat{T_{a}} \right)`

:math:`\mathrm{\Delta}_{t} = T_{s} - \theta_{r}`

Where :math:`\theta_{r}` is the potential temperature, as computed below:

:math:`\theta_{r} = \widehat{T_{a}} + \dfrac{g}{c_{p}^{\widehat{T_{a}}}}h_{t}`

:math:`c_{p}^{T} = 1005.60\  + (T - T_{FP}) \Bigl(0.017211\  + \ 0.000392(T - T_{FP})\Bigr)`


Where :math:`T_{FP}` is the freezing point of water in Kelvin, and :math:`g` is
Where:

    .. csv-table::
        :widths: auto

        ":math:`g`", "Gravitational acceleration"
        ":math:`T_{FP}`", "Freezing point in Kelvin"
        ":math:`c_{p}^{T}`", "Specific heat of air based on reference temperature :math:`T`"

In the above formulation :math:`c_{p}^{T}` is only valid for the range :math:`- 233.15K < T < 313.15K`,
which is a range that surface reservoirs within the USA will rarely exceed.

:math:`\mathrm{\Delta}_{q} = q_{s} - q_{r}`

:math:`q = \dfrac{\rho_{v}}{\rho_{a}}`

:math:`\rho_{a} = \rho_{d} + \rho_{v} = \dfrac{100e(1 - 0.000537*S)m_{w}}{R_{g}T_{a}} \
+ 1.2923\left(\dfrac{T_{FP}}{T_{a}}\right)\left(\dfrac{\widehat{p_{a}}}{1013.25}\right)`

Where:

    .. csv-table::
       :widths: auto

       ":math:`\rho_{a}`", "Density of the air at the water surface"
       ":math:`\rho_{d}`", "Density of dry air"
       ":math:`\rho_{v}`", "Water vapor density"
       ":math:`R_{g}`", "Ideal gas constant"
       ":math:`e`", "Vapor pressure"
       ":math:`S`", "Salinity (assumed to be zero)"
       ":math:`m_{w}`", "Molecular weight of water"
       ":math:`q_{s}`", "Solved by setting :math:`T_{a} = T_{s}` and :math:`RH = 1`"
       ":math:`q_{r}`", "Computed by setting :math:`T_{a} = \widehat{T_{a}}` and :math:`RH = \widehat{RH}`"


:math:`e_{s} = \left\{
\begin{matrix}
{( 0.00000346\, \widehat{p_{a}} + 1.0007 )6.1121e}^{\left(\frac{17.502{(T}_{a} - \
T_{FP})}{240.97 + {(T}_{a} - T_{FP})} \right)} & \text{over water} \\
{( 0.00000418\: \widehat{p_{a}} + 1.0003 )6.1115e}^{\left(\frac{22.452{(T}_{a} - \
T_{FP})}{272.55 + {(T}_{a} - T_{FP})} \right)} & \text{over ice}
\end{matrix}
\right.\ `

:math:`e = \widehat{\dfrac{RH}{100}}e_{s}`

Where :math:`e_{s}` is the saturation vapor pressure, and :math:`e` is the
actual vapor pressure.

Additionally, the following computations require the kinematic viscosity of the
air at the water surface, which is described below:

:math:`\nu_{s} = 0.00001326 \biggl(1.0 + T_{s}* \Bigl(0.006542 + T_{s}*(0.000008301 - 0.00000000484T_{s}) \Bigr) \biggr)`

Finally, the latent heat of vaporization or sublimation is needed for computing
the latent heat flux, which is described below:

:math:`l_{v} = \left\{
\begin{matrix}
\bigl( 28.34 - 0.00149\left( T_{s} - T_{k} \right) \bigr) 10^{5} & T_{s} < T_{FP} \\
\bigl( 25 - 0.02274\left( T_{s} - T_{k} \right) \bigr) 10^{5} & T_{s} \geq T_{FP}
\end{matrix}
\right.\ `

Based on these static variables, the iterative solution of the evaporation can begin.

Iterative Computations
~~~~~~~~~~~~~~~~~~~~~~

After computation of the initial variables, an initial iteration is performed to
estimate the Monin-Obukhov similarity (MOS) scaling parameters
:math:`\left(u_*, T_*, \text{ and } q_* \right)`, which represent the turbulent
exchanges of latent and sensible heat :math:`\left( H_l \text{ and } H_s \right)`.
These initial estimates assume neutral stratification
:math:`( \text{i.e } \frac{h_u}{l_o} = 0)`.
Estimating these parameters requires an initial estimate of the wind
friction velocity :math:`( u_{*})`, as shown below:

:math:`u_{*} = \hat{u}\sqrt{C_{d}}`

Where :math:`\hat{u}` is the observed wind speed, and :math:`C_{d}` is the drag coefficient.

The Donelan method initially estimates the drag coefficient (:math:`C_{d}`) as follows:

:math:`C_{d} = (0.37 + 0.137\hat{u} )10^{- 3}`

Where the Fischer method initially estimates the drag coefficient (:math:`C_{d}`) based
on the wind speed measurement (:math:`\hat{u}`) as follows:

:math:`C_{d} = \left\{
\begin{matrix}
{0.001} & \hat{u} < 5.0 \\
{0.0015} & \hat{u} > 15.0 \\
{0.001 + 0.0005\left( \frac{\hat{u} - 5}{10} \right)} & \text{otherwise}
\end{matrix}
\right.\ `

Note that the shear velocity is not allowed to drop below 0.01. The remaining
computations require roughness lengths for momentum
:math:`(z_u)`, temperature :math:`(z_T)` and humidity :math:`(z_q)`, which are
estimated by the COARE algorithm :ref:`(Fairall et al., 1996) <References>`.

:math:`z_{u} = h_{u}e^{\frac{- \kappa}{\sqrt{C_{d}}}} + C_{s}\dfrac{\nu_{s}}{\ u_{*}}`

:math:`z_{T} = a_{t}\dfrac{\nu_{s}}{u_{*}}{{R_{e}}^{*}}^{b_{t}}`

:math:`z_{q} = a_{q}\dfrac{\nu_{s}}{u_{*}}{{R_{e}}^{*}}^{b_{q}}`

Where :math:`C_{s}` is the smooth surface coefficient, *𝜅* is the von Karman
constant, :math:`{R_e}^{*}` is the roughness Reynolds number defined below,
and the COARE algorithm coefficients :math:`(a_{t}, b_{t}, a_{q}, b_{q})`
are performed with a table lookup based on :math:`{R_e}^{*}` (see Table 4).

:math:`{R_{e}}^{*} = \dfrac{\ u_{*}z_{u}}{\nu_{s}}`

.. _Table 4:

*Table 4 - Coefficients for the COARE algorithm*

.. csv-table::
   :header: ":math:`\mathbf{R_e}^{*}`", ":math:`\mathbf{a_t}`", ":math:`\mathbf{b_t}`", ":math:`\mathbf{a_q}`", ":math:`\mathbf{b_q}`"
   :widths: 1 1 1 1 1

   "0.135", "0.177", "0.0", "0.292", "0.0"
   "0.16", "1.376", "0.929", "1.808", "0.826"
   "1", "1.376", "0.929", "1.808", "0.826"
   "3", "1.026", "-0.599", "1.393", "-0.528"
   "10", "1.625", "-1.018", "1.956", "-0.87"
   "30", "4.661", "-1.475", "4.994", "-1.297"
   "100", "34.904", "-2.067", "30.709", "-1.845"
   "300", "1667.19", "-2.907", "1448.68", "-2.682"
   "1000", "5.88E+05", "-3.935", "2.98E+05", "-3.616"


Based on the roughness lengths, the transfer coefficients can be computed as follows:

:math:`C_{m} = \dfrac{{\kappa\ }^{2}}{\left( \ln\left( \frac{h_{u}}{h_{m}} \right) - \
\psi_{m} \right) \left(\ln\left( \frac{z_{0}}{z_{m}} \right) - \psi_{m}\right)}`

Where:

    :math:`h_{m} = h_{u}, z_{m} = z_{u}, \psi_{m} = \psi_{u} \text{ for } C_{D}`

    :math:`h_{m} = h_{T}, z_{m} = z_{T}, \psi_{m} = \psi_{T} \text{ for } C_{T}`

    :math:`h_{m} = h_{q}, z_{m} = z_{q}, \psi_{m} = \psi_{q} \text{ for } C_{q}`


:math:`\psi_{m} = \left\{
\begin{array}{cl}
2\ln\bigl( 0.5( 1 + x )\bigr) + 2\ln\bigl( 0.5 * ( 1 + x^{2} )\bigr) -2\tan^{-1} (x) + 1.570796  & \zeta < 0 \quad m = u \\
2\ln\bigl( 0.5(1 + x^{2} )\bigr)                                                                 & \zeta < 0 \quad m = T \text{ or } q \\
0                                                                                                & \hspace{.8cm} \zeta = 0      \\
-\bigl(0.7\zeta + 0.75(\zeta - 14.3)e^{-0.35\zeta} + 10.7\bigl)                                  & \hspace{.6cm} \zeta \leq 250  \\
-(0.7\zeta + 10.7)                                                                               & \hspace{.6cm} \zeta > 250
\end{array}
\right.\ `

    :math:`\zeta = \dfrac{h_{m}}{l_{o}}`

    :math:`{x = (1 - \ 16\zeta)}^{0.25}`

    Where :math:`\psi_{m} = 0` for the initial iteration.

From the above equations, the initial MOS scaling parameters can be computed as follows:

:math:`t_{*} = - \left(\dfrac{C_{T}\hat{u}\mathrm{\Delta}_{T}}{u_{*}}\right)`

:math:`q_{*} = - \left(\dfrac{C_{q}\hat{u}\mathrm{\Delta}_{q}}{u_{*}}\right)`

The final step in the first iteration is to compute the Obukhov length :math:`(l_o)` as follows:

:math:`l_{o} = \dfrac{\dfrac{\overline{T_{a}}u_{*}\ }{kg}}{t_{*} + \
\left(\dfrac{0.61\overline{T_{a}}q_{*}}{1 + 0.61\overline{q}}\right)}`

With these initial estimates, the evaporation routine will begin iteratively
estimating the MOS similarity scales, where a maximum of 20 iterations will be
performed. The stopping criteria of the process is when:

:math:`\dfrac{\left| u_{*_i} - {u_{{*}_{i - 1}}} \right|}{u_{*_i}} < 0.001 \text{  and  } \
\dfrac{\left| t_{*_i} - {t_{{*}_{i - 1}}} \right|}{t_{*_i}} < 0.001 \text{  and  } \
\dfrac{\left| q_{*_i} - {q_{{*}_{i - 1}}} \right|}{q_{*_i}} < 0.001`

Where |i| denotes the iteration number.

The iterations proceed as follows. Compute the transfer coefficients :math:`(C_{D}, C_{T}\text{ and } C_{q})`
with :math:`h_{u} = 10m` and the current estimates of :math:`l_{o}`, :math:`z_{u}`,
:math:`z_{T}` and :math:`z_{q}`. This step subsequently provides an estimate of the MOS similarity
scales. Recompute the transfer coefficients based on the current MOS similarity
scales and the actual :math:`h_{u}`. Modify wind speed to account for gustiness
as shown below:

:math:`u = \left\{
\begin{matrix}
{ \sqrt{ \hat{u}^{2} + 1.25^{2} \left( u_{*}\left( \frac{- 600.0}{\kappa\ l_{o}} \
\right)^{\frac{1}{3}} \right)^{2} } } & \text{ unstable stratification } (l_{o} < 0) \\
{\hat{u} + 0.5} & \text{ stable stratification } (0 \leq l_{o} < 1000) \\
{\hat{u}} & \text{ neutral stratification } (l_{o} \leq 1000)
\end{matrix}
\right.\ `

Finally recompute the MOS similarity scales and the Obukhov length, then apply
the convergence test. After the interative process is completed, compute the
sensible heat, latent heat and evaporative fluxes.

Radiation Computations
----------------------

Shortwave Radiation
~~~~~~~~~~~~~~~~~~~

Solar radiation provides energy to the water surface during daylight hours, and
is therefore a key component of the energy balance. The intensity of solar
radiation reaching the water surface is a function of both the zenith angle of
the sun, and the extent to which the atmosphere obscures radiation. The zenith
angle is affected by both seasonal and diurnal cycles, as well as the latitude
(:math:`\varphi`) of the reservoir. All computations of solar angles are based
on :ref:`Woolf (1968) <References>`. Seasonal affects on the solar radiation are
represented by the declination angle (:math:`\delta`), which ranges
from -23.44 to 23.44. Computations of the declination angle requires the below
equation, which converts the day of year to an angle:

:math:`d = \frac{360}{365.242}(JD - 1)`

Where :math:`JD` is the Julian day, with :math:`JD = 1` on January 1\ :sup:`st`.
This can be converted to the declination angle below:

:math:`\left.
\begin{array}{l}
\sin(\delta) = \sin(23.44)\sin\Bigl( 279.9348 + d + 1.914827\sin(d) - 0.079525\cos(d) \; + \\
\hspace{5.5cm} 0.019938 \bigl(2\sin(d)\cos(d)\bigr) - 0.001639 \bigl(2\cos^{2}(d) - 1\bigr)\Bigr)
\end{array} \right.`

The diurnal fluctuations of solar radiation are represented by the Hour Angle
:math:`(h_{s})`, as computed below:

:math:`h_{s} = 15\left( h_{gmt} - M \right) - lon`

Where :math:`h_{gmt}` is the hour of the day in GMT, :math:`lon` is the
longitude, and :math:`M` is the time of meridian passage computed below:

:math:`\left.
\begin{array}{l}
M = 12 + 0.12357\sin(d) - 0.004289\cos(d) + 0.153809\bigl( 2\sin(d)\cos(d) \bigr) \: + \\
\hspace{2cm} 0.060783 \bigl(2\cos^{2}(d) - 1 \bigr)
\end{array} \right.`

Based on the declination, the latitude and the hour angle, the zenith angle may be computed as follows:

:math:`\cos\left( \theta_{s} \right) = \sin(\varphi)\sin(\delta) + cos(\varphi)\cos{(\delta)cos(h_{s}})`

:math:`\theta_{s} = \cos^{- 1}\bigl(\cos( \theta_{s} )\bigr)`

Based on the zenith angle, and the cloud cover fraction at the low, middle, and
high layers of the atmosphere, the solar radiation reaching the water surface
is computed based on :ref:`Shapiro (1987) <References>`. In this document,
the derivation of the general case to the 3-layer implementation is not provided,
due to it’s complexity. For information on this derivation, see
:ref:`Shapiro (1987) <References>`. This is strictly the equation for the
3-layer case used in ResEvap:

:math:`I_{s \downarrow} = \dfrac{S_{e}T_{l}T_{m}T_{h}}{d_{l}\left( d_{h}d_{m} - \
R_{h}R_{l}{T_{m}}^{2} \right) - d_{h}R_{m}R_{w}{T_{l}}^{2} - R_{h}R_{w}{T_{m}}^{2}{T_{l}}^{2}}`

Where:

    .. csv-table::
        :widths: auto

        :math:`I_{s \downarrow}`, "Incoming solar radiation at the water surface"
        ":math:`T_{l}`, :math:`T_{m}`, and :math:`T_{h}`", "Transmissivities of the low, middle and high atmospheric layers"
        ":math:`R_{l}`, :math:`R_{m}`, and :math:`R_{h}`", "Reflectance of the low, middle and high atmospheric layers"
        ":math:`d_{l}`, :math:`d_{m}`, and :math:`d_{h}`", "Interactions between the different layers"
        :math:`S_{e}`, "Extraterrestrial solar radiation on a horizontal plane in :math:`W/m^{2}`"


    :math:`d_{h} = 1 - R_{h}R_{m}`

    :math:`d_{m} = 1 - R_{m}R_{l}`

    :math:`d_{l} = 1 - R_{l}R_{g}`

    :math:`S_{e} = 1369.2\Biggl( 1.0001399 + 0.0167261cos\left(\dfrac{2\pi(JD - 2)}{365.242}\right) \
    \Biggr)^{2} \cos( \theta_{s})`

In the above equations, :math:`R_{k}` and :math:`T_{k}` are a composite of the
overcast :math:`\left( R_{k}^{o}, T_{k}^{o} \right)` and clear sky
:math:`\left( R_{k}^{c} , T_{k}^{c} \right)` values, where a weight is determined
based on the zenith angle and the fractional cloud cover :math:`\left(f_{c_k}\right)`
in each layer |k|, and coefficients from Tables 5 to 9.

:math:`R_{k} = W_{k}R_{k}^{o} + \left( 1 - W_{k} \right)R_{k}^{c}`

:math:`T_{k} = W_{k}T_{k}^{o} + \left( 1 - W_{k} \right)T_{k}^{c}`

:math:`R_{k}^{c} = {r^{c}_{k_0}} + {r^{c}_{k_1}}\cos( \theta_{s} ) + \
{r^{c}_{k_2}}{\cos( \theta_{s} )}^{2} + {r^{c}_{k_3}}{\cos( \theta_{s} )}^{3}`

:math:`R_{k}^{o} = {r^{o}_{k_0}} + {r^{o}_{k_1}}\cos( \theta_{s} ) + \
{r^{o}_{k_2}}{\cos( \theta_{s} )}^{2} + {r^{o}_{k_3}}{\cos( \theta_{s} )}^{3}`

:math:`T_{k}^{c} = {t^{c}_{k_0}} + {t^{c}_{k_1}}\cos( \theta_{s} ) + \
{t^{c}_{k_2}}{\cos( \theta_{s} )}^{2} + {t^{c}_{k_3}}{\cos( \theta_{s} )}^{3}`

:math:`T_{k}^{o} = {t^{o}_{k_0}} + {t^{o}_{k_1}}\cos( \theta_{s} ) + \
{t^{o}_{k_2}}{\cos( \theta_{s} )}^{2} + {t^{o}_{k_3}}{\cos( \theta_{s} )}^{3}`

:math:`W_{k} = \left\{ \begin{matrix}
0 & f_{c} < 0.05 \\
1 & f_{c} > 0.95 \\
{c_{k_o}} + {c_{k_1}}\cos( \theta_{s} ) + {c_{k_2}}{f_{c_k}} + {c_{k_3}}\cos( \theta_{s} ){f_{c_k}} \
+ {c_{k_4}}{\cos( \theta_{s} )}^{2} + {c_{k_5}}{f_{c_k}}^{2} & otherwise
\end{matrix} \right.\ `

.. _Table 5:

*Table 5- Coefficients for the clear sky reflectivity computations*

.. csv-table::
   :header: "", ":math:`\mathbf{r^{c}_{k_0}}`", ":math:`\mathbf{r^{c}_{k_1}}`", ":math:`\mathbf{r^{c}_{k_2}}`", ":math:`\mathbf{r^{c}_{k_3}}`"
   :widths: 1 2 2 2 2

   "Low", "0.15946", "-0.42185", "0.48800", "-0.18492"
   "Mid", "0.15325", "-0.39620", "0.42095", "-0.14200"
   "High", "0.12395", "-0.34765", "0.39478", "-0.14627"


.. _Table 6:

*Table 6- Coefficients for the clear sky transmissivity computations*

.. csv-table::
   :header: "", ":math:`\mathbf{t^{c}_{k_0}}`", ":math:`\mathbf{t^{c}_{k_1}}`", ":math:`\mathbf{t^{c}_{k_2}}`", ":math:`\mathbf{t^{c}_{k_3}}`"
   :widths: 1 2 2 2 2

   "Low", "0.68679", "0.71012", "-0.71463", "0.22339"
   "Mid", "0.69318", "0.68227", "-0.64289", "0.17910"
   "High", "0.76977", "0.49407", "-0.44647", "0.11558"


.. _Table 7:

*Table 7- Coefficients for the overcast reflectivity computations*

.. csv-table::
   :header: "", ":math:`\mathbf{r^{o}_{k_0}}`", ":math:`\mathbf{r^{o}_{k_1}}`", ":math:`\mathbf{r^{o}_{k_2}}`", ":math:`\mathbf{r^{o}_{k_3}}`"
   :widths: 1 2 2 2 2

   "Low", "0.69143", "-0.14419", "-0.05100", "0.06682"
   "Mid", "0.61394", "-0.01469", "-0.17400", "0.14215"
   "High", "0.42111", "-0.04002", "-0.51833", "0.40540"

.. _Table 8:

*Table 8- Coefficients for the overcast transmissivity computations*

.. csv-table::
   :header: "", ":math:`\mathbf{t^{o}_{k_0}}`", ":math:`\mathbf{t^{o}_{k_1}}`", ":math:`\mathbf{t^{o}_{k_2}}`", ":math:`\mathbf{t^{o}_{k_3}}`"
   :widths: 1 2 2 2 2

   "Low", "0.15785", "0.32410", "-0.14458", "0.01457"
   "Mid", "0.23865", "0.20143", "-0.01183", "-0.07892"
   "High", "0.43562", "0.26094", "0.36428", "-0.38556"


.. _Table 9:

*Table 9- Coefficients for the clear sky and overcast weighting computations*

.. csv-table::
   :header: "", ":math:`\mathbf{c_{k_0}}`", ":math:`\mathbf{c_{k_1}}`", ":math:`\mathbf{c_{k_2}}`", ":math:`\mathbf{c_{k_3}}`", ":math:`\mathbf{c_{k_4}}`", ":math:`\mathbf{c_{k_5}}`"
   :widths: 1 2 2 2 2 2 2

   "Low", "1.512", "-1.176", "-2.160", "1.420", "-0.032", "1.422"
   "Mid", "1.429", "-1.207", "-2.008", "0.853", "0.324", "1.582"
   "High", "1.552", "-1.957", "-1.762", "2.067", "0.448", "0.932"


Longwave Radiation
~~~~~~~~~~~~~~~~~~

Longwave radiation both adds and removes energy from the reservoir. Outgoing
longwave radiation (:math:`I_{l \uparrow})` is the energy emitted by the reservoir,
representing a loss of energy, and :math:`T_{s}` is a function of the water
surface temperature, as shown in the equation below:

:math:`I_{l \uparrow} = \varepsilon_{w}\sigma{T_{s}}^{4}`

Where :math:`\sigma` is the Stefan-Boltzmann constant and :math:`\varepsilon_{w}`
is the emissivity of water.

Incoming longwave radiation :math:`\left(I_{l \downarrow}\right)` is radiation
emitted by the atmosphere that reaches the water surface. Within ResEvap, the
incoming longwave radiation is computed as the sum of the clear sky component
:math:`\left({I_{l \downarrow}}_{clear}\right)` and the cloud component
:math:`\left({I_{l \downarrow}}_{cloud}\right)`.

:math:`I_{l \downarrow} = {I_{l \downarrow}}_{clear} + {I_{l \downarrow}}_{cloud}`

The clear sky component is a function of the emissivity of the atmosphere
:math:`\left(\varepsilon_{atm}\right)`, and the measured air temperature:

:math:`{I_{l \downarrow}}_{clear} = \varepsilon_{atm}\sigma{\widehat{T_{a}}}^{4}`

Where the emissivity of the atmosphere is a function of the vapor pressure of
the atmosphere (:math:`e_{a}`) and measured air temperature, based
on :ref:`Crawford et al. (1999) <References>`:

:math:`\varepsilon_{atm} = 1.24\left( \frac{{\ e}_{a}}{\widehat{T_{a}}} \right)^{\frac{1}{7}}`

Similar to the evaporation computations, the vapor pressure is a function of the
saturation vapor pressure and the relative humidity:

:math:`{\ e}_{a} = \widehat{RH}*e_{s}`

Unlike the evaporation computations, the saturation vapor pressure is computed
with the Clausius-Clapeyron equation:

:math:`e_{s} = 6.13e^{\frac{l_{v}}{R_{v}}\left( \frac{1}{T_{k}} - \frac{1}{\widehat{\widehat{T_{a}}}} \right)}`

Where :math:`l_{v}` is the latent heat of vaporization, :math:`R_{v}` is the gas
constant for water vapor :math:`\left(461 \frac{J}{kg*K}\right)`. Note that this
is different than the formulation of saturation vapor pressure used in the
evaporation computations. This difference is likely a result of the radiation
model not using air pressure, but the differing computations is expected to have
negligible effects on the resulting longwave radiation computations.

:math:`l_{v} = \left( 3.166659 - 0.00243\widehat{T_{a}} \right)10^{6}`

Similar to :math:`e_{s}`, the formulation of :math:`l_{v}` is different than in
the evaporation computations. To be numerically equivalent, the equation would be:

:math:`l_{v} = \left( 3.1211431 - 0.002274\widehat{T_{a}} \right)10^{6}`

Although different, this is still expected to have negligible impacts on the
resulting longwave radiation computations.

The incoming longwave radiation from the cloud component of the atmosphere is a
function of the cloud cover in each layer :math:`(f_{c_k})` and
the height of the clouds in each layer :math:`(h_{c_k})`, as shown below:

:math:`\left.
\begin{array}{l}
{I_{l \downarrow}}_{cloud} = {f_{c_l}}( 94 - 5.8{h_{c_l}} ) \; + \\
\hspace{3cm}{f_{c_m}}(1 - {f_{c_l}})( 94 - 5.8{h_{c_m}} ) + {f_{c_h}}(1 - {f_{c_m}})(1 - {f_{c_l}})( 94 - 5.8{h_{c_h}})
\end{array} \right.`

:math:`\qquad`

:math:`{h_{c_k}} = \left( \begin{matrix}
{h_{c_k}} (\text{observed}) & \text{observed height available} \\
a\  - \ b*\Bigl( 1.0 - \Bigl| cos\bigl(c*(lat - d) \bigr) \Bigr|\Bigr) & \text{otherwise}
\end{matrix} \right.\ `

Table 10, Table 11, Table 12, and Table 13 provide the coefficients for computing
the cloud heights in the absence of observations.

.. _Table 10:

*Table 10 - Cloud height coefficients: Winter and latitude < 25*

.. csv-table::
    :header: "", "a", "b", "c", "d"
    :widths: 1 2 2 2 2

    "Low", "1.05", "0.6", "5.0", "25.0"
    "Mid", "4.1", "0.3", "4.0", "25.0"
    "High", "7.0", "1.5", "3.0", "30.0"


.. _Table 11:

*Table 11 - Cloud height coefficients: Winter and latitude > 25*

.. csv-table::
    :header: "", "a", "b", "c", "d"
    :widths: 1 2 2 2 2

    "Low", "1.05", "0.6", "1.5", "25.0"
    "Mid", "4.1", "2.0", "1.7", "25.0"
    "High", "7.0", "1.5", "3.0", "30.0"

.. _Table 12:

*Table 12 - Cloud height coefficients: Non-Winter Season and latitude < 25*

.. csv-table::
    :header: "", "a", "b", "c", "d"
    :widths: 1 2 2 2 2

    "Low", "1.15", "0.45", "5.0", "25.0"
    "Mid", "4.1", "2.0", "1.7", "25.0"
    "High", "7.0", "1.5", "3.0", "30.0"


.. _Table 13:

*Table 13 - Cloud height coefficients: Non-Winter Season and latitude > 25*

.. csv-table::
    :header: "", "a", "b", "c", "d"
    :widths: 1 2 2 2 2

    "Low", "1.15", "0.6", "1.5", "25.0"
    "Mid", "4.4", "1.2", "3.0", "25.0"
    "High", "7.0", "1.5", "3.0", "30.0"


Vertical Temperature Profile Computations
-----------------------------------------

Vertical transfer of heat within a reservoir is assumed to be a one-dimensional
process, where the reservoir is assumed to be laterally homogeneous. This allows
for ignoring effects of reservoir inflows and outflows. In the event that there
is a large lateral variation in temperature (i.e. long run-of-the-river reservoirs),
these computations will be unreliable. General guidance provided here is reservoirs with
a flushing time less than 30 days will violate the assumption of laterally homogeneity,
and therefore the vertical temperature profile computations should only be applied for
reservoirs with a flushing time greater than 30 days. Based on this assumption, vertical transfer
of heat is modeled first by assuming stable reservoir stratification, accounting
for diffusion of heat, and then accounting for any convective or turbulent mixing
that occurs in the reservoir profile. Vertical diffusion of heat within a
one-dimensional reservoir is governed by the equation below
:ref:`(Hondzo and Stefan 1993) <References>`:

:math:`\dfrac{dT_{w}}{dt} = \dfrac{1}{A}\dfrac{d}{dz}\left( K_{z}A\dfrac{dT_{w}}{dz} \right) + \
\dfrac{I_{z}}{\rho_{w}c_{p}}`

Where:

    .. csv-table::
       :widths: auto

       ":math:`T_{w}`", "Water temperature in |tempK|"
       ":math:`A`", "Area through which the heat is transferred"
       ":math:`K_{z}`", "Thermal diffusivity"
       ":math:`z`", "Depth"
       ":math:`I_{z}`", "Net radiation"
       ":math:`\rho_{w}`", "Density of water"
       ":math:`c_{p}`", "Heat capacity"

In order to initialize the computations, the density and heat capacity must be updated for
each layer.

:math:`{\rho_{w_i}} = 1000 - 0.019549\left| {T_{w_i}} - 277.15 \right|^{1.68}`

:math:`{c_{p_i}} = 4174.9 + 1.6659\left( e^\left({\frac{307.65 - {T_{w_i}}}{10.6}}\right) + \
e^ {-\left({\frac{307.65 - {T_{w_i}}}{10.6}}\right)} \right)`

In the above equations, |i| is the index of the layer, where :math:`i = 1`
is the bottom layer of the temperature profile. Next the thermal diffusivity is
computed for each layer as follows:

:math:`{K_{z_i}} = 0.00012\left( 0.000817{A_{s}}^{0.56}\left( {N_{i}}^{2} \right)^{- 0.43} \right)`

:math:`{N_{i}}^{2} = max\left(0.00007,\ \dfrac{g}{\overline{\rho_{w}}} \, \dfrac{{\rho_{w}}_{i} - \
{\rho_{w}}_{i - 1}}{z_{i} - z_{i - 1}}\right)`

:math:`\overline{\rho_{w}} = \dfrac{\sum_{i = 1}^{N}{{\rho_{w_i}}V_{i}}}{\sum_{i = 1}^{N}V_{i}}`

Where:

    .. csv-table::
       :widths: auto

       ":math:`\overline{\rho_{w}}`", "Average density over the entire water column"
       ":math:`z_{i}`", "Depth of the top of layer |i|"
       ":math:`N_{i}`", "Stability frequency of layer |i|"
       ":math:`A_{s}`", "Water surface area"

Note that :math:`\overline{\rho_{w}}` is computed as a volumetric
average, but should be the vertical average since this is a one-dimensional model.
Additionally, the net radiation of layer |i| is computed as follows:

:math:`{I_{z_i}} = \left\{ \begin{matrix}
\Bigl( I_{s \downarrow}\beta(1 - \alpha) + I_{l \downarrow} - I_{l \uparrow} - H_{l} - H_{s} \Bigr) \
\frac{A_{i}}{V_{i}} & \text{Surface Layer} \\
I_{s \downarrow}\beta(1 - \alpha)\frac{\left( e^\left({- \kappa_{a}z_{i}}\right) A_{i} - \
e^\left({- \kappa_{a}z_{i - 1}}\right) A_{i - 1} \right)}{V_{i}} & \text{otherwise}
\end{matrix} \right.\ `

:math:`\kappa_{a} = \dfrac{1.7}{SD}`

Where:

    .. csv-table::
       :widths: auto

       ":math:`I_{s \downarrow}`", "Incoming shortwave radiation"
       ":math:`\beta`", "Fraction of shortwave radiation that penetrates the water surface. :math:`(\beta = 0.4` is assumed)"
       ":math:`\alpha`", "Albedo. (:math:`\alpha = 0.08` is assumed for water)"
       ":math:`A_{i}^{u}`", "Area of the top of layer |i|"
       ":math:`\kappa_{a}`", "Bulk extinction coefficient for shortwave radiation"
       ":math:`SD`", "Secchi depth"
       ":math:`I_{l \downarrow}`", "Incoming longwave radiation"
       ":math:`I_{l \uparrow}`", "Outgoing longwave radiation"
       ":math:`H_{l}`", "Latent heat flux"
       ":math:`H_{s}`", "Sensible heat flux"

The assumed values for :math:`\beta` and :math:`\alpha` are reasonable for this application,
and can range from 0 to 1. Radiation computations and heat fluxes are described in previous sections.
The necessary areas for diffusion computations are described below:

:math:`A_{i} = f_{rating}\left( z_{i} \right)`

:math:`\overline{A_{l}} = \dfrac{A_{i} - A_{i - 1}}{2}`

In the above equations, :math:`f_{rating}` is the elevation-area rating function,
:math:`A_{i}`\ is the area of the top of layer |i|, and
:math:`\overline{A_{l}}` is the average area of layer |i|. Based on the
known information, ResEvap applies a discretized form of the vertical heat
diffusion equation. Discretization of the vertical diffusion equation is
performed below, using the theta method:

:math:`\dfrac{{T_{w}}_{i}^{t + 1} - {T_{w}}_{i}^{t}\ }{\mathrm{\Delta}t} = \
\dfrac{1}{\overline{A_{l}}}\,\dfrac{1}{\mathrm{\Delta}z}\left\lbrack {K_{z_i}}A_{i} \
\dfrac{{T_{w}}_{i + 1}^{t + \theta} - {T_{w}}_{i}^{t + \theta}}{\mathrm{\Delta}z} \right\rbrack \
+ \dfrac{{I_{z_i}}}{{\rho_{w_i}}{c_{p_i}}}`

:math:`{T_{w}}_{i}^{t + \theta} = \theta{T_{w}}_{i}^{t + 1} + (1 - \theta){T_{w}}_{i}^{t}`

Where:

    .. csv-table::
       :widths: auto

       ":math:`{T_{w}}_{i}^{t}`", "Temperature at the start of the timestep for layer |i|"
       ":math:`{T_{w}}_{i}^{t + 1}`", "Temperature at the end of the time step for layer |i|"
       ":math:`A_{i}`", "Area through which the heat is transferred"
       ":math:`\theta`", "Implicitness factor, which typically ranges from :math:`0.5 \leq \theta \leq 1`"


The solution for this equation follows the form below:

:math:`\left.
\begin{array}{l}
a_i{T_{w}}_{i - 1}^{t + 1} + b_i{T_{w}}_{i}^{t + 1} + c_i{T_w}_{i + 1}^{t + 1} = \\
\hspace{4.6cm} {T_w}_{i}^{t} + (1 - \theta)\Bigl( x^{u}( {T_w}_{i + 1}^{t} - {T_w}_{i}^{t} ) - x^{l}( {T_w}_{i}^{t} - \
{T_w}_{i - 1}^{t} ) \Bigr) + \dfrac{{I_{z_i}}}{{\rho_{w_i}}{c_{p_i}}}
\end{array} \right.`

:math:`x^{u} = \dfrac{\mathrm{\Delta}tA_{i}^{u}} {{\mathrm{\Delta}z}_{i}\overline{A_{l}}} \
\dfrac{ \frac{{K_{z_{i + 1}}} {\mathrm{\Delta}z}_{i + 1}} {{\rho_{w}}_{i + 1} {c_{p}}_{i + 1}} + \
\frac{{K_{z_i}} {\mathrm{\Delta}z}_{i}} { {\rho_{w_i}} {c_{p_i}}} } {0.5\left( {\mathrm{\Delta}z}_{i + 1} + \
{\mathrm{\Delta}z}_{i} \right)^{2}}`

:math:`x^{l} = \dfrac{\mathrm{\Delta}tA_{i}^{l}} {{\mathrm{\Delta}z}_{i}\overline{A_{l}}} \
\dfrac{ \frac{{K_{z_{i - 1}}} {\mathrm{\Delta}z}_{i - 1}} {{\rho_{w}}_{i - 1} {c_{p}}_{i - 1}} + \
\frac{{K_{z_i}} {\mathrm{\Delta}z}_{i}} { {\rho_{w_i}} {c_{p_i}}} } {0.5\left( {\mathrm{\Delta}z}_{i - 1} + \
{\mathrm{\Delta}z}_{i} \right)^{2}}`

:math:`a_{i} = - {\theta x}^{l}`

:math:`b_{i} = 1 + {\theta x}^{u} + {\theta x}^{l}`

:math:`c_{i} = - {\theta x}^{u}`

In the above equations, ResEvap assumes :math:`\theta = 1`, which makes it a
fully implicit solution. The provided equation is solved with the tridiagonal
algorithm, where :math:`a_{i}, b_{i}, \text{ and } c_{i}` are the diagonal
vectors, and the vector :math:`{T_{w}}_{1:N}^{t + 1}` is being solved.

At this point, the full surface profile has been modeled, assuming diffusion is
the primary mode of heat transfer within the reservoir. This assumption will
fail if the stratification in the reservoir has become unstable, forcing
convective mixing between layers, or if the wind over the reservoir creates
turbulent mixing. Modeling the effects of convective and turbulent mixing is
performed by progressively mixing downward from the surface, until there is
insufficient kinetic energy to mix deeper into the reservoir. The combined
depth of the layers affected by mixing is referred to as the surface mixing
layer (SML). Working downward from the surface, the potential energy of the SML,
assuming layer |i| is included, is evaluated as follows:

:math:`\left.
\begin{array}{l}
{PE_{SML_i}} = g \biggl( {\rho_{SML_{i - 1}}} V_{i - 1:N} \bigl( {z_{SML}^{com}}_{i - 1} - z_{i - 2} \bigr) \; - \\
\hspace{6cm} \Bigl( \rho_{i} V_{i:N} ( {z^{com}}_{i:N} - z_{i - 2} ) + \rho_{i - 1}V_{i - 1} \
( {z^{com}}_{i:i} - z_{i - 2} ) \Bigr) \biggr)
\end{array} \right.`

:math:`V_{i:N} = \sum_{k = i}^{N}V_{k}`

:math:`{T_{SML_i}} = \dfrac{\sum_{k = i}^{N}{{T_{w_k}}V_{k}{c_{p_k}}}}{\sum_{k = i}^{N}{V_{k}{c_{p_k}}}}`

:math:`{\rho_{SML_i}} = 1000 - 0.019549 \bigl| {T_{SML_i}} - 277.15 \bigr|^{1.68}`

:math:`z^{com}_{SML_i} = \rho_{SML_{i:N}}\sum_{k = i}^{N}\frac{V_{k}( z_{k} + z_{k - 1} )}{2}`

:math:`z^{com}_{i:j} = \sum_{k = i}^{j}\frac{\rho_{k}V_{k}( z_{k} + z_{k - 1} )}{2}`

Where:

    .. csv-table::
       :widths: 1 2

       ":math:`{\rho_{SML_i}}`", "Density of the SML with layer |i| included"
       ":math:`{T_{SML_i}}`", "Temperature of the SML with layer |i| included"
       ":math:`{z_{SML}^{com}}_{i}`", "Center of mass of the SML with layer |i| included"
       ":math:`{z^{com}}_{i:j}`", "Center of mass of layers |i| through |j|"
       ":math:`PE_{SML_i}`", "Difference in potential energy of the SML with layer |i| included and excluded from the mixed layer"


If :math:`PE_{SML_i} < 0`, then there is sufficient energy due
to density instability to force mixing of layers :math:`i - 1\!:\!N`. In this
case, the temperature of layers :math:`i - 1\!:\!N` is set to :math:`T_{w_{i:N}}`,
and the :math:`PE_{SML_{i - 1}}` is subsequently checked. Once a layer is
identified where :math:`PE_{SML_i} \geq 0`, the density profile is considered
stable. At this point, it is still possible deeper layers are in the SML, due to
the combined convective and wind driven turbulent energy. Therefore, the
turbulent kinetic energy :math:`({TKE})` must be computed, and compared against the
potential energy.

:math:`{TKE}_{i:N} = Ke_{c_{i:N}} + Ke_{u_{i:N}}`

:math:`Ke_{c_{i:N}} = \dfrac{\varepsilon_{c}g}{\rho_{N}\mathrm{\Delta}t} \biggl( \sum_{k = i}^{N} \Bigl( \rho_{k} \
( z_{k} - z_{k - 1} ) \frac{( z_{k} + z_{k - 1} )}{2} \Bigr) - \frac{( z_{N} + z_{i - 1} )}{2} \sum_{k = i}^{N} \
\Bigl( \rho_{k}( z_{k} - z_{k - 1} ) \Bigr) \biggr)`

:math:`Ke_{u_{i:N}} = \varepsilon_{u}\rho_{N}A_{N}{u_{*}^{w}}^{3}\mathrm{\Delta}t`

:math:`u_{*}^{w} = u_{*}\sqrt{\frac{\rho_{a}}{\rho_{N}}}`

Where:

    .. csv-table::
       :widths: auto

       ":math:`Ke_{c_{i:N}}`", "Kinetic energy of the SML with layer |i| included"
       ":math:`Ke_{u_{i:N}}`", "Kinetic energy from wind with layer |i| included"

If :math:`TKE_{i:N} \geq PE_{mix_i}` , then layer
|i| is considered in the SML, and the computations checks the deeper layer.

If :math:`TKE_{i:N} > PE_{mix_i}` , then the computation of vertical
temperature profile is complete.

At this point, the reservoir surface temperature computations have completed, and
ResEvap proceeds to the next time step. After each time step, the preliminary calculated
values for that hour are returned to the ResEvapAlgo class, which prepares the results to
be stored in the user's OpenDCS database of choice. The ResEvapAlgo class then calls the
ResEvap class to compute the results for the next time step. Once all time steps for a full
day—24 hours (or 23/25 hours during daylight saving time)—have been calculated, daily
average and total values for evaporation and water profile temperatures are calculated.
After the entire aggregation period is completed, the ResEvapAlgo class executes the write
action to store all values to the user's database.

.. _Appendix 1:

Appendix 1: Variable Definitions
================================

.. _6.1-evaporation-computations:

Evaporation Computations
------------------------

.. csv-table::
   :header: "Variable", "Description", "Units"
   :widths: 25, 50, 25

   ":math:`c_{p}^{T}`", "Specific heat of dry air, based on temperature :math:`T`", "|J/kgK|"
   ":math:`c_{d_0}`", "10-m, neutral-stability drag coefficient (from Donelan (1982))", "unitless"
   ":math:`C_{D}`", "Transfer coefficient for wind", "unitless"
   ":math:`C_{q}`", "Transfer coefficient for humidity", "unitless"
   ":math:`C_{T}`", "Transfer coefficient for temperature", "unitless"
   ":math:`e_{s}`", "Saturation vapor pressure", "|hPa|"
   ":math:`e`", "Vapor pressure", "|hPa|"
   ":math:`E`", "Evaporation", ":math:`mm / day`"
   ":math:`H_{l}`", "Latent heat flux", "|W/m2|"
   ":math:`H_{s}`", "Sensible heat flux", "|W/m2|"
   ":math:`h_{RH}`", "Height of relative humidity measurement", "|m|"
   ":math:`h_{T}`", "Height of air temperature measurement", "|m|"
   ":math:`h_{u}`", "Height of wind measurement", "|m|"
   ":math:`l_{o}`", "Obukhov length", "|m|"
   ":math:`l_{v}`", "Latent heat of vaporization or sublimation", "|J/kg|"
   ":math:`p_{a}`", "Air pressure", "|mb|"
   ":math:`q_{s}`", "Specific humidity at water surface", "unitless"
   ":math:`q_{r}`", "Specific humidity at reference temperature height", "unitless"
   ":math:`q_{*}`", "Humidity scale for air column stability", "unitless"
   ":math:`{R_{e}}^{*}`", "Roughness Reynolds number", "unitless"
   ":math:`RH`", "Relative humidity", "unitless"
   ":math:`S`", "Salinity", ":math:`psu`"
   ":math:`t_{*}`", "Temperature scale for air column stability", "unitless"
   ":math:`T_{a}`", "Air temperature", "|tempK|"
   ":math:`\widehat{T_{a}}`", "Air temperature measurement at reference height :math:`h_{T}`", "|tempK|"
   ":math:`T_{s}`", "Water surface temperature", "|tempK|"
   ":math:`\overline{T_a}`", "Average air temperature over the surface air layer (from water surface to :math:`h_{T})`", "|tempK|"
   ":math:`T_{w}`", "Water temperature", "|tempK|"
   ":math:`\hat{u}`", "Measured windspeed", "|m/s|"
   ":math:`u`", "Adjusted wind speed", "|m/s|"
   ":math:`u_{*}`", "Wind friction velocity", "|m/s|"
   ":math:`u_{r}`", "Windspeed at reference height", "|m/s|"
   ":math:`z_{u}`", "Roughness length for momentum", "|m|"
   ":math:`z_{T}`", "Roughness length for temperature", "|m|"
   ":math:`z_{q}`", "Roughness length for humidity", "|m|"
   ":math:`\Gamma_{d}`", "Dry adiabatic lapse rate", ":math:`K / m`"
   ":math:`\theta_{r}`", "Potential temperature (air temperature at water-air interface)", "|tempK|"
   ":math:`\rho_{v}`", "Water vapor density", "|kg/m3|"
   ":math:`\rho_{a}`", "Density of air", "|kg/m3|"
   ":math:`\rho_{d}`", "Dry density of air", "|kg/m3|"
   ":math:`\nu_{s}`", "Kinematic viscosity of air", "|m2/s|"

.. _6.2-radiation-computations:

Radiation Computations
----------------------
.. csv-table::
   :header: "Variable", "Description", "Units"
   :widths: 25, 50, 25

   ":math:`{e}_{a}`", "Vapor pressure of the atmosphere", "|hPa|"
   ":math:`{e}_{s}`", "Saturation vapor pressure", "|hPa|"
   ":math:`{f_{c_k}}`", "Fractional cloud cover of layer |k|", "unitless"
   ":math:`{h_{c_k}}`", "Height of clouds in layer |k| ", "|m|"
   ":math:`h_{gmt}`", "Hour of day in GMT", ":math:`hours`"
   ":math:`h_{s}`", "Hour angle of the sun", "|deg|"
   ":math:`I_{s \downarrow}`", "Incoming solar radiation reaching the water surface", "|W/m2|"
   ":math:`I_{l \uparrow}`", "Upwelling longwave radiation from the water surface", "|W/m2|"
   ":math:`I_{l \downarrow}`", "Downwelling longwave radiation reaching the water surface", "|W/m2|"
   ":math:`I_{l \downarrow_{clear}}`", "Clear sky component of the downwelling longwave radiation", "|W/m2|"
   ":math:`I_{l \downarrow_{cloud}}`", "Overcast component of the downwelling longwave radiation", "|W/m2|"
   ":math:`JD`", "Julian date where :math:`JD = 1` on January 1st", ":math:`days`"
   ":math:`l_{v}`", "Latent heat of vaporization", "|J/kg|"
   ":math:`R_{k}`", "Reflectance of layer |k|", "unitless"
   ":math:`R_{g}`", "Reflectance of the water surface", "unitless"
   ":math:`\widehat{RH}`", "Measured relative humidity", "unitless"
   ":math:`S_{e}`", "Extraterrestrial solar radiation on a horizontal plane", "|W/m2|"
   ":math:`\widehat{T_{a}}`", "Measured air temperature", "|tempK|"
   ":math:`T_{k}`", "Transmissivity of layer |k|", "unitless"
   ":math:`T_{s}`", "Water surface temperature", "|tempK|"
   ":math:`\delta`", "Solar declination angle", "|deg|"
   ":math:`\theta_{s}`", "Solar zenith angle", "|deg|"


.. _6.3-vertical-temperature-profile-computations:

Vertical Temperature Profile Computations
-----------------------------------------

.. csv-table::
   :header: "Variable", "Description", "Units"
   :widths: 25, 50, 25

   ":math:`A_{i}`", "Top area of layer |i| ", "|m2|"
   ":math:`\overline{A_l}`", "Average area of layer |i| ", "|m2|"
   ":math:`c_{p_i}`", "Heat capacity of water at layer |i| ", "|J/kgK|"
   ":math:`I_{z_i}`", "Radiative energy flux for layer |i| ", "|W/m3|"
   ":math:`Ke_{c_{i:N}}`", "Convective kinetic energy of layer |i| through the surface layer", "|J/kg|"
   ":math:`Ke_{u_{i:N}}`", "Wind driven kinetic energy of layer |i| through the surface layer", "|J/kg|"
   ":math:`{K_{z}}_{i}`", "Thermal diffusivity of layer |i| ", "|m2/s|"
   ":math:`N_{i}`", "Stability frequency of layer |i|", ":math:`1/s`"
   ":math:`SD`", "Secchi Depth", "|m|"
   ":math:`{T_{w}}_{i}`", "Water temperature of layer |i| ", "|tempK|"
   ":math:`{TKE}_{i:N}`", "Total kinetic energy of layer |i| through the surface layer", "|J/kg|"
   ":math:`{T_{SML}}_{i}`", "Temperature of the SML if layer |i| is the lowest layer", "|tempK|"
   ":math:`V_{i}`", "Volume of layer |i| ", "|m3|"
   ":math:`V_{i:N}`", "Volume of water from layer |i| to the surface", "|m3|"
   ":math:`z^{com}_{SML_i}`", "Depth of center of mass for SML, given layer |i| is lowest layer included in SML", "|m|"
   ":math:`z^{com}_{i:j}`", "Depth of the center of mass of layers |i| through |j| ", "|m|"
   ":math:`\varepsilon_{c}`", "Convective turbulent energy dissipation", "|m2/s3|"
   ":math:`\varepsilon_{u}`", "Wind driven turbulent energy dissipation", "|m2/s3|"
   ":math:`\kappa_{a}`", "Bulk extinction coefficient for penetrating shortwave radiation", ":math:`1/m`"
   ":math:`{\rho_{w}}_{i}`", "Density of water at layer |i| ", "|kg/m3|"
   ":math:`\overline{\rho_w}`", "Average water density across the entire profile", "|kg/m3|"
   ":math:`{\rho_{SML_i}}`", "Density of the SML if layer |i| is the lowest layer", "|kg/m3|"


.. _Appendix 2:

Appendix 2: Constant Values
===========================

.. csv-table::
   :header: "Variable", "Description", "Value", "Units"
   :widths: 25, 50, 25, 25

   ":math:`C_{s}`", "Smooth surface coefficient", ":math:`0.135`", "unitless"
   ":math:`g`", "Acceleration due to gravity", ":math:`9.81`", ":math:`m/s^{2}`"
   ":math:`m_{w}`", "Molecular weight of water", ":math:`0.0180160`", ":math:`\dfrac{kg}{mole}`"
   ":math:`R_{g}`", "The ideal gas constant", ":math:`8.31441`", ":math:`\dfrac{J}{mole*K}`"
   ":math:`R_{v}`", "Gas constant for water vapor", ":math:`461`", "|J/kgK|"
   ":math:`R_{w}`", "Reflectivity of water", ":math:`0.2`", "unitless"
   ":math:`T_{FP}`", "Freezing point in Kelvin", ":math:`273.15`", "|tempK|"
   ":math:`\alpha`", "Albedo of water", ":math:`0.08`", "unitless"
   ":math:`\beta`", "Light penetration fraction", ":math:`0.4`", "unitless"
   ":math:`\varepsilon_{c}`", "Convective dissipation", ":math:`0.5`", "|m2/s3|"
   ":math:`\varepsilon_{s}`", "Stirring dissipation", ":math:`0.4`", "|m2/s3|"
   ":math:`\varepsilon_{w}`", "Emissivity of water", ":math:`0.98`", "unitless"
   ":math:`\kappa`", "Von Karman constant", ":math:`0.4`", "unitless"
   ":math:`\sigma`", "Stefan-Boltzman constant", ":math:`5.67*10^{-8}`", ":math:`\dfrac{W}{m^{2}K^{4}}`"
   ":math:`\theta`", "Theta method factor", ":math:`1`", "unitless"


NOTE: The Stefan-Boltzman constant is :math:`5.669*10^{- 8}` in the computation
of the incoming longwave radiation, which is slightly different than the rest
of the computations. This is considered an insignificant difference.


.. _References:

References
==========

| Crawford, T.M, C.E. Duchon (1999) An Improved Parameterization for Estimating Effective
|             Atmospheric Emissivity for Use in Calculating Daytime Downwelling Longwave Radiation.
|             Journal of Applied Meteorology, Volume 38, Issue 4 (April 1999) pp 474-480.

| Daly, S. F. (2005), Reservoir Evaporation, U.S. Army Engineering Research and
|             Development Center, November 2015

| Donelan, M. A., Dobson, F. W., Smith, S. D., & Anderson, R. J. (1993). On the Dependence of Sea
|             Surface Roughness on Wave Development. Journal of Physical Oceanography, 23(9), pp. 2143-2149.
|             h​ttps://doi.org/10.1175/1520-0485(1993)023<2143:OTDOSS>2.0.CO;2

| Fairall, C.W., E.F. Bradley, D.P. Rogers, J.B. Edson, and G.S. Young, 1996: Bulk
|             parameterization of air-sea fluxes for Tropical Ocean-Global Atmosphere Coupled-Ocean
|             Atmosphere Response Experiment. J. Geophys. Res., 101, 3747–3764.

| Hondzo, M., and H. Stefan (1993) Lake Water Temperature Simulation Model. Journal of
|             Hydraulic Engineering, Vol. 119, No. 11, November, 1993 pp 1251-1273

| Shapiro, R. (1987) A simple model for the calculation of the flux of direct and diffuse
|             solar radiation through the atmosphere. Air Force Geophysics Laboratory,
|             Hanscom AFB MA 01731 AFGL-TR-87-0200

| Woolf, H. M. (1968) On the computation of solar elevation angles and the determination
|             of sunrise and sunset times. NASA TM X-1646, National Aeronautics and Space
|             Administration, Washington, D. C. September 1968

