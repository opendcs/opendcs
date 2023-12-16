# Contributing to Our Projects, Version 1.5

**NOTE: This CONTRIBUTING.md is for software contributions. You do not need to follow the Developer's Certificate of Origin (DCO) process for commenting on the OpenDCS repository documentation, such as CONTRIBUTING.md, INTENT.md, etc. or for submitting issues.**

Thanks for thinking about using or contributing to this software ("Project") and its documentation!

* [Policy & Legal Info](#policy)
* [Getting Started](#getting-started)
* [Submitting an Issue](#submitting-an-issue)
* [Submitting Code](#submitting-code)

## Policy

### 1. Introduction

The project maintainer for this Project will only accept contributions using the Developer's Certificate of Origin 1.1 located at [developercertificate.org](https://developercertificate.org) ("DCO"). The DCO is a legally binding statement asserting that you are the creator of your contribution, or that you otherwise have the authority to distribute the contribution, and that you are intentionally making the contribution available under the license associated with the Project ("License").

### 2. Developer Certificate of Origin Process

Before submitting contributing code to this repository for the first time, you'll need to sign a Developer Certificate of Origin (DCO) (see below). To agree to the DCO, add your name and email address to the [CONTRIBUTORS.md](https://github.com/opendcs/rest_api/main/CONTRIBUTORS.md) file. At a high level, adding your information to this file tells us that you have the right to submit the work you're contributing and indicates that you consent to our treating the contribution in a way consistent with the license associated with this software (as described in [LICENSE.md](https://github.com/opendcs/rest_api/blob/main/LICENSE.md)) and its documentation ("Project").

### 3. Important Points

Pseudonymous or anonymous contributions are permissible, but you must be reachable at the email address provided in the Signed-off-by line.

If your contribution is significant, you are also welcome to add your name and copyright date to the source file header.

U.S. Federal law prevents the government from accepting gratuitous services unless certain conditions are met. By submitting a pull request, you acknowledge that your services are offered without expectation of payment and that you expressly waive any future pay claims against the U.S. Federal government related to your contribution.

If you are a U.S. Federal government employee and use a `*.mil` or `*.gov` email address, we interpret your Signed-off-by to mean that the contribution was created in whole or in part by you and that your contribution is not subject to copyright protections.

### 4. DCO Text

The full text of the DCO is included below and is available online at [developercertificate.org](https://developercertificate.org):

```txt
Developer Certificate of Origin
Version 1.1

Copyright (C) 2004, 2006 The Linux Foundation and its contributors.
1 Letterman Drive
Suite D4700
San Francisco, CA, 94129

Everyone is permitted to copy and distribute verbatim copies of this
license document, but changing it is not allowed.

Developer's Certificate of Origin 1.1

By making a contribution to this project, I certify that:

(a) The contribution was created in whole or in part by me and I
    have the right to submit it under the open source license
    indicated in the file; or

(b) The contribution is based upon previous work that, to the best
    of my knowledge, is covered under an appropriate open source
    license and I have the right under that license to submit that
    work with modifications, whether created in whole or in part
    by me, under the same open source license (unless I am
    permitted to submit under a different license), as indicated
    in the file; or

(c) The contribution was provided directly to me by some other
    person who certified (a), (b) or (c) and I have not modified
    it.

(d) I understand and agree that this project and the contribution
    are public and that a record of the contribution (including all
    personal information I submit with it, including my sign-off) is
    maintained indefinitely and may be redistributed consistent with
    this project or the open source license(s) involved.
```

### 5. Project History

The OpenDCS API was developed for the U.S. Army Corps of Engineers.  It provides the means to interact with the OpenDCS database without the OpenDCS Java Client.  It was developed in conjuntion with the OpenDCS Web Client, which is designed to provide the same functionality as the OpenDCS Java Client.

The OpenDCS API was originally developed to target the open_tsdb database, served on PostgreSQL.  It can be run on Tomcat or Embedded Jetty.

The first implementation enabled the functionality of the 'Network List' page in the OpenDCS Java Client Decodes Database Editor.
After the 'Network List' page was proven out, the rest of the functionality of the Decodes Database Editor, as well as the Reference List Editor.

Currently, OpenDCS API is being reviewed and updated to build an official release.

### 6. You are a contractor funded by one of those agencies

Welcome. Please keep reading this document, regardless of the deliverables text in that contract you will be held to our standards and desires.

Management of the project itself is handled by volunteers, regardless of government employee or contractor employee status.
Your contributions will be reviewed and judged by a combination of volunteers (private citizens,gov't employees), and other contractors also paid to work on the software. We do not currently have a formal structure, but that will likely change in the future.

As software is used for getting field data from satellited based delivery platforms, collaboration happens with the Satellite Telemetry Interagency Working Group. Consider contacting NOAA and joining that group if you are not already part of it.

Commit access to the project will be granted to individuals after demonstration of concern for the project. Concern is a combination of participation in discussions, issues, pull-requests, and contributions of code that meets standards and respects the different systems used. Even a small amount.

Those with commit access *MUST* be willing and able to operate as volunteers for contributions from and discussions with others. However, that does not preclude getting paid for specific work. The project may alter the way commit access is granted in the future.

You are welcome to offer support for pay, and if any employees have commit access you can advertise that.

However, some additional points of order:

- Commit access will not be granted solely because a contract exists.
  - it will also not be taken away because a specific contract ends.
- The project is managed by The OpenDCS Consortium not a single vendor. Use the OpenDCS name in adverts/leaflets appropriately.
- Before starting on a large project
  - Review the project, milestones, discussions, and issues sections of each opendcs repository to know what current plans are.
  - Please contact us through GitHub tools or the mailing list *before* starting on a large project.

Commit access will be revoked if a given developer continually push changes that negatively affect other supported components.
Pull-requests will be denied if they don't me the guidelines below are not followed and the developer is unwilling to make corrections.

The following could result in a ban, of an individual:

- Harassing anyone participating in the project.
- Constantly providing contributions that don't meet standards and expecting others to fix it.


## Getting Started
The current java target is java 8. Please make sure you do not add java 9+ features at this time.

### Making Changes

Now you're ready to [clone the repository](https://help.github.com/articles/cloning-a-repository/) and start looking at things. If you are going to submit any changes you make, please read the Submitting changes section below.

### Coding Practices

Code style rules as well as other static analysis rules are setup for the [OpenDCS Organization on SonarCloud](https://sonarcloud.io/organizations/opendcs/quality_profiles/show?name=CWMS_ATO&language=java). 
At this time only the Java rules contain modifications from the base rules that SonarCloud uses by default (known as the Sonar Way).
If you feel that rules should be adjusted, please create a GitHub discussion topic.

## Submitting an Issue

You should feel free to [submit an issue](https://github.com/opendcs/rest_api/issues) on our GitHub repository for anything you find that needs attention on the website. That includes content, functionality, design, or anything else!

You will be prompted to select bug report or feature request and a template will be provided.
Please fill out the template as appropriate.

## Submitting Code

Please [fork](https://help.github.com/en/articles/fork-a-repo) the repository on GitHub and create a [branch in Git](https://git-scm.com/book/en/v2/Git-Branching-Basic-Branching-and-Merging) if you are making changes to existing code.

Once you have made your changes submit a [pull request](https://help.github.com/en/articles/creating-a-pull-request-from-a-fork).

Fill in the PR template as appropriate.

Pull requests should be kept small. If you are doing a larger effort please request merges as you make progress.
Failure to keep requests small will likely result in denial.

A large PR *may* be allowed in the following conditions

1. You coordinate with us about the changes, and we agree they need to be done at once.
2. You start the PR immediately and continuously update it with existing progress, so we can comment or decided when to merge things.
3. You add tests as you go to cover all these changes.

Changes made *must* pass any tests that exist with the built-in Postgres database.

Code Changes *should* have a test provided. Preferably automated, description of a procedure is acceptable.

A trivial change *may* be accepted without changes but comment and approval by other committers. However, tests are preferred.

### Check Your Changes

Before submitting your pull request, you should run the build process locally first to ensure things are working as expected.

If you are contributing to the project, please ensure your code meets SonarCloud's quality standards before submitting a pull request.

We use SonarCloud to perform static code analysis on our project.
SonarCloud is a cloud-based service that automatically analyzes and evaluates code quality on every commit and pull request.
It detects bugs, vulnerabilities, and code smells across a variety of languages and frameworks.

For more information on SonarCloud, visit the [SonarCloud documentation](https://sonarcloud.io/documentation).


### I figured out how to do something where can I document it

1. The documentation, submit a PR with your new example added.
2. The wiki at https://github.com/opendcs/rest_api/wiki
