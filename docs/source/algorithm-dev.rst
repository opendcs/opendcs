#####################
Algorithm Development
#####################

This Document is part of the OpenDCS Software Suite for environmental
data acquisition and processing. The project home is:
https://github.com/opendcs/opendcs

See INTENT.md at the project home for information on licensing.

.. contents. Table of Contents
   :depth: 3

.. NOTE:: 

    For those in the know, the Algorithm Editor still exists. However the below documentation
    will focus on the generalities of Algorithms and how to setup a simple maven or gradle project
    to get started as we have recently added some new quality-of-life features to algorithm development.

    If you need information about the algorithm editor please see our legacy documentation here: 
    https://github.com/user-attachments/files/16802076/CCP-DevGuide-DTK-5.1.pdf

.. NOTE::
    
    If you *really really* want to keep using the algorithm editor, please open an issue or discussion at
    https://github.com/opendcs/opendcs or on the mailing list. We would like to move away from maintaining it;
    however we do understand that it is convenient for users. If there is sufficient critical mass we will
    perform the appropriate modernizations to keep it alive.

Time Series
===========

A time series is a collection of numeric values over time `(time,value)`. In OpenDCS a "quality flag" is 
also presented `(time,value,quality)`. Each `(time,value,quality)` set is called a sample.

The values may be in a fixed interval, the time between values is constant for each value in the series.
A time series is a regular interval if, and only if, the time between each value is constant.

The values may be irregular, the time between values can vary between each sample.

The values may be predictable but not regular. A daily value in a daylight savings aware timezone is always 
`1 Day` apart, however the hours between two days of the year is different than the others.
CWMS Calls this a `LocalRegular` time series. The intervals are predictable so most regular interval operations can
be used but they are not strictly regular as defined above.


Writing Java Algorithms
=======================

Algorithm Types
---------------

OpenDCS Supports three types of algorithm, TimeSlice, Aggregate, and Running Aggregate.
Which one you set your algorithm to be depends on what you algorithm is trying to do.

Algorithms that operate over each individual sample of the time series