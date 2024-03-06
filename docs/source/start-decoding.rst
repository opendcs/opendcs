################################
OpenDCS DECODING - Introduction
################################

A significant piece of setting up a routing spec is 
writing the DECODING script.  This section is meant to
help new users get familiar with the DECODING format 
statements. 

A major bulk of the examples here are likely not typically
full raw messages from sources.  The sample messages are 
displayed for the purposes of providing examples of how 
the format statements function and decoding information.

Further details and advanced topics on the DECODING can be 
found in the DECODING manual :doc:`DECODES Guide <./legacy-decoding-guide>`

**********************************
DECODING Statements - Fundamentals
**********************************

Starting to write a DECODING script from scratch can seem overwheling
at the start.  To get started, users can use a few different 
strategies - including:

* See if a script already exists for this type of messages
* Break down or edit the message to something shorter and more manageable
* Use the TRACE button to debug scripts


+----------------------+----------------------------------------------------------+
| **Command**          | **Description**                                          |
+======================+==========================================================+
| nX                   | **Skip** n data characters                               |
+----------------------+----------------------------------------------------------+
| nP                   | **Position** to the nth character in the current line    |
+----------------------+----------------------------------------------------------+
| n/                   | **Skip** n data lines                                    |
+----------------------+----------------------------------------------------------+
| n\\                  | **Skip backward** n data lines                           |
+----------------------+----------------------------------------------------------+
| W                    | **Skip white space**                                     |
+----------------------+----------------------------------------------------------+

Table 1-1: DECODES Format Operations - Skipping



+----------------------+----------------------------------------------------------+
| **Command**          | **Description**                                          |
+======================+==========================================================+
| > *label*            | **Jump** to the format with the specified label          |
+----------------------+----------------------------------------------------------+
| *n*\(*operations*...)| **Repeat operations** enclosed in parenthesis n times    |
+----------------------+----------------------------------------------------------+

Table 1-2: DECODES Format Operations - Jump to Label & Repeat

+----------------------+----------------------------------------------------------+
| **Command**          | **Description**                                          |
+======================+==========================================================+
| csv(sens#,...)       | Parse a series of comma separated values                 |
+----------------------+----------------------------------------------------------+

Table 1-3: DECODES Format Operations - CSV Parser


+----------------------+------------------------------------------------------------------------+
| **Command**          | **Description**                                                        |
+======================+========================================================================+
| C(*n*\N,*label*\)    | **Check** if next n characters are N digit, decimal, or sign,          |
|                      | if at least one is not switch to *label*  or proceed to next statement |
+----------------------+------------------------------------------------------------------------+
| C(S,*label*\)        | **Check** if next character is a + or -                                |
|                      | if at least one is not switch to *label* or proceed to next statement  |
+----------------------+------------------------------------------------------------------------+
| C(*str*\,*label*\)   | **Check** if next n-length of string characters match 'str',           |
|                      | if at least one is not switch to *label* or proceed to next statement  |
+----------------------+------------------------------------------------------------------------+

Table 1-4: DECODES Format Operations - Check

+---------------------------+------------------------------------------------------------+
| **Command**               | **Description**                                            |
+===========================+============================================================+
| S(*n*\,N,*label*\)        | **Scan** n characters for N digit, decimal or sign         |
+---------------------------+------------------------------------------------------------+
| S(*n*\,S,*label*\)        | **Scan** n characters for S sign + or -                    |
+---------------------------+------------------------------------------------------------+
| S(*n*\,A,*label*\)        | **Scan** n characters for A letter, upper or lower case    |
+---------------------------+------------------------------------------------------------+
| S(*n*\,P,*label*\)        | **Scan** n characters for P pseudo-binary character or '/' |
+---------------------------+------------------------------------------------------------+
| S(*n*\,Xnn,*label*\)      | **Scan** n characters for X hex value *nn*                 |
+---------------------------+------------------------------------------------------------+
| S(*n*\,'*str*\',*label*\) | **Scan** n characters for exact string 'str'               |
+---------------------------+------------------------------------------------------------+

Table 1-5: DECODES Format Operations - Scan


+----------------------+----------------------------------------------------------+
| **Command**          | **Description**                                          |
+======================+==========================================================+
| nF(FT,DT,L,S,E)      | **Skip** n data characters                               |
+----------------------+----------------------------------------------------------+

Table 1-6: DECODES Format Operations - Fields


Skip Operations - nX, nP, n/, n\\
=================================

All of the following examples are for configurations with one sensor. 
In the examples below, skipping operations are demonstrated to help 
a new user understand how the statements work.  To start, consider that 
statements are commands telling the curser what to do as if starting 
from the top left of the file (message pasted in the browser).  Recall 
that statements are separated by commas.  In the examples below, it is
not necessary to delineate and have multiple labels, but this is done
for the sake of keeping the statements as simple as possible. The
field sesor label is the part that extracts the variable information
once the curser is at the data location in the file, and attributes
the information to a sensor. 

Skip Characters
---------------

::

   2024-02-20 00:48,176.448,0.001,0,0,0,0,p
   2024-02-20 00:54,176.443,0.001,0,0,0,0,p
   2024-02-20 01:00,176.445,0.002,0,0,0,0,p


+----------------------+-----------------------------+
| skip_17char          | 17X,>field_sensor           |
+----------------------+-----------------------------+
| field_sensor         | F(S,A,7D',',1)              |
+----------------------+-----------------------------+


When the message is pasted into the Sample Message browser
and DECODED, ignore the Date/Time since by default that
will populate with the latest hour.  Also, in the example
above the statement is only set to run once.  That is 
why only the first level is displayed. Note that the > or 
jump statement is used, see the next section for more details
on this operation.

Recall that the skip characters will run from where the 
operations is.  So in the following statement, first the 
curser will skip 10 characters from the start of the first
line, then proceed to the next label, which instructs
the operation/curser to skip another 7 characters.  The 
result is the same as the statement above, just divided 
into two statements to convey how the skip characters operate
from the position the operations is at.

+----------------------+-----------------------------+
| skip_10char          | 10X,>skip_07char            |
+----------------------+-----------------------------+
| skip_07char          | 7X,>field_sensor            |
+----------------------+-----------------------------+
| field_sensor         | F(S,A,7D',',1)              |
+----------------------+-----------------------------+

Skip to Position in Line
------------------------

::

   2024-02-20 00:48,176.448,0.001,0,0,0,0,p

When the above lines are pasted into the Sample Message
browser and DECODED,the position operation is used rather 
than the skip characters operation.  The result will
be the same as the top format.  This statement is ideal
for when messages are in a fixed format.

+----------------------+-----------------------------+
| position_18          | 18P,>field_sensor           |
+----------------------+-----------------------------+
| field_sensor         | F(S,A,7D',',1)              |
+----------------------+-----------------------------+

Skip Lines
-------------------

::

   line 1 message abc
   line 1 message xyz
   2024-02-20 01:00,176.445,0.002,0,0,0,0,p

When the above lines are pasted into the Sample Message
browser and DECODED the DECODING script will first skip
the first 2 lines then proceed with the field_sensor
label. 

+----------------------+-----------------------------+
| skip_2               | 2/,>field_sensor            |
+----------------------+-----------------------------+
| field_sensor         | 18P,F(S,A,7D',',1)          |
+----------------------+-----------------------------+

Skip Lines - Backwards
----------------------
::

   line 1 message, abc
   line 1 message, xyz
   2024-02-20 01:00,176.445,0.002,0,0,0,0,p
   line 4 message, mno
   line 5 message, efg
   
When the above lines are pasted into the Sample Message
browser and DECODED the DECODING script will first skip
the first 4 lines, then jump backward 2 lines and then
proceed with the field_sensor label. 

+----------------------+-----------------------------+
| skip_4               | 4/,>skip_back_2             |
+----------------------+-----------------------------+
| skip_back_2          | 2\\,>field_sensor           |
+----------------------+-----------------------------+
| field_sensor         | 18P,F(S,A,7D',',1)          |
+----------------------+-----------------------------+

Skip Whitespace
---------------

::

   2024-02-20  		176.445,0.002,0,0,0,0,p

+----------------------+-----------------------------+
| skip_date10          | 10x,>skip_white             |
+----------------------+-----------------------------+
| skip_white           | W,>field_sensor             |
+----------------------+-----------------------------+
| field_sensor         | F(S,A,7D',',1)              |
+----------------------+-----------------------------+

In the above example, the first line will skip the first ten
characters, then jump to the skip white space command and then
skip the white space. Then the cursor should be right before
the data for sensor one. 

Jump and Repeat Operations - >, n(operations...)
================================================

Jump to Label
-------------

Repeating Statements
--------------------





Field Operation - nF(FT,DT,L,S,E)
=================================

Field operations are used to extract time and sensor values from
the message. The general form is: 

nF(ft,dt,length,sensor # or fld-ID,E) 

* n is a repetition factor
* ft defines the type of field
* dt defines the type of data
* length defines the field length with operational delimiters
* sensor # the sensor number associated with this sensor-value field
* fld-id is used with DATE and TIME fields to specify different representations
* E is used with TIME fields to indicate that the recording of time is viewed as an event

Field - DATE
------------

In the examples below, DECODING field operations are displayed to 
convey how the date can be extracted from the message.  Ignore
the time that is showing up in the Decoded Data box - all examples
are by default showing 00:00.  DECODING Time is addressed next. 

There are four different fld-id options that can be used to 
extract date information versus parsing the date component 
individually (ie year, month day).  The four fld-id's are 
outlined below with examples of how to use them in DECODING
statements.


DATE - Fld-id 1
~~~~~~~~~~~~~~~

Fld-id 1 should be used when the date is in one of the following 
formats:

+-------------+--------------------+------------+
| **fld-id**  | **format**         | **length** |
+=============+====================+============+
| 1           | YY/MM/DD           | 8          |
+-------------+--------------------+------------+
| 1           | YY-MM-DD           | 8          |
+-------------+--------------------+------------+
| 1           | YY MM DD           | 8          |
+-------------+--------------------+------------+
| 1           | YYMMDD             | 6          |
+-------------+--------------------+------------+
| 1           | YYYY/MM/DD         | 10         |
+-------------+--------------------+------------+
| 1           | YYYY-MM-DD         | 10         |
+-------------+--------------------+------------+
| 1           | YYYY MM DD         | 10         |
+-------------+--------------------+------------+

The examples below outline how to extract the date
from the line.  Ignore the time displayed. Decoding 
TIME formats will be addressed further on.  In the 
example below the following parameters are defined.

* D for DATE
* A for ASCII
* 2,3,4,6,7,8 or 10 is for the length of the date format
* 1,2,3 or 4 is for fld-id

Here is a list of potential date field operations:

* F(D,A,8,1)
* F(D,A,6,1)
* F(D,A,10,1)
* F(D,A,8,2)
* F(D,A,7,2)
* F(D,A,6,2)
* F(D,A,5,2)
* F(D,A,3,2)
* F(D,A,2,2)
* F(D,A,5,3)
* F(D,A,4,3)
* F(D,A,8,4)
* F(D,A,6,4)
* F(D,A,10,4)


Examples where the date is 8 characters long.

::

   24/02/20 13:48:06,176.448,0.001,0,0,0,0,p

::

   24-02-20 13:48:06,176.448,0.001,0,0,0,0,p

::

   24 02 20 13:48:06,176.448,0.001,0,0,0,0,p

+-----------------+-----------------------------+
| get_date        | F(D,A,8,1),>get_var         |
+-----------------+-----------------------------+
| get_var         | 19P,F(S,A,7D',',1)          |
+-----------------+-----------------------------+

Example where the date is 6 characters long.

::

   240220 13:48:06,176.448,0.001,0,0,0,0,p

+-----------------+-----------------------------+
| get_date        | F(D,A,6,1),>get_var         |
+-----------------+-----------------------------+
| get_var         | 17P,F(S,A,7D',',1)          |
+-----------------+-----------------------------+

Examples where the date is 10 characters long.

::

   2024/02/20 13:48:06,176.448,0.001,0,0,0,0,p

::

   2024-02-20 13:48:06,176.448,0.001,0,0,0,0,p

::

   2024 02 20 13:48:06,176.448,0.001,0,0,0,0,p

+-----------------+-----------------------------+
| get_date        | F(D,A,10,1),>get_var        |
+-----------------+-----------------------------+
| get_var         | 21P,F(S,A,7D',',1)          |
+-----------------+-----------------------------+


DATE - Fld-id 2
~~~~~~~~~~~~~~~

Fld-id 2 should be used when the date is in one of the following 
formats:

+-------------+--------------------+------------+
| **fld-id**  | **format**         | **length** |
+=============+====================+============+
| 2           | YYYY-DDD           | 8          |
+-------------+--------------------+------------+
| 2           | YYYY/DDD           | 8          |
+-------------+--------------------+------------+
| 2           | YYYYDDD            | 7          |
+-------------+--------------------+------------+
| 2           | YY-DDD             | 6          |
+-------------+--------------------+------------+
| 2           | YY/DDD             | 6          |
+-------------+--------------------+------------+
| 2           | YYDDD              | 5          |
+-------------+--------------------+------------+
| 2           | DDD                | 3          |
+-------------+--------------------+------------+
| 2           | DD                 | 2          |
+-------------+--------------------+------------+

Examples where the date is 8 characters long.

::

   2024-051 13:48:06,176.448,0.001,0,0,0,0,p

::

   2024/051 13:48:06,176.448,0.001,0,0,0,0,p

+-----------------+-----------------------------+
| get_date        | F(D,A,8,2),>get_var         |
+-----------------+-----------------------------+
| get_var         | 19P,F(S,A,7D',',1)          |
+-----------------+-----------------------------+

Example where the date is 7 characters long.

::

   2024051 13:48:06,176.448,0.001,0,0,0,0,p

+-----------------+-----------------------------+
| get_date        | F(D,A,7,2),>get_var         |
+-----------------+-----------------------------+
| get_var         | 18P,F(S,A,7D',',1)          |
+-----------------+-----------------------------+

Examples where the date is 6 characters long.

::

   24-051 13:48:06,176.448,0.001,0,0,0,0,p

::

   24-051 13:48:06,176.448,0.001,0,0,0,0,p

+-----------------+-----------------------------+
| get_date        | F(D,A,6,2),>get_var         |
+-----------------+-----------------------------+
| get_var         | 17P,F(S,A,7D',',1)          |
+-----------------+-----------------------------+


Example where the date is 5 characters long.

::

   24051 13:48:06,176.448,0.001,0,0,0,0,p

+-----------------+-----------------------------+
| get_date        | F(D,A,5,2),>get_var         |
+-----------------+-----------------------------+
| get_var         | 16P,F(S,A,7D',',1)          |
+-----------------+-----------------------------+

Example where the date is 3 characters long.

::

   051 13:48:06,176.448,0.001,0,0,0,0,p

+-----------------+-----------------------------+
| get_date        | F(D,A,3,2),>get_var         |
+-----------------+-----------------------------+
| get_var         | 14P,F(S,A,7D',',1)          |
+-----------------+-----------------------------+

Example where the date is 2 characters long.

::

   51 13:48:06,176.448,0.001,0,0,0,0,p

+-----------------+-----------------------------+
| get_date        | F(D,A,2,2),>get_var         |
+-----------------+-----------------------------+
| get_var         | 13P,F(S,A,7D',',1)          |
+-----------------+-----------------------------+


DATE - Fld-id 3
~~~~~~~~~~~~~~~

Fld-id 3 should be used when the date is in one of the following 
formats:

+-------------+--------------------+------------+
| **fld-id**  | **format**         | **length** |
+=============+====================+============+
| 3           | MM/DD              | 5          |
+-------------+--------------------+------------+
| 3           | MM-DD              | 5          |
+-------------+--------------------+------------+
| 3           | MM DD              | 5          |
+-------------+--------------------+------------+
| 3           | MMDD               | 4          |
+-------------+--------------------+------------+


Examples where the date is 5 characters long.

::

   02/20 13:48:06,176.448,0.001,0,0,0,0,p

::

   02-20 13:48:06,176.448,0.001,0,0,0,0,p

::

   02 20 13:48:06,176.448,0.001,0,0,0,0,p

+-----------------+-----------------------------+
| get_date        | F(D,A,5,3),>get_var         |
+-----------------+-----------------------------+
| get_var         | 16P,F(S,A,7D',',1)          |
+-----------------+-----------------------------+

Example where the date is 4 characters long.

::

   0220 13:48:06,176.448,0.001,0,0,0,0,p

+-----------------+-----------------------------+
| get_date        | F(D,A,4,3),>get_var         |
+-----------------+-----------------------------+
| get_var         | 15P,F(S,A,7D',',1)          |
+-----------------+-----------------------------+


DATE - Fld-id 4
~~~~~~~~~~~~~~~

Fld-id 4 should be used when the date is in one of the following 
formats:

+-------------+--------------------+------------+
| **fld-id**  | **format**         | **length** |
+=============+====================+============+
| 4           | MM/DD/YY           | 8          |
+-------------+--------------------+------------+
| 4           | MM-DD-YY           | 8          |
+-------------+--------------------+------------+
| 4           | MM DD YY           | 8          |
+-------------+--------------------+------------+
| 4           | MMDDYY             | 6          |
+-------------+--------------------+------------+
| 4           | MM/DD/YYYY         | 10         |
+-------------+--------------------+------------+
| 4           | MM-DD-YYYY         | 10         |
+-------------+--------------------+------------+
| 4           | MM DD YYYY         | 10         |
+-------------+--------------------+------------+

Examples where the date is 8 characters long.

::

   02/20/24 13:48:06,176.448,0.001,0,0,0,0,p

::

   02-20-24 13:48:06,176.448,0.001,0,0,0,0,p

::

   02 20 24 13:48:06,176.448,0.001,0,0,0,0,p

+-----------------+-----------------------------+
| get_date        | F(D,A,8,4),>get_var         |
+-----------------+-----------------------------+
| get_var         | 19P,F(S,A,7D',',1)          |
+-----------------+-----------------------------+

Example where the date is 6 characters long.

::

   022024 13:48:06,176.448,0.001,0,0,0,0,p

+-----------------+-----------------------------+
| get_date        | F(D,A,6,4),>get_var         |
+-----------------+-----------------------------+
| get_var         | 17P,F(S,A,7D',',1)          |
+-----------------+-----------------------------+

Examples where the date is 10 characters long.

::

   02/20/2024 13:48:06,176.448,0.001,0,0,0,0,p

::

   02-20-2024 13:48:06,176.448,0.001,0,0,0,0,p

::

   02 20 2024 13:48:06,176.448,0.001,0,0,0,0,p

+-----------------+-----------------------------+
| get_date        | F(D,A,10,4),>get_var        |
+-----------------+-----------------------------+
| get_var         | 21P,F(S,A,7D',',1)          |
+-----------------+-----------------------------+


Field - TIME
------------

In the examples below, DECODING field operations are displayed to 
convey how the time can be extracted from the message.  The following 
example adds a time operations following a date operation.  

There are some banked time formats for when the field type is 'T' and 
when the data type is 'A' (ASCII). These formats are outlined in the 
table below.  Alternatively, a user could decode the time components
individually ( ie hour, min, sec, AM/PM).  There are also two optional
parameters for the field TIME.  The 'sensor #' and 'E' parameter 
signify that the time recorded is an event.  When DECODES encounters
a field description for a time and it has a sensor number and 'E' 
parameter, DECODES will use the value 1 as the data value associated
with that time.


+--------------------+------------+
| **format**         | **length** |
+====================+============+
| HH-MM-SS           | 8          |
+--------------------+------------+
| HH:MM:SS           | 8          |
+--------------------+------------+
| HHMMSS             | 6          |
+--------------------+------------+
| HH-MM              | 5          |
+--------------------+------------+
| HH:MM              | 5          |
+--------------------+------------+
| HHMM               | 4          |
+--------------------+------------+
| HMM                | 3          |
+--------------------+------------+
| MM                 | 2          |
+--------------------+------------+

The examples below outline how to extract the date from the line.
Ignore the time displayed. Decoding TIME formats will be addressed 
further on.  In the example below the following parameters are defined.

* T for TIME
* A for ASCII
* 6,7 or 8 is for the length of the date format
* 1 is for fld-id equal to 1

Here is a list of potential time field operations (not including
optional parameters):

* F(T,A,8)
* F(T,A,6)
* F(T,A,5)
* F(T,A,4)
* F(T,A,3)
* F(T,A,2)

Examples where the date is 8 characters long.

::

   2024-02-20 13-48-06,176.448,0.001,0,0,0,0,p
   
::

   2024-02-20 13:48:06,176.448,0.001,0,0,0,0,p

+-----------------+-----------------------------+
| get_date        | F(D,A,10,1),1X,>get_time    |
+-----------------+-----------------------------+
| get_time        | F(T,A,8),>get_var           |
+-----------------+-----------------------------+
| get_var         | 21P,F(S,A,7D',',1)          |
+-----------------+-----------------------------+

Example where the date is 6 characters long.

::

   2024-02-20 134806,176.448,0.001,0,0,0,0,p

+-----------------+-----------------------------+
| get_date        | F(D,A,10,1),1X,>get_time    |
+-----------------+-----------------------------+
| get_time        | F(T,A,6),>get_var           |
+-----------------+-----------------------------+
| get_var         | 19P,F(S,A,7D',',1)          |
+-----------------+-----------------------------+

Examples where the date is 5 characters long.

::

   2024-02-20 13-48,176.448,0.001,0,0,0,0,p
   
::

   2024-02-20 13:48,176.448,0.001,0,0,0,0,p

+-----------------+-----------------------------+
| get_date        | F(D,A,10,1),1X,>get_time    |
+-----------------+-----------------------------+
| get_time        | F(T,A,5),>get_var           |
+-----------------+-----------------------------+
| get_var         | 18P,F(S,A,7D',',1)          |
+-----------------+-----------------------------+


Example where the date is 4 characters long.

::

   2024-02-20 1348,176.448,0.001,0,0,0,0,p

+-----------------+-----------------------------+
| get_date        | F(D,A,10,1),1X,>get_time    |
+-----------------+-----------------------------+
| get_time        | F(T,A,4),>get_var           |
+-----------------+-----------------------------+
| get_var         | 17P,F(S,A,7D',',1)          |
+-----------------+-----------------------------+

Example where the date is 3 characters long.

::

   2024-02-20 948,176.448,0.001,0,0,0,0,p

+-----------------+-----------------------------+
| get_date        | F(D,A,10,1),1X,>get_time    |
+-----------------+-----------------------------+
| get_time        | F(T,A,3),>get_var           |
+-----------------+-----------------------------+
| get_var         | 16P,F(S,A,7D',',1)          |
+-----------------+-----------------------------+

Example where the date is 2 characters long.

::

   2024-02-20 48,176.448,0.001,0,0,0,0,p

+-----------------+-----------------------------+
| get_date        | F(D,A,10,1),1X,>get_time    |
+-----------------+-----------------------------+
| get_time        | F(T,A,2),>get_var           |
+-----------------+-----------------------------+
| get_var         | 15P,F(S,A,7D',',1)          |
+-----------------+-----------------------------+


Field - ASCII
-------------

Format-lbael 
::

   14.34,


Field - Pseudo-Binary
---------------------


CSV Operations - (sens#,...)
============================

Parse CSV
---------



Check Operation - C(*,*label*\)
===============================



Scan Operations - S(n,*,label)
==============================


Putting Commands Together
=========================

