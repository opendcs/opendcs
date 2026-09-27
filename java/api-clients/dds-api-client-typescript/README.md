# DDS over HTTP TypeScript client

This Gradle project generates a private TypeScript `fetch` client from the DDS over HTTP OpenAPI specification. The generated package stays under `build/generated/openApi`; it is not committed or published to npm.

The vendored specification is copied from [`opendcs/dcs_standards` commit `cc16677d`](https://github.com/opendcs/dcs_standards/blob/cc16677d6b49139e75a4658296d70f4f0cc6b11f/source/dds-http.yaml). Keeping the input in this repository makes normal builds reproducible and avoids requiring network access. When the standard changes, replace `src/main/openapi/dds-http.yaml` with the reviewed upstream version in the same pull request as any required client changes, then update this provenance link.

Generate, validate, and compile the client from the repository root:

```shell
./gradlew :dds-api-client-typescript:openApiValidate :dds-api-client-typescript:build
```

The built local package is available at:

```text
java/api-clients/dds-api-client-typescript/build/generated/openApi
```

JavaScript applications in this repository can consume it with a local `file:` dependency and a Gradle project dependency, following the existing `api-client-typescript` pattern.

See [KNOWN_GAPS.md](KNOWN_GAPS.md) for spec-generation and current LRGS implementation gaps exposed while integrating DCPMon.
