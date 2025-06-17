InflowCalc Algorithm
--------------------

Exec Class: decodes.cwms.algo.InflowEstimationAlgo

The "InflowEstimationAlgo" algorithm estimates inflow over an aggregate period by combining period-average release,
the period change in storage (as a rate), and any additional outflow series configured for the project.

Conceptually:
  Inflow = Average(Release) + d(Storage)/dt + Sum(Average(Additional Outflows))

Important Notes:

* The output time series must be a CWMS average series with a regular interval and a non-zero duration.
* The algorithm writes only at the end of an aggregate period aligned to the output time series’ interval and offset.
* When using tailwater-to-release or stage-to-storage conversions, you must supply appropriate rating specifications.
* For irregular input series, averaging is time-weighted across the period (see “How data averaging works” below).


Inputs, Outputs, and Properties
===============================

+-----------+----------------------------+--------------------------------------------------------------------------+
| Role      | Name                       | Description                                                              |
+===========+============================+==========================================================================+
| Input     | tailwater_tsid             | Tailwater stage series. If provided, also set ``tailwater_to_release_``  |
|           |                            | ``rating``. Can be regular or irregular. Example:                        |
|           |                            | ``<LOC>.Elev-Tailwater.Inst.0.0.<SOURCE>``                               |
+-----------+----------------------------+--------------------------------------------------------------------------+
| Input     | release_tsid               | Alternative to tailwater input. Direct release flow series.              |
|           |                            | Example: ``<LOC>.Flow-Out.Inst.0.0.<SOURCE>``                            |
+-----------+----------------------------+--------------------------------------------------------------------------+
| Input     | stage_pool_tsid            | Pool elevation series. If provided, also set                             |
|           |                            | ``stage_pool_storage_rating``. Can be regular or irregular.              |
+-----------+----------------------------+--------------------------------------------------------------------------+
| Input     | storage_tsid               | Alternative to stage input. Direct storage series (volume).              |
+-----------+----------------------------+--------------------------------------------------------------------------+
| Input     | outflow_1 … outflow_6      | Additional outflow series to include in the inflow balance. Optional.    |
|           |                            | Example: ``<LOC>.Flow-<Something>.Inst.0.0.<SOURCE>``                    |
+-----------+----------------------------+--------------------------------------------------------------------------+
| Output    | inflow                     | Period inflow rate (e.g., cms).                                          |
+-----------+----------------------------+--------------------------------------------------------------------------+
| Property  | tailwater_to_release_rating| Rating spec for converting tailwater stage to release flow.              |
|           |                            | Example format: ``<PROJ>.Stage;Flow.Linear.<AGENCY>``                    |
+-----------+----------------------------+--------------------------------------------------------------------------+
| Property  | stage_pool_storage_rating  | Rating spec for converting pool elevation to storage.                    |
|           |                            | Example format: ``<PROJ>.Elev;Stor.Linear.<AGENCY>``                     |
+-----------+----------------------------+--------------------------------------------------------------------------+


Behavior and Requirements
=========================

Validation
----------

The algorithm checks:

- Output time series must be CWMS (CwmsTsId), with:
  - ParamType = AVE (average)
  - Regular interval (no irregular outputs)
  - Non-zero duration (e.g., 1Hour, 1Day)
- At least one of these pairs must be provided:
  - tailwater_tsid + tailwater_to_release_rating, or
  - release_tsid
- At least one of these pairs must be provided:
  - stage_pool_tsid + stage_pool_storage_rating, or
  - storage_tsid

Aggregation window and boundaries
---------------------------------

- Aggregation type: Aggregating
- Lower bound open, upper bound closed:
  - The period end time is included in the computation.
- Output is set only when the current time aligns with the output series interval and offset.

Units and ratings
-----------------

- stage_pool_tsid → rated to storage using ``stage_pool_storage_rating`` (dependent units expected as m^3).
- tailwater_tsid → rated to release using ``tailwater_to_release_rating`` (dependent units expected as a flow unit, e.g., cms).
- release_tsid and outflow_1…outflow_6 are treated as flows and period-averaged to the output units.
- Storage-based holdout term d(Storage)/dt is converted to a flow rate using the aggregate period duration.

Insufficient data
-----------------

If there are not enough samples within the period to perform averaging or endpoint interpolation, the algorithm will skip output for that period.


How data averaging works
========================

Irregular series averaging (time-weighted stepped, end-of-period)
-----------------------------------------------------------------

When averaging an irregular input over the period:

1. The time window is [PeriodEnd - Duration, PeriodEnd].
2. The series is treated as stepped “end-of-period”:
   - For each sample time s within the window, its value applies from the previous sample (or window start) up to s.
3. Each segment contributes value × (segment length / total duration).
4. The period average is the sum of segment contributions.

This applies to:
- Rated release from tailwater (if provided and irregular),
- Direct release_tsid (if irregular),
- Each additional outflow series (if irregular).

Storage change over the period (holdout term)
---------------------------------------------

To compute d(Storage)/dt as a flow:

1. Convert or rate the input to storage (m^3) if using stage_pool_tsid; otherwise use storage_tsid directly.
2. Determine storage at the period end and at one duration earlier:
   - If exact samples do not exist at those endpoints, linearly interpolate between the adjacent samples.
3. Compute delta-storage: Stor_end − Stor_prev.
4. Divide by the duration in seconds to obtain a rate (e.g., cms).


Final inflow calculation
========================

For each aggregate period:

- Start with the period-averaged release.
- Add the holdout term d(Storage)/dt.
- For each configured additional outflow series, compute its period average and add to the sum.
- Write the total to the “inflow” output at the period end time.


Configuration tips
==================

- Choose a regular average output series (e.g., 15Minutes+AVE, 1Hour+AVE, 1Day+AVE) and specify a non-zero duration that matches your desired averaging window.
- If your input release is measured as tailwater stage, provide a suitable rating spec via ``tailwater_to_release_rating``.
- If you use pool elevation, provide ``stage_pool_storage_rating`` for Elev→Storage conversion.

Example property values (illustrative)
--------------------------------------

- tailwater_to_release_rating: ``<LOC>.Stage;Flow.Linear.<VERSION>``
- stage_pool_storage_rating: ``<LOC>.Elev;Stor.Linear.<VERSION>``

.. note::
   The algorithm only publishes an output on period boundaries that match the output series’ interval and offset. If inputs do not sufficiently cover the window or ratings are missing, no value will be written for that period.
