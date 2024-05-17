###################################
OpenDCS Algorithms - Resources
###################################


****************************
Algorithms Resources - Recap
****************************

There are a number of "Standard" or "Baseline" algorithms that come 
with OpenDCS when it is installed.  These can be thought of as banked 
algorithms or functions that can be called upon when setting up a computation.
These standard algorithms are generally basic arithmetic or time series
manipulation or transformations.  For example, the average algorithm
(AverageAlgorithm)can be used to create a daily or monthly time series 
from an hourly or daily time series, respectively.  In addition to 
simple arithmetic and transformations, there are some specific
algorithms that are commonly used in water resources management.
For example the incremental precipitation algorithm (IncrementalPrecip)
can be called upon to calculate an incremental precipitation time series
from a cumulative precipitation time series.  Additionally there are 
also some algorithms that calculate flow based on various time series
input.

This page is intended to provide further details and examples of the 
default algorithms.  Nearly all of these algorithms have properties
that can be set to tweak the way the algorithm is executed. For example,
the properties can address time shifts, missing values, change how default
upper and lower bounds are handled, etc.  It is recommended to 
understand how the basic default algorithm behaves before advancing 
to editing the properties.  

This page is divided into three sections:

#. OpenDCS Standard Algorithms
#. OpenDCS Custom Algorithms
#. OpenDCS Algorithm GUI Basics

As a recap, below is a table of algorithms that come with OpenDCS installs.

+--------------------+-------------------------+--------------------------------------------------+
|**Type**            |**Algorithm**            | **Exec Class**                                   |
+====================+=========================+==================================================+
| * Arithmetic or    |AddToPrevious            | decodes.tsdb.algo.AddToPrevious                  |
| * Transformation   +-------------------------+--------------------------------------------------+
|                    |AverageAlgorithm         | decodes.tsdb.algo.AverageAlgorithm               |
|                    +-------------------------+--------------------------------------------------+
|                    |ChooseOne                | decodes.tsdb.algo.ChooseOne                      |
|                    +-------------------------+--------------------------------------------------+
|                    |CopyAlgorithm            | decodes.tsdb.algo.CopyAlgorithm                  |
|                    +-------------------------+--------------------------------------------------+
|                    |CopyNoOverwrite          | decodes.tsdb.algo.CopyNoOverwrite                |
|                    +-------------------------+--------------------------------------------------+
|                    |DisAggregate             | decodes.tsdb.algo.DisAggregate                   |
|                    +-------------------------+--------------------------------------------------+
|                    |FillForward              | decodes.tsdb.algo.FillForward                    |
|                    +-------------------------+--------------------------------------------------+
|                    |Resample                 | decodes.tsdb.algo.Resample                       |
|                    +-------------------------+--------------------------------------------------+
|                    |RunningAverageAlgorithm  | decodes.tsdb.algo.RunningAverageAlgorithm        |
|                    +-------------------------+--------------------------------------------------+
|                    |ScalarAdder              | decodes.tsdb.algo.ScalerAdder                    |
|                    +-------------------------+--------------------------------------------------+
|                    |SubSample                | decodes.tsdb.algo.SubSample                      |
|                    +-------------------------+--------------------------------------------------+
|                    |SumOverTimeAlgorithm     | decodes.tsdb.algo.SumOverTimeAlgorithm           |
+--------------------+-------------------------+--------------------------------------------------+
| * Hydrologic       |BridgeClearance          | decodes.tsdb.algo.BridgeClearance                |
|                    +-------------------------+--------------------------------------------------+
|                    |EstimatedInflow          | decodes.tsdb.algo.EstimatedInflow                |
|                    +-------------------------+--------------------------------------------------+
|                    |IncrementalPrecip        | decodes.tsdb.algo.IncrementalPrecip              |
|                    +-------------------------+--------------------------------------------------+
|                    |RdbRating                | decodes.tsdb.algo.RdbRating                      |
|                    +-------------------------+--------------------------------------------------+
|                    |TabRating                | decodes.tsdb.algo.TabRating                      |
|                    +-------------------------+--------------------------------------------------+
|                    |UsgsEquation             | decodes.tsdb.algo.UsgsEquation                   |
|                    +-------------------------+--------------------------------------------------+
|                    |VirtualGage              | decodes.tsdb.algo.VirtualGage                    |
+--------------------+-------------------------+--------------------------------------------------+
| * Arithmetic or    |CentralRunningAverage    |                                                  |
| * Transformation   +-------------------------+--------------------------------------------------+
| * (Hidden)         |Division                 |                                                  |
|                    +-------------------------+--------------------------------------------------+
|                    |GroupAdder               |                                                  |
|                    +-------------------------+--------------------------------------------------+
|                    |Multiplication           |                                                  |
|                    +-------------------------+--------------------------------------------------+
|                    |PeriodToDate             |                                                  |
|                    +-------------------------+--------------------------------------------------+
|                    |Stat                     |                                                  |
+--------------------+-------------------------+--------------------------------------------------+
| * Hydrologic       |ExpressionParserAlgorithm|                                                  |
| * (Hidden)         +-------------------------+--------------------------------------------------+
|                    |FlowResIn                |                                                  |
|                    +-------------------------+--------------------------------------------------+
|                    |WeightedWaterTemperature |                                                  |
+--------------------+-------------------------+--------------------------------------------------+

The following two tables are the algorithms specific to CWMS or HDB.

+--------------------+-------------------------+--------------------------------------------------+
|**Type**            |**Algorithm**            | **Exec Class**                                   |
+====================+=========================+==================================================+
| * CWMS             |CentralRunningAverage    | decodes.cwms.rating.CwmsRatingMultIndep          |
|                    +-------------------------+--------------------------------------------------+
|                    |Division                 | decodes.cwms.rating.CwmsRatingSingleIndep        |
|                    +-------------------------+--------------------------------------------------+
|                    |GroupAdder               | decodes.cwms.validation.CwmsScreeningAlgorithm   |
|                    +-------------------------+--------------------------------------------------+
|                    |Multiplication           | decodes.cwms.validation.DatchkScreeningAlgorithm |
+--------------------+-------------------------+--------------------------------------------------+
| * HDB              |CallProcAlg              | decodes.hdb.algo.CallProcAlg                     |
|                    +-------------------------+--------------------------------------------------+
|                    |DynamicAggregateAlg      | decodes.hdb.algo.DynamicAggregateAlg             |
|                    +-------------------------+--------------------------------------------------+
|                    |EOPInterpAlg             | decodes.hdb.algo.EOPInterpAlg                    |
|                    +-------------------------+--------------------------------------------------+
|                    |EquationSolverAlg        | decodes.hdb.algo.EquationSolverAlg               |
|                    +-------------------------+--------------------------------------------------+
|                    |EstGLDAInflow            | decodes.hdb.algo.EstGLDAInflow                   |
|                    +-------------------------+--------------------------------------------------+
|                    |FLGUUnreg                | decodes.hdb.algo.FLGUUnreg                       |
|                    +-------------------------+--------------------------------------------------+
|                    |FlowToVolumeAlg          | decodes.hdb.algo.FlowToVolumeAlg                 |
|                    +-------------------------+--------------------------------------------------+
|                    |GLDAEvap                 | decodes.hdb.algo.GLDAEvap                        |
|                    +-------------------------+--------------------------------------------------+
|                    |GLDAUnreg                | decodes.hdb.algo.GLDAUnreg                       |
|                    +-------------------------+--------------------------------------------------+
|                    |GlenDeltaBSMBAlg         | decodes.hdb.algo.GlenDeltaBSMBAlg                |
|                    +-------------------------+--------------------------------------------------+
|                    |HdbLookupTimeShiftRating | decodes.hdb.algo.HdbLookupTimeShiftRating        |
|                    +-------------------------+--------------------------------------------------+
|                    |HdbShiftRating           | decodes.hdb.algo.HdbShiftRating                  |
|                    +-------------------------+--------------------------------------------------+
|                    |InflowAdvancedAlg        | decodes.hdb.algo.InflowAdvancedAlg               |
|                    +-------------------------+--------------------------------------------------+
|                    |InflowBasicAlg           | decodes.hdb.algo.InflowBasicAlg                  |
|                    +-------------------------+--------------------------------------------------+
|                    |MPRCUnreg                | decodes.hdb.algo.MPRCUnreg                       |
|                    +-------------------------+--------------------------------------------------+
|                    |NVRNUnreg                | decodes.hdb.algo.NVRNUnreg                       |
|                    +-------------------------+--------------------------------------------------+
|                    |ParshallFlume            | decodes.hdb.algo.ParshallFlume                   |
|                    +-------------------------+--------------------------------------------------+
|                    |PowerToEnergyAlg         | decodes.hdb.algo.PowerToEnergyAlg                |
|                    +-------------------------+--------------------------------------------------+
|                    |SideInflowAlg            | decodes.hdb.algo.SideInflowAlg                   |
|                    +-------------------------+--------------------------------------------------+
|                    |SimpleDisaggAlg          | decodes.hdb.algo.SimpleDisaggAlg                 |
|                    +-------------------------+--------------------------------------------------+
|                    |TimeWeightedAverageAlg   | decodes.hdb.algo.TimeWeightedAverageAlg          |
|                    +-------------------------+--------------------------------------------------+
|                    |VolumeToFlowAlg          | decodes.hdb.algo.VolumeToFlowAlg                 |
+--------------------+-------------------------+--------------------------------------------------+

***************************
OpenDCS Standard Algorithms
***************************


Standard - Arithmetic
=====================

In this section a number of the default standard algorithms are
presented.  These algorithms are grouped together because generally
these algorithms are for executing simple arithmetic.  

+-------------------------+----------------------------------------------------------+
|**Algorithm**            |**Description**                                           |
+=========================+==========================================================+
|AddToPrevious            |Adds the current value to the previous value              |
+-------------------------+----------------------------------------------------------+
|AverageAlgorithm         |Averages an 'input' parameter to an 'average' parameter   |
+-------------------------+----------------------------------------------------------+
|ChooseOne                |Given two inputs, output the best one                     |
+-------------------------+----------------------------------------------------------+
|CopyAlgorithm            |Copies an 'input' parameter to an 'output' parameter      |
+-------------------------+----------------------------------------------------------+
|CopyNoOverwrite          |Copies an 'input' parameter to an 'output' parameter      |
+-------------------------+----------------------------------------------------------+
|DisAggregate             |Spreads out the input values to outputs in various ways   |
+-------------------------+----------------------------------------------------------+
|FillForward              |Project an input value by copying it forward in time      |
+-------------------------+----------------------------------------------------------+
|Resample                 |Resample an input to an output with a different interval  |
+-------------------------+----------------------------------------------------------+
|RunningAverageAlgorithm  |Averages an 'input' parameter to an 'average' parameter   |
+-------------------------+----------------------------------------------------------+
|ScalarAdder              |Multiples up to 10 'input' values by coefficients         |
+-------------------------+----------------------------------------------------------+
|SubSample                |Convert a short interval to a longer interval             |
+-------------------------+----------------------------------------------------------+
|SumOverTimeAlgorithm     |Sums single 'input' parameter to a single 'sum' parameter |
+-------------------------+----------------------------------------------------------+

Recall, that when a computation is set up, the output is a 
separate time series.   Input time series are NOT being manipulated 
or edited.

AddToPrevious
-------------

Exec Class: decodes.tsdb.algo.AddToPrevious 

.. image:: ./media/resources/algorithms/im-001-excel-addtoprevious.JPG
   :alt:  algorithm add to previous
   :width: 500

The "Add To Previous" algorithm *AddToPrevious* adds the previous 
value to the current value. By default, the following criteria
are assumed or executed.

* If the previous time slice is missing, the prior non-missing value will be added to the current value.  
* If a current time slice is missing a value, then the corresponding output time slice will also be missing.

See the image above to better understand how the algorithm behaves.

+-----------+-----------------+
|**Role**   |**Role Name**    |
+===========+=================+
|Inputs     |input            |
+-----------+-----------------+
|Outputs    |output           |
+-----------+-----------------+

.. image:: ./media/resources/algorithms/im-002-comptest-addtoprevious.JPG
   :alt:  algorithm add to previous
   :width: 600

.. image:: ./media/resources/algorithms/im-003-comp-addtoprevious.JPG
   :alt:  algorithm add to previous
   :width: 600

AverageAlgorithm
----------------

Exec Class: decodes.tsdb.algo.AverageAlgorithm

.. image:: ./media/resources/algorithms/im-004-excel-averagealgorithm.JPG
   :alt:  algorithm average algorithm
   :width: 500

The "Average" algorithm *AverageAlgorithm* aggregates and calculates
an average over a period defined by the output parameter. By default,
the following criteria are assumed or executed.

* Minimum samples needed for algorithm is 1
* Average calculated will include the lower bound
* Average calculated will not include the upper bound
* Average value will be stored at the lower bound time slice, irregardless of upper/lower bounds defined  
* If an input is deleted, and as a result the minimum number of samples is no longer met, then the previously calculated output will be deleted.

See the images above and below to better understand how the algorithm behaves.

+-----------+-----------------+
|**Role**   |**Role Name**    |
+===========+=================+
|Inputs     |input            |
+-----------+-----------------+
|Outputs    |average          |
+-----------+-----------------+

.. image:: ./media/resources/algorithms/im-005-comptest-averagealgorithm.JPG
   :alt:  algorithm average algorithm
   :width: 600

.. image:: ./media/resources/algorithms/im-006-comp-averagealgorithm.JPG
   :alt:  algorithm average algorithm
   :width: 600

ChooseOne
---------

Exec Class: decodes.tsdb.algo.ChooseOne

.. image:: ./media/resources/algorithms/im-007-excel-chooseone.JPG
   :alt:  algorithm choose one
   :width: 500

The "Choose One" algorithm *ChooseOne* will choose one value 
(the best one) from two time series to output. Additionally, 
some upper and lower criteria limits can be applied. By default,
the following criteria are assumed or executed.


* If only one value is provided, and it is acceptable, use this value.
* If only one value is provided, and it is not acceptable, use neither.
* If two values are within the acceptable limits, use the higher value.
* If two values are provided but only one is acceptable, use the acceptable value.
* If two values are provided and neither is acceptable, use neither.
* Values higher than the upper limit (but not including), will be considered unacceptable.
* Values lower than the lower limit (but not including), will be considered unacceptable.

In the example above, the limits are set to an upper limit of 176.905
and a lower limit of 176.88.  This means that a value of 176.905 will be 
considered valid, but 176.906 will be invalid.  Likewise, a value of 
176.88 will be considered valid but anything lower will be considered 
invalid.

See the images above and below to better understand how the algorithm behaves.

+-----------+-----------------+
|**Role**   |**Role Name**    |
+===========+=================+
|Inputs     |input1           |
|           +-----------------+
|           |input2           |
+-----------+-----------------+
|Outputs    |output           |
+-----------+-----------------+

.. image:: ./media/resources/algorithms/im-008-comptest-chooseone.JPG
   :alt:  algorithm choose one
   :width: 600

.. image:: ./media/resources/algorithms/im-009-comp-chooseone.JPG
   :alt:  algorithm choose one
   :width: 600
   
CopyAlgorithm
-------------

Exec Class: decodes.tsdb.algo.CopyAlgorithm

.. image:: ./media/resources/algorithms/im-010-excel-copyalgorithm.JPG
   :alt:  algorithm choose one
   :width: 400

The "Copy" algorithm *CopyAlgorithm* will simply copy the 
values from one time series to another time series.  By
default the output will be at the exact same time slice
as the input.  

* If an input time series is missing and a value exists in the corresponding output time series, then the existing output value will remain (ie NOT be overwritten by a missing value)
* Will copy and save all decimal places (i.e. Decimal places displayed in window below are not representative of what the true output is if more decimal places are present in input)

See the images above and below to better understand how the algorithm behaves.

+-----------+-----------------+
|**Role**   |**Role Name**    |
+===========+=================+
|Inputs     |input            |
+-----------+-----------------+
|Outputs    |output           |
+-----------+-----------------+

.. image:: ./media/resources/algorithms/im-011-comptest-copyalgorithm.JPG
   :alt:  algorithm copy algorithm
   :width: 600

.. image:: ./media/resources/algorithms/im-012-comp-copyalgorithm.JPG
   :alt:  algorithm copy algorithm
   :width: 600

CopyNoOverwrite
---------------

Exec Class: decodes.tsdb.algo.CopyNoOverwrite

.. image:: ./media/resources/algorithms/im-013-excel-copynooverwrite.JPG
   :alt:  algorithm copy no overwrite
   :width: 500

By default the following criteria are met or assumed in the algorithm.

* If the output time series already has a value, it will NOT be overwritten by an input value or missing input (Computation Editor does not show what will be saved).
* The property "input_MISSING" is set to ignore. 

See the images above and below to better understand how the algorithm behaves.

+-----------+-----------------+
|**Role**   |**Role Name**    |
+===========+=================+
|Inputs     |input            |
+-----------+-----------------+
|Outputs    |output           |
+-----------+-----------------+

.. image:: ./media/resources/algorithms/im-014-comptest-copynooverwrite.JPG
   :alt:  algorithm copy no overwrite
   :width: 600

.. image:: ./media/resources/algorithms/im-015-comp-copynooverwrite.JPG
   :alt:  algorithm copy no overwrite
   :width: 600

DisAggregate
------------

Exec Class: decodes.tsdb.algo.DisAggregate

.. image:: ./media/resources/algorithms/im-016-excel-disaggregate.JPG
   :alt:  algorithm disaggregate - fill and split
   :width: 500

.. image:: ./media/resources/algorithms/im-017-excel-disaggregate.JPG
   :alt:  algorithm disaggregate - fill and split
   :width: 500

The "disaggregate" algorithm or *DisAggregate* will take an input
time series and spread the values to an output time series.  This 
algorithm requires that the interval of the input is equal to or 
longer than the output.  For example, this algorithm is ideal for 
converting a daily time series to an hourly time series, or a monthly
to a daily time series.  There are two methods that this algorithm
can be invoked.  It will either **fill** the new time series with the input 
time value, or **split** the input over *x* time slices.

By default the following criteria are met or assumed in the algorithm.

* The lower bound of the disaggregated time window is equal to the input time slice.
* By default the property "method" will be set to **fill**.
* Only two options for computation method: **split** or **fill**.
* If an input value is split over x intervals, at least 5 decimal places will save.

See the images above and below to better understand how the algorithm behaves.

+-----------+-----------------+
|**Role**   |**Role Name**    |
+===========+=================+
|Inputs     |input            |
+-----------+-----------------+
|Outputs    |output           |
+-----------+-----------------+

.. image:: ./media/resources/algorithms/im-018-comptest-disaggregate-fill.JPG
   :alt:  algorithm disaggregate - fill
   :width: 600

.. image:: ./media/resources/algorithms/im-019-comptest-disaggregate-split.JPG
   :alt:  algorithm disaggregate - split
   :width: 600

.. image:: ./media/resources/algorithms/im-020-comptest-disaggregate-fill.JPG
   :alt:  algorithm disaggregate - fill
   :width: 600

.. image:: ./media/resources/algorithms/im-021-comptest-disaggregate-split.JPG
   :alt:  algorithm disaggregate - split
   :width: 600

FillForward
-----------

Exec Class: decodes.tsdb.algo.FillForward

.. image:: ./media/resources/algorithms/im-023-excel-fillforward.JPG
   :alt:  algorithm fill forward
   :width: 500

The "fill forward" algorithm or *FillForward* will take an input
time series slice and apply the value x number of time slices forwards,
starting with the current time slice.  For example, if the property *NumIntervals*
is set **4** then the values at time slice t will be copied to the same time slice 
t in the output time series, and then copied to 3 time slices forward in time.

By default the following criteria are met or assumed in the algorithm.

* Default *NumIntervals* property is set to 4.

See the images above and below to better understand how the algorithm behaves.

+-----------+-----------------+
|**Role**   |**Role Name**    |
+===========+=================+
|Inputs     |input            |
+-----------+-----------------+
|Outputs    |output           |
+-----------+-----------------+

.. image:: ./media/resources/algorithms/im-024-comptest-fillforward.JPG
   :alt:  algorithm fill forward
   :width: 600

.. image:: ./media/resources/algorithms/im-025-comp-fillforward.JPG
   :alt:  algorithm fill forward
   :width: 600

Resample
--------

Exec Class: decodes.tsdb.algo.Resample

.. image:: ./media/resources/algorithms/im-026-excel-resample.JPG
   :alt:  algorithm resample
   :width: 500

The "resample" algorithm or *Resample* will take an input
time series at some resolution and apply it to a higher time
resolution. For example, an input may be a daily time series 
while the output may be hourly. Or an input may be a monthly
time series while the output may be daily.

By default the following criteria are met or assumed in the algorithm.

* Properties *method* options are **fill** and **interp**
* Default Method is **interp**

See the images above and below to better understand how the algorithm behaves.

+-----------+-----------------+
|**Role**   |**Role Name**    |
+===========+=================+
|Inputs     |input            |
+-----------+-----------------+
|Outputs    |output           |
+-----------+-----------------+

.. image:: ./media/resources/algorithms/im-027-comptest-resample-interp.JPG
   :alt:  algorithm resample - interp
   :width: 500

.. image:: ./media/resources/algorithms/im-028-comptest-resample-fill.JPG
   :alt:  algorithm resample - fill
   :width: 500

.. image:: ./media/resources/algorithms/im-029-comp-resample.JPG
   :alt:  algorithm resample
   :width: 450

RunningAverageAlgorithm
-----------------------

Exec Class: decodes.tsdb.algo.RunningAverageAlgorithm

.. image:: ./media/resources/algorithms/im-030-excel-runningaverage.JPG
   :alt:  algorithm running average
   :width: 500

The "running average" algorithm or *RunningAverageAlgorithm* 
will take an input time series at some resolution and calculate
an average based on the previous aggregate periodo interval. 
For example, if the aggregate period interval is one week,
then the running average for a daily time series will be calculated
based on the previous 6 days and current day.  

By default the following criteria are met or assumed in the algorithm.

+-----------+-----------------+
|**Role**   |**Role Name**    |
+===========+=================+
|Inputs     |input            |
+-----------+-----------------+
|Outputs    |average          |
+-----------+-----------------+

See the images above and below to better understand how the algorithm behaves.

.. image:: ./media/resources/algorithms/im-031-comptest-runningaverage.JPG
   :alt:  algorithm running average
   :width: 600

.. image:: ./media/resources/algorithms/im-032-comp-runningaverage.JPG
   :alt:  algorithm running average
   :width: 600

ScalarAdder
-----------

Exec Class: decodes.tsdb.algo.ScalerAdder

.. image:: ./media/resources/algorithms/im-033-excel-scaleradder.JPG
   :alt:  scaler adder
   :width: 600

The "scaler adder" algorithm or *ScalarAdder* will calculate a
sum over a specific time slice from multiple time series. There is 
an option to include multipliers to each time series.  There are no
restrictions for the multipliers. 

+-----------+-----------------+
|**Role**   |**Role Name**    |
+===========+=================+
|Inputs     |input1           |
|           +-----------------+
|           |input2           |
|           +-----------------+
|           |input3           |
|           +-----------------+
|           |input4           |
|           +-----------------+
|           |input5           |
|           +-----------------+
|           |input6           |
|           +-----------------+
|           |input7           |
|           +-----------------+
|           |input8           |
|           +-----------------+
|           |input9           |
|           +-----------------+
|           |input10          |
+-----------+-----------------+
|Outputs    |output           |
+-----------+-----------------+

.. image:: ./media/resources/algorithms/im-034-comptest-scaleradder.JPG
   :alt:  algorithm scaler adder
   :width: 600

.. image:: ./media/resources/algorithms/im-035-comp-scaleradder.JPG
   :alt:  algorithm scaler adder
   :width: 600


SubSample
---------

Exec Class: decodes.tsdb.algo.SubSample

.. image:: ./media/resources/algorithms/im-036-excel-subsample.JPG
   :alt:  algorithm subsample
   :width: 600

The "sub sample" algorithm or *SubSample* will turn a time series 
of a high resolution to a lower resolution.  For example, an hourly
instantaneous time series might be converted into a daily instantaneous 
time series by sampling the top of day hour.  Or a 15-minute time series
may be turned into an hourly time series by sampling the top of hour
values.

+-----------+-----------------+
|**Role**   |**Role Name**    |
+===========+=================+
|Inputs     |input            |
+-----------+-----------------+
|Outputs    |output           |
+-----------+-----------------+

.. image:: ./media/resources/algorithms/im-037-comptest-subsample.JPG
   :alt:  algorithm subsample
   :width: 600

.. image:: ./media/resources/algorithms/im-038-comp-subsample.JPG
   :alt:  algorithm subsample
   :width: 600

SumOverTimeAlgorithm
--------------------

Exec Class: decodes.tsdb.algo.SumOverTimeAlgorithm

.. image:: ./media/resources/algorithms/im-039-excel-sumovertime.JPG
   :alt:  algorithm sumovertime
   :width: 600

The "sum over time" algorithm or *SumOverTimeAlgorithm* will 
calculate the sum over a given period of time.  For example
hourly values can be added up over a day to calculate a daily
time series.  Or a daily time series can be added up to calculate
a weekly or monthly total.  By default the aggLowerBoundClosed
property is False and the aggUpperBoundClosed property is True.
The minSamplesNeeded by default is 1.

+-----------+-----------------+
|**Role**   |**Role Name**    |
+===========+=================+
|Inputs     |input            |
+-----------+-----------------+
|Outputs    |output           |
+-----------+-----------------+

.. image:: ./media/resources/algorithms/im-040-comptest-sumovertime.JPG
   :alt:  algorithm sumovertime
   :width: 600

.. image:: ./media/resources/algorithms/im-041-comp-sumovertime.JPG
   :alt:  algorithm sumovertime
   :width: 600

Standard - Hydrologic
=====================

+-------------------+-------------------------------------------------------+
|**Algorithm**      |**Description**                                        |
+===================+=======================================================+
|BridgeClearance    |Subtract water level from constant 'low chord'         |
+-------------------+-------------------------------------------------------+
|EstimatedInflow    |Estimate inflow based on change in storage and outflow |
+-------------------+-------------------------------------------------------+
|IncrementalPrecip  |Compute incremental precip from cumulative precip      |
+-------------------+-------------------------------------------------------+
|RdbRating          |Implements rating table computations - flow vs stage   |
+-------------------+-------------------------------------------------------+
|TabRating          |Implements rating table computations - flow vs stage   |
+-------------------+-------------------------------------------------------+
|UsgsEquation       |USGS Equation O = A* (B + I)^C + D                     |
+-------------------+-------------------------------------------------------+
|VirtualGage        |Compute virtual elevation based on two other gages     |
+-------------------+-------------------------------------------------------+

BridgeClearance
---------------

Exec Class: decodes.tsdb.algo.BridgeClearance

.. image:: ./media/resources/algorithms/im-042-excel-bridgeclearance.JPG
   :alt: algorithm bridge clearance
   :width: 600

.. image:: ./media/resources/algorithms/im-043-comptest-bridgeclearance.JPG
   :alt: algorithm bridge clearance
   :width: 600

.. image:: ./media/resources/algorithms/im-044-comp-bridgeclearance.JPG
   :alt: algorithm bridge clearance
   :width: 600

EstimatedInflow
---------------

Exec Class: decodes.tsdb.algo.EstimatedInflow


.. ./media/resources/algorithms/im-045-excel-estimatedinflow.JPG
   algorithm estimated inflow
   600

.. ./media/resources/algorithms/im-046-comptest-estimatedinflow.JPG
   algorithm estimated inflow
   :width: 600

.. ./media/resources/algorithms/im-047-comp-estimatedinflow.JPG
   :alt:  algorithm estimated inflow
   :width: 600


IncrementalPrecip
-----------------

Exec Class: decodes.tsdb.algo.IncrementalPrecip


.. ./media/resources/algorithms/im-048-excel-incrementalprecip.JPG
   algorithm incremental precip
   600

.. ./media/resources/algorithms/im-049-comptest-incrementalprecip.JPG
   algorithm incremental precip
   600

.. ./media/resources/algorithms/im-050-comp-incrementalprecip.JPG
   algorithm incremental precip
   600


RdbRating
---------

Exec Class: decodes.tsdb.algo.RdbRating

... more content coming soon ...

TabRating
---------

Exec Class: decodes.tsdb.algo.TabRating

... more content coming soon ...

UsgsEquation
------------

Exec Class: decodes.tsdb.algo.UsgsEquation

... more content coming soon ...

VirtualGage
-----------

Exec Class: decodes.tsdb.algo.VirtualGage

... more content coming soon ...




Standard - Arithmetic - Hidden 
==============================

... more content coming soon ...


Standard - Hydrologic Specific - Hidden
=======================================

... more content coming soon ...


CWMS Only - Hydrologic
======================

+-----------------------+-------------------------------------------------------+
|**Algorithm**          |**Description**                                        |
+=======================+=======================================================+
|CwmsRatingMultiIndep   |Implements CWMS Rating Computations                    |
+-----------------------+-------------------------------------------------------+
|CwmsRatingSingleIndep  |Implements CWMS Rating Computations                    |
+-----------------------+-------------------------------------------------------+
|CwmsScreening          |CWMS Validation with CWMS Screening Records            |
+-----------------------+-------------------------------------------------------+
|DatchkScreening        |CWMS Validation with DATCHK files                      |
+-----------------------+-------------------------------------------------------+

CwmsRatingMultiIndep
--------------------

Exec Class: decodes.cwms.rating.CwmsRatingMultIndep

CwmsRatingSingleIndep
---------------------

Exec Class: decodes.cwms.rating.CwmsRatingSingleIndep

CwmsScreening
-------------

Exec Class: decodes.cwms.validation.CwmsScreeningAlgorithm

DatchkScreening
---------------

Exec Class: decodes.cwms.validation.DatchkScreeningAlgorithm

HDB Only - Hydrologic
=====================

+-------------------------+--------------------------------------------------+
|**Algorithm**            |**Description**                                   |
+=========================+==================================================+
|CallProcAlg              | decodes.hdb.algo.CallProcAlg                     |
+-------------------------+--------------------------------------------------+
|DynamicAggregateAlg      | decodes.hdb.algo.DynamicAggregateAlg             |
+-------------------------+--------------------------------------------------+
|EOPInterpAlg             | decodes.hdb.algo.EOPInterpAlg                    |
+-------------------------+--------------------------------------------------+
|EquationSolverAlg        | decodes.hdb.algo.EquationSolverAlg               |
+-------------------------+--------------------------------------------------+
|EstGLDAInflow            | decodes.hdb.algo.EstGLDAInflow                   |
+-------------------------+--------------------------------------------------+
|FLGUUnreg                | decodes.hdb.algo.FLGUUnreg                       |
+-------------------------+--------------------------------------------------+
|FlowToVolumeAlg          | decodes.hdb.algo.FlowToVolumeAlg                 |
+-------------------------+--------------------------------------------------+
|GLDAEvap                 | decodes.hdb.algo.GLDAEvap                        |
+-------------------------+--------------------------------------------------+
|GLDAUnreg                | decodes.hdb.algo.GLDAUnreg                       |
+-------------------------+--------------------------------------------------+
|GlenDeltaBSMBAlg         | decodes.hdb.algo.GlenDeltaBSMBAlg                |
+-------------------------+--------------------------------------------------+
|HdbLookupTimeShiftRating | decodes.hdb.algo.HdbLookupTimeShiftRating        |
+-------------------------+--------------------------------------------------+
|HdbShiftRating           | decodes.hdb.algo.HdbShiftRating                  |
+-------------------------+--------------------------------------------------+
|InflowAdvancedAlg        | decodes.hdb.algo.InflowAdvancedAlg               |
+-------------------------+--------------------------------------------------+
|InflowBasicAlg           | decodes.hdb.algo.InflowBasicAlg                  |
+-------------------------+--------------------------------------------------+
|MPRCUnreg                | decodes.hdb.algo.MPRCUnreg                       |
+-------------------------+--------------------------------------------------+
|NVRNUnreg                | decodes.hdb.algo.NVRNUnreg                       |
+-------------------------+--------------------------------------------------+
|ParshallFlume            | decodes.hdb.algo.ParshallFlume                   |
+-------------------------+--------------------------------------------------+
|PowerToEnergyAlg         | decodes.hdb.algo.PowerToEnergyAlg                |
+-------------------------+--------------------------------------------------+
|SideInflowAlg            | decodes.hdb.algo.SideInflowAlg                   |
+-------------------------+--------------------------------------------------+
|SimpleDisaggAlg          | decodes.hdb.algo.SimpleDisaggAlg                 |
+-------------------------+--------------------------------------------------+
|TimeWeightedAverageAlg   | decodes.hdb.algo.TimeWeightedAverageAlg          |
+-------------------------+--------------------------------------------------+
|VolumeToFlowAlg          | decodes.hdb.algo.VolumeToFlowAlg                 |
+-------------------------+--------------------------------------------------+



*************************
OpenDCS Custom Algorithms
*************************

In addition to the standard algorithms outlined above, users can 
set up custom algorithms that can be called upon in calculations.
There are two methods for creating custom algorithms: python and java.

Python Algorithms
=================

Exec Class: decodes.tsdb.algo.PythonAlgorithm


Java Algorithms
===============

****************************
OpenDCS Algorithm GUI Basics
****************************

... more content coming soon ...
