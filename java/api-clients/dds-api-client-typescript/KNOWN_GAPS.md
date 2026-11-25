# DDS API gaps

The generated client deliberately follows the reviewed `dcs_standards` OpenAPI document. It does not reshape responses to match the current LRGS implementation or the earlier DCPMon mocks. Integration work exposed the following gaps.

## Current LRGS implementation does not match the specification

- `GET /dds/data/summary` is specified and required by DCPMon, but is not implemented by `DdsHttp`.
- `POST /dds/data/search` is specified but is not implemented by `DdsHttp`.
- `GET /dds/data/query` and `GET /dds/data/next` currently return a bare array of records with `id`, `dataSource`, `retrievedTime`, and `msg`. The specification defines a `dcpMessages` object containing `total` and `messages`, with GOES or Iridium message fields.
- `GET /dds/sources` currently returns an array of names. The specification defines an array of `dataSource` objects containing at least `name` and `type`.
- Several query parameters are optional in the specification, but `DdsHttp.queryData` assumes at least one DCP address when logging the request.

These are server contract gaps. Consumers should not compensate by maintaining a second response model; the LRGS endpoints and their integration tests should be brought into agreement with the standard.

## Specification and generated-type gaps

- `dcpMessage` uses `dataSource.type` as a discriminator property. OpenAPI discriminators name a direct payload property, not a nested property path. OpenAPI Generator therefore warns and emits code that looks for a literal `dataSource.type` JSON key instead of the nested value returned by the API. A direct required discriminator such as `dataSourceType` would allow safe generated union narrowing.
- Operations do not define `operationId`. The generator creates names such as `dataSummaryGet`; explicit operation IDs would make the public client API intentional and stable.
- The top-level fields in `statusGroupSummary` and `dcpMessages` are optional. If successful responses always contain them, the specification should mark them required so UI code does not need meaningless fallback values.
- `dataSourceType` combines a known string enum with an unrestricted string in `oneOf`. Those branches overlap, limiting useful validation and generated narrowing.
- `dcpIdentifier.type` is a string, but its `example` is an array. The example should be a single value or use the appropriate multiple-example form.

Until the discriminator is corrected upstream, DCPMon restricts its detail query to GOES and uses a narrow presentation-side cast only when rendering GOES-specific columns. All summary, count, identifier, battery, and message-total fields are consumed directly from generated models.
