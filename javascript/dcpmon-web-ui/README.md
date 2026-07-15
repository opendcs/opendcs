# DCP Monitor

Join the scope/initial discussion here : https://github.com/opendcs/dcpmon/discussions/1

## License
Apache 2.0


## Other Links
- OpenDCS Project: https://github.com/opendcs/opendcs
- OpenDCS Web App: https://github.com/opendcs/rest_api/tree/main/opendcs-web-client

## TODO:
- Using existing code after mocking to talk to actual LRGS instance

## Design Convos

- Design point, if we make "GOES Channel" it's own endpoint and it's expected, then we're commiting again to GOES first instead of "data first". The channel knowledge seems like a detail that should've be provided by the LRGS itself, or if so a more generic /datasource/metadata type end point. 


## Endpoints
- Swagger Docs?
  - Client libs?
- After I have mocked, make my official request for the endpoints here:
  - [DDS-Over-Http](https://github.com/opendcs/dcs_standards/blob/propose/dds-over-http/source/dds-http.yaml)
  - PR from a fork to this branch
  - Could also use forked version to run the generator for mock data


## TODO:

Pull https://github.com/opendcs/dcs_standards/blob/propose/dds-over-http/source/dds-http.yaml
Down and redo mock to match and go from there
  - put a banneron the github pages saying this is a fake demo page