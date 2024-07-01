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
#. OpenDCS Custom Python Algorithms
#. OpenDCS Algorithm GUI Basics

As a recap, below is a table of algorithms that come with OpenDCS installs.

+--------------------+-------------------------+-------------------------------------------+
|**Type**            |**Algorithm**            | **Exec Class **                           |
+====================+=========================+===========================================+
| * Arithmetic or    |AddToPrevious            | decodes.tsdb.algo.AddToPrevious           |
| * Transformation   +-------------------------+-------------------------------------------+
|                    |AverageAlgorithm         | decodes.tsdb.algo.AverageAlgorithm        |
|                    +-------------------------+-------------------------------------------+
|                    |ChooseOne                | decodes.tsdb.algo.ChooseOne               |
|                    +-------------------------+-------------------------------------------+
|                    |CopyAlgorithm            | decodes.tsdb.algo.CopyAlgorithm           |
|                    +-------------------------+-------------------------------------------+
|                    |CopyNoOverwrite          | decodes.tsdb.algo.CopyNoOverwrite         |
|                    +-------------------------+-------------------------------------------+
|                    |DisAggregate             | decodes.tsdb.algo.DisAggregate            |
|                    +-------------------------+-------------------------------------------+
|                    |FillForward              | decodes.tsdb.algo.FillForward             |
|                    +-------------------------+-------------------------------------------+
|                    |Resample                 | decodes.tsdb.algo.Resample                |
|                    +-------------------------+-------------------------------------------+
|                    |RunningAverageAlgorithm  | decodes.tsdb.algo.RunningAverageAlgorithm |
|                    +-------------------------+-------------------------------------------+
|                    |ScalarAdder              | decodes.tsdb.algo.ScalerAdder             |
|                    +-------------------------+-------------------------------------------+
|                    |SubSample                | decodes.tsdb.algo.SubSample               |
|                    +-------------------------+-------------------------------------------+
|                    |SumOverTimeAlgorithm     | decodes.tsdb.algo.SumOverTimeAlgorithm    |
|                    +-------------------------+-------------------------------------------+
|                    |ToIrregularUsingPattern  | decodes.cwms.algo.ToIrregularUsingPattern |
+--------------------+-------------------------+-------------------------------------------+
| * Hydrologic       |BridgeClearance          | decodes.tsdb.algo.BridgeClearance         |
|                    +-------------------------+-------------------------------------------+
|                    |EstimatedInflow          | decodes.tsdb.algo.EstimatedInflow         |
|                    +-------------------------+-------------------------------------------+
|                    |IncrementalPrecip        | decodes.tsdb.algo.IncrementalPrecip       |
|                    +-------------------------+-------------------------------------------+
|                    |RdbRating                | decodes.tsdb.algo.RdbRating               |
|                    +-------------------------+-------------------------------------------+
|                    |TabRating                | decodes.tsdb.algo.TabRating               |
|                    +-------------------------+-------------------------------------------+
|                    |UsgsEquation             | decodes.tsdb.algo.UsgsEquation            |
|                    +-------------------------+-------------------------------------------+
|                    |VirtualGage              | decodes.tsdb.algo.VirtualGage             |
+--------------------+-------------------------+-------------------------------------------+
| * Arithmetic or    |CentralRunningAverage    |                                           |
| * Transformation   +-------------------------+-------------------------------------------+
| * (Hidden)         |Division                 |                                           |
|                    +-------------------------+-------------------------------------------+
|                    |GroupAdder               |                                           |
|                    +-------------------------+-------------------------------------------+
|                    |Multiplication           |                                           |
|                    +-------------------------+-------------------------------------------+
|                    |PeriodToDate             |                                           |
|                    +-------------------------+-------------------------------------------+
|                    |Stat                     |                                           |
+--------------------+-------------------------+-------------------------------------------+
| * Hydrologic       |ExpressionParserAlgorithm|                                           |
| * (Hidden)         +-------------------------+-------------------------------------------+
|                    |FlowResIn                |                                           |
|                    +-------------------------+-------------------------------------------+
|                    |WeightedWaterTemperature |                                           |
+--------------------+-------------------------+-------------------------------------------+

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
|ToIrregularUsingPattern  |Interpolates an 'input' to have dates matching Pattern    |
+-------------------------+----------------------------------------------------------+


Recall, that when a computation is set up, the output is a 
separate time series.   Input time series are NOT being manipulated 
or edited.

AddToPrevious
-------------

.. image:: ./media/resources/algorithms/im-01-excel-addtoprevious.JPG
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

.. image:: ./media/resources/algorithms/im-02-comptest-addtoprevious.JPG
   :alt:  algorithm add to previous
   :width: 600

.. image:: ./media/resources/algorithms/im-03-comp-addtoprevious.JPG
   :alt:  algorithm add to previous
   :width: 600

AverageAlgorithm
----------------

.. image:: ./media/resources/algorithms/im-04-excel-averagealgorithm.JPG
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


.. image:: ./media/resources/algorithms/im-05-comptest-averagealgorithm.JPG
   :alt:  algorithm average algorithm
   :width: 600

.. image:: ./media/resources/algorithms/im-06-comp-averagealgorithm.JPG
   :alt:  algorithm average algorithm
   :width: 600


ChooseOne
---------

.. image:: ./media/resources/algorithms/im-07-excel-chooseone.JPG
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

.. image:: ./media/resources/algorithms/im-08-comptest-chooseone.JPG
   :alt:  algorithm choose one
   :width: 600

.. image:: ./media/resources/algorithms/im-09-comp-chooseone.JPG
   :alt:  algorithm choose one
   :width: 600
   
CopyAlgorithm
-------------

.. image:: ./media/resources/algorithms/im-10-excel-copyalgorithm.JPG
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

.. image:: ./media/resources/algorithms/im-11-comptest-copyalgorithm.JPG
   :alt:  algorithm copy algorithm
   :width: 600

.. image:: ./media/resources/algorithms/im-12-comp-copyalgorithm.JPG
   :alt:  algorithm copy algorithm
   :width: 600

CopyNoOverwrite
---------------

.. image:: ./media/resources/algorithms/im-13-excel-copynooverwrite.JPG
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

.. image:: ./media/resources/algorithms/im-14-comptest-copynooverwrite.JPG
   :alt:  algorithm copy no overwrite
   :width: 600

.. image:: ./media/resources/algorithms/im-15-comp-copynooverwrite.JPG
   :alt:  algorithm copy no overwrite
   :width: 600

DisAggregate
------------

.. image:: ./media/resources/algorithms/im-16-excel-disaggregate.JPG
   :alt:  algorithm disaggregate - fill and split
   :width: 500

.. image:: ./media/resources/algorithms/im-17-excel-disaggregate.JPG
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

.. image:: ./media/resources/algorithms/im-18-comptest-disaggregate-fill.JPG
   :alt:  algorithm disaggregate - fill
   :width: 600

.. image:: ./media/resources/algorithms/im-19-comptest-disaggregate-split.JPG
   :alt:  algorithm disaggregate - split
   :width: 600

.. image:: ./media/resources/algorithms/im-20-comptest-disaggregate-fill.JPG
   :alt:  algorithm disaggregate - fill
   :width: 600

.. image:: ./media/resources/algorithms/im-21-comptest-disaggregate-split.JPG
   :alt:  algorithm disaggregate - split
   :width: 600

FillForward
-----------

.. image:: ./media/resources/algorithms/im-23-excel-fillforward.JPG
   :alt:  algorithm fill forward
   :width: 500

.. image:: ./media/resources/algorithms/im-24-comptest-fillforward.JPG
   :alt:  algorithm fill forward
   :width: 600

.. image:: ./media/resources/algorithms/im-25-comp-fillforward.JPG
   :alt:  algorithm fill forward
   :width: 600

Resample
--------

.. image:: ./media/resources/algorithms/im-26-excel-resample.JPG
   :alt:  algorithm resample
   :width: 500

.. image:: ./media/resources/algorithms/im-27-comptest-resample-interp.JPG
   :alt:  algorithm resample - interp
   :width: 600

.. image:: ./media/resources/algorithms/im-28-comptest-resample-fill.JPG
   :alt:  algorithm resample - fill
   :width: 600

.. image:: ./media/resources/algorithms/im-28-comptest-resample-fill.JPG
   :alt:  algorithm resample
   :width: 600

RunningAverageAlgorithm
-----------------------

... more content coming soon ...


ScalarAdder
-----------

... more content coming soon ...

SubSample
---------

... more content coming soon ...

SumOverTimeAlgorithm
--------------------

... more content coming soon ...

ToIrregularUsingPattern
--------------------

The ToIrregularusingPattern Algorithm generates a new time series data set from an existing
irregular or regular interval time series data set. The times for the new time series are
defined by the times of a second time series data set. Values for the new time series are
computed from the original time series data by interpolating between the two bounding input
point using three methods. The data type of the original time series data governs how values
are interpolated.

* Data type "INST-VAL" (or "INST-CUM") considers the value to change linearly over the interval from the previous data value to the current data value.
* Data type "PER-AVER" considers the value to be constant at the current data value over the interval.
* Data type "PER-CUM" considers the value to increase from 0.0 (at the start of the interval) up to the current value over the interval.

Interpolation of the three data types is illustrated below.

.. image:: ./media/resources/algorithms/im-30-comp-ToIrregular.JPG
   :alt:  algorithm ToIrregularUsingPattern
   :width: 500

+-----------+-----------------+
|**Role**   |**Role Name**    |
+===========+=================+
|Inputs     |input            |
+-----------+-----------------+
|Inputs     |pattern          |
+-----------+-----------------+
|Outputs    |output           |
+-----------+-----------------+

... more content coming soon ...



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

... more content coming soon ...

EstimatedInflow
---------------

... more content coming soon ...

IncrementalPrecip
-----------------

... more content coming soon ...

RdbRating
---------

... more content coming soon ...

TabRating
---------

... more content coming soon ...

UsgsEquation
------------

... more content coming soon ...

VirtualGage
-----------

... more content coming soon ...


Standard - Arithmetic - Hidden 
==============================

... more content coming soon ...


Standard - Hydrologic Specific - Hidden
=======================================

... more content coming soon ...


********************************
OpenDCS Custom Python Algorithms
********************************


... more content coming soon ...

****************************
OpenDCS Algorithm GUI Basics
****************************

... more content coming soon ...

Where are algorithms stored?
============================

Basics of properties
====================

... more content coming soon ...