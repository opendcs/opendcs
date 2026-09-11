# DCP Monitor

Standalone DCPMon UI prototype for OpenDCS issue 2078.

This app is intentionally not connected to the main OpenDCS web UI routes yet.
It keeps the DCPMon screens and tests isolated while matching the current
OpenDCS React UI stack where practical.

## Commands

- `../../gradlew :dcpmon-web-ui:build`
- `npm run dev`
- `npm run test -- --run`
- `npm run build`

## Notes

- Runtime requests use the locally generated `opendcs-dds-api` client and the
  DDS-over-HTTP endpoints under `/dds`.
- The UI discovers status groups from the LRGS `/dds/groups` endpoint and uses
  the canonical network-list name returned by the server. Set
  `VITE_DCPMON_DEFAULT_GROUP` to prefer a configured group; if it is absent or
  unavailable, the first configured group is selected.
- Development requests under `/dds` are proxied to an LRGS at
  `http://localhost:7001` by default.
- Tests intercept the same DDS endpoint contract with MSW; mocks are test data,
  not a separate application API.
- Set `VITE_USE_MOCKS=false` to disable MSW and set `DDS_PROXY_TARGET` to the
  running LRGS HTTP origin when exercising the live API.
- `docker compose up --build` serves a production build at
  `http://localhost:7200` and proxies `/dds` to the Compose LRGS. That stack
  enables deterministic development fixtures; production builds never enable
  MSW. The fixture build prefers its configured `SWT` group, but deployed
  instances are not limited to SWT.
- UI components use `react-bootstrap` and Bootstrap 5 to stay close to
  `javascript/opendcs-web-ui-react`.
- The Gradle build generates and compiles the private TypeScript client before
  installing or building this application. For direct npm commands, first run
  `../../gradlew :dds-api-client-typescript:build` from this directory.

Known server/spec mismatches are tracked beside the generated client in
`java/api-clients/dds-api-client-typescript/KNOWN_GAPS.md`.
