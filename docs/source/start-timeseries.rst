###################################
OpenDCS Time Series - Introduction
###################################

OpenDCS Suite includes an extention called OpenTSBD. 
TSBD = Time Series Database. 

The outputs and import utilities are 

Overview - what is a Time Series?
=================================

Definitions:

A **time series** is a series of data points ordered in time. [1]
[1] https://towardsdatascience.com/time-series-introduction-7484bc25739a

A **sample** is a measurement at a point in time.

Types of Time Series
--------------------

There are multiple types of time series for which OpenDCS is
compatible.  They are outlined below:

* **Regular** : The time between each sample is identical, regardless of time zone.
* **Irregular**: There is no set interval between samples, or is one expected.
* **PseudoRegular**: There is an expected, possible rough, interval between samples but it can be reasonably be more or less.

Constituent Parts
-----------------

* Value/Sample: the measurement of interest
* Interval: distance between samples
* Sample time: ?duration? the sample was measured
* Period: synonym for interval
* Sample Rate: synonym for interval 

In OpenDCS, there are 6 components that make up a time series. 

* *location* -  refers to a Site in the database.  This is the site name or location.
* *param* - refers to a data type in the database (ie precipitation, stage, flow, etc).
* *statcode* - qualified how the data was measured or calculated (ie instantaneous, average, etc).
* *interval* - one of the valid intervals in the database (ie 5-minute, 15-minute, hourly, daily, monthly)
* *duration* - refers to the duration over which the value was measured or calculated
* *version* -a free-form string used to distinguish between different versions of a time series (ie raw vs rev)


Example of Time Series
======================

Regular Hourly - DCP Stage, Precip, Water Temp and Volt
-------------------------------------------------------


.. image:: ./media/start/timeseries/im-02-dcp-message.JPG
   :alt: time series - dcp
   :width: 550



Regular Hourly - Water Levels
-----------------------------

*Location.Stage.Inst.1Hour.0.xxx-raw*

.. image:: ./media/start/timeseries/im-03-levels-hourly.JPG
   :alt: time series - hourly water levels
   :width: 550


Regular Daily - Water Levels
-----------------------------

*Location.Stage.Ave.1Day.1Day.xxx-raw*

.. image:: ./media/start/timeseries/im-04-levels-daily.JPG
   :alt: time series - hourly water levels
   :width: 550


