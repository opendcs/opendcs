# DCP Monitor

Standalone DCPMon UI prototype for OpenDCS issue 2078.

This app is intentionally not connected to the main OpenDCS web UI routes yet.
It keeps the DCPMon screens, mock data, and tests isolated while matching the
current OpenDCS React UI stack where practical.

## Commands

- `npm run dev`
- `npm run test`
- `npm run build`

## Notes

- Mock endpoints are served by MSW at `/dcpmon/api`.
- UI components use `react-bootstrap` and Bootstrap 5 to stay close to
  `javascript/opendcs-web-ui-react`.
- DDS/OpenDCS API client integration is deferred until the DCPMon surface is
  ready to connect to the main UI.
