# BLU January 2024 staged data (not yet a test)

Not under `Comps/`, so `AlgorithmTestsIT` does not discover it. To make a test, copy files into a
`Comps/InflowEstimationAlgo/<name>/` folder (`timeseries/inputs`, `timeseries/expectedOutputs`, `rating`)
with a matching `*Comp.xml`.

- Source: NWP database export (CWMS_20 schema, office NWDP), window 2024-01-02 00:00 to 2024-01-06 00:00 UTC, all times UTC.
- Units: m3 (storage), cms (flows), m (elevation and stage). Quality code 0 on every row.
- `inputs/`: BLU storage, BLU release (Flow-Out), BLU forebay elevation (rated storage), BLUO tailwater stage (rated release).
  Flow-Out and BLUO stage are irregular (690 rows); the rest are 15-minute (385 rows).
- `reference/`: NWP's stored BLU Flow-In (3Hours, 6Hours, 1Day) and BLUO.Flow. These do NOT equal the algorithm's output
  from the stored inputs (differences up to ~6 cms), so they are for comparison, not exact expected values.
- `rating/BLUO_Stage_Flow_Linear_USGS-NWIS.xml`: only the version effective 2023-12-01 (in effect for this window), office-id changed to SPK.
  The forebay-to-storage rating is already in `Comps/InflowEstimationAlgo/3Hour/rating/`.
- `BLUO` is not in `shared/test-sites.xml`; add it before using the tailwater stage as an input.
