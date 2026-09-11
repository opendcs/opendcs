# DDS API gaps

The generated client deliberately follows the reviewed `dcs_standards` OpenAPI document. It does not reshape responses to match the current LRGS implementation or the earlier DCPMon mocks. Integration work exposed the following gaps.

## Remaining LRGS implementation gaps

- `POST /dds/data/search` is specified but is not implemented by `DdsHttp`.
- Iridium retrieval remains outside the DCPMon integration. DCPMon deliberately
  requests GOES messages.

These are server contract gaps. Consumers should not compensate by maintaining a second response model; the LRGS endpoints and their integration tests should be brought into agreement with the standard.

## Specification and generated-type gaps

- Operations do not define `operationId`. The generator creates names such as `dataSummaryGet`; explicit operation IDs would make the public client API intentional and stable.
- `dataSourceType` combines a known string enum with an unrestricted string in `oneOf`. Those branches overlap, limiting useful validation and generated narrowing.
- `dcpIdentifier.type` is a string, but its `example` is an array. The example should be a single value or use the appropriate multiple-example form.

The message discriminator is now a required top-level `messageType`, so DCPMon
uses generated union narrowing without presentation-side casts. All summary,
count, identifier, schedule, battery, and message fields are consumed directly
from generated models.
