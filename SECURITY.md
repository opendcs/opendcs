# Security Policy

## Supported Versions

The following versions are supported.

| Version  | Supported          |
| -------  | ------------------ |
| 8.0.x    | :white_check_mark: - except local operations. |
| 7.0.x    | :white_check_mark: |
| 6.8.14   | :white_check_mark: - only the most egregious issues will be addressed. |

8.0 is in development and we are making some drastic changes. Security issues will be addressed promptly. There are no formal 8.0 releases at this time.
All of the deployed (to ghcr.io) container images tagged with `main-*` are the current development that will be 8.0.x.

## Reporting a Vulnerability

To report a vulnerability please go to [issues](https://github.com/opendcs/opendcs/issues), click New Issues, and select Report a Vulnerability.

Provide as much detail as you are willing so we can get started on fixing any issues.


## Regarding Static Analysis tools

The OpenDCS project, and it's sibling projects, use a combination of SonarCloud, Spotbugs, PMD,
Dependabot, and checkstyle to perform static and dynamic analysis of code.

Given a specific issue from other tools we will evaluate it on the merits. We will not sift through a large report that is provided.
